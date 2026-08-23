import { and, desc, eq, or, sql, type SQL } from "drizzle-orm";
import type { Database } from "../db/client.js";
import { accounts, conversations, listings, messages } from "../db/schema.js";
import { publish } from "../lib/events.js";
import { ApiError } from "../lib/http.js";
import { isBlockedEitherWay } from "./listings.js";
import { dispatchMessagePush } from "./push.js";

/**
 * Threads between a buyer and a seller about one ad.
 *
 * The pair (listing_id, buyer_id) is unique in the schema, so "open a
 * conversation" is idempotent by construction rather than by a race-prone
 * check-then-insert.
 */

export const INBOX_PAGE_SIZE = 20;
export const MESSAGE_PAGE_SIZE = 30;
const PREVIEW_LENGTH = 120;

function encodeCursor(at: Date, id: string): string {
  return Buffer.from(`${at.toISOString()}|${id}`).toString("base64url");
}

function decodeCursor(cursor: string): { at: Date; id: string } {
  const [iso, id] = Buffer.from(cursor, "base64url").toString("utf8").split("|");
  const at = iso ? new Date(iso) : new Date(NaN);
  if (!id || Number.isNaN(at.getTime())) {
    throw ApiError.badRequest("invalid_cursor", "That page cursor is not valid.");
  }
  return { at, id };
}

/** Loads a thread and refuses anyone who is not one of its two participants. */
async function requireParticipant(db: Database, conversationId: string, accountId: string) {
  const [row] = await db
    .select()
    .from(conversations)
    .where(eq(conversations.id, conversationId))
    .limit(1);

  if (!row) throw ApiError.notFound("That conversation no longer exists.");
  if (row.buyerId !== accountId && row.sellerId !== accountId) {
    // Deliberately the same message as a missing thread, so probing ids cannot
    // be used to discover which conversations exist.
    throw ApiError.forbidden("That conversation no longer exists.");
  }
  return row;
}

export async function openConversation(
  db: Database,
  buyerId: string,
  listingId: string,
  options: { now?: Date } = {},
): Promise<{ id: string; created: boolean }> {
  const now = options.now ?? new Date();

  const [listing] = await db.select().from(listings).where(eq(listings.id, listingId)).limit(1);
  if (!listing || listing.status === "REMOVED" || listing.status === "BLOCKED") {
    throw ApiError.notFound("That ad is no longer available.");
  }
  if (listing.sellerId === buyerId) {
    throw ApiError.badRequest("cannot_message_self", "This is your own ad.");
  }
  if (await isBlockedEitherWay(db, buyerId, listing.sellerId)) {
    throw ApiError.forbidden("You cannot message this seller.");
  }

  const [seller] = await db.select().from(accounts).where(eq(accounts.id, listing.sellerId)).limit(1);
  if (!seller || seller.isBanned || seller.deletedAt) {
    throw ApiError.notFound("That seller is no longer active.");
  }
  if (!seller.allowMessages) {
    throw ApiError.forbidden("This seller has turned off messages.");
  }

  // The unique index does the work; a concurrent duplicate is a no-op rather
  // than an error.
  const inserted = await db
    .insert(conversations)
    .values({
      listingId,
      buyerId,
      sellerId: listing.sellerId,
      createdAt: now,
      lastMessageAt: now,
    })
    .onConflictDoNothing()
    .returning();

  if (inserted[0]) return { id: inserted[0].id, created: true };

  const [existing] = await db
    .select({ id: conversations.id })
    .from(conversations)
    .where(and(eq(conversations.listingId, listingId), eq(conversations.buyerId, buyerId)))
    .limit(1);

  if (!existing) throw new Error("conversation vanished between insert and select");
  return { id: existing.id, created: false };
}

/**
 * The inbox, from whichever side the caller is on. A seller and a buyer see the
 * same thread with their own unread count and each other as "the other party".
 */
export async function listConversations(
  db: Database,
  accountId: string,
  options: { limit?: number; cursor?: string } = {},
) {
  const limit = Math.min(Math.max(options.limit ?? INBOX_PAGE_SIZE, 1), 50);

  const conditions: SQL[] = [
    or(eq(conversations.buyerId, accountId), eq(conversations.sellerId, accountId))!,
  ];
  if (options.cursor) {
    const { at, id } = decodeCursor(options.cursor);
    conditions.push(sql`(${conversations.lastMessageAt}, ${conversations.id}) < (${at}, ${id})`);
  }

  const other = sql`CASE WHEN ${conversations.buyerId} = ${accountId}
                         THEN ${conversations.sellerId} ELSE ${conversations.buyerId} END`;

  const rows = await db
    .select({
      id: conversations.id,
      listingId: conversations.listingId,
      listingTitle: listings.title,
      listingEmoji: listings.emoji,
      listingPriceHalalas: listings.priceHalalas,
      listingStatus: listings.status,
      lastMessageAt: conversations.lastMessageAt,
      lastMessagePreview: conversations.lastMessagePreview,
      unread: sql<number>`CASE WHEN ${conversations.buyerId} = ${accountId}
                               THEN ${conversations.buyerUnread} ELSE ${conversations.sellerUnread} END`,
      iAmSeller: sql<boolean>`${conversations.sellerId} = ${accountId}`,
      otherId: accounts.id,
      otherHandle: accounts.handle,
      otherName: accounts.displayName,
      otherEmoji: accounts.emoji,
      otherCity: accounts.city,
      otherIsVerified: accounts.isVerified,
    })
    .from(conversations)
    .innerJoin(listings, eq(listings.id, conversations.listingId))
    .innerJoin(accounts, sql`${accounts.id} = ${other}`)
    .where(and(...conditions))
    .orderBy(desc(conversations.lastMessageAt), desc(conversations.id))
    .limit(limit + 1);

  const hasMore = rows.length > limit;
  const items = hasMore ? rows.slice(0, limit) : rows;
  const last = items[items.length - 1];

  return {
    items,
    nextCursor: hasMore && last ? encodeCursor(last.lastMessageAt, last.id) : null,
  };
}

export async function listMessages(
  db: Database,
  conversationId: string,
  accountId: string,
  options: { limit?: number; cursor?: string } = {},
) {
  await requireParticipant(db, conversationId, accountId);
  const limit = Math.min(Math.max(options.limit ?? MESSAGE_PAGE_SIZE, 1), 100);

  const conditions: SQL[] = [eq(messages.conversationId, conversationId)];
  if (options.cursor) {
    const { at, id } = decodeCursor(options.cursor);
    conditions.push(sql`(${messages.sentAt}, ${messages.id}) < (${at}, ${id})`);
  }

  const rows = await db
    .select({
      id: messages.id,
      senderId: messages.senderId,
      body: messages.body,
      sentAt: messages.sentAt,
      readAt: messages.readAt,
    })
    .from(messages)
    .where(and(...conditions))
    .orderBy(desc(messages.sentAt), desc(messages.id))
    .limit(limit + 1);

  const hasMore = rows.length > limit;
  const items = hasMore ? rows.slice(0, limit) : rows;
  const last = items[items.length - 1];

  return {
    items,
    nextCursor: hasMore && last ? encodeCursor(last.sentAt, last.id) : null,
  };
}

export async function sendMessage(
  db: Database,
  conversationId: string,
  senderId: string,
  body: string,
  options: { now?: Date } = {},
) {
  const now = options.now ?? new Date();
  const thread = await requireParticipant(db, conversationId, senderId);

  const counterpart = thread.buyerId === senderId ? thread.sellerId : thread.buyerId;
  if (await isBlockedEitherWay(db, senderId, counterpart)) {
    throw ApiError.forbidden("You cannot message this person.");
  }

  const [listing] = await db
    .select({ status: listings.status })
    .from(listings)
    .where(eq(listings.id, thread.listingId))
    .limit(1);
  if (!listing || listing.status === "REMOVED" || listing.status === "BLOCKED") {
    throw ApiError.conflict("listing_unavailable", "That ad has been taken down.");
  }

  const text = body.trim();
  if (!text) throw ApiError.badRequest("empty_message", "Write something first.");

  const [saved] = await db
    .insert(messages)
    .values({ conversationId, senderId, body: text, sentAt: now })
    .returning();
  if (!saved) throw new Error("insert returned no row");

  // One statement so the counters cannot drift from the message that caused
  // them: whichever side did not send is the side that gains an unread.
  await db
    .update(conversations)
    .set({
      lastMessageAt: now,
      lastMessagePreview: text.slice(0, PREVIEW_LENGTH),
      buyerUnread: sql`${conversations.buyerUnread} + CASE WHEN ${conversations.sellerId} = ${senderId} THEN 1 ELSE 0 END`,
      sellerUnread: sql`${conversations.sellerUnread} + CASE WHEN ${conversations.buyerId} = ${senderId} THEN 1 ELSE 0 END`,
    })
    .where(eq(conversations.id, conversationId));

  publish({
    type: "message",
    conversationId,
    messageId: saved.id,
    senderId,
    body: saved.body,
    sentAt: saved.sentAt.toISOString(),
  });

  // Not awaited: FCM is a third party over the network, and the sender should
  // not wait on it — the message is already committed and already delivered to
  // anyone with the thread open. dispatchMessagePush returns immediately when
  // push is unconfigured, so this costs nothing in development or in the
  // verifier. The catch is mandatory; an unhandled rejection here would take
  // the process down.
  void dispatchMessagePush(db, {
    conversationId,
    senderId,
    messageId: saved.id,
    body: saved.body,
  }).catch((error) => console.error("[push] dispatch failed", error));

  return saved;
}

/** Clears only the caller's counter — reading does not mark the other side read. */
export async function markConversationRead(
  db: Database,
  conversationId: string,
  accountId: string,
  options: { now?: Date } = {},
) {
  const now = options.now ?? new Date();
  const thread = await requireParticipant(db, conversationId, accountId);
  const isBuyer = thread.buyerId === accountId;

  await db
    .update(conversations)
    .set(isBuyer ? { buyerUnread: 0 } : { sellerUnread: 0 })
    .where(eq(conversations.id, conversationId));

  await db
    .update(messages)
    .set({ readAt: now })
    .where(
      and(
        eq(messages.conversationId, conversationId),
        sql`${messages.senderId} <> ${accountId}`,
        sql`${messages.readAt} IS NULL`,
      ),
    );

  return { unread: 0 };
}

export async function totalUnread(db: Database, accountId: string): Promise<number> {
  const [row] = await db
    .select({
      total: sql<number>`COALESCE(SUM(CASE WHEN ${conversations.buyerId} = ${accountId}
                                           THEN ${conversations.buyerUnread}
                                           ELSE ${conversations.sellerUnread} END), 0)::int`,
    })
    .from(conversations)
    .where(or(eq(conversations.buyerId, accountId), eq(conversations.sellerId, accountId)));

  return row?.total ?? 0;
}

export async function assertParticipant(db: Database, conversationId: string, accountId: string) {
  return requireParticipant(db, conversationId, accountId);
}
