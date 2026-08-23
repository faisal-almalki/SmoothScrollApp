import { serve } from "@hono/node-server";
import { sql } from "drizzle-orm";
import { Hono } from "hono";
import { HTTPException } from "hono/http-exception";
import { createDb } from "./db/client.js";
import { env } from "./lib/env.js";
import { ApiError, errorBody } from "./lib/http.js";
import { requestLogger } from "./lib/logging.js";
import { isPushConfigured } from "./lib/push.js";
import { InvalidPhoneError } from "./lib/phone.js";
import type { AppBindings } from "./middleware/auth.js";
import { accountRoutes, authRoutes } from "./routes/auth.js";
import { listingRoutes, safetyRoutes, sellerRoutes } from "./routes/listings.js";
import { feedRoutes, photoRoutes, videoRoutes } from "./routes/media.js";
import { internalRoutes } from "./routes/internal.js";
import { conversationRoutes } from "./routes/messaging.js";
import { pushRoutes } from "./routes/push.js";

const app = new Hono<AppBindings>();

// First, so every later middleware and handler runs inside a logged request and
// the request id exists before anything might want to mention it.
app.use("*", requestLogger({ json: env.isProduction() }));

// One connection per process, attached to every request so handlers never
// import a module-level database and stay drivable from tests.
const db = createDb();
app.use("*", async (c, next) => {
  c.set("db", db);
  await next();
});

/**
 * The deploy platform's health check. Deliberately does not touch the database:
 * a health check that fails when Neon is briefly unreachable makes the platform
 * kill and restart a process that would have recovered on its own.
 */
app.get("/health", (c) => c.json({ ok: true, at: new Date().toISOString() }));

/**
 * Readiness, which does touch the database, for a deploy script that wants to
 * know the new release can actually serve before it shifts traffic.
 */
app.get("/ready", async (c) => {
  try {
    await c.get("db").execute(sql`SELECT 1`);
    return c.json({ ok: true, database: "up", push: isPushConfigured() });
  } catch (error) {
    console.error("[ready] database unreachable", error);
    return c.json({ ok: false, database: "down" }, 503);
  }
});

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
// Push registration lives under /account because a token always belongs to
// whoever is signed in; Hono merges this with accountRoutes.
app.route("/account", pushRoutes);
app.route("/internal", internalRoutes);

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
  // Never leak an internal message to a client; log it against the request id
  // so a user quoting the id from their error is enough to find the stack.
  console.error(`[unhandled] req=${c.get("requestId")}`, error);
  return c.json(
    errorBody(new ApiError(500, "internal_error", "Something went wrong. Try again.")),
    500,
  );
});

const port = env.port();

// Fail at startup rather than on the first request that needs them. A container
// that will not serve is obvious; one that serves 500s is not.
env.databaseUrl();
env.jwtSecret();

serve({ fetch: app.fetch, port, hostname: "0.0.0.0" }, () => {
  console.log(
    `SmoothScroll API on port ${port} ` +
      `(${env.isProduction() ? "production" : "development"}, ` +
      `push ${isPushConfigured() ? "enabled" : "disabled"})`,
  );
});

export { app };
