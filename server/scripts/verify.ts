import { PGlite } from "@electric-sql/pglite";
import { drizzle } from "drizzle-orm/pglite";
import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";

// Set before anything reads it: the auth service signs tokens with this.
process.env.JWT_SECRET ??= "verify-only-secret-not-used-anywhere-real";

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


console.log(
  `\n[1m${failed === 0 ? "[32mAll checks passed" : "[31mFAILURES"}[0m  ${passed} passed, ${failed} failed\n`,
);
await db.close();
process.exit(failed === 0 ? 0 : 1);
