import { Hono } from "hono";
import { z } from "zod";
import type { AppBindings } from "../middleware/auth.js";
import { requireAuth } from "../middleware/auth.js";
import { env } from "../lib/env.js";
import { ApiError, parseBody } from "../lib/http.js";
import { isValidSaudiPhone } from "../lib/phone.js";
import {
  logout,
  logoutEverywhere,
  refreshSession,
  requestAccountDeletion,
  requestOtp,
  verifyOtp,
} from "../services/auth.js";

const phoneSchema = z
  .string()
  .min(9)
  .max(20)
  .refine(isValidSaudiPhone, "Enter a Saudi mobile number, for example 05xxxxxxxx.");

const requestSchema = z.object({ phone: phoneSchema });
const verifySchema = z.object({
  phone: phoneSchema,
  code: z.string().regex(/^\d{6}$/, "The code is six digits."),
  city: z.string().min(2).max(60).optional(),
});
const refreshSchema = z.object({ refreshToken: z.string().min(20) });

export const authRoutes = new Hono<AppBindings>();

authRoutes.post("/otp/request", async (c) => {
  const { phone } = await parseBody(c, requestSchema);
  // Returning the code outside production is what makes the app testable
  // before an SMS provider is wired up. It is never exposed in production.
  const result = await requestOtp(c.get("db"), phone, { exposeCode: !env.isProduction() });
  return c.json({
    phone: result.phone,
    expiresAt: result.expiresAt.toISOString(),
    ...(result.devCode ? { devCode: result.devCode } : {}),
  });
});

authRoutes.post("/otp/verify", async (c) => {
  const body = await parseBody(c, verifySchema);
  const tokens = await verifyOtp(c.get("db"), body.phone, body.code, { city: body.city });
  return c.json(tokens, tokens.isNewAccount ? 201 : 200);
});

authRoutes.post("/refresh", async (c) => {
  const { refreshToken } = await parseBody(c, refreshSchema);
  return c.json(await refreshSession(c.get("db"), refreshToken));
});

authRoutes.post("/logout", async (c) => {
  const { refreshToken } = await parseBody(c, refreshSchema);
  await logout(c.get("db"), refreshToken);
  return c.body(null, 204);
});

authRoutes.post("/logout-everywhere", requireAuth, async (c) => {
  await logoutEverywhere(c.get("db"), c.get("account").id);
  return c.body(null, 204);
});

export const accountRoutes = new Hono<AppBindings>();

accountRoutes.get("/", requireAuth, (c) => {
  const account = c.get("account");
  return c.json({
    id: account.id,
    handle: account.handle,
    displayName: account.displayName,
    bio: account.bio,
    emoji: account.emoji,
    city: account.city,
    // authPhone is the login identity and is never returned, even to its owner's
    // client, so it cannot leak through a cached response or a screenshot.
    publicPhone: account.allowCalls ? account.publicPhone : null,
    allowCalls: account.allowCalls,
    allowMessages: account.allowMessages,
    isVerified: account.isVerified,
    followerCount: account.followerCount,
    rating: account.rating,
    ratingCount: account.ratingCount,
    memberSince: account.memberSince.toISOString(),
  });
});

/** Google Play requires account deletion to be reachable from inside the app. */
accountRoutes.delete("/", requireAuth, async (c) => {
  const confirm = c.req.query("confirm");
  if (confirm !== "true") {
    throw ApiError.badRequest(
      "confirmation_required",
      "Deleting an account is permanent. Repeat the request with ?confirm=true.",
    );
  }
  const result = await requestAccountDeletion(c.get("db"), c.get("account").id);
  return c.json({
    deletedAt: result.deletedAt.toISOString(),
    purgeAfter: result.purgeAfter.toISOString(),
    message: "Your account is scheduled for deletion. Sign in again before then to keep it.",
  });
});
