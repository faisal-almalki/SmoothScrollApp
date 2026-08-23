import { createHash, createHmac, randomBytes, randomInt, timingSafeEqual } from "node:crypto";
import { sign, verify } from "hono/jwt";
import { ACCESS_TOKEN_TTL_SECONDS, env } from "./env.js";

/**
 * Secrets never touch the database in a readable form.
 *
 * OTP codes are only six digits, so a plain hash would fall to a rainbow table
 * in seconds — they are HMAC'd with the server secret, which an attacker with a
 * database dump does not have. Refresh tokens are 256 bits of randomness, so a
 * plain SHA-256 is enough and avoids a second secret dependency.
 */

export function generateOtpCode(): string {
  return String(randomInt(0, 1_000_000)).padStart(6, "0");
}

export function hashOtpCode(code: string, phone: string): string {
  // The phone is mixed in so the same code for two numbers hashes differently.
  return createHmac("sha256", env.jwtSecret()).update(`${phone}:${code}`).digest("hex");
}

export function generateRefreshToken(): string {
  return randomBytes(32).toString("base64url");
}

export function hashRefreshToken(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}

/** Constant-time comparison, so a wrong code cannot be found by timing. */
export function safeEqual(a: string, b: string): boolean {
  const left = Buffer.from(a);
  const right = Buffer.from(b);
  if (left.length !== right.length) return false;
  return timingSafeEqual(left, right);
}

/**
 * Pinned explicitly rather than left to default. Accepting whatever algorithm a
 * token claims is the classic JWT confusion attack, and Hono now requires the
 * choice to be made at the call site.
 */
const JWT_ALGORITHM = "HS256" as const;

export interface AccessTokenPayload {
  sub: string;
  exp: number;
  [key: string]: unknown;
}

export async function signAccessToken(accountId: string): Promise<string> {
  const payload: AccessTokenPayload = {
    sub: accountId,
    exp: Math.floor(Date.now() / 1000) + ACCESS_TOKEN_TTL_SECONDS,
  };
  return sign(payload, env.jwtSecret(), JWT_ALGORITHM);
}

/** Returns the account id, or null when the token is invalid or expired. */
export async function verifyAccessToken(token: string): Promise<string | null> {
  try {
    const payload = (await verify(token, env.jwtSecret(), JWT_ALGORITHM)) as AccessTokenPayload;
    return typeof payload.sub === "string" ? payload.sub : null;
  } catch {
    return null;
  }
}
