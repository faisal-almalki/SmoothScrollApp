# SmoothScroll

SmoothScroll is a video commerce app for Android: a TikTok style vertical feed where every video
is shoppable, creators sell their own products, and live rooms let you buy while you watch.

It is built on top of a high performance Media3 ExoPlayer feed with caching, prefetching and
thumbnail scrubbing, so the shopping layer never costs you scroll performance.

## Shopping

- **Shoppable feed:** every video carries a product tag. A bag pill expands over the video a beat
  after playback starts; tapping it opens the buy sheet without leaving the feed.
- **Live shopping:** a Live tab of creators selling on camera. Each room has a running viewer
  count, a live chat with a purchase ticker, a pinned product the seller is talking about, and a
  flash sale clock. The live price applies only while the clock is running.
- **Shop tab:** search across products and creators, filter by category, and a trending grid with
  the live rooms surfaced on top.
- **Cart and checkout:** one cart shared by the feed, the live rooms and the shop tab, with a free
  shipping threshold, tax estimate, address form, payment method and an order confirmation.
- **Orders:** every order is kept, including whether it was bought in the feed or in a live.

## Selling

Every account on this platform is a shop, so the creator page and the storefront are the same page.

- **Storefront:** a creator's listings, follower count, seller rating, and a shortcut into their
  live room when they are streaming.
- **Creator Studio:** gross sales, payout after the platform fee, units sold, orders, and the
  listings you manage.
- **List a product:** a short form (title, price, stock, category, one variant axis) with a live
  preview of the card buyers will see.
- **Tag products on a video:** the publishing step after trimming and editing, where you choose
  which of your listings the video sells.

## Video playback

- **Smooth Scrolling Performance:** designed to handle fast scrolling with optimized performance.
- **Thumbnail Preview:** view video thumbnails while seeking to different durations in the video.
- **Caching and Prefetching:** efficiently caches and prefetches video and thumbnail data.
- **Video creation:** create a custom video by selecting a video, adding audio, trimming, and
  adding a text overlay.

## Commerce architecture

The commerce feature lives under `ui/commerce`:

| Package | Responsibility |
| --- | --- |
| `commerce/model` | Products, sellers, cart, orders, live streams. Prices are integer minor units. |
| `commerce/data` | Seeded catalogue and repositories (cart, orders, products, seller, live). |
| `commerce/viewmodel` | `CartViewModel`, `ShopViewModel`, `LiveViewModel`, `CreatorViewModel`. |
| `commerce/view` | Shared components: product card, buy sheet, price rows, feed overlay. |
| `commerce/shop`, `commerce/cart`, `commerce/live`, `commerce/profile`, `commerce/creator` | Screens. |

The commerce view models are resolved once in `MainScreen` at activity scope, so the cart the feed
adds to is the same cart the shop tab and checkout read.

### What is real and what is not

There is no commerce backend behind this build, so:

- The catalogue is seeded in `CommerceCatalog`; products you list in Creator Studio are layered on
  top of it and do enter the feed's tag pool, so your own listings show up on feed videos.
- Cart, orders, listings, follows and video tags persist across launches via `SharedPreferences`
  (`CommerceStore`), not a server.
- Live rooms play a looping video source. The viewer count, chat and purchase ticker are simulated
  in `LiveRepository`; there is no ingest or WebRTC.
- Checkout records a payment method on the order. **Nothing is charged and no payment processor is
  integrated.**
- Videos you create are not uploaded, so a video you tag products on is saved against a local id
  rather than published to the feed.
- Products have no photographs. Each one renders as a gradient derived from its id plus an emoji,
  so the grids stay stable and work offline.


# Video Demo

https://github.com/user-attachments/assets/3898de67-bd3e-4132-8085-c849ce3d133c

## Upcoming Features

- **Adaptive Video Streaming:** Adjust video quality based on network conditions to provide a better streaming experience.
- **Video Preview during Adaptive Streaming:** Preview video content while watching adaptive video streams.
- **Configurable Playback Behavior:** Customize video playback settings such as auto-play and buffering strategies.
- **Customize Video:** User can customize video by trimming video, select audio then apply to video, changing some audio and video effect, then can export that change to local
- **Load more:** Load more video for short video

## Video Playback and Thumbnail Preview

When seeking through a video, the app provides a preview of the video's thumbnail to give users an idea of the content at the selected point. The thumbnails are preloaded and cached to ensure quick display.

## API Response

The app expects responses from the server (Fetched from JSON file) in the following format:

```json
{
    "status": "success",
    "video_id": "24a0033e-0e0c-42c4-9f3a-1f0ce9d1a1d8",
    "processing_status": "completed",
    "video_url": "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_1.mp4",
    "title": "Video title",
    "tags": [
        "Tag A",
        "TAG B",
        "TAG C"
    ],
    "owner": {
        "name": "Name of video owner",
        "email": "Email"
    },
    "metadata": {
        "duration": 25000,
        "width": 480.0,
        "height": 360.0,
        "bitrate": "413kbps",
        "codec": "H.264"
    },
    "thumbnails": {
        "small": [
            {
                "thumbnailUrl": "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/thumbnails/video_1/small/thumbnail_0.jpg",
                "time": 0
            }
        ],
        "medium": [
            {
                "thumbnailUrl": "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/thumbnails/video_1/medium/thumbnail_0.jpg",
                "time": 0
            }
        ]
    },
    "previews": []
}
```

- status: Indicates the success or failure of the request.
- video_id: Unique identifier for the video.
- processing_status: Current processing status of the video.
- video_url: URL where the video is hosted.
- metadata: Contains details about the video such as duration, width, height, bitrate, and codec.
- thumbnails: Provides URLs for thumbnails of the video at various intervals and sizes (small, medium).
- previews: Additional preview information (currently not used in this version, will support for DASH and HLS format).
- title: Video title
- tags: List tag of this video
- owner: Owner who post video

# Author
Dennis (Duy) Bui (https://www.linkedin.com/in/duy-bui-4bb54b143/)

# Noted
- On this project, I don't focus on architecture, so when you guys apply to real project, consider to move it to correct module
- Content video of streaming will use open source Video
