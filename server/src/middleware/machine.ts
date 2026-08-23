import type { MiddlewareHandler } from "hono";
import { ApiError } from "../lib/http.js";
import { safeEqual } from "../lib/tokens.js";
import type { AppBindings } from "./auth.js";

/**
 * Guards the endpoints whose caller is a machine, not a person: the
 * transcoder's callback and the maintenance sweep. Neither has an account, so
 * neither can present a bearer token.
 *
 * An unset secret denies rather than allows. The alternative — treating "no
 * secret configured" as "no check needed" — turns one forgotten environment
 * variable into an open endpoint that deletes accounts.
 */
export function requireMachineSecret(
  variableName: string,
  headerName: string,
): MiddlewareHandler<AppBindings> {
  return async (c, next) => {
    const expected = process.env[variableName];
    if (!expected || expected.trim() === "") {
      console.error(`[machine] ${variableName} is not set; ${c.req.path} is closed.`);
      throw ApiError.forbidden("Not allowed.");
    }
    const presented = c.req.header(headerName);
    // Constant-time, so the response time cannot be used to guess the secret
    // one character at a time.
    if (!presented || !safeEqual(presented, expected)) {
      throw ApiError.forbidden("Not allowed.");
    }
    await next();
  };
}
