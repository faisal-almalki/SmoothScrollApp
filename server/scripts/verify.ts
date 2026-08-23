import { PGlite } from "@electric-sql/pglite";
import { drizzle } from "drizzle-orm/pglite";
import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";

// Set before anything reads it: the auth service signs tokens with this.
process.env.JWT_SECRET ??= "verify-only-secret-not-used-anywhere-real";

import { eq } from "drizzle-orm";
import * as schema from "../src/db/schema.js";
import { normaliseSaudiPhone } from "../src/lib/phone.js";
import {
  loadActiveAccount,
  logout,
  refreshSession,
  requestAccountDeletion,
  requestOtp,
  verifyOtp,
} from "../src/services/auth.js";
import {
  blockAccount,
  browseListings,
  bumpListing,
  createListing,
  followSeller,
  getListing,
  removeListing,
  reportSomething,
  setListingSold,
  unblockAccount,
  unfollowSeller,
  updateListing,
} from "../src/services/listings.js";
import { listenerCount, subscribe } from "../src/lib/events.js";
import {
  attachPhoto,
  confirmVideoUpload,
  createPhotoUploadUrl,
  createVideoUploadUrl,
  deletePhoto,
  getFeed,
  listPhotos,
  markVideoReady,
  reorderPhotos,
  setVideoListings,
} from "../src/services/media.js";
import {
  listConversations,
  listMessages,
  markConversationRead,
  openConversation,
  sendMessage,
  totalUnread,
} from "../src/services/messaging.js";
import {
  maintenanceBacklog,
  purgeDeadSessions,
  purgeDeletedAccounts,
  purgeExpiredOtpCodes,
  runMaintenance,
} from "../src/services/maintenance.js";
import {
  clearPushTokensForAccount,
  dispatchMessagePush,
  listPushTokens,
  registerPushToken,
  unregisterPushToken,
} from "../src/services/push.js";
import { isPushConfigured } from "../src/lib/push.js";

/**
 * Runs the generated migration against a real Postgres engine (PGlite is
 * Postgres compiled to WASM) and then exercises the queries the app actually
 * makes, plus the constraints that are supposed to protect it.
 *
 * This exists because the build environment cannot reach Neon. Verifying here
 * means the migration is known-good before anyone points it at a real database.
 */

const MIGRATIONS_DIR = join(import.meta.dirname, "..", "drizzle");

let passed = 0;
let failed = 0;

function ok(label: string, detail = "") {
  passed++;
  console.log(`  [32m✓[0m ${label}${detail ? `  [90m${detail}[0m` : ""}`);
}

function bad(label: string, detail = "") {
  failed++;
  console.log(`  [31m✗[0m ${label}${detail ? `  [90m${detail}[0m` : ""}`);
}

function expect(label: string, condition: boolean, detail = "") {
  condition ? ok(label, detail) : bad(label, detail);
}

/** Asserts a statement is rejected — used to prove a constraint really bites. */
async function expectRejected(db: PGlite, label: string, statement: string) {
  try {
    await db.exec(statement);
    bad(label, "statement was accepted, but should have been rejected");
  } catch (error) {
    const message = error instanceof Error ? error.message.split("\n")[0] : String(error);
    ok(label, message);
  }
}

const db = new PGlite();

console.log("\n[1mSchema[0m");
const version = (await db.query<{ version: string }>("select version()")).rows[0]!.version;
ok("engine", version.split(" on ")[0]);

const files = readdirSync(MIGRATIONS_DIR).filter((f) => f.endsWith(".sql")).sort();
for (const file of files) {
  const sql = readFileSync(join(MIGRATIONS_DIR, file), "utf8");
  // drizzle separates statements with this marker rather than plain semicolons,
  // which would split function bodies and dollar-quoted strings.
  const statements = sql.split("--> statement-breakpoint").map((s) => s.trim()).filter(Boolean);
  for (const statement of statements) {
    await db.exec(statement);
  }
  ok(`applied ${file}`, `${statements.length} statements`);
}

const tables = await db.query<{ count: number }>(
  `select count(*)::int as count from information_schema.tables where table_schema = 'public'`,
);
expect("tables created", tables.rows[0]!.count === 16, `${tables.rows[0]!.count} tables`);

const indexes = await db.query<{ count: number }>(
  `select count(*)::int as count from pg_indexes where schemaname = 'public'`,
);
ok("indexes created", `${indexes.rows[0]!.count} indexes`);

/* ============================================================
   Seed — a slice of the app's own data
   ============================================================ */
console.log("\n[1mSeed[0m");

const FARIS = "11111111-1111-1111-1111-111111111111";
const TECH = "22222222-2222-2222-2222-222222222222";
const BUYER = "33333333-3333-3333-3333-333333333333";

await db.exec(`
  INSERT INTO accounts (id, auth_phone, handle, display_name, bio, emoji, city, public_phone, allow_calls, is_verified, rating, rating_count, follower_count) VALUES
    ('${FARIS}', '+966501234567', 'faris.motors', 'Faris', 'Cars only.', '🚗', 'Riyadh', '+966501234567', true, true, 4.9, 214, 48200),
    ('${TECH}', '+966555000111', 'tech.souq', 'Tech Souq', 'Used phones.', '📱', 'Riyadh', NULL, false, true, 4.7, 1842, 132600),
    ('${BUYER}', '+966533222111', 'ahmed', 'Ahmed', '', '🙂', 'Jeddah', NULL, false, false, 5, 0, 0);
`);
ok("accounts", "3 rows");

await db.exec(`
  INSERT INTO listings (id, seller_id, title, description, price_halalas, category, condition, emoji, city) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', '${FARIS}', 'Toyota Land Cruiser GXR 2019', 'One owner, 148,000 km, full agency service history.', 18500000, 'Cars', 'USED', '🚙', 'Riyadh'),
    ('aaaaaaaa-0000-0000-0000-000000000002', '${FARIS}', 'Toyota Camry 2021, low mileage', '62,000 km, never had an accident.', 7900000, 'Cars', 'USED', '🚗', 'Riyadh'),
    ('aaaaaaaa-0000-0000-0000-000000000003', '${TECH}', 'iPhone 15 Pro Max 256GB', 'Battery health 94%.', 340000, 'Electronics', 'LIKE_NEW', '📱', 'Riyadh'),
    ('aaaaaaaa-0000-0000-0000-000000000004', '${TECH}', 'MacBook Air M2', '38 charge cycles.', 380000, 'Electronics', 'LIKE_NEW', '💻', 'Jeddah');
`);
ok("listings", "4 rows");

/* ============================================================
   The queries the app actually makes
   ============================================================ */
console.log("\n[1mQueries[0m");

const browse = await db.query(
  `SELECT title, price_halalas FROM listings WHERE status = 'ACTIVE' ORDER BY bumped_at DESC, title`,
);
expect("browse: active ads", browse.rows.length === 4, `${browse.rows.length} ads`);

const search = await db.query<{ title: string }>(
  `SELECT title FROM listings
   WHERE search @@ websearch_to_tsquery('simple', $1) AND status = 'ACTIVE'`,
  ["toyota"],
);
expect("search: full text", search.rows.length === 2, `"toyota" → ${search.rows.length} ads`);

const searchArabic = await db.query(
  `SELECT title FROM listings WHERE search @@ websearch_to_tsquery('simple', $1)`,
  ["Riyadh"],
);
expect("search: matches city text too", searchArabic.rows.length === 3, `${searchArabic.rows.length} ads`);

const byCity = await db.query(
  `SELECT title FROM listings WHERE city = $1 AND status = 'ACTIVE' ORDER BY bumped_at DESC`,
  ["Jeddah"],
);
expect("browse: city filter", byCity.rows.length === 1, `Jeddah → ${byCity.rows.length}`);

// The contact rule: a number is only ever returned when the seller published it.
const contact = await db.query<{ display_name: string; visible_phone: string | null }>(
  `SELECT display_name,
          CASE WHEN allow_calls AND public_phone IS NOT NULL THEN public_phone END AS visible_phone
   FROM accounts WHERE id IN ($1, $2) ORDER BY display_name`,
  [FARIS, TECH],
);
expect(
  "contact: number hidden unless published",
  contact.rows[0]!.visible_phone === "+966501234567" && contact.rows[1]!.visible_phone === null,
  "Faris shows a number, Tech Souq does not",
);

// Opening a thread twice must reuse it, which is what ON CONFLICT gives us.
for (let i = 0; i < 2; i++) {
  await db.query(
    `INSERT INTO conversations (listing_id, buyer_id, seller_id)
     VALUES ($1, $2, $3) ON CONFLICT (listing_id, buyer_id) DO NOTHING`,
    ["aaaaaaaa-0000-0000-0000-000000000001", BUYER, FARIS],
  );
}
const threads = await db.query<{ count: number }>(
  `SELECT count(*)::int AS count FROM conversations`,
);
expect("messaging: one thread per ad per buyer", threads.rows[0]!.count === 1, "opened twice, 1 row");

const convo = await db.query<{ id: string }>(`SELECT id FROM conversations LIMIT 1`);
await db.query(
  `INSERT INTO messages (conversation_id, sender_id, body) VALUES ($1, $2, $3)`,
  [convo.rows[0]!.id, BUYER, "Is it still available?"],
);
const inbox = await db.query<{ title: string; last: string }>(
  `SELECT l.title, m.body AS last
   FROM conversations c
   JOIN listings l ON l.id = c.listing_id
   JOIN LATERAL (SELECT body FROM messages WHERE conversation_id = c.id ORDER BY sent_at DESC LIMIT 1) m ON true
   WHERE c.buyer_id = $1`,
  [BUYER],
);
expect(
  "messaging: inbox joins ad + last message",
  inbox.rows[0]?.title === "Toyota Land Cruiser GXR 2019" && inbox.rows[0]?.last === "Is it still available?",
);

// The feed: a video and the ads it carries, ordered so position 0 is the pill.
await db.exec(`
  INSERT INTO videos (id, seller_id, caption, playback_url, status)
  VALUES ('bbbbbbbb-0000-0000-0000-000000000001', '${FARIS}', '2019 GXR walkaround', 'https://cdn/x.m3u8', 'READY');
  INSERT INTO video_listings (video_id, listing_id, position) VALUES
    ('bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 0),
    ('bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000002', 1);
`);
const feed = await db.query<{ title: string; position: number }>(
  `SELECT l.title, vl.position
   FROM videos v
   JOIN video_listings vl ON vl.video_id = v.id
   JOIN listings l ON l.id = vl.listing_id
   WHERE v.status = 'READY' ORDER BY vl.position`,
);
expect(
  "feed: video carries ordered ads",
  feed.rows.length === 2 && feed.rows[0]!.title.includes("Land Cruiser"),
  `${feed.rows.length} tagged, first = pill`,
);

// Marking sold must remove the ad from browse without deleting it.
await db.query(`UPDATE listings SET status = 'SOLD' WHERE id = $1`, [
  "aaaaaaaa-0000-0000-0000-000000000003",
]);
const afterSold = await db.query<{ count: number }>(
  `SELECT count(*)::int AS count FROM listings WHERE status = 'ACTIVE'`,
);
expect("sold: leaves browse, row survives", afterSold.rows[0]!.count === 3, "3 active, 4 total");

/* ============================================================
   Constraints — prove the guarantees actually bite
   ============================================================ */
console.log("\n[1mConstraints[0m");

await expectRejected(
  db,
  "cannot enable calls without a number",
  `INSERT INTO accounts (auth_phone, handle, display_name, city, allow_calls)
   VALUES ('+966500000009', 'nonumber', 'No Number', 'Riyadh', true)`,
);

await expectRejected(
  db,
  "cannot post a negative price",
  `INSERT INTO listings (seller_id, title, price_halalas, category, city)
   VALUES ('${FARIS}', 'Broken', -1, 'Other', 'Riyadh')`,
);

await expectRejected(
  db,
  "cannot follow yourself",
  `INSERT INTO follows (follower_id, seller_id) VALUES ('${FARIS}', '${FARIS}')`,
);

await expectRejected(
  db,
  "cannot message yourself",
  `INSERT INTO conversations (listing_id, buyer_id, seller_id)
   VALUES ('aaaaaaaa-0000-0000-0000-000000000002', '${FARIS}', '${FARIS}')`,
);

await expectRejected(
  db,
  "cannot send an empty message",
  `INSERT INTO messages (conversation_id, sender_id, body)
   VALUES ('${convo.rows[0]!.id}', '${BUYER}', '   ')`,
);

await expectRejected(
  db,
  "handles are unique regardless of case",
  `INSERT INTO accounts (auth_phone, handle, display_name, city)
   VALUES ('+966500000008', 'Faris.Motors', 'Impostor', 'Riyadh')`,
);

await expectRejected(
  db,
  "cannot report the same thing twice",
  `INSERT INTO reports (reporter_id, target_type, target_id, reason) VALUES
     ('${BUYER}', 'LISTING', 'aaaaaaaa-0000-0000-0000-000000000001', 'spam'),
     ('${BUYER}', 'LISTING', 'aaaaaaaa-0000-0000-0000-000000000001', 'spam')`,
);

// Deleting an account must not strand its ads or threads.
await db.query(`DELETE FROM accounts WHERE id = $1`, [TECH]);
const orphans = await db.query<{ count: number }>(
  `SELECT count(*)::int AS count FROM listings WHERE seller_id = $1`,
  [TECH],
);
expect("deleting an account removes its ads", orphans.rows[0]!.count === 0);

/* ============================================================
   Auth — the same service code the API runs, driven against
   this in-process Postgres with time passed in explicitly.
   ============================================================ */
console.log("\n\x1b[1mAuth\x1b[0m");

const orm = drizzle(db, { schema });
const T0 = new Date("2026-01-01T10:00:00Z");
const minutes = (n: number) => new Date(T0.getTime() + n * 60_000);

expect(
  "phone: every shape normalises to one number",
  ["0512345678", "512345678", "+966512345678", "00966512345678", "05 1234 5678"].every(
    (input) => normaliseSaudiPhone(input) === "+966512345678",
  ),
  "+966512345678",
);

try {
  normaliseSaudiPhone("0412345678");
  bad("phone: rejects a non-mobile number");
} catch {
  ok("phone: rejects a non-mobile number", "landline prefix refused");
}

const signup = await requestOtp(orm, "0512345678", { now: T0, exposeCode: true });
expect("otp: issued", typeof signup.devCode === "string" && signup.devCode.length === 6);

const stored = await db.query<{ code_hash: string }>(`SELECT code_hash FROM otp_codes LIMIT 1`);
expect(
  "otp: stored hashed, never in the clear",
  !stored.rows[0]!.code_hash.includes(signup.devCode!),
  "column holds an HMAC",
);

const session = await verifyOtp(orm, "0512345678", signup.devCode!, { now: minutes(1) });
expect("otp: correct code signs in", session.isNewAccount && !!session.accessToken);

const created = await db.query<{ handle: string }>(
  `SELECT handle FROM accounts WHERE auth_phone = '+966512345678'`,
);
expect(
  "signup: account created with a usable handle",
  created.rows[0]?.handle === "user5678",
  created.rows[0]?.handle,
);

try {
  await verifyOtp(orm, "0512345678", signup.devCode!, { now: minutes(2) });
  bad("otp: a used code cannot be replayed");
} catch (e) {
  ok("otp: a used code cannot be replayed", (e as Error).message);
}

const wrong = await requestOtp(orm, "0555111222", { now: minutes(3), exposeCode: true });
const wrongCode = wrong.devCode === "000000" ? "111111" : "000000";
try {
  await verifyOtp(orm, "0555111222", wrongCode, { now: minutes(4) });
  bad("otp: wrong code rejected");
} catch (e) {
  ok("otp: wrong code rejected", (e as Error).message);
}

let lockedOut = "";
for (let i = 0; i < 5; i++) {
  try {
    await verifyOtp(orm, "0555111222", wrongCode, { now: minutes(4) });
  } catch (e) {
    lockedOut = (e as Error).message;
  }
}
expect("otp: locks out after repeated wrong codes", lockedOut.includes("Too many wrong attempts"), lockedOut);

const stale = await requestOtp(orm, "0555333444", { now: minutes(10), exposeCode: true });
try {
  await verifyOtp(orm, "0555333444", stale.devCode!, { now: minutes(16) });
  bad("otp: expires after five minutes");
} catch (e) {
  ok("otp: expires after five minutes", (e as Error).message);
}

let limited = "";
for (let i = 0; i < 4; i++) {
  try {
    await requestOtp(orm, "0566777888", { now: minutes(20) });
  } catch (e) {
    limited = (e as Error).message;
  }
}
expect("otp: rate limited per number", limited.includes("Too many codes"), limited);

const rotated = await refreshSession(orm, session.refreshToken, { now: minutes(30) });
expect("refresh: issues a new pair", rotated.refreshToken !== session.refreshToken);
try {
  await refreshSession(orm, session.refreshToken, { now: minutes(31) });
  bad("refresh: a spent token cannot be reused");
} catch (e) {
  ok("refresh: a spent token cannot be reused", (e as Error).message);
}

await logout(orm, rotated.refreshToken, minutes(32));
try {
  await refreshSession(orm, rotated.refreshToken, { now: minutes(33) });
  bad("logout: revokes the session");
} catch (e) {
  ok("logout: revokes the session", (e as Error).message);
}

expect("auth: a live account resolves", (await loadActiveAccount(orm, session.accountId)) !== null);

await requestAccountDeletion(orm, session.accountId, minutes(40));
expect("deletion: account stops resolving", (await loadActiveAccount(orm, session.accountId)) === null);

const rowSurvives = await db.query<{ count: number }>(
  `SELECT count(*)::int AS count FROM accounts WHERE auth_phone = '+966512345678' AND deleted_at IS NOT NULL`,
);
expect(
  "deletion: row kept for the grace period",
  rowSurvives.rows[0]!.count === 1,
  "soft-deleted, recoverable",
);

const returning = await requestOtp(orm, "0512345678", { now: minutes(50), exposeCode: true });
const restored = await verifyOtp(orm, "0512345678", returning.devCode!, { now: minutes(51) });
expect(
  "deletion: signing in again cancels it",
  !restored.isNewAccount && (await loadActiveAccount(orm, restored.accountId)) !== null,
);

/* ============================================================
   Listings — browse, ownership, follows and blocking, driven
   through the same service functions the routes call.
   ============================================================ */
console.log("\n\x1b[1mListings\x1b[0m");

const L0 = new Date("2026-02-01T09:00:00Z");
const lmin = (n: number) => new Date(L0.getTime() + n * 60_000);
const hours = (n: number) => new Date(L0.getTime() + n * 3_600_000);

const sellerA = await verifyOtp(
  orm, "0501110001",
  (await requestOtp(orm, "0501110001", { now: lmin(0), exposeCode: true })).devCode!,
  { now: lmin(1) },
);
const sellerB = await verifyOtp(
  orm, "0501110002",
  (await requestOtp(orm, "0501110002", { now: lmin(2), exposeCode: true })).devCode!,
  { now: lmin(3) },
);
const shopper = await verifyOtp(
  orm, "0501110003",
  (await requestOtp(orm, "0501110003", { now: lmin(4), exposeCode: true })).devCode!,
  { now: lmin(5) },
);

// Twelve ads from seller A, one minute apart so the ordering is deterministic.
const posted: string[] = [];
for (let i = 0; i < 12; i++) {
  const row = await createListing(
    orm, sellerA.accountId,
    {
      title: `Test ad ${String(i).padStart(2, "0")} camera`,
      description: "Verification fixture.",
      priceHalalas: (i + 1) * 10_000,
      category: i % 2 === 0 ? "Electronics" : "Furniture",
      condition: i % 3 === 0 ? "NEW" : "USED",
      city: i % 2 === 0 ? "Riyadh" : "Jeddah",
    },
    { now: lmin(10 + i) },
  );
  posted.push(row.id);
}
const sellerBAd = await createListing(
  orm, sellerB.accountId,
  { title: "Seller B bicycle", priceHalalas: 55_000, category: "Other", city: "Dammam" },
  { now: lmin(30) },
);
ok("seeded", "13 ads across 2 sellers");

// Keyset paging must return every row exactly once, with no gaps or repeats.
const seen: string[] = [];
let cursor: string | null = null;
let pages = 0;
do {
  const page = await browseListings(orm, { limit: 5, ...(cursor ? { cursor } : {}) });
  seen.push(...page.items.map((r: { id: string }) => r.id));
  cursor = page.nextCursor;
  pages++;
} while (cursor && pages < 20);

const fixtureSeen = seen.filter((id) => posted.includes(id) || id === sellerBAd.id);
expect(
  "paging: every ad returned exactly once",
  new Set(fixtureSeen).size === fixtureSeen.length && fixtureSeen.length === 13,
  `${pages} pages, ${fixtureSeen.length} ads, no duplicates`,
);

// Bumping mid-scroll is exactly what breaks OFFSET paging; a keyset survives it.
const firstPage = await browseListings(orm, { limit: 5 });
await bumpListing(orm, posted[0]!, sellerA.accountId, { now: hours(48) });
const secondPage = await browseListings(orm, { limit: 5, cursor: firstPage.nextCursor! });
const overlap = firstPage.items
  .map((r: { id: string }) => r.id)
  .filter((id: string) => secondPage.items.some((r: { id: string }) => r.id === id));
expect("paging: a bump mid-scroll does not repeat rows", overlap.length === 0);

const searched = await browseListings(orm, { q: "camera", limit: 50 });
expect(
  "browse: full-text filter",
  searched.items.length === 12,
  `"camera" → ${searched.items.length}`,
);

const combined = await browseListings(orm, {
  q: "camera",
  city: "Riyadh",
  minPriceHalalas: 30_000,
  maxPriceHalalas: 90_000,
  limit: 50,
});
const inRange = combined.items.every(
  (r: { city: string; priceHalalas: number }) =>
    r.city === "Riyadh" && r.priceHalalas >= 30_000 && r.priceHalalas <= 90_000,
);
expect(
  "browse: filters combine",
  inRange && combined.items.length > 0,
  `${combined.items.length} ads in Riyadh between SAR 300 and 900`,
);

const byCondition = await browseListings(orm, { condition: "NEW", sellerId: sellerA.accountId, limit: 50 });
expect(
  "browse: condition + seller filter",
  byCondition.items.length === 4 && byCondition.items.every((r: { condition: string }) => r.condition === "NEW"),
  `${byCondition.items.length} new ads`,
);

// The number must only ever come out when the seller published it.
await orm
  .update(schema.accounts)
  .set({ publicPhone: "+966501110001", allowCalls: true })
  .where(eq(schema.accounts.id, sellerA.accountId));
const withPhone = await browseListings(orm, { sellerId: sellerA.accountId, limit: 1 });
const withoutPhone = await browseListings(orm, { sellerId: sellerB.accountId, limit: 1 });
expect(
  "browse: phone shown only when published",
  withPhone.items[0]!.sellerPhone === "+966501110001" && withoutPhone.items[0]!.sellerPhone === null,
);

// Ownership.
try {
  await updateListing(orm, posted[1]!, sellerB.accountId, { title: "Hijacked" });
  bad("ownership: a stranger cannot edit an ad");
} catch (e) {
  ok("ownership: a stranger cannot edit an ad", (e as Error).message);
}
try {
  await removeListing(orm, posted[1]!, shopper.accountId);
  bad("ownership: a stranger cannot delete an ad");
} catch (e) {
  ok("ownership: a stranger cannot delete an ad", (e as Error).message);
}
const edited = await updateListing(orm, posted[1]!, sellerA.accountId, { priceHalalas: 12_345 });
expect("ownership: the owner can edit", edited.priceHalalas === 12_345);

// Sold and removed both leave browse; only removal hides the ad entirely.
await setListingSold(orm, posted[2]!, sellerA.accountId, true);
const afterSoldBrowse = await browseListings(orm, { q: "camera", limit: 50 });
expect(
  "sold: leaves browse",
  !afterSoldBrowse.items.some((r: { id: string }) => r.id === posted[2]),
  `${afterSoldBrowse.items.length} still listed`,
);
await setListingSold(orm, posted[2]!, sellerA.accountId, false);
const relisted = await browseListings(orm, { q: "camera", limit: 50 });
expect("sold: can be relisted", relisted.items.some((r: { id: string }) => r.id === posted[2]));

await removeListing(orm, posted[3]!, sellerA.accountId);
try {
  await getListing(orm, posted[3]!);
  bad("removed: the ad is gone");
} catch (e) {
  ok("removed: the ad is gone", (e as Error).message);
}

// Bump cooldown.
try {
  await bumpListing(orm, posted[4]!, sellerA.accountId, { now: lmin(20) });
  bad("bump: cooled down for a day");
} catch (e) {
  ok("bump: cooled down for a day", (e as Error).message);
}
const bumped = await bumpListing(orm, posted[4]!, sellerA.accountId, { now: hours(30) });
expect("bump: allowed after the cooldown", bumped.bumpedAt.getTime() === hours(30).getTime());

// View counting, including the seller not inflating their own.
const before = await getListing(orm, posted[5]!, { viewerId: shopper.accountId, countView: true });
const after = await getListing(orm, posted[5]!, { viewerId: shopper.accountId, countView: true });
expect("views: counted for visitors", after.viewCount === before.viewCount + 1);
const ownView = await getListing(orm, posted[5]!, { viewerId: sellerA.accountId, countView: true });
const stillSame = await getListing(orm, posted[5]!);
expect("views: the seller's own visit does not count", stillSame.viewCount === ownView.viewCount);

// Follows keep the denormalised counter honest.
await followSeller(orm, shopper.accountId, sellerA.accountId);
const twice = await followSeller(orm, shopper.accountId, sellerA.accountId);
const afterFollow = (await orm
  .select({ n: schema.accounts.followerCount })
  .from(schema.accounts)
  .where(eq(schema.accounts.id, sellerA.accountId)))[0]!;
expect(
  "follow: counted once even when repeated",
  afterFollow.n === 1 && twice.changed === false,
  `follower_count = ${afterFollow.n}`,
);
await unfollowSeller(orm, shopper.accountId, sellerA.accountId);
await unfollowSeller(orm, shopper.accountId, sellerA.accountId);
const afterUnfollow = (await orm
  .select({ n: schema.accounts.followerCount })
  .from(schema.accounts)
  .where(eq(schema.accounts.id, sellerA.accountId)))[0]!;
expect(
  "follow: unfollowing twice cannot go negative",
  afterUnfollow.n === 0,
  `follower_count = ${afterUnfollow.n}`,
);

// Blocking hides ads in both directions. The blocker needs an ad of their own
// for the reverse direction to be observable at all.
const shopperAd = await createListing(
  orm, shopper.accountId,
  { title: "Shopper spare monitor", priceHalalas: 40_000, category: "Electronics", city: "Riyadh" },
  { now: lmin(40) },
);

const beforeBlock = await browseListings(orm, { viewerId: shopper.accountId, limit: 50 });
const sawSellerB = beforeBlock.items.some((r: { id: string }) => r.id === sellerBAd.id);
const sellerBBefore = await browseListings(orm, { viewerId: sellerB.accountId, limit: 50 });
const sawShopperAd = sellerBBefore.items.some((r: { id: string }) => r.id === shopperAd.id);

await blockAccount(orm, shopper.accountId, sellerB.accountId);

const afterBlock = await browseListings(orm, { viewerId: shopper.accountId, limit: 50 });
expect(
  "block: the blocked seller's ads disappear",
  sawSellerB && !afterBlock.items.some((r: { id: string }) => r.id === sellerBAd.id),
);

// The person who was blocked also stops seeing the blocker, so blocking cannot
// be detected by the ads simply staying visible.
const sellerBView = await browseListings(orm, { viewerId: sellerB.accountId, limit: 50 });
expect(
  "block: it cuts both ways",
  sawShopperAd && !sellerBView.items.some((r: { id: string }) => r.id === shopperAd.id),
  "the blocked seller stops seeing the blocker's ads too",
);

// An unrelated seller is untouched by someone else's block.
expect(
  "block: unrelated sellers are unaffected",
  sellerBView.items.some((r: { id: string }) => posted.includes(r.id)),
  "seller A's ads still visible to seller B",
);

try {
  await getListing(orm, sellerBAd.id, { viewerId: shopper.accountId });
  bad("block: a blocked ad cannot be opened directly");
} catch (e) {
  ok("block: a blocked ad cannot be opened directly", (e as Error).message);
}

await unblockAccount(orm, shopper.accountId, sellerB.accountId);
const afterUnblock = await browseListings(orm, { viewerId: shopper.accountId, limit: 50 });
expect(
  "block: unblocking restores them",
  afterUnblock.items.some((r: { id: string }) => r.id === sellerBAd.id),
);

// Reporting is idempotent for the reporter.
const firstReport = await reportSomething(orm, shopper.accountId, "LISTING", posted[6]!, "spam");
const secondReport = await reportSomething(orm, shopper.accountId, "LISTING", posted[6]!, "spam");
expect(
  "report: filed once, repeating is not an error",
  firstReport.alreadyReported === false && secondReport.alreadyReported === true,
);

/* ============================================================
   Messaging — inbox, unread bookkeeping, and who is allowed to
   talk to whom.
   ============================================================ */
console.log("\n\x1b[1mMessaging\x1b[0m");

const M0 = new Date("2026-03-01T09:00:00Z");
const mmin = (n: number) => new Date(M0.getTime() + n * 60_000);

const carSeller = await verifyOtp(
  orm, "0502220001",
  (await requestOtp(orm, "0502220001", { now: mmin(0), exposeCode: true })).devCode!,
  { now: mmin(1) },
);
const buyer = await verifyOtp(
  orm, "0502220002",
  (await requestOtp(orm, "0502220002", { now: mmin(2), exposeCode: true })).devCode!,
  { now: mmin(3) },
);
const stranger = await verifyOtp(
  orm, "0502220003",
  (await requestOtp(orm, "0502220003", { now: mmin(4), exposeCode: true })).devCode!,
  { now: mmin(5) },
);

const carAd = await createListing(
  orm, carSeller.accountId,
  { title: "Nissan Patrol 2020", priceHalalas: 21_000_000, category: "Cars", city: "Riyadh" },
  { now: mmin(6) },
);

const opened = await openConversation(orm, buyer.accountId, carAd.id, { now: mmin(7) });
const reopened = await openConversation(orm, buyer.accountId, carAd.id, { now: mmin(8) });
expect(
  "thread: opening twice reuses the same one",
  opened.created && !reopened.created && opened.id === reopened.id,
);

try {
  await openConversation(orm, carSeller.accountId, carAd.id, { now: mmin(9) });
  bad("thread: a seller cannot message their own ad");
} catch (e) {
  ok("thread: a seller cannot message their own ad", (e as Error).message);
}

// Unread moves for the recipient only.
await sendMessage(orm, opened.id, buyer.accountId, "Is it still available?", { now: mmin(10) });
expect(
  "unread: the recipient gains one, the sender does not",
  (await totalUnread(orm, carSeller.accountId)) === 1 &&
    (await totalUnread(orm, buyer.accountId)) === 0,
);

await sendMessage(orm, opened.id, carSeller.accountId, "Yes, still available.", { now: mmin(11) });
await sendMessage(orm, opened.id, carSeller.accountId, "Free to view tomorrow.", { now: mmin(12) });
expect(
  "unread: counts accumulate per side",
  (await totalUnread(orm, buyer.accountId)) === 2 &&
    (await totalUnread(orm, carSeller.accountId)) === 1,
);

// Reading clears only the caller's side.
await markConversationRead(orm, opened.id, buyer.accountId, { now: mmin(13) });
expect(
  "read: clears the caller only",
  (await totalUnread(orm, buyer.accountId)) === 0 &&
    (await totalUnread(orm, carSeller.accountId)) === 1,
);

// Both sides see the same thread, each with their own view of it.
const buyerInbox = await listConversations(orm, buyer.accountId);
const sellerInbox = await listConversations(orm, carSeller.accountId);
const buyerRow = buyerInbox.items.find((r: { id: string }) => r.id === opened.id);
const sellerRow = sellerInbox.items.find((r: { id: string }) => r.id === opened.id);
expect(
  "inbox: one thread, two perspectives",
  buyerRow?.otherId === carSeller.accountId &&
    sellerRow?.otherId === buyer.accountId &&
    buyerRow?.iAmSeller === false &&
    sellerRow?.iAmSeller === true,
);
expect(
  "inbox: carries the ad and the last message",
  buyerRow?.listingTitle === "Nissan Patrol 2020" &&
    buyerRow?.lastMessagePreview === "Free to view tomorrow.",
  buyerRow?.lastMessagePreview,
);
expect(
  "inbox: each side sees its own unread count",
  buyerRow?.unread === 0 && sellerRow?.unread === 1,
);

// A third party is refused on both read and write.
try {
  await listMessages(orm, opened.id, stranger.accountId);
  bad("access: a non-participant cannot read");
} catch (e) {
  ok("access: a non-participant cannot read", (e as Error).message);
}
try {
  await sendMessage(orm, opened.id, stranger.accountId, "hello", { now: mmin(14) });
  bad("access: a non-participant cannot send");
} catch (e) {
  ok("access: a non-participant cannot send", (e as Error).message);
}

// Paging over a long thread.
for (let i = 0; i < 25; i++) {
  await sendMessage(orm, opened.id, buyer.accountId, `follow up ${i}`, { now: mmin(20 + i) });
}
const collected: string[] = [];
let mcursor: string | null = null;
let mpages = 0;
do {
  const page = await listMessages(orm, opened.id, buyer.accountId, {
    limit: 10,
    ...(mcursor ? { cursor: mcursor } : {}),
  });
  collected.push(...page.items.map((m: { id: string }) => m.id));
  mcursor = page.nextCursor;
  mpages++;
} while (mcursor && mpages < 20);
expect(
  "messages: paging returns every one exactly once",
  new Set(collected).size === collected.length && collected.length === 28,
  `${mpages} pages, ${collected.length} messages`,
);

// A seller who turned messages off cannot be reached.
const quietSeller = await verifyOtp(
  orm, "0502220004",
  (await requestOtp(orm, "0502220004", { now: mmin(50), exposeCode: true })).devCode!,
  { now: mmin(51) },
);
const quietAd = await createListing(
  orm, quietSeller.accountId,
  { title: "Quiet seller sofa", priceHalalas: 90_000, category: "Furniture", city: "Jeddah" },
  { now: mmin(52) },
);
await orm
  .update(schema.accounts)
  .set({ allowMessages: false })
  .where(eq(schema.accounts.id, quietSeller.accountId));
try {
  await openConversation(orm, buyer.accountId, quietAd.id, { now: mmin(53) });
  bad("consent: messages off means unreachable");
} catch (e) {
  ok("consent: messages off means unreachable", (e as Error).message);
}

// Blocking closes both opening a thread and sending in an existing one.
const blockedAd = await createListing(
  orm, carSeller.accountId,
  { title: "Second car for sale", priceHalalas: 5_000_000, category: "Cars", city: "Riyadh" },
  { now: mmin(60) },
);
await blockAccount(orm, carSeller.accountId, stranger.accountId);
try {
  await openConversation(orm, stranger.accountId, blockedAd.id, { now: mmin(61) });
  bad("block: cannot open a thread with someone who blocked you");
} catch (e) {
  ok("block: cannot open a thread with someone who blocked you", (e as Error).message);
}

await blockAccount(orm, carSeller.accountId, buyer.accountId);
try {
  await sendMessage(orm, opened.id, buyer.accountId, "hello again", { now: mmin(62) });
  bad("block: cannot send inside an existing thread");
} catch (e) {
  ok("block: cannot send inside an existing thread", (e as Error).message);
}
await unblockAccount(orm, carSeller.accountId, buyer.accountId);

// Taking the ad down closes the thread to new messages but keeps the history.
await removeListing(orm, blockedAd.id, carSeller.accountId);
const removedThread = await openConversation(orm, buyer.accountId, carAd.id, { now: mmin(70) });
await removeListing(orm, carAd.id, carSeller.accountId);
try {
  await sendMessage(orm, removedThread.id, buyer.accountId, "still there?", { now: mmin(71) });
  bad("removed ad: the thread stops accepting messages");
} catch (e) {
  ok("removed ad: the thread stops accepting messages", (e as Error).message);
}
const historyStillThere = await listMessages(orm, removedThread.id, buyer.accountId, { limit: 5 });
expect(
  "removed ad: the history survives",
  historyStillThere.items.length > 0,
  `${historyStillThere.items.length} messages still readable`,
);

// The realtime registry must actually deliver, so this runs on a live thread
// rather than one whose ad was taken down above.
const liveSeller = await verifyOtp(
  orm, "0502220005",
  (await requestOtp(orm, "0502220005", { now: mmin(90), exposeCode: true })).devCode!,
  { now: mmin(91) },
);
const liveAd = await createListing(
  orm, liveSeller.accountId,
  { title: "Live stream fixture bicycle", priceHalalas: 70_000, category: "Other", city: "Riyadh" },
  { now: mmin(92) },
);
const liveThread = await openConversation(orm, buyer.accountId, liveAd.id, { now: mmin(93) });

const received: string[] = [];
const stop = subscribe(liveThread.id, (event) => received.push(event.body));
expect("stream: a listener is registered", listenerCount(liveThread.id) === 1);

await sendMessage(orm, liveThread.id, liveSeller.accountId, "live ping", { now: mmin(94) });
expect(
  "stream: the message reaches the listener",
  received.length === 1 && received[0] === "live ping",
  `${received.length} event(s) delivered`,
);

// A listener on another thread must not receive it.
const otherReceived: string[] = [];
const stopOther = subscribe(opened.id, (event) => otherReceived.push(event.body));
await sendMessage(orm, liveThread.id, buyer.accountId, "second ping", { now: mmin(95) });
expect(
  "stream: events do not leak across threads",
  received.length === 2 && otherReceived.length === 0,
);
stopOther();

stop();
await sendMessage(orm, liveThread.id, liveSeller.accountId, "after unsubscribe", { now: mmin(96) });
expect(
  "stream: unsubscribing stops delivery and frees the slot",
  listenerCount(liveThread.id) === 0 && received.length === 2,
);

/* ============================================================
   Media — photos on ads, videos in the feed.
   ============================================================ */
console.log("\n\x1b[1mMedia\x1b[0m");

const D0 = new Date("2026-04-01T09:00:00Z");
const dmin = (n: number) => new Date(D0.getTime() + n * 60_000);

const photog = await verifyOtp(
  orm, "0503330001",
  (await requestOtp(orm, "0503330001", { now: dmin(0), exposeCode: true })).devCode!,
  { now: dmin(1) },
);
const intruder = await verifyOtp(
  orm, "0503330002",
  (await requestOtp(orm, "0503330002", { now: dmin(2), exposeCode: true })).devCode!,
  { now: dmin(3) },
);

const photoAd = await createListing(
  orm, photog.accountId,
  { title: "Canon EOS R6 with lens", priceHalalas: 850_000, category: "Electronics", city: "Riyadh" },
  { now: dmin(4) },
);

const upload = await createPhotoUploadUrl(
  orm, photoAd.id, photog.accountId,
  { contentType: "image/jpeg", sizeBytes: 2_400_000, filename: "front.jpg" },
  { now: dmin(5) },
);
expect(
  "upload: presigned URL issued with an expiry",
  upload.uploadUrl.startsWith("https://") &&
    upload.key.startsWith(`listing-photos/${photog.accountId}/`) &&
    upload.expiresAt.getTime() === dmin(5).getTime() + 15 * 60_000,
  upload.isPlaceholder ? "placeholder driver (no storage configured)" : "signed",
);

expect(
  "upload: the key is namespaced to its owner",
  !upload.key.includes(intruder.accountId),
  "a leaked key cannot be guessed into another account",
);

try {
  await createPhotoUploadUrl(orm, photoAd.id, photog.accountId, {
    contentType: "application/pdf", sizeBytes: 1000,
  });
  bad("upload: rejects a disallowed content type");
} catch (e) {
  ok("upload: rejects a disallowed content type", (e as Error).message);
}

try {
  await createPhotoUploadUrl(orm, photoAd.id, photog.accountId, {
    contentType: "image/jpeg", sizeBytes: 40 * 1024 * 1024,
  });
  bad("upload: rejects an oversized file");
} catch (e) {
  ok("upload: rejects an oversized file", (e as Error).message);
}

try {
  await createPhotoUploadUrl(orm, photoAd.id, intruder.accountId, {
    contentType: "image/jpeg", sizeBytes: 1000,
  });
  bad("upload: a stranger cannot upload to someone else's ad");
} catch (e) {
  ok("upload: a stranger cannot upload to someone else's ad", (e as Error).message);
}

// Attach four photos and check the positions come out contiguous.
const photoIds: string[] = [];
for (let i = 0; i < 4; i++) {
  const row = await attachPhoto(
    orm, photoAd.id, photog.accountId,
    { key: `listing-photos/${photog.accountId}/p${i}.jpg`, width: 1600, height: 1200 },
    { now: dmin(6 + i) },
  );
  photoIds.push(row.id);
}
const afterAdd = await listPhotos(orm, photoAd.id);
expect(
  "photos: positions are 0..n-1 in order",
  afterAdd.map((p: { position: number }) => p.position).join(",") === "0,1,2,3",
  afterAdd.map((p: { position: number }) => p.position).join(","),
);

// Reorder — the case that trips the unique index if done naively.
const reversed = [...photoIds].reverse();
const afterReorder = await reorderPhotos(orm, photoAd.id, photog.accountId, reversed);
expect(
  "photos: reversing the order succeeds",
  afterReorder.map((p: { id: string }) => p.id).join(",") === reversed.join(","),
);
expect(
  "photos: positions stay unique and contiguous after reorder",
  afterReorder.map((p: { position: number }) => p.position).join(",") === "0,1,2,3",
);

// A swap of just two is the tightest case for the unique index.
const swapped = [reversed[1]!, reversed[0]!, reversed[2]!, reversed[3]!];
const afterSwap = await reorderPhotos(orm, photoAd.id, photog.accountId, swapped);
expect(
  "photos: swapping two adjacent photos succeeds",
  afterSwap.map((p: { id: string }) => p.id).join(",") === swapped.join(","),
);

try {
  await reorderPhotos(orm, photoAd.id, photog.accountId, [photoIds[0]!]);
  bad("photos: a partial order is refused");
} catch (e) {
  ok("photos: a partial order is refused", (e as Error).message);
}

try {
  await reorderPhotos(orm, photoAd.id, intruder.accountId, swapped);
  bad("photos: a stranger cannot reorder");
} catch (e) {
  ok("photos: a stranger cannot reorder", (e as Error).message);
}

// Deleting from the middle must close the gap.
const afterDelete = await deletePhoto(orm, photoAd.id, photog.accountId, swapped[1]!);
expect(
  "photos: deleting closes the gap",
  afterDelete.length === 3 &&
    afterDelete.map((p: { position: number }) => p.position).join(",") === "0,1,2",
  afterDelete.map((p: { position: number }) => p.position).join(","),
);

try {
  await deletePhoto(orm, photoAd.id, intruder.accountId, afterDelete[0]!.id);
  bad("photos: a stranger cannot delete");
} catch (e) {
  ok("photos: a stranger cannot delete", (e as Error).message);
}

// The cap.
for (let i = 0; i < 7; i++) {
  await attachPhoto(orm, photoAd.id, photog.accountId, {
    key: `listing-photos/${photog.accountId}/extra${i}.jpg`,
  });
}
try {
  await attachPhoto(orm, photoAd.id, photog.accountId, {
    key: `listing-photos/${photog.accountId}/eleventh.jpg`,
  });
  bad("photos: the ten-photo cap holds");
} catch (e) {
  ok("photos: the ten-photo cap holds", (e as Error).message);
}

/* ---------- videos ---------- */

const made = await createVideoUploadUrl(
  orm, photog.accountId,
  { contentType: "video/mp4", sizeBytes: 18_000_000, caption: "Camera walkthrough" },
  { now: dmin(30) },
);
expect("video: created as UPLOADING", made.video.status === "UPLOADING");

try {
  await createVideoUploadUrl(orm, photog.accountId, { contentType: "video/avi", sizeBytes: 100 });
  bad("video: rejects a disallowed container");
} catch (e) {
  ok("video: rejects a disallowed container", (e as Error).message);
}

let seamCalledWith: string | null = null;
const confirmed = await confirmVideoUpload(
  orm, made.video.id, photog.accountId, made.upload.key,
  { now: dmin(31), onVideoUploaded: async (_id, key) => { seamCalledWith = key; } },
);
expect(
  "video: confirming moves it to PROCESSING and calls the transcoder seam",
  confirmed.status === "PROCESSING" && seamCalledWith === made.upload.key,
);

try {
  await confirmVideoUpload(orm, made.video.id, photog.accountId, made.upload.key, { now: dmin(32) });
  bad("video: cannot be confirmed twice");
} catch (e) {
  ok("video: cannot be confirmed twice", (e as Error).message);
}

const feedBeforeReady = await getFeed(orm, { limit: 10 });
expect(
  "feed: a PROCESSING video is not shown",
  !feedBeforeReady.items.some((v: { id: string }) => v.id === made.video.id),
  "not playable, so not in the feed",
);

const ready = await markVideoReady(orm, made.video.id, {
  playbackUrl: "https://cdn.example.com/v/abc.m3u8",
  thumbnailUrl: "https://cdn.example.com/v/abc.jpg",
  durationMs: 24_000,
  width: 1080,
  height: 1920,
});
expect("video: the transcoder callback flips it to READY", ready.status === "READY");

// Tag two ads, checking that position 0 is the pill.
const secondAd = await createListing(
  orm, photog.accountId,
  { title: "Camera bag", priceHalalas: 45_000, category: "Electronics", city: "Riyadh" },
  { now: dmin(35) },
);
await setVideoListings(orm, made.video.id, photog.accountId, [photoAd.id, secondAd.id]);

const readyFeed = await getFeed(orm, { limit: 10 });
const feedItem = readyFeed.items.find((v: { id: string }) => v.id === made.video.id) as
  | { listings: { id: string; title: string }[]; sellerHandle: string }
  | undefined;
expect(
  "feed: a READY video appears with its ads in order",
  feedItem?.listings.length === 2 && feedItem.listings[0]!.id === photoAd.id,
  feedItem?.listings.map((l) => l.title).join(" then "),
);

try {
  await setVideoListings(orm, made.video.id, intruder.accountId, [photoAd.id]);
  bad("video: a stranger cannot tag ads on it");
} catch (e) {
  ok("video: a stranger cannot tag ads on it", (e as Error).message);
}

const otherAd = await createListing(
  orm, intruder.accountId,
  { title: "Not my ad", priceHalalas: 10_000, category: "Other", city: "Jeddah" },
  { now: dmin(36) },
);
try {
  await setVideoListings(orm, made.video.id, photog.accountId, [otherAd.id]);
  bad("video: cannot tag someone else's ad");
} catch (e) {
  ok("video: cannot tag someone else's ad", (e as Error).message);
}

// The feed honours blocks the same way browse does.
const feedViewer = await verifyOtp(
  orm, "0503330003",
  (await requestOtp(orm, "0503330003", { now: dmin(40), exposeCode: true })).devCode!,
  { now: dmin(41) },
);
const beforeFeedBlock = await getFeed(orm, { limit: 10, viewerId: feedViewer.accountId });
const sawVideo = beforeFeedBlock.items.some((v: { id: string }) => v.id === made.video.id);
await blockAccount(orm, feedViewer.accountId, photog.accountId);
const afterFeedBlock = await getFeed(orm, { limit: 10, viewerId: feedViewer.accountId });
expect(
  "feed: a blocked seller disappears from it too",
  sawVideo && !afterFeedBlock.items.some((v: { id: string }) => v.id === made.video.id),
);
await unblockAccount(orm, feedViewer.accountId, photog.accountId);

// A sold ad drops off the video without removing the video.
await setListingSold(orm, secondAd.id, photog.accountId, true);
const feedAfterSold = await getFeed(orm, { limit: 10 });
const itemAfterSold = feedAfterSold.items.find((v: { id: string }) => v.id === made.video.id) as
  | { listings: unknown[] }
  | undefined;
expect(
  "feed: a sold ad drops off the video",
  itemAfterSold?.listings.length === 1,
  `${itemAfterSold?.listings.length} ad still tagged`,
);





/* ============================================================
   Push registration — the device side of notifications.
   ============================================================ */
console.log("\n\x1b[1mPush\x1b[0m");

const P0 = new Date("2026-06-01T09:00:00Z");
const pmin = (n: number) => new Date(P0.getTime() + n * 60_000);

async function makeAccount(phone: string, at: Date) {
  const req = await requestOtp(orm, phone, { now: at, exposeCode: true });
  return verifyOtp(orm, phone, req.devCode!, { now: new Date(at.getTime() + 30_000) });
}

const pusher = await makeAccount("0561110001", pmin(0));
const pushee = await makeAccount("0561110002", pmin(2));

await registerPushToken(orm, pusher.accountId, "device-token-aaaaaaaaaa", "android", {
  now: pmin(3),
});
expect(
  "push: a device registers",
  (await listPushTokens(orm, pusher.accountId)).length === 1,
);

// The app re-registers on every launch; that must not accumulate rows.
await registerPushToken(orm, pusher.accountId, "device-token-aaaaaaaaaa", "android", {
  now: pmin(4),
});
expect(
  "push: re-registering the same device does not duplicate it",
  (await listPushTokens(orm, pusher.accountId)).length === 1,
);

// Two people, one phone. The token must follow the account that owns it now,
// or the second person receives the first person's notifications.
await registerPushToken(orm, pushee.accountId, "device-token-aaaaaaaaaa", "android", {
  now: pmin(5),
});
expect(
  "push: a shared device moves to whoever signed in last",
  (await listPushTokens(orm, pusher.accountId)).length === 0 &&
    (await listPushTokens(orm, pushee.accountId)).length === 1,
);

const notMine = await unregisterPushToken(orm, pusher.accountId, "device-token-aaaaaaaaaa");
expect(
  "push: you cannot unregister someone else's device",
  notMine.removed === 0 && (await listPushTokens(orm, pushee.accountId)).length === 1,
);

try {
  await registerPushToken(orm, pushee.accountId, "device-token-bbbbbbbbbb", "windows-phone", {
    now: pmin(6),
  });
  bad("push: an unknown platform is rejected");
} catch (e) {
  ok("push: an unknown platform is rejected", (e as Error).message);
}

try {
  await registerPushToken(orm, pushee.accountId, "   ", "android", { now: pmin(6) });
  bad("push: an empty token is rejected");
} catch (e) {
  ok("push: an empty token is rejected", (e as Error).message);
}

// The whole point of the configuration check: with no FCM credentials the send
// path is inert rather than broken. That is the state of CI and of every
// developer machine, so it is the state that has to be proven safe.
expect("push: no credentials configured in the verifier", !isPushConfigured());

const pushAd = await createListing(
  orm, pushee.accountId,
  { title: "Prayer rug", priceHalalas: 12_000, category: "Home", city: "Riyadh" },
  { now: pmin(7) },
);
const pushThread = await openConversation(orm, pusher.accountId, pushAd.id, { now: pmin(8) });

const dispatched = await dispatchMessagePush(orm, {
  conversationId: pushThread.id,
  senderId: pusher.accountId,
  messageId: "00000000-0000-0000-0000-000000000000",
  body: "Still available?",
});
expect(
  "push: with no credentials the send is a clean skip, not an error",
  dispatched.skipped && dispatched.sent === 0,
);

// And the same through the real send path, which fires it and does not wait.
const pushedMessage = await sendMessage(orm, pushThread.id, pusher.accountId, "Still available?", {
  now: pmin(9),
});
expect(
  "push: sending a message still succeeds with push disabled",
  pushedMessage.body === "Still available?",
);

await registerPushToken(orm, pushee.accountId, "device-token-cccccccccc", "ios", {
  now: pmin(10),
});
await requestAccountDeletion(orm, pushee.accountId, pmin(11));
expect(
  "push: asking to be deleted silences every device immediately",
  (await listPushTokens(orm, pushee.accountId)).length === 0,
  "without waiting for the grace period",
);

const cleared = await clearPushTokensForAccount(orm, pusher.accountId);
expect("push: clearing an account with no devices is not an error", cleared.removed === 0);

/* ============================================================
   Housekeeping — the scheduled sweep. Time is passed in, so a
   thirty-day grace period is tested in milliseconds.
   ============================================================ */
console.log("\n\x1b[1mHousekeeping\x1b[0m");

const H0 = new Date("2026-07-01T03:00:00Z");
const days = (n: number) => new Date(H0.getTime() + n * 86_400_000);

const doomed = await makeAccount("0562220001", H0);
const reprieved = await makeAccount("0562220002", new Date(H0.getTime() + 60_000));
const untouched = await makeAccount("0562220003", new Date(H0.getTime() + 120_000));

const doomedAd = await createListing(
  orm, doomed.accountId,
  { title: "Old bicycle", priceHalalas: 30_000, category: "Sports", city: "Jeddah" },
  { now: days(0) },
);
await registerPushToken(orm, doomed.accountId, "doomed-device-token-1", "android", {
  now: days(0),
});

await requestAccountDeletion(orm, doomed.accountId, days(0));
// Asked 29 days later, so it is still inside its grace period at day 31.
await requestAccountDeletion(orm, reprieved.accountId, days(29));

// Earlier sections delete accounts of their own, so the interesting property is
// not the absolute count — it is that the number a monitor would report is
// exactly the number the sweep then removes.
const backlogBefore = await maintenanceBacklog(orm, { now: days(31) });
const purgedAccounts = await purgeDeletedAccounts(orm, { now: days(31) });
expect(
  "housekeeping: the backlog predicts exactly what the sweep removes",
  purgedAccounts === backlogBefore.accounts && purgedAccounts > 0,
  `${backlogBefore.accounts} due, ${purgedAccounts} removed`,
);

const doomedGone = await db.query<{ count: number }>(
  `SELECT count(*)::int AS count FROM accounts WHERE id = $1`,
  [doomed.accountId],
);
expect(
  "housekeeping: the account past its grace period is hard-deleted",
  doomedGone.rows[0]!.count === 0,
);

const stillThere = await db.query<{ count: number }>(
  `SELECT count(*)::int AS count FROM accounts WHERE id IN ($1, $2)`,
  [reprieved.accountId, untouched.accountId],
);
expect(
  "housekeeping: an account still inside its grace period survives",
  stillThere.rows[0]!.count === 2,
);

// The grace period is the last point at which anything is recoverable. Past it
// the cascade has to be total, or a deleted person's ads outlive them.
const leftovers = await db.query<{ listings: number; tokens: number; sessions: number }>(
  `SELECT (SELECT count(*) FROM listings WHERE id = $1)::int            AS listings,
          (SELECT count(*) FROM push_tokens WHERE account_id = $2)::int AS tokens,
          (SELECT count(*) FROM sessions WHERE account_id = $2)::int    AS sessions`,
  [doomedAd.id, doomed.accountId],
);
expect(
  "housekeeping: purging an account takes its ads, devices and sessions with it",
  leftovers.rows[0]!.listings === 0 &&
    leftovers.rows[0]!.tokens === 0 &&
    leftovers.rows[0]!.sessions === 0,
);

// A live code and expired ones, to prove the sweep can tell them apart.
await requestOtp(orm, "0563330001", { now: days(31), exposeCode: true });
const liveCodes = await db.query<{ count: number }>(
  `SELECT count(*)::int AS count FROM otp_codes WHERE expires_at > $1`,
  [days(31)],
);
const deletedCodes = await purgeExpiredOtpCodes(orm, { now: days(31) });
const survivingCodes = await db.query<{ count: number }>(
  `SELECT count(*)::int AS count FROM otp_codes`,
);
expect(
  "housekeeping: expired login codes go and unexpired ones stay",
  deletedCodes > 0 && survivingCodes.rows[0]!.count === liveCodes.rows[0]!.count,
  `${deletedCodes} deleted, ${survivingCodes.rows[0]!.count} still live`,
);

// A session revoked moments ago is kept on purpose: a refresh token presented
// after rotation is how token theft shows up, and that needs the row.
const fresh = await makeAccount("0564440001", days(31));
await logout(orm, fresh.refreshToken, days(31));
const otherDead = await purgeDeadSessions(orm, { now: days(32) });
const revokedStill = await db.query<{ count: number }>(
  `SELECT count(*)::int AS count FROM sessions WHERE account_id = $1`,
  [fresh.accountId],
);
expect(
  "housekeeping: a just-revoked session is kept as evidence",
  revokedStill.rows[0]!.count === 1,
  `${otherDead} other dead session(s) swept`,
);

await purgeDeadSessions(orm, { now: days(45) });
const revokedGone = await db.query<{ count: number }>(
  `SELECT count(*)::int AS count FROM sessions WHERE account_id = $1`,
  [fresh.accountId],
);
expect(
  "housekeeping: past the evidence window it is swept",
  revokedGone.rows[0]!.count === 0,
);

// Idempotence, which is what lets a scheduler retry on a timeout without
// worrying about what a duplicate run would do: two runs at the same instant,
// and the second must find nothing left.
const firstRun = await runMaintenance(orm, { now: days(45) });
const secondRun = await runMaintenance(orm, { now: days(45) });
expect(
  "housekeeping: a second run at the same instant removes nothing",
  secondRun.accountsPurged === 0 &&
    secondRun.otpCodesDeleted === 0 &&
    secondRun.sessionsDeleted === 0,
  `first run removed ${firstRun.accountsPurged} account(s), ` +
    `${firstRun.otpCodesDeleted} code(s), ${firstRun.sessionsDeleted} session(s)`,
);

const backlogAfter = await maintenanceBacklog(orm, { now: days(45) });
expect(
  "housekeeping: the backlog is empty once the sweep has run",
  backlogAfter.accounts === 0 && backlogAfter.otpCodes === 0 && backlogAfter.sessions === 0,
);


console.log(
  `\n[1m${failed === 0 ? "[32mAll checks passed" : "[31mFAILURES"}[0m  ${passed} passed, ${failed} failed\n`,
);
await db.close();
process.exit(failed === 0 ? 0 : 1);
