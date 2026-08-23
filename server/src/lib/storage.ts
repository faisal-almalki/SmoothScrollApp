import { createHash, createHmac } from "node:crypto";

/**
 * Presigned direct-to-storage uploads.
 *
 * The phone uploads straight to object storage and only tells the API the key
 * afterwards, so photos and video never pass through this server — which is what
 * keeps it small and cheap.
 *
 * SigV4 is implemented here with node:crypto rather than pulling in the AWS SDK.
 * The SDK is tens of megabytes and this needs exactly one signature. The scheme
 * is S3-compatible, so Cloudflare R2, Backblaze B2 and S3 itself all work by
 * changing environment variables only.
 */

export const PHOTO_CONTENT_TYPES = ["image/jpeg", "image/png", "image/webp", "image/heic"] as const;
export const VIDEO_CONTENT_TYPES = ["video/mp4", "video/quicktime"] as const;

export const MAX_PHOTO_BYTES = 12 * 1024 * 1024;
export const MAX_VIDEO_BYTES = 300 * 1024 * 1024;
export const UPLOAD_URL_TTL_SECONDS = 15 * 60;

export interface PresignedUpload {
  uploadUrl: string;
  key: string;
  expiresAt: Date;
  /** True when no storage credentials are configured and the URL is a stand-in. */
  isPlaceholder: boolean;
}

interface StorageConfig {
  endpoint: string;
  region: string;
  bucket: string;
  accessKeyId: string;
  secretAccessKey: string;
  publicBaseUrl: string;
}

function readConfig(): StorageConfig | null {
  const endpoint = process.env.S3_ENDPOINT;
  const bucket = process.env.S3_BUCKET;
  const accessKeyId = process.env.S3_ACCESS_KEY_ID;
  const secretAccessKey = process.env.S3_SECRET_ACCESS_KEY;
  if (!endpoint || !bucket || !accessKeyId || !secretAccessKey) return null;
  return {
    endpoint: endpoint.replace(/\/+$/, ""),
    region: process.env.S3_REGION || "auto",
    bucket,
    accessKeyId,
    secretAccessKey,
    publicBaseUrl: (process.env.S3_PUBLIC_BASE_URL || `${endpoint}/${bucket}`).replace(/\/+$/, ""),
  };
}

/** RFC 3986, which is stricter than encodeURIComponent about these six. */
function encodeRfc3986(value: string): string {
  return encodeURIComponent(value).replace(
    /[!'()*]/g,
    (c) => `%${c.charCodeAt(0).toString(16).toUpperCase()}`,
  );
}

function encodeKeyPath(key: string): string {
  return key.split("/").map(encodeRfc3986).join("/");
}

function hmac(key: Buffer | string, data: string): Buffer {
  return createHmac("sha256", key).update(data, "utf8").digest();
}

function sha256Hex(data: string): string {
  return createHash("sha256").update(data, "utf8").digest("hex");
}

function signingKey(secret: string, date: string, region: string, service: string): Buffer {
  return hmac(hmac(hmac(hmac(`AWS4${secret}`, date), region), service), "aws4_request");
}

/**
 * Builds a presigned PUT URL. The signature covers the method, path, query and
 * the host header; the body is left unsigned so the phone can stream the file
 * without hashing it first.
 */
function presignPut(
  config: StorageConfig,
  key: string,
  expiresInSeconds: number,
  now: Date,
): string {
  const url = new URL(`${config.endpoint}/${config.bucket}/${encodeKeyPath(key)}`);
  const amzDate = now.toISOString().replace(/[:-]|\.\d{3}/g, "");
  const dateStamp = amzDate.slice(0, 8);
  const scope = `${dateStamp}/${config.region}/s3/aws4_request`;

  const query: Record<string, string> = {
    "X-Amz-Algorithm": "AWS4-HMAC-SHA256",
    "X-Amz-Credential": `${config.accessKeyId}/${scope}`,
    "X-Amz-Date": amzDate,
    "X-Amz-Expires": String(expiresInSeconds),
    "X-Amz-SignedHeaders": "host",
  };

  const canonicalQuery = Object.keys(query)
    .sort()
    .map((name) => `${encodeRfc3986(name)}=${encodeRfc3986(query[name]!)}`)
    .join("&");

  const canonicalRequest = [
    "PUT",
    url.pathname,
    canonicalQuery,
    `host:${url.host}\n`,
    "host",
    "UNSIGNED-PAYLOAD",
  ].join("\n");

  const stringToSign = [
    "AWS4-HMAC-SHA256",
    amzDate,
    scope,
    sha256Hex(canonicalRequest),
  ].join("\n");

  const signature = createHmac(
    "sha256",
    signingKey(config.secretAccessKey, dateStamp, config.region, "s3"),
  )
    .update(stringToSign, "utf8")
    .digest("hex");

  return `${url.origin}${url.pathname}?${canonicalQuery}&X-Amz-Signature=${signature}`;
}

export function createUploadUrl(
  key: string,
  options: { now?: Date; expiresInSeconds?: number } = {},
): PresignedUpload {
  const now = options.now ?? new Date();
  const ttl = options.expiresInSeconds ?? UPLOAD_URL_TTL_SECONDS;
  const expiresAt = new Date(now.getTime() + ttl * 1000);
  const config = readConfig();

  if (!config) {
    // PLACEHOLDER DRIVER — used when no storage credentials are configured, so
    // the verifier and local development work without a cloud account. It is
    // not a working upload target; the host is a reserved invalid TLD so a
    // request to it fails loudly rather than silently going somewhere real.
    return {
      uploadUrl: `https://storage.invalid/${key}?placeholder=1&expires=${expiresAt.toISOString()}`,
      key,
      expiresAt,
      isPlaceholder: true,
    };
  }

  return { uploadUrl: presignPut(config, key, ttl, now), key, expiresAt, isPlaceholder: false };
}

/** Where the object will be readable once uploaded. */
export function publicUrlFor(key: string): string {
  const config = readConfig();
  if (!config) return `https://storage.invalid/${key}`;
  return `${config.publicBaseUrl}/${encodeKeyPath(key)}`;
}

export function isStorageConfigured(): boolean {
  return readConfig() !== null;
}

/** Keys are namespaced by owner so a leaked key cannot be guessed into another account. */
export function buildObjectKey(
  kind: "listing-photos" | "videos",
  ownerId: string,
  filename: string,
): string {
  const safe = filename.replace(/[^a-zA-Z0-9._-]/g, "_").slice(0, 60) || "file";
  const stamp = Date.now().toString(36);
  const noise = Math.random().toString(36).slice(2, 10);
  return `${kind}/${ownerId}/${stamp}-${noise}-${safe}`;
}
