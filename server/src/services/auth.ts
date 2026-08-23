import { and, desc, eq, gt, isNull, sql } from "drizzle-orm";
import type { Database } from "../db/client.js";
import { accounts, otpCodes, sessions } from "../db/schema.js";
import {
  ACCOUNT_DELETION_GRACE_DAYS,
  OTP_MAX_ATTEMPTS,
  OTP_MAX_REQUESTS_PER_WINDOW,
  OTP_TTL_SECONDS,
  OTP_WINDOW_SECONDS,
  REFRESH_TOKEN_TTL_DAYS,
} from "../lib/env.js";
import { ApiError } from "../lib/http.js";
import { maskPhone, normaliseSaudiPhone } from "../lib/phone.js";
import { clearPushTokensForAccount } from "./push.js";
import {
  generateOtpCode,
  generateRefreshToken,
  hashOtpCode,
  hashRefreshToken,
  safeEqual,
  signAccessToken,
} from "../lib/tokens.js";

/**
 * Sign-in by SMS code.
 *
 * Everything here takes `db` and `now` as arguments rather than reaching for a
 * module-level connection or the system clock, which is what lets the verifier
 * drive the identical code against an in-process Postgres and fast-forward time
 * to test expiry.
 */

export interface AuthTokens {
  accountId: string;
  accessToken: string;
  refreshToken: string;
  isNewAccount: boolean;
}

/** Set only in development, where the code is returned instead of texted. */
export interface OtpRequestResult {
  phone: string;
  expiresAt: Date;
  devCode?: string;
}

/**
 * The seam a real SMS provider drops into. Unifonic and Twilio are the usual
 * choices for Saudi numbers; both are a single HTTP call from here.
 */
export type SmsSender = (phone: string, code: string) => Promise<void>;

export const logOnlySmsSender: SmsSender = async (phone, code) => {
  console.log(`[sms] ${maskPhone(phone)} -> code ${code}`);
};

export async function requestOtp(
  db: Database,
  rawPhone: string,
  options: { now?: Date; sendSms?: SmsSender; exposeCode?: boolean } = {},
): Promise<OtpRequestResult> {
  const now = options.now ?? new Date();
  const phone = normaliseSaudiPhone(rawPhone);

  // Rate limit before generating anything, so a flood costs one SELECT.
  const windowStart = new Date(now.getTime() - OTP_WINDOW_SECONDS * 1000);
  const recent = await db
    .select({ count: sql<number>`count(*)::int` })
    .from(otpCodes)
    .where(and(eq(otpCodes.phone, phone), gt(otpCodes.createdAt, windowStart)));

  if ((recent[0]?.count ?? 0) >= OTP_MAX_REQUESTS_PER_WINDOW) {
    throw ApiError.tooManyRequests(
      "Too many codes requested. Wait a few minutes and try again.",
    );
  }

  const code = generateOtpCode();
  const expiresAt = new Date(now.getTime() + OTP_TTL_SECONDS * 1000);

  await db.insert(otpCodes).values({
    phone,
    codeHash: hashOtpCode(code, phone),
    expiresAt,
    createdAt: now,
  });

  await (options.sendSms ?? logOnlySmsSender)(phone, code);

  return { phone, expiresAt, ...(options.exposeCode ? { devCode: code } : {}) };
}

export async function verifyOtp(
  db: Database,
  rawPhone: string,
  code: string,
  options: { now?: Date; city?: string } = {},
): Promise<AuthTokens> {
  const now = options.now ?? new Date();
  const phone = normaliseSaudiPhone(rawPhone);

  const [candidate] = await db
    .select()
    .from(otpCodes)
    .where(and(eq(otpCodes.phone, phone), isNull(otpCodes.consumedAt)))
    .orderBy(desc(otpCodes.createdAt))
    .limit(1);

  if (!candidate) {
    throw ApiError.badRequest("otp_not_found", "Request a code first.");
  }
  if (candidate.expiresAt.getTime() <= now.getTime()) {
    throw ApiError.badRequest("otp_expired", "That code has expired. Request a new one.");
  }
  if (candidate.attempts >= OTP_MAX_ATTEMPTS) {
    throw ApiError.tooManyRequests("Too many wrong attempts. Request a new code.");
  }

  // Count the attempt before checking it, so a crash mid-check cannot be used
  // to retry for free.
  await db
    .update(otpCodes)
    .set({ attempts: candidate.attempts + 1 })
    .where(eq(otpCodes.id, candidate.id));

  if (!safeEqual(candidate.codeHash, hashOtpCode(code, phone))) {
    throw ApiError.badRequest("otp_incorrect", "That code is not right.");
  }

  await db
    .update(otpCodes)
    .set({ consumedAt: now })
    .where(eq(otpCodes.id, candidate.id));

  const existing = await findAccountByPhone(db, phone);
  const account = existing ?? (await createAccount(db, phone, options.city ?? "Riyadh", now));

  // Signing in cancels a pending deletion — the person came back.
  if (existing?.deletedAt) {
    await db.update(accounts).set({ deletedAt: null }).where(eq(accounts.id, existing.id));
  }
  if (account.isBanned) {
    throw ApiError.forbidden("This account has been suspended.");
  }

  const tokens = await issueTokens(db, account.id, now);
  return { ...tokens, isNewAccount: !existing };
}

async function findAccountByPhone(db: Database, phone: string) {
  const [row] = await db.select().from(accounts).where(eq(accounts.authPhone, phone)).limit(1);
  return row ?? null;
}

/**
 * First sign-in creates the profile. The handle is derived from the number's
 * last four digits plus a suffix on collision, so nobody has to invent one
 * before they can look around.
 */
async function createAccount(db: Database, phone: string, city: string, now: Date) {
  const tail = phone.slice(-4);
  for (let attempt = 0; attempt < 10; attempt++) {
    const handle = attempt === 0 ? `user${tail}` : `user${tail}${attempt}`;
    try {
      const [row] = await db
        .insert(accounts)
        .values({
          authPhone: phone,
          handle,
          displayName: `User ${tail}`,
          city,
          memberSince: now,
        })
        .returning();
      if (row) return row;
    } catch (error) {
      const message = error instanceof Error ? error.message : "";
      if (message.includes("accounts_handle_key")) continue;
      throw error;
    }
  }
  throw ApiError.conflict("handle_unavailable", "Could not allocate a handle. Try again.");
}

async function issueTokens(db: Database, accountId: string, now: Date) {
  const refreshToken = generateRefreshToken();
  await db.insert(sessions).values({
    accountId,
    refreshTokenHash: hashRefreshToken(refreshToken),
    expiresAt: new Date(now.getTime() + REFRESH_TOKEN_TTL_DAYS * 86_400_000),
    createdAt: now,
  });
  return { accountId, accessToken: await signAccessToken(accountId), refreshToken };
}

/**
 * Refresh rotates: the presented token is revoked and a new one issued. A
 * stolen token is therefore usable at most once, and the theft shows up as the
 * real user being logged out.
 */
export async function refreshSession(
  db: Database,
  refreshToken: string,
  options: { now?: Date } = {},
): Promise<AuthTokens> {
  const now = options.now ?? new Date();
  const [session] = await db
    .select()
    .from(sessions)
    .where(eq(sessions.refreshTokenHash, hashRefreshToken(refreshToken)))
    .limit(1);

  if (!session || session.revokedAt || session.expiresAt.getTime() <= now.getTime()) {
    throw ApiError.unauthorized("Your session has expired. Sign in again.");
  }

  await db.update(sessions).set({ revokedAt: now }).where(eq(sessions.id, session.id));

  const [account] = await db.select().from(accounts).where(eq(accounts.id, session.accountId)).limit(1);
  if (!account || account.isBanned || account.deletedAt) {
    throw ApiError.unauthorized("Your session has expired. Sign in again.");
  }

  const tokens = await issueTokens(db, session.accountId, now);
  return { ...tokens, isNewAccount: false };
}

export async function logout(db: Database, refreshToken: string, now = new Date()): Promise<void> {
  await db
    .update(sessions)
    .set({ revokedAt: now })
    .where(eq(sessions.refreshTokenHash, hashRefreshToken(refreshToken)));
}

export async function logoutEverywhere(db: Database, accountId: string, now = new Date()): Promise<void> {
  await db
    .update(sessions)
    .set({ revokedAt: now })
    .where(and(eq(sessions.accountId, accountId), isNull(sessions.revokedAt)));
}

/**
 * In-app account deletion, which Google Play requires. The row is marked rather
 * than dropped so the person has a grace period to change their mind; every
 * session is revoked immediately so the app stops working straight away.
 */
export async function requestAccountDeletion(
  db: Database,
  accountId: string,
  now = new Date(),
): Promise<{ deletedAt: Date; purgeAfter: Date }> {
  await db.update(accounts).set({ deletedAt: now }).where(eq(accounts.id, accountId));
  await logoutEverywhere(db, accountId, now);
  // The row survives the grace period, but its devices must stop being notified
  // the moment the person asks to be gone.
  await clearPushTokensForAccount(db, accountId);
  return {
    deletedAt: now,
    purgeAfter: new Date(now.getTime() + ACCOUNT_DELETION_GRACE_DAYS * 86_400_000),
  };
}

/** Resolves a bearer token's subject to a usable account, or null. */
export async function loadActiveAccount(db: Database, accountId: string) {
  const [account] = await db.select().from(accounts).where(eq(accounts.id, accountId)).limit(1);
  if (!account || account.isBanned || account.deletedAt) return null;
  return account;
}
