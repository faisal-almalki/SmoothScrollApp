import { serve } from "@hono/node-server";
import { Hono } from "hono";
import { HTTPException } from "hono/http-exception";
import { logger } from "hono/logger";
import { createDb } from "./db/client.js";
import { env } from "./lib/env.js";
import { ApiError, errorBody } from "./lib/http.js";
import { InvalidPhoneError } from "./lib/phone.js";
import type { AppBindings } from "./middleware/auth.js";
import { accountRoutes, authRoutes } from "./routes/auth.js";
import { listingRoutes, safetyRoutes, sellerRoutes } from "./routes/listings.js";
import { feedRoutes, photoRoutes, videoRoutes } from "./routes/media.js";
import { conversationRoutes } from "./routes/messaging.js";

const app = new Hono<AppBindings>();

app.use("*", logger());

// One connection per process, attached to every request so handlers never
// import a module-level database and stay drivable from tests.
const db = createDb();
app.use("*", async (c, next) => {
  c.set("db", db);
  await next();
});

app.get("/health", (c) => c.json({ ok: true, at: new Date().toISOString() }));

app.route("/auth", authRoutes);
app.route("/account", accountRoutes);
app.route("/listings", listingRoutes);
app.route("/sellers", sellerRoutes);
app.route("/conversations", conversationRoutes);
// Photo routes share the /listings prefix; Hono merges the two routers.
app.route("/listings", photoRoutes);
app.route("/videos", videoRoutes);
app.route("/feed", feedRoutes);
app.route("/", safetyRoutes);

app.notFound((c) =>
  c.json(errorBody(ApiError.notFound("No such endpoint.")), 404),
);

app.onError((error, c) => {
  if (error instanceof ApiError) {
    return c.json(errorBody(error), error.status);
  }
  if (error instanceof InvalidPhoneError) {
    return c.json(
      errorBody(ApiError.badRequest("invalid_phone", "Enter a Saudi mobile number.")),
      400,
    );
  }
  if (error instanceof HTTPException) {
    return c.json(errorBody(new ApiError(500, "http_error", error.message)), error.status);
  }
  // Never leak an internal message to a client; log it instead.
  console.error("[unhandled]", error);
  return c.json(
    errorBody(new ApiError(500, "internal_error", "Something went wrong. Try again.")),
    500,
  );
});

const port = env.port();
serve({ fetch: app.fetch, port }, () => {
  console.log(`SmoothScroll API on http://localhost:${port}`);
});

export { app };
