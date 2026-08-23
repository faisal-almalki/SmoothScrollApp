import { createSign } from "node:crypto";

/**
 * Firebase Cloud Messaging, HTTP v1.
 *
 * Written against the REST API with node:crypto rather than firebase-admin for
 * the same reason SigV4 was: the whole of what is needed here is one RS256
 * signature and two POSTs, against tens of megabytes of SDK that would also
 * drag in its own gRPC stack.
 *
 * The legacy `fcm.googleapis.com/fcm/send` server-key API is dead — Google
 * turned it off in 2024 — so v1 with a service account is the only option.
 *
 * With no credentials configured, `isPushConfigured()` is false and every send
 * is a logged skip. That is the same shape as SmsSender: the feature is absent,
 * not broken, and the app is fully usable without it.
 */

/** Must match the channel the Android app creates, or the notification is silent. */
export const MESSAGES_CHANNEL_ID = "messages";

const TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
const SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
/** Refresh a minute early rather than racing the expiry on a slow request. */
const TOKEN_SKEW_SECONDS = 60;

interface ServiceAccount {
  projectId: string;
  clientEmail: string;
  privateKey: string;
}

let cachedAccount: ServiceAccount | null | undefined;

/**
 * The whole service-account JSON in one variable. It is what the Firebase
 * console hands you, it survives `fly secrets set` unmodified, and it cannot
 * end up half-configured the way three separate variables can.
 */
function loadServiceAccount(): ServiceAccount | null {
  if (cachedAccount !== undefined) return cachedAccount;

  const raw = process.env.FCM_SERVICE_ACCOUNT_JSON;
  if (!raw || raw.trim() === "") {
    cachedAccount = null;
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as {
      project_id?: string;
      client_email?: string;
      private_key?: string;
    };
    if (!parsed.project_id || !parsed.client_email || !parsed.private_key) {
      throw new Error("missing project_id, client_email or private_key");
    }
    cachedAccount = {
      projectId: parsed.project_id,
      clientEmail: parsed.client_email,
      // Shells and secret stores mangle real newlines, so the key is commonly
      // pasted with literal backslash-n. Accept both rather than making the
      // operator debug a PEM parse error.
      privateKey: parsed.private_key.replace(/\\n/g, "\n"),
    };
    return cachedAccount;
  } catch (error) {
    console.error(
      "[push] FCM_SERVICE_ACCOUNT_JSON is set but unusable, push is disabled:",
      (error as Error).message,
    );
    cachedAccount = null;
    return null;
  }
}

export function isPushConfigured(): boolean {
  return loadServiceAccount() !== null;
}

/** Test seam: forget the parsed credentials so a changed env is picked up. */
export function resetPushConfig(): void {
  cachedAccount = undefined;
  accessToken = null;
}

function base64url(input: Buffer | string): string {
  return Buffer.from(input).toString("base64url");
}

let accessToken: { value: string; expiresAt: number } | null = null;

/**
 * Exchanges a self-signed JWT for a Google access token, caching it until it is
 * nearly expired. Tokens last an hour, so this is one extra round trip per hour
 * rather than one per notification.
 */
async function getAccessToken(account: ServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (accessToken && accessToken.expiresAt - TOKEN_SKEW_SECONDS > now) {
    return accessToken.value;
  }

  const header = base64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = base64url(
    JSON.stringify({
      iss: account.clientEmail,
      scope: SCOPE,
      aud: TOKEN_ENDPOINT,
      iat: now,
      exp: now + 3600,
    }),
  );
  const signer = createSign("RSA-SHA256");
  signer.update(`${header}.${claims}`);
  const signature = signer.sign(account.privateKey).toString("base64url");
  const assertion = `${header}.${claims}.${signature}`;

  const response = await fetch(TOKEN_ENDPOINT, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });

  if (!response.ok) {
    // The body can echo the assertion; log the status only.
    throw new Error(`token exchange failed with ${response.status}`);
  }

  const body = (await response.json()) as { access_token: string; expires_in: number };
  accessToken = { value: body.access_token, expiresAt: now + body.expires_in };
  return body.access_token;
}

export interface PushMessage {
  token: string;
  title: string;
  body: string;
  /** Values must be strings — FCM rejects anything else in the data payload. */
  data?: Record<string, string>;
  /** Groups notifications from one conversation so ten messages are one row. */
  collapseKey?: string;
}

export type PushResult =
  | { status: "sent" }
  | { status: "skipped"; reason: string }
  /** The device uninstalled or the token rotated — the caller should drop it. */
  | { status: "stale" }
  | { status: "failed"; reason: string };

export async function sendPush(message: PushMessage): Promise<PushResult> {
  const account = loadServiceAccount();
  if (!account) return { status: "skipped", reason: "FCM_SERVICE_ACCOUNT_JSON is not set" };

  let token: string;
  try {
    token = await getAccessToken(account);
  } catch (error) {
    return { status: "failed", reason: (error as Error).message };
  }

  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${account.projectId}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token: message.token,
          // Both halves on purpose: `notification` is what the system tray
          // renders when the app is dead, `data` is what the app reads to open
          // the right thread when it is alive.
          notification: { title: message.title, body: message.body },
          data: message.data ?? {},
          android: {
            priority: "HIGH",
            ...(message.collapseKey ? { collapse_key: message.collapseKey } : {}),
            notification: { channel_id: MESSAGES_CHANNEL_ID, sound: "default" },
          },
        },
      }),
    },
  );

  if (response.ok) return { status: "sent" };

  const text = await response.text().catch(() => "");
  // 404 UNREGISTERED and 400 on the token itself both mean the token is dead.
  if (response.status === 404 || text.includes("UNREGISTERED") || text.includes("INVALID_ARGUMENT")) {
    return { status: "stale" };
  }
  return { status: "failed", reason: `FCM returned ${response.status}` };
}
