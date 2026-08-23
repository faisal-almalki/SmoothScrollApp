import { neon } from "@neondatabase/serverless";
import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { createHash } from "node:crypto";

/**
 * Applies the generated migrations to Neon.
 *
 * Each file runs once and is recorded with a checksum, so re-running is safe and
 * an edited migration that has already been applied is reported rather than
 * silently skipped.
 *
 *   DATABASE_URL="postgres://..." npm run db:migrate
 */

const MIGRATIONS_DIR = join(import.meta.dirname, "..", "drizzle");

const url = process.env.DATABASE_URL;
if (!url) {
  console.error(
    "\nDATABASE_URL is not set.\n\n" +
      "  1. Create a project at https://console.neon.tech\n" +
      "  2. Copy the pooled connection string\n" +
      '  3. DATABASE_URL="postgres://..." npm run db:migrate\n',
  );
  process.exit(1);
}

const sql = neon(url);

await sql`
  CREATE TABLE IF NOT EXISTS _migrations (
    name text PRIMARY KEY,
    checksum text NOT NULL,
    applied_at timestamptz NOT NULL DEFAULT now()
  )
`;

const applied = new Map<string, string>(
  (
    (await sql`SELECT name, checksum FROM _migrations`) as {
      name: string;
      checksum: string;
    }[]
  ).map((r) => [r.name, r.checksum]),
);

const files = readdirSync(MIGRATIONS_DIR)
  .filter((f) => f.endsWith(".sql"))
  .sort();

let ran = 0;

for (const file of files) {
  const body = readFileSync(join(MIGRATIONS_DIR, file), "utf8");
  const checksum = createHash("sha256").update(body).digest("hex").slice(0, 16);
  const previous = applied.get(file);

  if (previous === checksum) {
    console.log(`  = ${file} (already applied)`);
    continue;
  }
  if (previous && previous !== checksum) {
    console.error(
      `\n  ! ${file} was already applied but its contents changed.\n` +
        `    Add a new migration instead of editing this one.\n`,
    );
    process.exit(1);
  }

  const statements = body
    .split("--> statement-breakpoint")
    .map((s) => s.trim())
    .filter(Boolean);

  for (const statement of statements) {
    // Plain-function form runs an arbitrary statement; the tagged-template form
    // is for parameterised queries.
    await sql(statement);
  }
  await sql`INSERT INTO _migrations (name, checksum) VALUES (${file}, ${checksum})`;
  console.log(`  + ${file} (${statements.length} statements)`);
  ran++;
}

console.log(
  ran === 0
    ? "\nDatabase already up to date.\n"
    : `\nApplied ${ran} migration${ran === 1 ? "" : "s"}.\n`,
);
