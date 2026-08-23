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
| 6 | Wire the Android app to the API | **done, verified** |
| 7 | Deploy, push notifications, housekeeping | **done, verified** |

---

## From nothing to a working app

Everything below is required except where marked. Roughly twenty minutes.

### 1. A database

```bash
cd server
cp .env.example .env
```

Sign up at **[console.neon.tech](https://console.neon.tech)**, create a project,
open **Connection Details** and copy the **pooled** string — the host contains
`-pooler`. Paste it into `DATABASE_URL` in `.env`, then generate a signing key:

```bash
echo "JWT_SECRET=\"$(openssl rand -base64 32)\"" >> .env
npm install
npm run db:migrate      # 16 tables, 43 indexes, every constraint
```

> Use the **pooled** string, not the direct one. Connections here are opened and
> closed constantly; the pooler is what stops Neon running out of them.

Check it before going further — this needs no database and no network:

```bash
npm run db:verify       # 131 checks
```

### 2. The API, locally

```bash
npm run dev             # http://localhost:8787
curl localhost:8787/health
```

### 3. The Android app against it

Open the project root in Android Studio and run the **debug** build. It already
points at `http://10.0.2.2:8787`, which is how the emulator addresses your
laptop — nothing to configure.

On a **physical phone** on the same Wi-Fi, use your machine's LAN address:

```kotlin
// app/build.gradle.kts, defaultConfig
buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.20:8787\"")
```

### 4. Sign in, post an ad, message a seller

There is no SMS provider yet, and you do not need one. Outside production the
server returns the code in the response and the login screen displays it.

1. **Account → Sign in**, enter any Saudi mobile number (`0512345678`).
2. The six-digit code appears on screen. Enter it. The account is created on
   first use.
3. **Post** — title, price, category, city, photos. It appears in Browse
   immediately.
4. Sign in as a second number on another emulator, open the ad, and **Message
   the seller**. With both apps open the reply arrives live over SSE.
5. To try calling: **Account → Contact settings**, publish a phone number, turn
   Calls on. A Call button appears on your ads and opens the dialler with the
   number filled in. Nothing is dialled without confirmation, and the app never
   asks for the call permission.

> The number a buyer sees is one the seller typed in and chose to publish. There
> is no masking, no proxy number and no relay — publishing is a deliberate act,
> and a database constraint makes "Call button with no number" impossible.

### 5. Deploy it

```bash
fly launch --no-deploy --copy-config    # rename the app in fly.toml first
fly secrets set DATABASE_URL="postgres://…" \
                JWT_SECRET="$(openssl rand -base64 32)" \
                MAINTENANCE_SECRET="$(openssl rand -hex 32)"
fly deploy
curl https://your-api.fly.dev/ready
```

Then point the release build at it:

```kotlin
// app/build.gradle.kts, buildTypes.release
buildConfigField("String", "API_BASE_URL", "\"https://your-api.fly.dev\"")
```

### 6. Optional, in the order they start mattering

| What | Why | Where |
| --- | --- | --- |
| **SMS provider** | Real users cannot see a `devCode`. Unifonic or Twilio. | `SmsSender`, `src/services/auth.ts` |
| **Object storage** | Photos and video need somewhere to live. R2 is cheapest. | `S3_*` in `.env` |
| **Push notifications** | Replies arrive when the app is closed. | *Push notifications* below |
| **Maintenance schedule** | Deletion requests are otherwise never carried out. | *Housekeeping* below |
| **Video transcoding** | Uploaded video stays `PROCESSING` and never reaches the feed. | `onVideoUploaded`, `src/services/media.ts` |

## Commands

| Command | What it does |
| --- | --- |
| `npm run db:verify` | Runs the schema and the app's real queries against an in-process Postgres. **No database or network needed.** |
| `npm run db:migrate` | Applies pending migrations to Neon. Safe to re-run. |
| `npm run db:generate` | Regenerates SQL after editing `src/db/schema.ts`. |
| `npm run typecheck` | Type-checks without emitting. |
| `npm run dev` | Runs the API locally with reload. |
| `npm run build` | Compiles to `dist/`. |
| `npm start` | Runs the compiled build (`dist/src/index.js`). |
| `npm run db:migrate:prod` | Migrations from the compiled build, for the deploy release step. |

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
Push          ✓ re-registering a device does not duplicate it
              ✓ a shared phone moves the token to whoever signed in last
              ✓ you cannot unregister someone else's device
              ✓ with no FCM credentials the send is a skip, not an error
              ✓ deletion silences every device at once
Housekeeping  ✓ the backlog predicts exactly what the sweep removes
              ✓ an account inside its grace period survives
              ✓ purging one takes its ads, devices and sessions with it
              ✓ expired codes go, unexpired stay
              ✓ a just-revoked session is kept as evidence
              ✓ a second run at the same instant removes nothing
All checks passed  131 passed, 0 failed
```

The auth checks drive the **same service functions the API calls**, not a copy:
`requestOtp`, `verifyOtp`, `refreshSession` and friends take the database and
the current time as arguments, so the verifier can point them at PGlite and
fast-forward the clock to test expiry.

It runs in CI on every push, so a broken migration is caught before it ever
reaches a real database.

The push and housekeeping checks matter more than their line count suggests:
both features are ones you would otherwise only find out were broken in
production, months apart — a notification that never arrives, or a deletion that
silently never happened.

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

## Push notifications

| Endpoint | Purpose |
| --- | --- |
| `POST /account/push-tokens` | Register this device. Body `{ token, platform }`. |
| `DELETE /account/push-tokens` | Unregister one device. Body `{ token }`. |
| `DELETE /account/push-tokens/all` | Unregister every device for the account. |

One notification is sent: a new message on one of your ads. It fires from the
same place the SSE event is published, so the two cannot get out of step.

**It is not awaited.** FCM is a third party over the network, and whoever sent
the message should not wait on it — the message is already committed and already
delivered to anyone with the thread open.

**With no credentials it is inert, not broken.** `isPushConfigured()` is checked
before any query, so on a server with no FCM service account — CI, every
developer machine, the verifier — a send costs one boolean and touches neither
the database nor the network. The registration endpoint still succeeds and
returns `deliveryEnabled: false`, so the app can say notifications are off
rather than waiting for something that is never coming.

**A dead token is deleted, not retried.** FCM answers `UNREGISTERED` for an
uninstalled app; that row is removed rather than left to waste a request on
every future message.

`firebase-admin` is not used. FCM HTTP v1 is one RS256 signature and two POSTs,
both in `src/lib/push.ts` with `node:crypto` — the SDK is tens of megabytes and
drags in its own gRPC stack. (The legacy server-key API is not an option: Google
switched it off in 2024.)

### Turning it on

1. **[console.firebase.google.com](https://console.firebase.google.com)** →
   create a project → add an Android app with package name
   `com.densitech.scrollsmooth`.
2. Download `google-services.json` into `app/`, and add the plugin — two lines:
   ```kotlin
   // settings.gradle.kts is already fine; in app/build.gradle.kts:
   plugins {
       id("com.google.gms.google-services") version "4.4.2"
   }
   ```
   That file is **not** in this repository on purpose. It is per-project
   configuration belonging to whoever ships the app, and a committed placeholder
   would produce an APK that looks configured and is not. Without it Firebase
   logs `Default FirebaseApp failed to initialize` once at startup, every push
   call becomes a no-op, and the app runs normally.
3. Project settings → Service accounts → **Generate new private key**. Give the
   whole JSON to the server on one line:
   ```bash
   fly secrets set FCM_SERVICE_ACCOUNT_JSON="$(cat service-account.json | tr -d '\n')"
   ```

---

## Housekeeping

| Endpoint | Purpose |
| --- | --- |
| `GET /internal/maintenance` | What a run would remove. Safe to poll. |
| `POST /internal/maintenance` | Run the sweep. |

Three jobs behind one call: hard-delete accounts whose thirty-day grace period
has expired, delete expired login codes, delete dead sessions.

It is **a route driven by an external scheduler**, not an in-process timer. A
timer runs once per instance, so two instances run it twice — and it does not
run at all while the process is asleep, which is exactly what a scale-to-zero
host does at 3am. `.github/workflows/maintenance.yml` drives it nightly; any
other scheduler works the same way, because the endpoint takes a shared secret
rather than a platform token.

```bash
curl -X POST https://your-api.fly.dev/internal/maintenance \
     -H "X-Maintenance-Secret: $MAINTENANCE_SECRET"
```

**An unset secret closes the endpoint rather than opening it.** Treating "no
secret configured" as "no check needed" is how one forgotten environment
variable becomes a public endpoint that deletes accounts.

**Deletion past the grace period is total.** Every foreign key is
`ON DELETE CASCADE`, so the account's listings, photos, messages, devices and
sessions go in the same statement. That is the point of the grace period: up to
it, nothing is lost; past it, nothing is kept.

**A revoked session is kept for a further week.** A refresh token presented
after rotation is how token theft announces itself, and that signal is worth
more than the row costs.

---

## Deploying

`Dockerfile` and `fly.toml` target **Fly.io**. Railway, Render or any container
host works from the same Dockerfile; what does *not* work is a pure edge-function
platform, because this server holds SSE connections open for as long as someone
has a chat thread on screen and those platforms bill and time out per
invocation.

```bash
fly launch --no-deploy --copy-config    # once; rename the app first
fly secrets set DATABASE_URL="postgres://…" JWT_SECRET="$(openssl rand -base64 32)"
fly deploy
```

`fly deploy` runs `node dist/scripts/migrate.js` as a release command before any
new container takes traffic, so a migration that fails fails the deploy instead
of releasing a server the schema does not fit.

Two health endpoints, deliberately different:

- **`/health`** does not touch the database. The platform restarts a container
  whose health check fails, and a check that fails during a brief Neon hiccup
  gets a process killed that would have recovered on its own.
- **`/ready`** does, plus reports whether push is configured. Use it from a
  deploy script that wants to know a release can actually serve.

> **One machine, for now.** `auto_stop_machines` is off and
> `min_machines_running` is 1 because the SSE registry lives in one process's
> memory (see *Why SSE and not WebSocket*), and a machine stopped for idleness
> drops every chat stream it was holding. Before scaling out, move that registry
> to `LISTEN`/`NOTIFY` or Redis — then raise the count and turn auto-stop back on.

Put the Neon project in the region matching `primary_region`. The API talks to
the database on nearly every request, so the hop between them is the one worth
optimising — not the hop from the phone.

---

## Logs

One structured line per request: method, path, status, duration, request id, and
the account id when the caller is signed in. JSON in production because every
log pipeline parses JSON and none parses prose; a short human line otherwise.

```json
{"at":"2026-08-23T15:04:11.427Z","level":"info","requestId":"a1b2…","method":"POST","path":"/conversations","status":201,"durationMs":38,"accountId":"9f3c…"}
```

The request id is echoed back as `X-Request-Id` and repeated on any unhandled
error, so a user quoting the id from their error message is enough to find the
stack.

**What is deliberately absent: the query string, the body, and the headers.**
Those carry phone numbers, OTP codes and bearer tokens, and a log file is the
wrong home for all three — it is copied, shipped to third parties and kept far
longer than the data deserves. The logged path is `c.req.path`, which excludes
the query string *by construction* rather than by a redaction pass someone will
eventually forget to update.

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
