import { and, desc, eq, gte, lte, sql, type SQL } from "drizzle-orm";
import type { Database } from "../db/client.js";
import { accounts, blocks, follows, listingPhotos, listings, reports } from "../db/schema.js";
import { ApiError } from "../lib/http.js";

/**
 * Browsing and managing ads.
 *
 * Every function takes `db` and an explicit `now`, so the verifier drives this
 * exact code against an in-process Postgres rather than a stand-in.
 */

export type ListingStatus = "ACTIVE" | "SOLD" | "REMOVED" | "BLOCKED";
export type ListingCondition = "NEW" | "LIKE_NEW" | "USED";

/** One bump per listing per day, so bumping cannot be used to flood the feed. */
export const BUMP_COOLDOWN_HOURS = 24;
export const BROWSE_PAGE_SIZE = 20;
export const BROWSE_MAX_PAGE_SIZE = 50;

export interface BrowseFilters {
  q?: string;
  city?: string;
  category?: string;
  condition?: ListingCondition;
  minPriceHalalas?: number;
  maxPriceHalalas?: number;
  sellerId?: string;
  limit?: number;
  cursor?: string;
  /** When set, ads involving a block in either direction are hidden. */
  viewerId?: string;
}

export interface BrowsePage<T> {
  items: T[];
  nextCursor: string | null;
}

/**
 * Keyset cursor over (bumped_at, id).
 *
 * OFFSET would be simpler but degrades as the table grows and — worse for a feed
 * people scroll — silently skips or repeats rows when an ad is bumped between
 * pages. A keyset is stable under concurrent writes.
 */
function encodeCursor(bumpedAt: Date, id: string): string {
  return Buffer.from(`${bumpedAt.toISOString()}|${id}`).toString("base64url");
}

function decodeCursor(cursor: string): { bumpedAt: Date; id: string } {
  const [iso, id] = Buffer.from(cursor, "base64url").toString("utf8").split("|");
  const bumpedAt = iso ? new Date(iso) : new Date(NaN);
  if (!id || Number.isNaN(bumpedAt.getTime())) {
    throw ApiError.badRequest("invalid_cursor", "That page cursor is not valid.");
  }
  return { bumpedAt, id };
}

/** The seller fields a browse card needs, with the number gated on consent. */
const sellerSummary = {
  sellerId: accounts.id,
  sellerHandle: accounts.handle,
  sellerName: accounts.displayName,
  sellerEmoji: accounts.emoji,
  sellerCity: accounts.city,
  sellerIsVerified: accounts.isVerified,
  sellerRating: accounts.rating,
  sellerAllowCalls: accounts.allowCalls,
  sellerAllowMessages: accounts.allowMessages,
  // Never select public_phone unguarded: the CASE is the only path it takes out
  // of the database, so a forgotten check cannot leak it.
  sellerPhone: sql<string | null>`CASE WHEN ${accounts.allowCalls} THEN ${accounts.publicPhone} END`,
};

const listingColumns = {
  id: listings.id,
  title: listings.title,
  description: listings.description,
  priceHalalas: listings.priceHalalas,
  isNegotiable: listings.isNegotiable,
  category: listings.category,
  condition: listings.condition,
  emoji: listings.emoji,
  city: listings.city,
  status: listings.status,
  viewCount: listings.viewCount,
  postedAt: listings.postedAt,
  bumpedAt: listings.bumpedAt,
};

/**
 * Hides ads where a block exists in either direction: someone you blocked
 * should not reach you, and someone who blocked you should not see your ads
 * either.
 */
function blockFilter(viewerId: string): SQL {
  return sql`NOT EXISTS (
    SELECT 1 FROM ${blocks} b
    WHERE (b.blocker_id = ${viewerId} AND b.blocked_id = ${listings.sellerId})
       OR (b.blocker_id = ${listings.sellerId} AND b.blocked_id = ${viewerId})
  )`;
}

export async function browseListings(db: Database, filters: BrowseFilters = {}) {
  const limit = Math.min(Math.max(filters.limit ?? BROWSE_PAGE_SIZE, 1), BROWSE_MAX_PAGE_SIZE);

  const conditions: SQL[] = [eq(listings.status, "ACTIVE")];

  if (filters.q?.trim()) {
    conditions.push(sql`${listings.search} @@ websearch_to_tsquery('simple', ${filters.q.trim()})`);
  }
  if (filters.city) conditions.push(eq(listings.city, filters.city));
  if (filters.category) conditions.push(eq(listings.category, filters.category));
  if (filters.condition) conditions.push(eq(listings.condition, filters.condition));
  if (filters.sellerId) conditions.push(eq(listings.sellerId, filters.sellerId));
  if (filters.minPriceHalalas !== undefined) {
    conditions.push(gte(listings.priceHalalas, filters.minPriceHalalas));
  }
  if (filters.maxPriceHalalas !== undefined) {
    conditions.push(lte(listings.priceHalalas, filters.maxPriceHalalas));
  }
  if (filters.viewerId) conditions.push(blockFilter(filters.viewerId));
  if (filters.cursor) {
    const { bumpedAt, id } = decodeCursor(filters.cursor);
    conditions.push(sql`(${listings.bumpedAt}, ${listings.id}) < (${bumpedAt}, ${id})`);
  }

  // One extra row tells us whether another page exists without a second query.
  const rows = await db
    .select({ ...listingColumns, ...sellerSummary })
    .from(listings)
    .innerJoin(accounts, eq(accounts.id, listings.sellerId))
    .where(and(...conditions))
    .orderBy(desc(listings.bumpedAt), desc(listings.id))
    .limit(limit + 1);

  const hasMore = rows.length > limit;
  const items = hasMore ? rows.slice(0, limit) : rows;
  const last = items[items.length - 1];

  return {
    items,
    nextCursor: hasMore && last ? encodeCursor(last.bumpedAt, last.id) : null,
  } satisfies BrowsePage<unknown>;
}

export async function getListing(
  db: Database,
  id: string,
  options: { viewerId?: string; countView?: boolean } = {},
) {
  const [row] = await db
    .select({ ...listingColumns, sellerIdRaw: listings.sellerId, ...sellerSummary })
    .from(listings)
    .innerJoin(accounts, eq(accounts.id, listings.sellerId))
    .where(eq(listings.id, id))
    .limit(1);

  if (!row || row.status === "REMOVED") {
    throw ApiError.notFound("That ad is no longer available.");
  }

  if (options.viewerId) {
    const [blocked] = await db
      .select({ one: sql<number>`1` })
      .from(blocks)
      .where(
        sql`(${blocks.blockerId} = ${options.viewerId} AND ${blocks.blockedId} = ${row.sellerIdRaw})
         OR (${blocks.blockerId} = ${row.sellerIdRaw} AND ${blocks.blockedId} = ${options.viewerId})`,
      )
      .limit(1);
    if (blocked) throw ApiError.notFound("That ad is no longer available.");
  }

  const photos = await db
    .select({ id: listingPhotos.id, url: listingPhotos.url, position: listingPhotos.position })
    .from(listingPhotos)
    .where(eq(listingPhotos.listingId, id))
    .orderBy(listingPhotos.position);

  // A seller looking at their own ad should not inflate its view count.
  if (options.countView && options.viewerId !== row.sellerIdRaw) {
    await db
      .update(listings)
      .set({ viewCount: sql`${listings.viewCount} + 1` })
      .where(eq(listings.id, id));
  }

  return { ...row, photos };
}

export interface NewListingInput {
  title: string;
  description?: string;
  priceHalalas: number;
  isNegotiable?: boolean;
  category: string;
  condition?: ListingCondition;
  emoji?: string;
  city: string;
}

export async function createListing(
  db: Database,
  sellerId: string,
  input: NewListingInput,
  options: { now?: Date } = {},
) {
  const now = options.now ?? new Date();
  const [row] = await db
    .insert(listings)
    .values({
      sellerId,
      title: input.title.trim(),
      description: input.description?.trim() ?? "",
      priceHalalas: input.priceHalalas,
      isNegotiable: input.isNegotiable ?? true,
      category: input.category,
      condition: input.condition ?? "USED",
      emoji: input.emoji?.trim() || "📦",
      city: input.city,
      postedAt: now,
      bumpedAt: now,
    })
    .returning();
  if (!row) throw new Error("insert returned no row");
  return row;
}

/** Loads an ad and refuses unless the caller owns it. */
async function requireOwned(db: Database, listingId: string, accountId: string) {
  const [row] = await db.select().from(listings).where(eq(listings.id, listingId)).limit(1);
  if (!row) throw ApiError.notFound("That ad no longer exists.");
  if (row.sellerId !== accountId) {
    throw ApiError.forbidden("This ad belongs to someone else.");
  }
  return row;
}

export async function updateListing(
  db: Database,
  listingId: string,
  accountId: string,
  patch: Partial<NewListingInput>,
) {
  await requireOwned(db, listingId, accountId);

  const changes: Record<string, unknown> = {};
  if (patch.title !== undefined) changes.title = patch.title.trim();
  if (patch.description !== undefined) changes.description = patch.description.trim();
  if (patch.priceHalalas !== undefined) changes.priceHalalas = patch.priceHalalas;
  if (patch.isNegotiable !== undefined) changes.isNegotiable = patch.isNegotiable;
  if (patch.category !== undefined) changes.category = patch.category;
  if (patch.condition !== undefined) changes.condition = patch.condition;
  if (patch.emoji !== undefined) changes.emoji = patch.emoji.trim() || "📦";
  if (patch.city !== undefined) changes.city = patch.city;

  if (Object.keys(changes).length === 0) {
    throw ApiError.badRequest("nothing_to_update", "No fields were provided.");
  }

  const [row] = await db.update(listings).set(changes).where(eq(listings.id, listingId)).returning();
  if (!row) throw new Error("update returned no row");
  return row;
}

export async function setListingSold(db: Database, listingId: string, accountId: string, isSold: boolean) {
  const current = await requireOwned(db, listingId, accountId);
  if (current.status === "REMOVED" || current.status === "BLOCKED") {
    throw ApiError.conflict("listing_unavailable", "That ad can no longer be changed.");
  }
  const [row] = await db
    .update(listings)
    .set({ status: isSold ? "SOLD" : "ACTIVE" })
    .where(eq(listings.id, listingId))
    .returning();
  if (!row) throw new Error("update returned no row");
  return row;
}

/**
 * Removal is a status change, not a delete: conversations already reference the
 * ad, and a buyer's inbox should not lose its subject line.
 */
export async function removeListing(db: Database, listingId: string, accountId: string) {
  await requireOwned(db, listingId, accountId);
  await db.update(listings).set({ status: "REMOVED" }).where(eq(listings.id, listingId));
}

export async function bumpListing(
  db: Database,
  listingId: string,
  accountId: string,
  options: { now?: Date } = {},
) {
  const now = options.now ?? new Date();
  const current = await requireOwned(db, listingId, accountId);

  const nextAllowed = new Date(current.bumpedAt.getTime() + BUMP_COOLDOWN_HOURS * 3_600_000);
  if (now < nextAllowed) {
    throw ApiError.tooManyRequests(
      `You can bump this ad again in ${Math.ceil((nextAllowed.getTime() - now.getTime()) / 3_600_000)} hours.`,
    );
  }

  const [row] = await db
    .update(listings)
    .set({ bumpedAt: now })
    .where(eq(listings.id, listingId))
    .returning();
  if (!row) throw new Error("update returned no row");
  return row;
}

/* ============================================================
   Follows
   ============================================================ */

/**
 * follower_count is denormalised because every browse card reads it and a
 * COUNT(*) per card does not scale. It is kept honest by only ever moving it
 * when the insert or delete actually changed a row.
 */
export async function followSeller(db: Database, followerId: string, sellerId: string) {
  if (followerId === sellerId) {
    throw ApiError.badRequest("cannot_follow_self", "You cannot follow yourself.");
  }
  const inserted = await db
    .insert(follows)
    .values({ followerId, sellerId })
    .onConflictDoNothing()
    .returning();

  if (inserted.length > 0) {
    await db
      .update(accounts)
      .set({ followerCount: sql`${accounts.followerCount} + 1` })
      .where(eq(accounts.id, sellerId));
  }
  return { following: true, changed: inserted.length > 0 };
}

export async function unfollowSeller(db: Database, followerId: string, sellerId: string) {
  const removed = await db
    .delete(follows)
    .where(and(eq(follows.followerId, followerId), eq(follows.sellerId, sellerId)))
    .returning();

  if (removed.length > 0) {
    await db
      .update(accounts)
      .set({ followerCount: sql`GREATEST(${accounts.followerCount} - 1, 0)` })
      .where(eq(accounts.id, sellerId));
  }
  return { following: false, changed: removed.length > 0 };
}

/* ============================================================
   Safety
   ============================================================ */

export type ReportTarget = "LISTING" | "ACCOUNT" | "MESSAGE" | "LIVE_STREAM";

export async function reportSomething(
  db: Database,
  reporterId: string,
  targetType: ReportTarget,
  targetId: string,
  reason: string,
  note?: string,
) {
  const inserted = await db
    .insert(reports)
    .values({ reporterId, targetType, targetId, reason, note: note ?? null })
    .onConflictDoNothing()
    .returning();

  // Reporting twice is not an error for the person doing it — the thing is
  // already flagged, which is what they wanted.
  return { reported: true, alreadyReported: inserted.length === 0 };
}

export async function blockAccount(db: Database, blockerId: string, blockedId: string) {
  if (blockerId === blockedId) {
    throw ApiError.badRequest("cannot_block_self", "You cannot block yourself.");
  }
  await db.insert(blocks).values({ blockerId, blockedId }).onConflictDoNothing();
  return { blocked: true };
}

export async function unblockAccount(db: Database, blockerId: string, blockedId: string) {
  await db
    .delete(blocks)
    .where(and(eq(blocks.blockerId, blockerId), eq(blocks.blockedId, blockedId)));
  return { blocked: false };
}

export async function isBlockedEitherWay(db: Database, a: string, b: string): Promise<boolean> {
  const [row] = await db
    .select({ one: sql<number>`1` })
    .from(blocks)
    .where(
      sql`(${blocks.blockerId} = ${a} AND ${blocks.blockedId} = ${b})
       OR (${blocks.blockerId} = ${b} AND ${blocks.blockedId} = ${a})`,
    )
    .limit(1);
  return Boolean(row);
}
