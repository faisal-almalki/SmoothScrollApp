# SmoothScroll

SmoothScroll is a video classifieds app for Android: a TikTok style vertical feed where every
video carries an ad, sellers go live to show what they are selling, and buyers reach the seller
directly by message or by phone. Think حراج, but the listings are videos.

There is no cart and no checkout. Deals happen between the two people, the way classifieds work.

It is built on a high performance Media3 ExoPlayer feed with caching, prefetching and thumbnail
scrubbing, so the marketplace layer never costs you scroll performance.

## Buying

- **Shoppable feed:** every video carries an ad tag. A pill expands over the video a beat after
  playback starts, showing the price and the city; tapping it opens the ad without leaving the feed.
- **Contact the seller:** message them in the app, or call them when they have chosen to publish a
  number. Calls hand the number to the system dialler, so nothing is dialled without you pressing
  the button, and no call permission is requested.
- **Live rooms:** sellers showing their ads on camera, with a running viewer count and chat. The ad
  being discussed sits pinned above the composer with call and view buttons on it.
- **Browse:** search across ads, sellers and cities; filter by category, condition and city; newest
  ads first.
- **Messages:** one thread per ad, so a seller always knows which ad a question is about.

## Selling

Every account is a seller. The profile and the shop are the same page.

- **Post an ad:** a short form — title, price, negotiable or not, category, condition, city. Nothing
  a person filming on a phone would abandon halfway through.
- **Contact settings:** publish a phone number or keep it private. Messages can be turned off too.
  Clearing the number turns calls off rather than leaving a dead button on your ads.
- **My ads:** views and enquiries per ad, and a "mark sold" toggle that pulls an ad out of browse
  and the feed without deleting it.
- **Tag ads on a video:** the publishing step after trimming and editing, where you choose which of
  your ads the video advertises.

## Video playback

- **Smooth scrolling performance:** designed to handle fast scrolling with optimized performance.
- **Thumbnail preview:** view video thumbnails while seeking to different durations in the video.
- **Caching and prefetching:** efficiently caches and prefetches video and thumbnail data.
- **Video creation:** select a video, add audio, trim it, and add a text overlay.

## Architecture

The marketplace lives under `ui/commerce`:

| Package | Responsibility |
| --- | --- |
| `commerce/model` | Listings, sellers, conversations, live streams. Prices are integer minor units. |
| `commerce/data` | Seeded catalogue and repositories (listings, messages, seller, live). |
| `commerce/viewmodel` | `BrowseViewModel`, `MessagesViewModel`, `LiveViewModel`, `MyListingsViewModel`. |
| `commerce/view` | Shared components: listing card, detail sheet, contact actions, feed overlay. |
| `commerce/browse`, `commerce/messages`, `commerce/live`, `commerce/profile`, `commerce/creator` | Screens. |

The marketplace view models are resolved once in `MainScreen` at activity scope, so the feed, the
live rooms and the browse tab share one set of listings and one inbox.

Bottom navigation is Home · Browse · Post · Live · Account.

### The backend

`server/` is a real API: TypeScript, Hono, Drizzle, Postgres on Neon. Phone-code sign-in,
listings, messaging with live delivery over SSE, presigned media uploads, push notifications
and a nightly housekeeping sweep. `server/README.md` walks through it from an empty Neon
project to a deployed API in about twenty minutes.

The app talks to it through `CommerceSync`, which keeps the repositories synchronous: it pulls
in the background and pushes into the same StateFlows the screens already collect. That is also
where the offline behaviour comes from — with no server reachable, the seeded catalogue stays on
screen and everything still renders.

Point a debug build at a local server with `API_BASE_URL` in `app/build.gradle.kts`. It defaults
to `http://10.0.2.2:8787`, which is how the emulator addresses your laptop.

### What is real and what is not

With a server running: sign-in, listings, browse and search, conversations, live message
delivery, photo and video upload, and push notifications are all real. Without one, the app
falls back to seeded local data and everything below applies.

- The fallback catalogue is seeded in `CommerceCatalog`; ads you post are layered on top of it and
  enter the feed's tag pool, so your own ads show up on feed videos.
- Offline, listings, conversations, follows, your profile and your contact preferences persist
  across launches via `SharedPreferences` (`CommerceStore`) and never leave the device.
- Calling is real either way: the app builds an `ACTION_DIAL` intent and hands it to the system
  dialler. The number is one the seller typed in and chose to publish — there is no masking and
  no proxy number.
- **No SMS provider is wired up.** Outside production the server returns the sign-in code in the
  response and the login screen shows it, so the flow is testable today.
- **Video is uploaded but not transcoded.** The transcoder seam is explicit and currently a no-op,
  so an uploaded video stays `PROCESSING` and does not reach the feed until a provider is wired in.
- Live rooms play a looping video source. The viewer count and chat are simulated in
  `LiveRepository`; there is no ingest or WebRTC.
- Ads with no photographs render as a gradient derived from the id plus an emoji, so grids stay
  stable and work offline.
- Prices are shown in SAR and the seeded cities are Saudi. The UI is English; it is not localised
  to Arabic or RTL yet.

## Building

`./gradlew assembleDebug` builds without signing secrets. `.github/workflows/branch-build.yml` runs
that on every push to a `claude/**` branch, alongside a job that typechecks the server and runs its
131 schema and behaviour checks against an in-process Postgres. The release workflow builds signed
APKs on `feat/video-creation-phase2`.

Push notifications need a Firebase project. There is deliberately no `google-services.json` in this
repository — it is per-project configuration belonging to whoever ships the app, and a committed
placeholder would produce an APK that looks configured and is not. Without it the app builds and
runs normally; notifications simply do not arrive. `server/README.md` has the two lines that turn
it on.

# Video Demo

https://github.com/user-attachments/assets/3898de67-bd3e-4132-8085-c849ce3d133c

## Upcoming

- **Adaptive video streaming:** adjust video quality based on network conditions.
- **Video preview during adaptive streaming.**
- **Configurable playback behaviour:** auto-play and buffering strategies.
- **Load more:** paginate the short video feed.
- **Arabic and RTL localisation.**

# Author
Dennis (Duy) Bui (https://www.linkedin.com/in/duy-bui-4bb54b143/)

# Noted
- On this project, I don't focus on architecture, so when you guys apply to real project, consider to move it to correct module
- Content video of streaming will use open source Video
