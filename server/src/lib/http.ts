import type { Context } from "hono";
import { HTTPException } from "hono/http-exception";

/**
 * One error shape for the whole API, so the Android client can branch on a
 * stable `code` instead of parsing prose.
 */
export interface ApiErrorBody {
  error: { code: string; message: string; details?: unknown };
}

export class ApiError extends HTTPException {
  readonly code: string;
  readonly details?: unknown;

  constructor(status: 400 | 401 | 403 | 404 | 409 | 422 | 429 | 500, code: string, message: string, details?: unknown) {
    super(status, { message });
    this.code = code;
    this.details = details;
  }

  static badRequest(code: string, message: string, details?: unknown) {
    return new ApiError(400, code, message, details);
  }
  static unauthorized(message = "Sign in to continue.") {
    return new ApiError(401, "unauthorized", message);
  }
  static forbidden(message: string) {
    return new ApiError(403, "forbidden", message);
  }
  static notFound(message: string) {
    return new ApiError(404, "not_found", message);
  }
  static conflict(code: string, message: string) {
    return new ApiError(409, code, message);
  }
  static tooManyRequests(message: string) {
    return new ApiError(429, "rate_limited", message);
  }
}

export function errorBody(error: ApiError): ApiErrorBody {
  return {
    error: {
      code: error.code,
      message: error.message,
      ...(error.details !== undefined ? { details: error.details } : {}),
    },
  };
}

/** Parses and validates a JSON body, turning a schema failure into a 422. */
export async function parseBody<T>(
  c: Context,
  schema: { safeParse: (input: unknown) => { success: true; data: T } | { success: false; error: { issues: unknown } } },
): Promise<T> {
  let raw: unknown;
  try {
    raw = await c.req.json();
  } catch {
    throw ApiError.badRequest("invalid_json", "The request body is not valid JSON.");
  }
  const result = schema.safeParse(raw);
  if (!result.success) {
    throw new ApiError(422, "validation_failed", "Some fields are missing or invalid.", result.error.issues);
  }
  return result.data;
}
