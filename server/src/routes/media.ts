import { Hono } from "hono";
import { z } from "zod";
import { parseBody } from "../lib/http.js";
import { verifyAccessToken } from "../lib/tokens.js";
import type { AppBindings } from "../middleware/auth.js";
import { requireAuth } from "../middleware/auth.js";
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
} from "../services/media.js";

const uploadUrlSchema = z.object({
  contentType: z.string().min(3).max(80),
  sizeBytes: z.number().int().positive(),
  filename: z.string().max(120).optional(),
});
const attachSchema = z.object({
  key: z.string().min(3).max(400),
  width: z.number().int().positive().optional(),
  height: z.number().int().positive().optional(),
});
const orderSchema = z.object({ photoIds: z.array(z.string().uuid()).min(1).max(10) });
const videoUploadSchema = uploadUrlSchema.extend({ caption: z.string().max(500).optional() });
const confirmSchema = z.object({ key: z.string().min(3).max(400) });
const readySchema = z.object({
  playbackUrl: z.string().url(),
  thumbnailUrl: z.string().url().optional(),
  durationMs: z.number().int().positive().optional(),
  width: z.number().int().positive().optional(),
  height: z.number().int().positive().optional(),
});
const tagSchema = z.object({ listingIds: z.array(z.string().uuid()).max(6) });

/** Photo routes hang off an ad, so they are mounted under /listings/:id. */
export const photoRoutes = new Hono<AppBindings>();

photoRoutes.get("/:id/photos", async (c) => c.json(await listPhotos(c.get("db"), c.req.param("id"))));

photoRoutes.post("/:id/photos/upload-url", requireAuth, async (c) => {
  const body = await parseBody(c, uploadUrlSchema);
  const upload = await createPhotoUploadUrl(c.get("db"), c.req.param("id"), c.get("account").id, body);
  return c.json({ ...upload, expiresAt: upload.expiresAt.toISOString() });
});

photoRoutes.post("/:id/photos", requireAuth, async (c) => {
  const body = await parseBody(c, attachSchema);
  const photo = await attachPhoto(c.get("db"), c.req.param("id"), c.get("account").id, body);
  return c.json(photo, 201);
});

photoRoutes.patch("/:id/photos/order", requireAuth, async (c) => {
  const { photoIds } = await parseBody(c, orderSchema);
  return c.json(await reorderPhotos(c.get("db"), c.req.param("id"), c.get("account").id, photoIds));
});

photoRoutes.delete("/:id/photos/:photoId", requireAuth, async (c) =>
  c.json(
    await deletePhoto(c.get("db"), c.req.param("id"), c.get("account").id, c.req.param("photoId")),
  ),
);

export const videoRoutes = new Hono<AppBindings>();

videoRoutes.post("/upload-url", requireAuth, async (c) => {
  const body = await parseBody(c, videoUploadSchema);
  const result = await createVideoUploadUrl(c.get("db"), c.get("account").id, body);
  return c.json(
    { video: result.video, upload: { ...result.upload, expiresAt: result.upload.expiresAt.toISOString() } },
    201,
  );
});

videoRoutes.post("/:id/confirm", requireAuth, async (c) => {
  const { key } = await parseBody(c, confirmSchema);
  return c.json(await confirmVideoUpload(c.get("db"), c.req.param("id"), c.get("account").id, key));
});

/**
 * The transcoder's callback. Guarded by a shared secret rather than a user
 * token, because the caller is a machine with no account.
 */
videoRoutes.post("/:id/ready", async (c) => {
  const secret = process.env.TRANSCODER_SECRET;
  if (!secret || c.req.header("X-Transcoder-Secret") !== secret) {
    return c.json({ error: { code: "forbidden", message: "Not allowed." } }, 403);
  }
  const body = await parseBody(c, readySchema);
  return c.json(await markVideoReady(c.get("db"), c.req.param("id"), body));
});

videoRoutes.post("/:id/listings", requireAuth, async (c) => {
  const { listingIds } = await parseBody(c, tagSchema);
  const tagged = await setVideoListings(c.get("db"), c.req.param("id"), c.get("account").id, listingIds);
  return c.json({ tagged });
});

export const feedRoutes = new Hono<AppBindings>();

feedRoutes.get("/", async (c) => {
  const header = c.req.header("Authorization");
  const token = header?.toLowerCase().startsWith("bearer ") ? header.slice(7) : undefined;
  const viewerId = token ? ((await verifyAccessToken(token)) ?? undefined) : undefined;

  const limitRaw = c.req.query("limit");
  return c.json(
    await getFeed(c.get("db"), {
      limit: limitRaw ? Number(limitRaw) : undefined,
      cursor: c.req.query("cursor"),
      viewerId,
    }),
  );
});
