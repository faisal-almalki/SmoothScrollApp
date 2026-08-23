import { Hono } from "hono";
import { z } from "zod";
import { parseBody } from "../lib/http.js";
import { isPushConfigured } from "../lib/push.js";
import type { AppBindings } from "../middleware/auth.js";
import { requireAuth } from "../middleware/auth.js";
import {
  clearPushTokensForAccount,
  registerPushToken,
  unregisterPushToken,
} from "../services/push.js";

/**
 * Device registration for push notifications. Mounted under /account, so a
 * token always belongs to whoever is signed in — there is no path that
 * registers a token for an arbitrary account id.
 */
export const pushRoutes = new Hono<AppBindings>();

const registerSchema = z.object({
  token: z.string().min(10).max(512),
  platform: z.enum(["android", "ios"]).default("android"),
});

pushRoutes.post("/push-tokens", requireAuth, async (c) => {
  const body = await parseBody(c, registerSchema);
  const result = await registerPushToken(
    c.get("db"),
    c.get("account").id,
    body.token,
    body.platform,
  );
  // The client is told whether the server can actually send, so a device that
  // registers against a server with no FCM credentials can say "notifications
  // are off" rather than waiting silently for something that will never arrive.
  return c.json({ ...result, deliveryEnabled: isPushConfigured() });
});

/**
 * The token is in the body rather than the path because FCM tokens contain
 * characters that need escaping in a URL and are long enough to be truncated by
 * some proxies' path limits.
 */
pushRoutes.delete("/push-tokens", requireAuth, async (c) => {
  const body = await parseBody(c, z.object({ token: z.string().min(10).max(512) }));
  return c.json(await unregisterPushToken(c.get("db"), c.get("account").id, body.token));
});

/** Signing out of every device should stop every device's notifications too. */
pushRoutes.delete("/push-tokens/all", requireAuth, async (c) => {
  return c.json(await clearPushTokensForAccount(c.get("db"), c.get("account").id));
});
