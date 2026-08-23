/**
 * Configuration, read once at startup so a missing value fails immediately
 * rather than on the first request that happens to need it.
 */

function required(name: string): string {
  const value = process.env[name];
  if (!value) throw new Error(`${name} is not set. See .env.example.`);
  return value;
}

function optional(name: string, fallback: string): string {
  return process.env[name] || fallback;
}

export const env = {
  databaseUrl: () => required("DATABASE_URL"),
  jwtSecret: () => required("JWT_SECRET"),
  publicBaseUrl: () => optional("PUBLIC_BASE_URL", "http://localhost:8787"),
  port: () => Number(optional("PORT", "8787")),
  isProduction: () => process.env.NODE_ENV === "production",
};

/** Access tokens are short so a leaked one stops working quickly. */
export const ACCESS_TOKEN_TTL_SECONDS = 15 * 60;
/** Refresh tokens are long, but rotate on every use and can be revoked. */
export const REFRESH_TOKEN_TTL_DAYS = 60;

export const OTP_TTL_SECONDS = 5 * 60;
export const OTP_MAX_ATTEMPTS = 5;
/** Requests allowed per phone number inside the window, to blunt SMS bombing. */
export const OTP_MAX_REQUESTS_PER_WINDOW = 3;
export const OTP_WINDOW_SECONDS = 15 * 60;

/**
 * How long a deleted account is recoverable before a job hard-deletes it.
 * Google Play requires deletion to be available in-app; it does not require it
 * to be instant, and a grace period saves people who tap by mistake.
 */
export const ACCOUNT_DELETION_GRACE_DAYS = 30;
