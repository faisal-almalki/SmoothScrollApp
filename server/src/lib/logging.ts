import { randomUUID } from "node:crypto";
import type { MiddlewareHandler } from "hono";
// Type-only, so this does not create a runtime cycle with the middleware.
import type { AppBindings } from "../middleware/auth.js";

/**
 * One structured line per request.
 *
 * In production it is JSON, because every log pipeline worth using parses JSON
 * and nothing parses prose reliably. In development it is a short human line,
 * because nobody wants to read JSON in a terminal while writing a feature.
 *
 * What is deliberately absent: the query string, the request body, headers, and
 * anything derived from them. Those carry phone numbers, OTP codes and bearer
 * tokens, and a log file is the wrong place for any of the three — it is copied,
 * shipped to third parties and kept far longer than the data deserves. The path
 * is `c.req.path`, which excludes the query string by construction rather than
 * by a redaction pass that someone will eventually forget to update.
 */

/** Correlates the lines of one request; echoed back so a user can quote it. */
const REQUEST_ID_HEADER = "X-Request-Id";

/** An id from outside is only trusted to be an id, never to be safe to print. */
function safeIncomingId(value: string | undefined): string | null {
  if (!value) return null;
  const trimmed = value.trim();
  if (trimmed.length === 0 || trimmed.length > 64) return null;
  return /^[A-Za-z0-9._:-]+$/.test(trimmed) ? trimmed : null;
}

export interface RequestLog {
  at: string;
  level: "info" | "warn" | "error";
  requestId: string;
  method: string;
  path: string;
  status: number;
  durationMs: number;
  accountId?: string;
}

function levelFor(status: number): RequestLog["level"] {
  if (status >= 500) return "error";
  if (status >= 400) return "warn";
  return "info";
}

function write(line: RequestLog, asJson: boolean) {
  if (asJson) {
    console.log(JSON.stringify(line));
    return;
  }
  const who = line.accountId ? ` acct=${line.accountId.slice(0, 8)}` : "";
  console.log(
    `${line.method} ${line.path} ${line.status} ${line.durationMs}ms ` +
      `req=${line.requestId.slice(0, 8)}${who}`,
  );
}

/**
 * `json` is a parameter rather than a read of NODE_ENV inside the middleware so
 * the format is decided once at startup and can be forced either way in a test.
 */
export function requestLogger(options: { json: boolean }): MiddlewareHandler<AppBindings> {
  return async (c, next) => {
    const requestId = safeIncomingId(c.req.header(REQUEST_ID_HEADER)) ?? randomUUID();
    c.set("requestId", requestId);
    c.header(REQUEST_ID_HEADER, requestId);

    const started = performance.now();
    try {
      await next();
    } finally {
      // Read the account after the handler, because requireAuth is what puts it
      // there — before `next()` it is never set.
      // Typed as always present because requireAuth guarantees it downstream of
      // itself; on a public route it really is undefined at runtime.
      const account = c.get("account") as { id: string } | undefined;
      write(
        {
          at: new Date().toISOString(),
          level: levelFor(c.res.status),
          requestId,
          method: c.req.method,
          path: c.req.path,
          status: c.res.status,
          durationMs: Math.round(performance.now() - started),
          ...(account ? { accountId: account.id } : {}),
        },
        options.json,
      );
    }
  };
}
