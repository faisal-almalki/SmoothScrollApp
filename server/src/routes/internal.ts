import { Hono } from "hono";
import type { AppBindings } from "../middleware/auth.js";
import { requireMachineSecret } from "../middleware/machine.js";
import { maintenanceBacklog, runMaintenance } from "../services/maintenance.js";

/**
 * Scheduled housekeeping, exposed as a route so any scheduler can drive it —
 * fly.io machines, a GitHub Actions cron, Upstash QStash, or curl from a
 * laptop. Nothing about it assumes a particular host.
 *
 *   curl -X POST https://your-api/internal/maintenance \
 *        -H "X-Maintenance-Secret: $MAINTENANCE_SECRET"
 */
export const internalRoutes = new Hono<AppBindings>();

const guard = requireMachineSecret("MAINTENANCE_SECRET", "X-Maintenance-Secret");

/** Read-only: what a run would remove. Safe to poll from a monitor. */
internalRoutes.get("/maintenance", guard, async (c) => {
  return c.json({ backlog: await maintenanceBacklog(c.get("db")) });
});

internalRoutes.post("/maintenance", guard, async (c) => {
  const report = await runMaintenance(c.get("db"));
  // Deletion is irreversible, so it is logged whether or not it removed
  // anything — "the job ran and found nothing" and "the job never ran" look
  // identical in a database and very different in a log.
  console.log(JSON.stringify({ at: report.ranAt, event: "maintenance", ...report }));
  return c.json(report);
});
