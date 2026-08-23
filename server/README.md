# SmoothScroll — server

API and database for the SmoothScroll video classifieds app.

**Stack:** TypeScript · [Hono](https://hono.dev) · [Drizzle](https://orm.drizzle.team) · Postgres on [Neon](https://neon.tech)

---

## Status

| Phase | What | State |
| --- | --- | --- |
| 1 | Database schema + migrations | **done, verified** |
| 2 | Auth — phone OTP + JWT | **done, verified** |
| 3 | Listings API — CRUD, search, paging | **done, verified** |
| 4 | Messaging API + realtime | **done, verified** |
| 5 | Media uploads — photos, video | **done, verified** |
| 6 | Wire the Android app to the API | next |
| 7 | Deploy + push notifications | pending |

---

## Getting the database running

You need a Neon project. It is free to start and takes about a minute.

1. Sign up at **[console.neon.tech](https://console.neon.tech)** and create a project.
2. Open **Connection Details** and copy the **pooled** connection string. It looks like:
   ```
   postgres://user:pass@ep-something-pooler.eu-central-1.aws.neon.tech/neondb?sslmode=require
   ```
3. From this directory:
   ```bash
   cp .env.example .env      # then paste your string into DATABASE_URL
   npm install
   npm run db:migrate
   ```

That creates all 16 tables, 43 indexes and every constraint.

> Use the **pooled** string (the host contains `-pooler`), not the direct one.
> Serverless functions open and close connections constantly; the pooler is what
> stops Neon running out of them.

---

## Commands

| Command | What it does |
| --- | --- |
| `npm run db:verify` | Runs the schema and the app's real queries against an in-process Postgres. **No database or network needed.** |
| `npm run db:migrate` | Applies pending migrations to Neon. Safe to re-run. |
| `npm run db:generate` | Regenerates SQL after editing `src/db/schema.ts`. |
| `npm run typecheck` | Type-checks without emitting. |
| `npm run dev` | Runs the API locally with reload. |

### `db:verify` — why it exists

The environment this was built in cannot reach Neon, so the schema is verified
against **PGlite** — real PostgreSQL compiled to WebAssembly. It applies the
migrations, seeds a slice of the app's data, runs the queries the client actually
makes, and then proves each constraint rejects what it should:

```
Schema        ✓ PostgreSQL 18.3   ✓ 16 tables   ✓ 43 indexes
Queries       ✓ browse  ✓ full-text search  ✓ city filter
              ✓ contact visibility  ✓ one thread per ad  ✓ feed tags
Constraints   ✓ no calls without a number   ✓ no negative price
              ✓ no self-follow  ✓ no self-message  ✓ no empty message
              ✓ case-insensitive handles  ✓ no duplicate reports
Auth          ✓ phone normalisation  ✓ codes stored hashed  ✓ no replay
              ✓ wrong-code lockout  ✓ expiry  ✓ per-number rate limit
              ✓ refresh rotation  ✓ logout  ✓ deletion + grace period
Listings      ✓ keyset paging, no gaps or repeats even mid-bump
              ✓ combined filters  ✓ phone gated on consent  ✓ ownership
              ✓ sold/relist  ✓ bump cooldown  ✓ view counting
              ✓ follower counter stays honest  ✓ blocking cuts both ways
Messaging     ✓ thread reuse  ✓ unread moves for the recipient only
              ✓ non-participants refused  ✓ consent and blocks respected
              ✓ history survives a removed ad  ✓ live delivery, no leaks
Media         ✓ presigned uploads  ✓ type and size limits  ✓ 10-photo cap
              ✓ positions contiguous after add, reorder, swap, delete
              ✓ owner-only on every mutation  ✓ UPLOADING → PROCESSING → READY
              ✓ feed hides unready video, sold ads and blocked sellers
All checks passed  111 passed, 0 failed
```

The auth checks drive the **same service functions the API calls**, not a copy:
`requestOtp`, `verifyOtp`, `refreshSession` and friends take the database and
the current time as arguments, so the verifier can point them at PGlite and
fast-forward the clock to test expiry.

It runs in CI on every push, so a broken migration is caught before it ever
reaches a real database.

---

## Auth

Sign-in is a phone number and a six-digit SMS code. There are no passwords.

| Endpoint | Purpose |
| --- | --- |
| `POST /auth/otp/request` | Send a code. Rate limited to 3 per number per 15 min. |
| `POST /auth/otp/verify` | Exchange the code for tokens. Creates the account on first use. |
| `POST /auth/refresh` | Rotate the refresh token, get a new access token. |
| `POST /auth/logout` | Revoke one session. |
| `POST /auth/logout-everywhere` | Revoke all sessions for the account. |
| `GET /account` | The signed-in profile. |
| `DELETE /account?confirm=true` | In-app deletion, as Google Play requires. |

**No SMS provider is wired up yet.** Outside production the code is written to
the log and returned in the response as `devCode`, so the Android app can be
built and tested end to end today. `SmsSender` in `src/services/auth.ts` is the
one function to implement — Unifonic or Twilio are a single HTTP call from
there.

A few decisions worth knowing:

- **Codes are HMAC'd, not hashed.** Six digits fall to a rainbow table in
  seconds, so the code is HMAC'd with the server secret and mixed with the phone
  number. A database dump on its own yields nothing.
- **The attempt counter increments before the comparison**, so crashing
  mid-check cannot be used to retry for free. Five wrong tries burn the code.
- **Refresh tokens rotate.** Exchanging one revokes it. A stolen token is
  usable at most once, and the theft surfaces as the real user being signed out.
- **`auth_phone` is never returned by the API**, not even to its owner. The
  number buyers see is a separate column that only appears when `allow_calls` is
  on.
- **Deletion is soft, with a 30-day grace period.** Every session is revoked
  immediately so the app stops working at once, but signing in again during the
  window brings the account back.

---

## Listings

| Endpoint | Purpose |
| --- | --- |
| `GET /listings` | Browse. Filters: `q`, `city`, `category`, `condition`, `minPrice`, `maxPrice`, `sellerId`, `limit`, `cursor`. |
| `GET /listings/:id` | One ad with photos and seller. Counts a view. |
| `POST /listings` | Post an ad. |
| `PATCH /listings/:id` | Edit. Owner only. |
| `POST /listings/:id/sold` | Toggle sold. Owner only. |
| `DELETE /listings/:id` | Remove. Owner only. |
| `POST /listings/:id/bump` | Move back to the top. Once per day. |
| `POST /sellers/:id/follow` · `DELETE` | Follow and unfollow. |
| `POST /reports` · `POST /blocks` · `DELETE /blocks/:id` | Safety. |

**Paging is keyset, not `OFFSET`.** The cursor encodes `(bumped_at, id)`. `OFFSET`
gets slower as the table grows, and — worse for a feed people scroll — silently
skips or repeats ads when one is bumped between pages. The verifier tests exactly
that case: it pages through, bumps an ad mid-scroll, and asserts no row repeats.

**A seller's number leaves the database through one `CASE` expression** and
nowhere else, so a route that forgets to check cannot leak it.

**Blocking cuts both ways.** If you block someone, their ads vanish from your
browse *and* yours vanish from theirs — otherwise a blocked person can tell they
were blocked by watching the ads stay visible.

**Removal is a status change, not a delete.** Conversations reference the ad, and
a buyer's inbox should not lose its subject line because a seller tidied up.

---

## Messaging

| Endpoint | Purpose |
| --- | --- |
| `GET /conversations` | Inbox, keyset paged. Works from either side. |
| `POST /conversations` | `{ listingId }` — opens or reuses the thread. |
| `GET /conversations/unread-count` | Total unread, for the badge. |
| `GET /conversations/:id/messages` | Keyset paged, newest first. |
| `POST /conversations/:id/messages` | Send. |
| `POST /conversations/:id/read` | Clear your own unread count. |
| `GET /conversations/:id/stream` | Live messages over Server-Sent Events. |

**One thread per (ad, buyer)**, enforced by a unique index rather than a
check-then-insert, so opening a conversation is idempotent even under a race.

**Unread counters move in the same statement as the message**, using a `CASE` on
which side sent it, so they cannot drift from the messages that caused them.
Reading clears only the caller's side.

**A non-participant gets the same "no longer exists" message as a missing
thread**, so conversation ids cannot be probed to discover what exists.

**Taking an ad down freezes the thread but keeps the history** — no more
messages, but both sides can still read what was said.

### Why SSE and not WebSocket

The app only needs server-to-client push here; messages are sent over ordinary
POSTs. SSE is plain HTTP, so it passes through proxies and serverless hosts that
will not do an upgrade handshake, and both browsers and OkHttp reconnect on
their own. A socket's duplex channel would buy nothing.

> **Single-instance only.** The subscriber registry in `src/lib/events.ts` lives
> in one process's memory: with two instances, a message sent on A will not reach
> a listener on B. Before scaling out, swap that file's `publish`/`subscribe` for
> Postgres `LISTEN`/`NOTIFY` or Redis pub/sub. Nothing outside it changes — which
> is why it is isolated there.

---

## Media

| Endpoint | Purpose |
| --- | --- |
| `POST /listings/:id/photos/upload-url` | Presigned PUT. Owner only. |
| `POST /listings/:id/photos` | Confirm the upload, attach at the next position. |
| `PATCH /listings/:id/photos/order` | Reorder. |
| `DELETE /listings/:id/photos/:photoId` | Remove and close the gap. |
| `POST /videos/upload-url` · `/:id/confirm` | Upload a feed video. |
| `POST /videos/:id/ready` | Transcoder callback. Shared-secret header, not a user token. |
| `POST /videos/:id/listings` | Tag your ads. Position 0 is the pill. |
| `GET /feed` | Ready videos newest first, each with its ordered ads. |

**The phone uploads straight to storage.** The API only issues a presigned URL
and records the key afterwards, so photos and video never pass through this
server — which is what keeps it small and cheap to run.

**SigV4 is implemented in `src/lib/storage.ts` with `node:crypto`**, not the AWS
SDK, which is tens of megabytes for what is one signature. It is S3-compatible,
so Cloudflare R2, Backblaze B2 and S3 all work by changing environment variables
only. With none set, a clearly marked placeholder driver is used so local
development and the verifier need no cloud account.

**Object keys are namespaced by owner**, so a leaked key cannot be edited into
someone else's namespace.

### Why reordering photos takes two statements

The obvious single `UPDATE … FROM (VALUES …)` fails. The unique index on
`(listing_id, position)` is checked row by row, so any statement that swaps two
positions trips it mid-flight. That was verified against Postgres directly
rather than assumed:

```
SINGLE STATEMENT: FAILED -> duplicate key value violates unique constraint
```

So the rows are parked in negative positions first — which no valid row ever
uses — and then written to their final values. The two statements are not in a
transaction because the Neon HTTP driver cannot hold one open; the window is
tiny and owner-only, and a crash between them leaves every position negative,
which a repair pass at the top of the same function corrects on the next call.

---

## Schema notes

Three decisions carry over from the Kotlin client and are load-bearing.

**Money is an integer.** `price_halalas` is a `bigint` of minor units, never a
float. The client already does this, so there is no conversion layer and no
rounding drift. `SAR 185,000` is stored as `18500000`.

**Every account is a seller.** There is no separate merchant table — the person
browsing and the person posting are the same row in `accounts`. This mirrors the
app, where a profile and a shop are the same page.

**A published phone number is a deliberate act.** `auth_phone` is the login
identity and is never returned by the API. `public_phone` is what buyers see, and
only when `allow_calls` is true. A database constraint makes the invalid state
unrepresentable:

```sql
CHECK (NOT allow_calls OR public_phone IS NOT NULL)
```

So an account can never end up advertising a Call button with nothing behind it.

### Full-text search

`listings.search` is a **generated** `tsvector` over title, description, city and
category, with a GIN index. Being generated means it can never drift out of sync
with the row — there is no trigger to forget.

It uses the `simple` dictionary rather than `english` on purpose: the corpus is
mixed Arabic and English, and the English stemmer mangles Arabic. `simple` treats
both alike. If search quality needs more later, the upgrade is `pg_trgm` for
fuzzy matching, not a different stemmer.

### Tables

| Group | Tables |
| --- | --- |
| Identity | `accounts` · `otp_codes` · `sessions` · `push_tokens` |
| Marketplace | `listings` · `listing_photos` · `follows` |
| Feed | `videos` · `video_listings` |
| Messaging | `conversations` · `messages` |
| Live | `live_streams` · `live_stream_listings` · `live_chat_messages` |
| Safety | `reports` · `blocks` |

`reports` and `blocks` are not optional extras — Google Play requires report and
block for any app carrying user content, and it is one of the more common
rejection reasons.

---

## Cost

Neon's free tier covers development comfortably. The paid tiers start around
\$19/month and scale with storage and compute hours; a marketplace of this shape
stays cheap until it has real traffic.

The expensive piece later is **not** the database — it is live video. Ingest and
delivery through a provider like Mux, LiveKit or Agora is billed per streaming
hour and per viewer hour, and that bill starts the day the feature ships whether
anyone watches or not.
