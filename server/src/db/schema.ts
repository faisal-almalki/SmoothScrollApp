import { sql } from "drizzle-orm";
import {
  bigint,
  boolean,
  check,
  index,
  integer,
  pgEnum,
  pgTable,
  primaryKey,
  real,
  smallint,
  text,
  timestamp,
  unique,
  uniqueIndex,
  uuid,
} from "drizzle-orm/pg-core";
import { customType } from "drizzle-orm/pg-core";

/**
 * Postgres schema for SmoothScroll, mirroring the Kotlin domain in
 * `app/src/main/java/.../ui/commerce/model`.
 *
 * Two conventions carry over from the client and are load-bearing:
 *
 *  - Money is an integer number of minor units (halalas), never a float. The
 *    client already does this; keeping it identical means no conversion layer.
 *  - Every account is a seller. There is no separate merchant table: the person
 *    browsing and the person posting are the same row.
 */

/** tsvector isn't in drizzle's builtins, so it's declared once here. */
const tsvector = customType<{ data: string }>({
  dataType() {
    return "tsvector";
  },
});

export const listingCondition = pgEnum("listing_condition", [
  "NEW",
  "LIKE_NEW",
  "USED",
]);

export const listingStatus = pgEnum("listing_status", [
  "ACTIVE",
  "SOLD",
  "REMOVED",
  "BLOCKED",
]);

export const videoStatus = pgEnum("video_status", [
  "UPLOADING",
  "PROCESSING",
  "READY",
  "FAILED",
]);

export const liveStatus = pgEnum("live_status", ["SCHEDULED", "LIVE", "ENDED"]);

export const reportTarget = pgEnum("report_target", [
  "LISTING",
  "ACCOUNT",
  "MESSAGE",
  "LIVE_STREAM",
]);

export const reportState = pgEnum("report_state", [
  "OPEN",
  "REVIEWING",
  "ACTIONED",
  "DISMISSED",
]);

/* ============================================================
   ACCOUNTS
   ============================================================ */

/**
 * A person. `auth_phone` is the login identity and is never exposed by the API.
 * `public_phone` is what a buyer sees, and only when `allow_calls` is on — the
 * two are deliberately separate columns so publishing a number is a choice, not
 * a side effect of signing up.
 */
export const accounts = pgTable(
  "accounts",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    authPhone: text("auth_phone").notNull(),
    handle: text("handle").notNull(),
    displayName: text("display_name").notNull(),
    bio: text("bio").notNull().default(""),
    emoji: text("emoji").notNull().default("🙂"),
    city: text("city").notNull(),
    avatarUrl: text("avatar_url"),

    publicPhone: text("public_phone"),
    allowCalls: boolean("allow_calls").notNull().default(false),
    allowMessages: boolean("allow_messages").notNull().default(true),

    rating: real("rating").notNull().default(5),
    ratingCount: integer("rating_count").notNull().default(0),
    followerCount: integer("follower_count").notNull().default(0),
    isVerified: boolean("is_verified").notNull().default(false),

    isBanned: boolean("is_banned").notNull().default(false),
    memberSince: timestamp("member_since", { withTimezone: true })
      .notNull()
      .defaultNow(),
    lastSeenAt: timestamp("last_seen_at", { withTimezone: true }),
    /** Set when the user asks for deletion; a job hard-deletes after the grace period. */
    deletedAt: timestamp("deleted_at", { withTimezone: true }),
  },
  (t) => [
    uniqueIndex("accounts_auth_phone_key").on(t.authPhone),
    uniqueIndex("accounts_handle_key").on(sql`lower(${t.handle})`),
    // A number can only be public if there is one to show.
    check(
      "accounts_phone_published_has_number",
      sql`NOT ${t.allowCalls} OR ${t.publicPhone} IS NOT NULL`,
    ),
    check("accounts_rating_range", sql`${t.rating} >= 0 AND ${t.rating} <= 5`),
  ],
);

/**
 * Short-lived login codes. Stored hashed so a database leak does not hand out
 * live codes, and rate limited by the attempt counter.
 */
export const otpCodes = pgTable(
  "otp_codes",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    phone: text("phone").notNull(),
    codeHash: text("code_hash").notNull(),
    expiresAt: timestamp("expires_at", { withTimezone: true }).notNull(),
    attempts: smallint("attempts").notNull().default(0),
    consumedAt: timestamp("consumed_at", { withTimezone: true }),
    createdAt: timestamp("created_at", { withTimezone: true })
      .notNull()
      .defaultNow(),
  },
  (t) => [index("otp_codes_phone_idx").on(t.phone, t.createdAt.desc())],
);

/** Refresh tokens, so a stolen access token expires quickly and can be revoked. */
export const sessions = pgTable(
  "sessions",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    accountId: uuid("account_id")
      .notNull()
      .references(() => accounts.id, { onDelete: "cascade" }),
    refreshTokenHash: text("refresh_token_hash").notNull(),
    deviceLabel: text("device_label"),
    expiresAt: timestamp("expires_at", { withTimezone: true }).notNull(),
    revokedAt: timestamp("revoked_at", { withTimezone: true }),
    createdAt: timestamp("created_at", { withTimezone: true })
      .notNull()
      .defaultNow(),
  },
  (t) => [
    uniqueIndex("sessions_refresh_hash_key").on(t.refreshTokenHash),
    index("sessions_account_idx").on(t.accountId),
  ],
);

/** Firebase Cloud Messaging tokens, one row per install. */
export const pushTokens = pgTable(
  "push_tokens",
  {
    token: text("token").primaryKey(),
    accountId: uuid("account_id")
      .notNull()
      .references(() => accounts.id, { onDelete: "cascade" }),
    platform: text("platform").notNull().default("android"),
    updatedAt: timestamp("updated_at", { withTimezone: true })
      .notNull()
      .defaultNow(),
  },
  (t) => [index("push_tokens_account_idx").on(t.accountId)],
);

/* ============================================================
   LISTINGS
   ============================================================ */

/**
 * A classified ad.
 *
 * `search` is a generated tsvector so full-text search never needs a trigger to
 * stay in sync, and `simple` is used rather than `english` because the corpus is
 * mixed Arabic and English — the English stemmer would mangle Arabic titles.
 */
export const listings = pgTable(
  "listings",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    sellerId: uuid("seller_id")
      .notNull()
      .references(() => accounts.id, { onDelete: "cascade" }),
    title: text("title").notNull(),
    description: text("description").notNull().default(""),
    priceHalalas: bigint("price_halalas", { mode: "number" }).notNull(),
    isNegotiable: boolean("is_negotiable").notNull().default(true),
    category: text("category").notNull(),
    condition: listingCondition("condition").notNull().default("USED"),
    emoji: text("emoji").notNull().default("📦"),
    city: text("city").notNull(),
    status: listingStatus("status").notNull().default("ACTIVE"),
    viewCount: integer("view_count").notNull().default(0),
    postedAt: timestamp("posted_at", { withTimezone: true })
      .notNull()
      .defaultNow(),
    bumpedAt: timestamp("bumped_at", { withTimezone: true })
      .notNull()
      .defaultNow(),
    search: tsvector("search").generatedAlwaysAs(
      sql`to_tsvector('simple', coalesce(title,'') || ' ' || coalesce(description,'') || ' ' || coalesce(city,'') || ' ' || coalesce(category,''))`,
    ),
  },
  (t) => [
    index("listings_search_idx").using("gin", t.search),
    // The browse tab's default query: active ads, newest first, optionally by city.
    index("listings_browse_idx")
      .on(t.bumpedAt.desc())
      .where(sql`status = 'ACTIVE'`),
    index("listings_city_idx")
      .on(t.city, t.bumpedAt.desc())
      .where(sql`status = 'ACTIVE'`),
    index("listings_category_idx")
      .on(t.category, t.bumpedAt.desc())
      .where(sql`status = 'ACTIVE'`),
    index("listings_seller_idx").on(t.sellerId, t.postedAt.desc()),
    check("listings_price_non_negative", sql`${t.priceHalalas} >= 0`),
  ],
);

/** Ordered photos. Position 0 is the one shown on the browse card. */
export const listingPhotos = pgTable(
  "listing_photos",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    listingId: uuid("listing_id")
      .notNull()
      .references(() => listings.id, { onDelete: "cascade" }),
    url: text("url").notNull(),
    width: integer("width"),
    height: integer("height"),
    position: smallint("position").notNull().default(0),
    createdAt: timestamp("created_at", { withTimezone: true })
      .notNull()
      .defaultNow(),
  },
  (t) => [
    index("listing_photos_listing_idx").on(t.listingId, t.position),
    unique("listing_photos_position_key").on(t.listingId, t.position),
  ],
);

export const follows = pgTable(
  "follows",
  {
    followerId: uuid("follower_id")
      .notNull()
      .references(() => accounts.id, { onDelete: "cascade" }),
    sellerId: uuid("seller_id")
      .notNull()
      .references(() => accounts.id, { onDelete: "cascade" }),
    createdAt: timestamp("created_at", { withTimezone: true })
      .notNull()
      .defaultNow(),
  },
  (t) => [
    primaryKey({ columns: [t.followerId, t.sellerId] }),
    index("follows_seller_idx").on(t.sellerId),
    check("follows_not_self", sql`${t.followerId} <> ${t.sellerId}`),
  ],
);

/* ============================================================
   VIDEOS — the feed
   ============================================================ */

export const videos = pgTable(
  "videos",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    sellerId: uuid("seller_id")
      .notNull()
      .references(() => accounts.id, { onDelete: "cascade" }),
    caption: text("caption").notNull().default(""),
    /** HLS manifest once transcoding finishes; the raw upload before that. */
    playbackUrl: text("playback_url"),
    thumbnailUrl: text("thumbnail_url"),
    durationMs: integer("duration_ms"),
    width: integer("width"),
    height: integer("height"),
    status: videoStatus("status").notNull().default("UPLOADING"),
    viewCount: integer("view_count").notNull().default(0),
    createdAt: timestamp("created_at", { withTimezone: true })
      .notNull()
      .defaultNow(),
  },
  (t) => [
    index("videos_feed_idx")
      .on(t.createdAt.desc())
      .where(sql`status = 'READY'`),
    index("videos_seller_idx").on(t.sellerId, t.createdAt.desc()),
  ],
);

/**
 * What a video is advertising. Position 0 becomes the pill over the player, so
 * the ordering is meaningful rather than incidental.
 */
export const videoListings = pgTable(
  "video_listings",
  {
    videoId: uuid("video_id")
      .notNull()
      .references(() => videos.id, { onDelete: "cascade" }),
    listingId: uuid("listing_id")
      .notNull()
      .references(() => listings.id, { onDelete: "cascade" }),
    position: smallint("position").notNull().default(0),
  },
  (t) => [
    primaryKey({ columns: [t.videoId, t.listingId] }),
    index("video_listings_listing_idx").on(t.listingId),
  ],
);

/* ============================================================
   MESSAGING
   ============================================================ */

/**
 * One thread per (buyer, listing) pair, which is what keeps a seller's inbox
 * legible: every conversation is anchored to the ad it is about.
 */
export const conversations = pgTable(
  "conversations",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    listingId: uuid("listing_id")
      .notNull()
      .references(() => listings.id, { onDelete: "cascade" }),
    buyerId: uuid("buyer_id")
      .notNull()
      .references(() => accounts.id, { onDelete: "cascade" }),
    sellerId: uuid("seller_id")
      .notNull()
      .references(() => accounts.id, { onDelete: "cascade" }),
    lastMessageAt: timestamp("last_message_at", { withTimezone: true })
      .notNull()
      .defaultNow(),
    lastMessagePreview: text("last_message_preview").notNull().default(""),
    buyerUnread: integer("buyer_unread").notNull().default(0),
    sellerUnread: integer("seller_unread").notNull().default(0),
    createdAt: timestamp("created_at", { withTimezone: true })
      .notNull()
      .defaultNow(),
  },
  (t) => [
    unique("conversations_listing_buyer_key").on(t.listingId, t.buyerId),
    index("conversations_buyer_idx").on(t.buyerId, t.lastMessageAt.desc()),
    index("conversations_seller_idx").on(t.sellerId, t.lastMessageAt.desc()),
    check("conversations_not_self", sql`${t.buyerId} <> ${t.sellerId}`),
  ],
);

export const messages = pgTable(
  "messages",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    conversationId: uuid("conversation_id")
      .notNull()
      .references(() => conversations.id, { onDelete: "cascade" }),
    senderId: uuid("sender_id")
      .notNull()
      .references(() => accounts.id, { onDelete: "cascade" }),
    body: text("body").notNull(),
    sentAt: timestamp("sent_at", { withTimezone: true }).notNull().defaultNow(),
    readAt: timestamp("read_at", { withTimezone: true }),
  },
  (t) => [
    index("messages_conversation_idx").on(t.conversationId, t.sentAt.desc()),
    check("messages_body_not_empty", sql`length(btrim(${t.body})) > 0`),
  ],
);

/* ============================================================
   LIVE
   ============================================================ */

export const liveStreams = pgTable(
  "live_streams",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    sellerId: uuid("seller_id")
      .notNull()
      .references(() => accounts.id, { onDelete: "cascade" }),
    title: text("title").notNull(),
    topic: text("topic").notNull().default(""),
    /** Set by the streaming provider once ingest starts. */
    playbackUrl: text("playback_url"),
    ingestKey: text("ingest_key"),
    status: liveStatus("status").notNull().default("SCHEDULED"),
    viewerCount: integer("viewer_count").notNull().default(0),
    peakViewerCount: integer("peak_viewer_count").notNull().default(0),
    startedAt: timestamp("started_at", { withTimezone: true }),
    endedAt: timestamp("ended_at", { withTimezone: true }),
    createdAt: timestamp("created_at", { withTimezone: true })
      .notNull()
      .defaultNow(),
  },
  (t) => [
    index("live_streams_live_idx")
      .on(t.startedAt.desc())
      .where(sql`status = 'LIVE'`),
    index("live_streams_seller_idx").on(t.sellerId),
  ],
);

export const liveStreamListings = pgTable(
  "live_stream_listings",
  {
    streamId: uuid("stream_id")
      .notNull()
      .references(() => liveStreams.id, { onDelete: "cascade" }),
    listingId: uuid("listing_id")
      .notNull()
      .references(() => listings.id, { onDelete: "cascade" }),
    isPinned: boolean("is_pinned").notNull().default(false),
    position: smallint("position").notNull().default(0),
  },
  (t) => [primaryKey({ columns: [t.streamId, t.listingId] })],
);

export const liveChatMessages = pgTable(
  "live_chat_messages",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    streamId: uuid("stream_id")
      .notNull()
      .references(() => liveStreams.id, { onDelete: "cascade" }),
    accountId: uuid("account_id")
      .notNull()
      .references(() => accounts.id, { onDelete: "cascade" }),
    body: text("body").notNull(),
    sentAt: timestamp("sent_at", { withTimezone: true }).notNull().defaultNow(),
  },
  (t) => [index("live_chat_stream_idx").on(t.streamId, t.sentAt.desc())],
);

/* ============================================================
   SAFETY — required before Google Play will accept a UGC app
   ============================================================ */

export const reports = pgTable(
  "reports",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    reporterId: uuid("reporter_id")
      .notNull()
      .references(() => accounts.id, { onDelete: "cascade" }),
    targetType: reportTarget("target_type").notNull(),
    targetId: uuid("target_id").notNull(),
    reason: text("reason").notNull(),
    note: text("note"),
    state: reportState("state").notNull().default("OPEN"),
    createdAt: timestamp("created_at", { withTimezone: true })
      .notNull()
      .defaultNow(),
    resolvedAt: timestamp("resolved_at", { withTimezone: true }),
  },
  (t) => [
    index("reports_open_idx")
      .on(t.createdAt.desc())
      .where(sql`state = 'OPEN'`),
    index("reports_target_idx").on(t.targetType, t.targetId),
    unique("reports_one_per_reporter").on(t.reporterId, t.targetType, t.targetId),
  ],
);

export const blocks = pgTable(
  "blocks",
  {
    blockerId: uuid("blocker_id")
      .notNull()
      .references(() => accounts.id, { onDelete: "cascade" }),
    blockedId: uuid("blocked_id")
      .notNull()
      .references(() => accounts.id, { onDelete: "cascade" }),
    createdAt: timestamp("created_at", { withTimezone: true })
      .notNull()
      .defaultNow(),
  },
  (t) => [
    primaryKey({ columns: [t.blockerId, t.blockedId] }),
    check("blocks_not_self", sql`${t.blockerId} <> ${t.blockedId}`),
  ],
);

export type Account = typeof accounts.$inferSelect;
export type NewAccount = typeof accounts.$inferInsert;
export type Listing = typeof listings.$inferSelect;
export type NewListing = typeof listings.$inferInsert;
export type Conversation = typeof conversations.$inferSelect;
export type Message = typeof messages.$inferSelect;
export type LiveStream = typeof liveStreams.$inferSelect;
