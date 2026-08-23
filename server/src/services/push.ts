import { and, eq, inArray } from "drizzle-orm";
import type { Database } from "../db/client.js";
import { accounts, conversations, listings, pushTokens } from "../db/schema.js";
import { isPushConfigured, sendPush } from "../lib/push.js";
import { ApiError } from "../lib/http.js";

/**
 * Device tokens and the one notification the app actually sends: a new message.
 *
 * Nothing here throws into a request. A notification that fails to send is a
 * notification the user did not get; it is not a reason for the message they
 * sent to come back as an error.
 */

const PLATFORMS = new Set(["android", "ios"]);
/** FCM tokens are ~160 chars; the cap is a sanity bound, not a format check. */
const MAX_TOKEN_LENGTH = 512;
const PREVIEW_LENGTH = 120;

/**
 * Upsert rather than insert: the same device re-registers on every launch, and
 * a token can move between accounts when two people share a phone — the row is
 * keyed by the token, so re-registering reassigns it instead of delivering
 * someone else's messages to the previous owner.
 */
export async function registerPushToken(
  db: Database,
  accountId: string,
  token: string,
  platform: string,
  options: { now?: Date } = {},
): Promise<{ registered: true }> {
  const now = options.now ?? new Date();
  const trimmed = token.trim();

  if (!trimmed || trimmed.length > MAX_TOKEN_LENGTH) {
    throw ApiError.badRequest("invalid_token", "That device token is not valid.");
  }
  if (!PLATFORMS.has(platform)) {
    throw ApiError.badRequest("invalid_platform", "Platform must be android or ios.");
  }

  await db
    .insert(pushTokens)
    .values({ token: trimmed, accountId, platform, updatedAt: now })
    .onConflictDoUpdate({
      target: pushTokens.token,
      set: { accountId, platform, updatedAt: now },
    });

  return { registered: true };
}

/**
 * Scoped to the caller's own tokens, so signing out cannot silently unregister
 * a device belonging to someone else who guessed a token.
 */
export async function unregisterPushToken(
  db: Database,
  accountId: string,
  token: string,
): Promise<{ removed: number }> {
  const result = await db
    .delete(pushTokens)
    .where(and(eq(pushTokens.token, token.trim()), eq(pushTokens.accountId, accountId)))
    .returning({ token: pushTokens.token });
  return { removed: result.length };
}

/** Every device of one account, for logout-everywhere and for hard deletion. */
export async function clearPushTokensForAccount(
  db: Database,
  accountId: string,
): Promise<{ removed: number }> {
  const result = await db
    .delete(pushTokens)
    .where(eq(pushTokens.accountId, accountId))
    .returning({ token: pushTokens.token });
  return { removed: result.length };
}

export async function listPushTokens(db: Database, accountId: string): Promise<string[]> {
  const rows = await db
    .select({ token: pushTokens.token })
    .from(pushTokens)
    .where(eq(pushTokens.accountId, accountId));
  return rows.map((r) => r.token);
}

export interface MessagePushInput {
  conversationId: string;
  senderId: string;
  messageId: string;
  body: string;
}

/**
 * Notifies the side that did not send.
 *
 * The configuration check comes first and before any query, so on a server with
 * no FCM credentials — local development, CI, the verifier — this costs one
 * boolean and touches neither the database nor the network.
 */
export async function dispatchMessagePush(
  db: Database,
  input: MessagePushInput,
): Promise<{ sent: number; stale: number; skipped: boolean }> {
  if (!isPushConfigured()) return { sent: 0, stale: 0, skipped: true };

  const [thread] = await db
    .select({
      buyerId: conversations.buyerId,
      sellerId: conversations.sellerId,
      listingTitle: listings.title,
    })
    .from(conversations)
    .innerJoin(listings, eq(listings.id, conversations.listingId))
    .where(eq(conversations.id, input.conversationId))
    .limit(1);
  if (!thread) return { sent: 0, stale: 0, skipped: true };

  const recipientId = thread.buyerId === input.senderId ? thread.sellerId : thread.buyerId;

  const [sender] = await db
    .select({ displayName: accounts.displayName })
    .from(accounts)
    .where(eq(accounts.id, input.senderId))
    .limit(1);

  const tokens = await listPushTokens(db, recipientId);
  if (tokens.length === 0) return { sent: 0, stale: 0, skipped: false };

  const title = sender?.displayName || "رسالة جديدة";
  const preview = input.body.slice(0, PREVIEW_LENGTH);

  const results = await Promise.all(
    tokens.map(async (token) => {
      try {
        return await sendPush({
          token,
          title,
          body: preview,
          collapseKey: input.conversationId,
          data: {
            type: "message",
            conversationId: input.conversationId,
            messageId: input.messageId,
            listingTitle: thread.listingTitle,
          },
        });
      } catch (error) {
        return { status: "failed" as const, reason: (error as Error).message };
      }
    }),
  );

  // A dead token stays dead; leaving it costs a wasted request on every future
  // message and, eventually, a table full of tokens for uninstalled apps.
  const stale = tokens.filter((_, i) => results[i]?.status === "stale");
  if (stale.length > 0) {
    await db.delete(pushTokens).where(inArray(pushTokens.token, stale));
  }

  for (const result of results) {
    if (result.status === "failed") console.error("[push] send failed:", result.reason);
  }

  return {
    sent: results.filter((r) => r.status === "sent").length,
    stale: stale.length,
    skipped: false,
  };
}
