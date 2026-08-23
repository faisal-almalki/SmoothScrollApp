import type { MiddlewareHandler } from "hono";
import type { Database } from "../db/client.js";
import type { Account } from "../db/schema.js";
import { ApiError } from "../lib/http.js";
import { verifyAccessToken } from "../lib/tokens.js";
import { loadActiveAccount } from "../services/auth.js";

export interface AppBindings {
  Variables: {
    db: Database;
    account: Account;
  };
}

function bearerToken(header: string | undefined): string | null {
  if (!header) return null;
  const [scheme, token] = header.split(" ");
  if (!token || scheme?.toLowerCase() !== "bearer") return null;
  return token;
}

/**
 * Rejects anything without a live account behind the token. Banned and deleted
 * accounts are turned away here rather than in each route, so a route can never
 * forget to check.
 */
export const requireAuth: MiddlewareHandler<AppBindings> = async (c, next) => {
  const token = bearerToken(c.req.header("Authorization"));
  if (!token) throw ApiError.unauthorized();

  const accountId = await verifyAccessToken(token);
  if (!accountId) throw ApiError.unauthorized("Your session has expired. Sign in again.");

  const account = await loadActiveAccount(c.get("db"), accountId);
  if (!account) throw ApiError.unauthorized("Your session has expired. Sign in again.");

  c.set("account", account);
  await next();
};
