import { and, isNotNull, lt, or, sql } from "drizzle-orm";
import type { Database } from "../db/client.js";
import { accounts, otpCodes, sessions } from "../db/schema.js";
import { ACCOUNT_DELETION_GRACE_DAYS } from "../lib/env.js";

/**
 * The housekeeping a long-running database needs and no request will ever do.
 *
 * Three jobs, one entry point, driven by an external scheduler rather than an
 * in-process timer: a timer runs once per instance, so two instances would run
 * it twice, and it does not run at all while the process is asleep — which is
 * exactly what a scale-to-zero host does at 3am.
 *
 * Everything takes `now` so the verifier can fast-forward past a grace period
 * instead of waiting thirty days.
 */

export interface MaintenanceReport {
  accountsPurged: number;
  otpCodesDeleted: number;
  sessionsDeleted: number;
  ranAt: string;
}

/**
 * Hard-deletes accounts whose grace period has run out.
 *
 * Every dependent row goes with them: the foreign keys are ON DELETE CASCADE,
 * so listings, photos, messages, tokens and follows disappear in the same
 * statement. That is the point of the grace period — up to here the row is
 * merely flagged and everything is recoverable; past it, nothing is.
 */
export async function purgeDeletedAccounts(
  db: Database,
  options: { now?: Date } = {},
): Promise<number> {
  const now = options.now ?? new Date();
  const cutoff = new Date(now.getTime() - ACCOUNT_DELETION_GRACE_DAYS * 86_400_000);

  const purged = await db
    .delete(accounts)
    .where(and(isNotNull(accounts.deletedAt), lt(accounts.deletedAt, cutoff)))
    .returning({ id: accounts.id });

  return purged.length;
}

/**
 * Login codes are useless the moment they expire or are used. They are kept for
 * neither audit nor analytics — a table of code hashes keyed by phone number is
 * a liability and nothing else — so they are deleted rather than archived.
 */
export async function purgeExpiredOtpCodes(
  db: Database,
  options: { now?: Date } = {},
): Promise<number> {
  const now = options.now ?? new Date();
  // Consumed codes linger only so a replay is rejected while the code is still
  // in someone's SMS inbox; past the expiry there is nothing left to replay.
  const deleted = await db
    .delete(otpCodes)
    .where(lt(otpCodes.expiresAt, now))
    .returning({ id: otpCodes.id });

  return deleted.length;
}

/**
 * Expired and revoked sessions. Revoked rows are kept for a further grace
 * window rather than deleted immediately, because a refresh token presented
 * after rotation is how token theft announces itself, and that signal is worth
 * more than the row costs.
 */
export async function purgeDeadSessions(
  db: Database,
  options: { now?: Date; revokedGraceDays?: number } = {},
): Promise<number> {
  const now = options.now ?? new Date();
  const graceDays = options.revokedGraceDays ?? 7;
  const revokedCutoff = new Date(now.getTime() - graceDays * 86_400_000);

  const deleted = await db
    .delete(sessions)
    .where(
      or(
        lt(sessions.expiresAt, now),
        and(isNotNull(sessions.revokedAt), lt(sessions.revokedAt, revokedCutoff)),
      ),
    )
    .returning({ id: sessions.id });

  return deleted.length;
}

export async function runMaintenance(
  db: Database,
  options: { now?: Date } = {},
): Promise<MaintenanceReport> {
  const now = options.now ?? new Date();

  // Accounts first: purging one cascades its sessions away, so the session
  // sweep afterwards has less to do rather than racing it.
  const accountsPurged = await purgeDeletedAccounts(db, { now });
  const otpCodesDeleted = await purgeExpiredOtpCodes(db, { now });
  const sessionsDeleted = await purgeDeadSessions(db, { now });

  return {
    accountsPurged,
    otpCodesDeleted,
    sessionsDeleted,
    ranAt: now.toISOString(),
  };
}

/** Counts what a run would remove, for a dry run or a monitoring endpoint. */
export async function maintenanceBacklog(
  db: Database,
  options: { now?: Date } = {},
): Promise<{ accounts: number; otpCodes: number; sessions: number }> {
  const now = options.now ?? new Date();
  const cutoff = new Date(now.getTime() - ACCOUNT_DELETION_GRACE_DAYS * 86_400_000);

  const [row] = await db
    .select({
      accounts: sql<number>`(SELECT count(*) FROM ${accounts}
                              WHERE ${accounts.deletedAt} IS NOT NULL
                                AND ${accounts.deletedAt} < ${cutoff})::int`,
      otpCodes: sql<number>`(SELECT count(*) FROM ${otpCodes}
                              WHERE ${otpCodes.expiresAt} < ${now})::int`,
      sessions: sql<number>`(SELECT count(*) FROM ${sessions}
                              WHERE ${sessions.expiresAt} < ${now})::int`,
    })
    .from(sql`(SELECT 1) AS one`);

  return {
    accounts: row?.accounts ?? 0,
    otpCodes: row?.otpCodes ?? 0,
    sessions: row?.sessions ?? 0,
  };
}
