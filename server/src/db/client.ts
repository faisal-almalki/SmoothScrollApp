import { neon, neonConfig, Pool } from "@neondatabase/serverless";
import { drizzle } from "drizzle-orm/neon-serverless";
import { drizzle as drizzleHttp } from "drizzle-orm/neon-http";
import type { PgDatabase } from "drizzle-orm/pg-core";
import * as schema from "./schema.js";

/**
 * Two ways into the same Neon database.
 *
 * The HTTP client is a single round trip per query and works from edge runtimes
 * where TCP is unavailable — it is the right default for ordinary request
 * handling. It cannot hold a transaction open across statements, so anything
 * that needs one (posting an ad plus its photos, sending a message plus bumping
 * the conversation) uses the pooled WebSocket client instead.
 */

function requireDatabaseUrl(): string {
  const url = process.env.DATABASE_URL;
  if (!url) {
    throw new Error(
      "DATABASE_URL is not set. Copy .env.example to .env and paste your Neon " +
        "connection string (Neon dashboard → Connection Details → Pooled connection).",
    );
  }
  return url;
}

/** Query client. Use for reads and single-statement writes. */
export function createDb() {
  return drizzleHttp(neon(requireDatabaseUrl()), { schema });
}

/** Transaction client. Open one pool per process, not per request. */
export function createPooledDb() {
  // Neon's driver speaks WebSocket; in Node it needs the ws polyfill wired up,
  // which @neondatabase/serverless does itself from v0.9 onwards.
  neonConfig.poolQueryViaFetch = true;
  const pool = new Pool({ connectionString: requireDatabaseUrl() });
  return { db: drizzle(pool, { schema }), pool };
}

export { schema };

/**
 * Deliberately loose so one set of service functions runs against Neon in
 * production and against PGlite in `scripts/verify.ts`. Both are Postgres and
 * both satisfy the query builder; pinning the concrete driver type here would
 * force the tests to fake something instead of exercising the real code.
 */
export type Database = PgDatabase<any, any, any>;
