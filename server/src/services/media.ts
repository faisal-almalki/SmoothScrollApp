import { and, asc, desc, eq, inArray, sql, type SQL } from "drizzle-orm";
import type { Database } from "../db/client.js";
import { accounts, blocks, listingPhotos, listings, videoListings, videos } from "../db/schema.js";
import { ApiError } from "../lib/http.js";
import {
  MAX_PHOTO_BYTES,
  MAX_VIDEO_BYTES,
  PHOTO_CONTENT_TYPES,
  VIDEO_CONTENT_TYPES,
  buildObjectKey,
  createUploadUrl,
  publicUrlFor,
} from "../lib/storage.js";

/**
 * Photos on ads, videos in the feed, and the join between them.
 *
 * A classifieds app whose ads have no photographs does not convince anyone, so
 * this is the piece that makes the product real.
 */

export const MAX_PHOTOS_PER_LISTING = 10;
export const FEED_PAGE_SIZE = 10;

async function requireOwnedListing(db: Database, listingId: string, accountId: string) {
  const [row] = await db.select().from(listings).where(eq(listings.id, listingId)).limit(1);
  if (!row) throw ApiError.notFound("That ad no longer exists.");
  if (row.sellerId !== accountId) throw ApiError.forbidden("This ad belongs to someone else.");
  return row;
}

async function requireOwnedVideo(db: Database, videoId: string, accountId: string) {
  const [row] = await db.select().from(videos).where(eq(videos.id, videoId)).limit(1);
  if (!row) throw ApiError.notFound("That video no longer exists.");
  if (row.sellerId !== accountId) throw ApiError.forbidden("This video belongs to someone else.");
  return row;
}

/* ============================================================
   Photos
   ============================================================ */

export async function createPhotoUploadUrl(
  db: Database,
  listingId: string,
  accountId: string,
  input: { contentType: string; sizeBytes: number; filename?: string },
  options: { now?: Date } = {},
) {
  await requireOwnedListing(db, listingId, accountId);

  if (!(PHOTO_CONTENT_TYPES as readonly string[]).includes(input.contentType)) {
    throw ApiError.badRequest(
      "unsupported_type",
      `Photos must be one of ${PHOTO_CONTENT_TYPES.join(", ")}.`,
    );
  }
  if (input.sizeBytes <= 0 || input.sizeBytes > MAX_PHOTO_BYTES) {
    throw ApiError.badRequest(
      "file_too_large",
      `Photos must be under ${Math.floor(MAX_PHOTO_BYTES / (1024 * 1024))} MB.`,
    );
  }

  const counted = await db
    .select({ count: sql<number>`count(*)::int` })
    .from(listingPhotos)
    .where(eq(listingPhotos.listingId, listingId));
  if ((counted[0]?.count ?? 0) >= MAX_PHOTOS_PER_LISTING) {
    throw ApiError.conflict(
      "photo_limit",
      `An ad can have at most ${MAX_PHOTOS_PER_LISTING} photos.`,
    );
  }

  const key = buildObjectKey("listing-photos", accountId, input.filename ?? "photo.jpg");
  return createUploadUrl(key, { now: options.now });
}

export async function attachPhoto(
  db: Database,
  listingId: string,
  accountId: string,
  input: { key: string; width?: number; height?: number },
  options: { now?: Date } = {},
) {
  await requireOwnedListing(db, listingId, accountId);

  const existing = await db
    .select({ position: listingPhotos.position })
    .from(listingPhotos)
    .where(eq(listingPhotos.listingId, listingId))
    .orderBy(desc(listingPhotos.position))
    .limit(1);

  const attached = await db
    .select({ count: sql<number>`count(*)::int` })
    .from(listingPhotos)
    .where(eq(listingPhotos.listingId, listingId));
  if ((attached[0]?.count ?? 0) >= MAX_PHOTOS_PER_LISTING) {
    throw ApiError.conflict(
      "photo_limit",
      `An ad can have at most ${MAX_PHOTOS_PER_LISTING} photos.`,
    );
  }

  const nextPosition = (existing[0]?.position ?? -1) + 1;

  const [row] = await db
    .insert(listingPhotos)
    .values({
      listingId,
      url: publicUrlFor(input.key),
      width: input.width ?? null,
      height: input.height ?? null,
      position: nextPosition,
      createdAt: options.now ?? new Date(),
    })
    .returning();
  if (!row) throw new Error("insert returned no row");
  return row;
}

export async function listPhotos(db: Database, listingId: string) {
  return db
    .select({
      id: listingPhotos.id,
      url: listingPhotos.url,
      width: listingPhotos.width,
      height: listingPhotos.height,
      position: listingPhotos.position,
    })
    .from(listingPhotos)
    .where(eq(listingPhotos.listingId, listingId))
    .orderBy(asc(listingPhotos.position));
}

/**
 * Reorders photos.
 *
 * This cannot be one UPDATE: the unique index on (listing_id, position) is
 * checked row by row, so any statement that swaps two positions trips it
 * mid-flight — verified directly against Postgres rather than assumed. So the
 * rows are first parked in negative positions, which no valid row ever uses,
 * and then written to their final values.
 *
 * The two statements are not wrapped in a transaction because the Neon HTTP
 * driver cannot hold one open. The window is tiny and only the owner can reach
 * it, and a crash between the two leaves every position negative — which the
 * repair pass at the top of this function then fixes on the next call.
 */
export async function reorderPhotos(
  db: Database,
  listingId: string,
  accountId: string,
  orderedPhotoIds: string[],
) {
  await requireOwnedListing(db, listingId, accountId);

  await repairNegativePositions(db, listingId);

  const current = await db
    .select({ id: listingPhotos.id })
    .from(listingPhotos)
    .where(eq(listingPhotos.listingId, listingId));

  const currentIds = new Set(current.map((r: { id: string }) => r.id));
  if (orderedPhotoIds.length !== currentIds.size ||
      !orderedPhotoIds.every((id) => currentIds.has(id))) {
    throw ApiError.badRequest(
      "incomplete_order",
      "Send every photo id for this ad, exactly once.",
    );
  }

  // Park everything below zero so the final writes cannot collide.
  await db
    .update(listingPhotos)
    .set({ position: sql`-1 - ${listingPhotos.position}` })
    .where(eq(listingPhotos.listingId, listingId));

  for (const [index, photoId] of orderedPhotoIds.entries()) {
    await db
      .update(listingPhotos)
      .set({ position: index })
      .where(and(eq(listingPhotos.id, photoId), eq(listingPhotos.listingId, listingId)));
  }

  return listPhotos(db, listingId);
}

/** Undoes a half-finished reorder by mapping negatives back to their originals. */
async function repairNegativePositions(db: Database, listingId: string) {
  const stranded = await db
    .select({ count: sql<number>`count(*)::int` })
    .from(listingPhotos)
    .where(and(eq(listingPhotos.listingId, listingId), sql`${listingPhotos.position} < 0`));

  if ((stranded[0]?.count ?? 0) === 0) return;

  await db
    .update(listingPhotos)
    .set({ position: sql`-1 - ${listingPhotos.position}` })
    .where(and(eq(listingPhotos.listingId, listingId), sql`${listingPhotos.position} < 0`));
}

export async function deletePhoto(
  db: Database,
  listingId: string,
  accountId: string,
  photoId: string,
) {
  await requireOwnedListing(db, listingId, accountId);

  const removed = await db
    .delete(listingPhotos)
    .where(and(eq(listingPhotos.id, photoId), eq(listingPhotos.listingId, listingId)))
    .returning();

  if (removed.length === 0) throw ApiError.notFound("That photo is not on this ad.");

  // Close the gap so positions stay 0..n-1 and the next upload lands correctly.
  const remaining = await db
    .select({ id: listingPhotos.id })
    .from(listingPhotos)
    .where(eq(listingPhotos.listingId, listingId))
    .orderBy(asc(listingPhotos.position));

  await db
    .update(listingPhotos)
    .set({ position: sql`-1 - ${listingPhotos.position}` })
    .where(eq(listingPhotos.listingId, listingId));

  for (const [index, row] of remaining.entries()) {
    await db.update(listingPhotos).set({ position: index }).where(eq(listingPhotos.id, row.id));
  }

  return listPhotos(db, listingId);
}

/* ============================================================
   Videos
   ============================================================ */

export async function createVideoUploadUrl(
  db: Database,
  accountId: string,
  input: { contentType: string; sizeBytes: number; filename?: string; caption?: string },
  options: { now?: Date } = {},
) {
  if (!(VIDEO_CONTENT_TYPES as readonly string[]).includes(input.contentType)) {
    throw ApiError.badRequest(
      "unsupported_type",
      `Videos must be one of ${VIDEO_CONTENT_TYPES.join(", ")}.`,
    );
  }
  if (input.sizeBytes <= 0 || input.sizeBytes > MAX_VIDEO_BYTES) {
    throw ApiError.badRequest(
      "file_too_large",
      `Videos must be under ${Math.floor(MAX_VIDEO_BYTES / (1024 * 1024))} MB.`,
    );
  }

  const now = options.now ?? new Date();
  const key = buildObjectKey("videos", accountId, input.filename ?? "video.mp4");
  const upload = createUploadUrl(key, { now });

  const [row] = await db
    .insert(videos)
    .values({
      sellerId: accountId,
      caption: input.caption?.trim() ?? "",
      status: "UPLOADING",
      createdAt: now,
    })
    .returning();
  if (!row) throw new Error("insert returned no row");

  return { video: row, upload };
}

/**
 * Called once the phone finishes its PUT. The transcoder seam lives here: a
 * real deployment hands the key to Mux, Cloudflare Stream or an ffmpeg worker
 * and waits for the callback below.
 */
export async function confirmVideoUpload(
  db: Database,
  videoId: string,
  accountId: string,
  key: string,
  options: { now?: Date; onVideoUploaded?: (videoId: string, key: string) => Promise<void> } = {},
) {
  const video = await requireOwnedVideo(db, videoId, accountId);
  if (video.status !== "UPLOADING") {
    throw ApiError.conflict("already_confirmed", "That upload was already confirmed.");
  }

  const [row] = await db
    .update(videos)
    .set({ status: "PROCESSING", playbackUrl: publicUrlFor(key) })
    .where(eq(videos.id, videoId))
    .returning();
  if (!row) throw new Error("update returned no row");

  // TRANSCODER SEAM — no-op until a provider is wired up. Without one the video
  // stays PROCESSING, which is honest: it is not playable yet.
  await options.onVideoUploaded?.(videoId, key);

  return row;
}

/** The transcoder's callback. Flips the video to READY and makes it feedable. */
export async function markVideoReady(
  db: Database,
  videoId: string,
  input: { playbackUrl: string; thumbnailUrl?: string; durationMs?: number; width?: number; height?: number },
) {
  const [row] = await db
    .update(videos)
    .set({
      status: "READY",
      playbackUrl: input.playbackUrl,
      thumbnailUrl: input.thumbnailUrl ?? null,
      durationMs: input.durationMs ?? null,
      width: input.width ?? null,
      height: input.height ?? null,
    })
    .where(eq(videos.id, videoId))
    .returning();

  if (!row) throw ApiError.notFound("That video no longer exists.");
  return row;
}

/** Attaches ads to a video. Position 0 becomes the pill over the player. */
export async function setVideoListings(
  db: Database,
  videoId: string,
  accountId: string,
  listingIds: string[],
) {
  await requireOwnedVideo(db, videoId, accountId);

  if (listingIds.length > 0) {
    const owned = await db
      .select({ id: listings.id })
      .from(listings)
      .where(and(inArray(listings.id, listingIds), eq(listings.sellerId, accountId)));

    if (owned.length !== listingIds.length) {
      throw ApiError.badRequest(
        "not_your_listing",
        "You can only tag your own ads on a video.",
      );
    }
  }

  await db.delete(videoListings).where(eq(videoListings.videoId, videoId));
  for (const [index, listingId] of listingIds.entries()) {
    await db.insert(videoListings).values({ videoId, listingId, position: index });
  }

  return listingIds.length;
}

/* ============================================================
   Feed
   ============================================================ */

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

/**
 * The shoppable feed: ready videos newest first, each with its ordered ads.
 * Blocks are honoured exactly as in browse, so a blocked seller disappears
 * from the feed too rather than only from search.
 */
export async function getFeed(
  db: Database,
  options: { limit?: number; cursor?: string; viewerId?: string } = {},
) {
  const limit = Math.min(Math.max(options.limit ?? FEED_PAGE_SIZE, 1), 30);

  const conditions: SQL[] = [eq(videos.status, "READY")];
  if (options.cursor) {
    const { at, id } = decodeCursor(options.cursor);
    conditions.push(sql`(${videos.createdAt}, ${videos.id}) < (${at}, ${id})`);
  }
  if (options.viewerId) {
    conditions.push(sql`NOT EXISTS (
      SELECT 1 FROM ${blocks} b
      WHERE (b.blocker_id = ${options.viewerId} AND b.blocked_id = ${videos.sellerId})
         OR (b.blocker_id = ${videos.sellerId} AND b.blocked_id = ${options.viewerId})
    )`);
  }

  const rows = await db
    .select({
      id: videos.id,
      caption: videos.caption,
      playbackUrl: videos.playbackUrl,
      thumbnailUrl: videos.thumbnailUrl,
      durationMs: videos.durationMs,
      width: videos.width,
      height: videos.height,
      createdAt: videos.createdAt,
      sellerId: accounts.id,
      sellerHandle: accounts.handle,
      sellerName: accounts.displayName,
      sellerEmoji: accounts.emoji,
      sellerCity: accounts.city,
      sellerIsVerified: accounts.isVerified,
      sellerAllowMessages: accounts.allowMessages,
      sellerPhone: sql<string | null>`CASE WHEN ${accounts.allowCalls} THEN ${accounts.publicPhone} END`,
    })
    .from(videos)
    .innerJoin(accounts, eq(accounts.id, videos.sellerId))
    .where(and(...conditions))
    .orderBy(desc(videos.createdAt), desc(videos.id))
    .limit(limit + 1);

  const hasMore = rows.length > limit;
  const page = hasMore ? rows.slice(0, limit) : rows;
  const last = page[page.length - 1];

  // One query for every video's ads rather than one per video.
  const videoIds = page.map((r: { id: string }) => r.id);
  const tagged = videoIds.length
    ? await db
        .select({
          videoId: videoListings.videoId,
          position: videoListings.position,
          id: listings.id,
          title: listings.title,
          priceHalalas: listings.priceHalalas,
          isNegotiable: listings.isNegotiable,
          emoji: listings.emoji,
          city: listings.city,
          condition: listings.condition,
          status: listings.status,
        })
        .from(videoListings)
        .innerJoin(listings, eq(listings.id, videoListings.listingId))
        .where(and(inArray(videoListings.videoId, videoIds), eq(listings.status, "ACTIVE")))
        .orderBy(asc(videoListings.position))
    : [];

  const byVideo = new Map<string, unknown[]>();
  for (const row of tagged as { videoId: string }[]) {
    const list = byVideo.get(row.videoId) ?? [];
    list.push(row);
    byVideo.set(row.videoId, list);
  }

  return {
    items: page.map((video: { id: string }) => ({
      ...video,
      listings: byVideo.get(video.id) ?? [],
    })),
    nextCursor: hasMore && last ? encodeCursor(last.createdAt, last.id) : null,
  };
}
