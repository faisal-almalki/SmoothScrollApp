import { Hono } from "hono";
import { z } from "zod";
import { ApiError, parseBody } from "../lib/http.js";
import type { AppBindings } from "../middleware/auth.js";
import { requireAuth } from "../middleware/auth.js";
import { verifyAccessToken } from "../lib/tokens.js";
import {
  BROWSE_MAX_PAGE_SIZE,
  blockAccount,
  browseListings,
  bumpListing,
  createListing,
  followSeller,
  getListing,
  removeListing,
  reportSomething,
  setListingSold,
  unblockAccount,
  unfollowSeller,
  updateListing,
} from "../services/listings.js";

const conditionSchema = z.enum(["NEW", "LIKE_NEW", "USED"]);

const createSchema = z.object({
  title: z.string().min(3).max(120),
  description: z.string().max(4000).optional(),
  priceHalalas: z.number().int().min(0).max(1_000_000_00_000),
  isNegotiable: z.boolean().optional(),
  category: z.string().min(2).max(40),
  condition: conditionSchema.optional(),
  emoji: z.string().max(8).optional(),
  city: z.string().min(2).max(60),
});

const patchSchema = createSchema.partial();
const soldSchema = z.object({ isSold: z.boolean() });
const reportSchema = z.object({
  targetType: z.enum(["LISTING", "ACCOUNT", "MESSAGE", "LIVE_STREAM"]),
  targetId: z.string().uuid(),
  reason: z.string().min(2).max(80),
  note: z.string().max(1000).optional(),
});
const blockSchema = z.object({ accountId: z.string().uuid() });

/**
 * Browse works signed out, but a signed-in viewer gets their blocks applied.
 * The token is read optionally rather than through requireAuth so an anonymous
 * request is not rejected.
 */
async function optionalViewerId(c: {
  req: { header: (name: string) => string | undefined };
}): Promise<string | undefined> {
  const header = c.req.header("Authorization");
  const token = header?.toLowerCase().startsWith("bearer ") ? header.slice(7) : undefined;
  if (!token) return undefined;
  return (await verifyAccessToken(token)) ?? undefined;
}

function intParam(value: string | undefined, name: string): number | undefined {
  if (value === undefined || value === "") return undefined;
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 0) {
    throw ApiError.badRequest("invalid_query", `${name} must be a whole number.`);
  }
  return parsed;
}

export const listingRoutes = new Hono<AppBindings>();

listingRoutes.get("/", async (c) => {
  const q = c.req.query();
  const condition = q.condition ? conditionSchema.safeParse(q.condition) : null;
  if (condition && !condition.success) {
    throw ApiError.badRequest("invalid_query", "condition must be NEW, LIKE_NEW or USED.");
  }

  const page = await browseListings(c.get("db"), {
    q: q.q,
    city: q.city,
    category: q.category,
    condition: condition?.success ? condition.data : undefined,
    minPriceHalalas: intParam(q.minPrice, "minPrice"),
    maxPriceHalalas: intParam(q.maxPrice, "maxPrice"),
    sellerId: q.sellerId,
    limit: intParam(q.limit, "limit") ?? undefined,
    cursor: q.cursor,
    viewerId: await optionalViewerId(c),
  });

  return c.json({ ...page, pageSize: Math.min(Number(q.limit ?? 20), BROWSE_MAX_PAGE_SIZE) });
});

listingRoutes.get("/:id", async (c) => {
  const listing = await getListing(c.get("db"), c.req.param("id"), {
    viewerId: await optionalViewerId(c),
    countView: true,
  });
  return c.json(listing);
});

listingRoutes.post("/", requireAuth, async (c) => {
  const body = await parseBody(c, createSchema);
  const listing = await createListing(c.get("db"), c.get("account").id, body);
  return c.json(listing, 201);
});

listingRoutes.patch("/:id", requireAuth, async (c) => {
  const body = await parseBody(c, patchSchema);
  const listing = await updateListing(c.get("db"), c.req.param("id"), c.get("account").id, body);
  return c.json(listing);
});

listingRoutes.post("/:id/sold", requireAuth, async (c) => {
  const { isSold } = await parseBody(c, soldSchema);
  const listing = await setListingSold(c.get("db"), c.req.param("id"), c.get("account").id, isSold);
  return c.json(listing);
});

listingRoutes.delete("/:id", requireAuth, async (c) => {
  await removeListing(c.get("db"), c.req.param("id"), c.get("account").id);
  return c.body(null, 204);
});

listingRoutes.post("/:id/bump", requireAuth, async (c) => {
  const listing = await bumpListing(c.get("db"), c.req.param("id"), c.get("account").id);
  return c.json(listing);
});

export const sellerRoutes = new Hono<AppBindings>();

sellerRoutes.post("/:id/follow", requireAuth, async (c) =>
  c.json(await followSeller(c.get("db"), c.get("account").id, c.req.param("id"))),
);

sellerRoutes.delete("/:id/follow", requireAuth, async (c) =>
  c.json(await unfollowSeller(c.get("db"), c.get("account").id, c.req.param("id"))),
);

export const safetyRoutes = new Hono<AppBindings>();

safetyRoutes.post("/reports", requireAuth, async (c) => {
  const body = await parseBody(c, reportSchema);
  const result = await reportSomething(
    c.get("db"),
    c.get("account").id,
    body.targetType,
    body.targetId,
    body.reason,
    body.note,
  );
  return c.json(result, 201);
});

safetyRoutes.post("/blocks", requireAuth, async (c) => {
  const { accountId } = await parseBody(c, blockSchema);
  return c.json(await blockAccount(c.get("db"), c.get("account").id, accountId), 201);
});

safetyRoutes.delete("/blocks/:id", requireAuth, async (c) =>
  c.json(await unblockAccount(c.get("db"), c.get("account").id, c.req.param("id"))),
);
