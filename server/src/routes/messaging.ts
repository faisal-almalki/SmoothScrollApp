import { Hono } from "hono";
import { streamSSE } from "hono/streaming";
import { z } from "zod";
import { ApiError, parseBody } from "../lib/http.js";
import { subscribe, type ConversationEvent } from "../lib/events.js";
import type { AppBindings } from "../middleware/auth.js";
import { requireAuth } from "../middleware/auth.js";
import {
  assertParticipant,
  listConversations,
  listMessages,
  markConversationRead,
  openConversation,
  sendMessage,
  totalUnread,
} from "../services/messaging.js";

const openSchema = z.object({ listingId: z.string().uuid() });
const sendSchema = z.object({ body: z.string().min(1).max(4000) });

function pageLimit(value: string | undefined): number | undefined {
  if (!value) return undefined;
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1) {
    throw ApiError.badRequest("invalid_query", "limit must be a positive whole number.");
  }
  return parsed;
}

export const conversationRoutes = new Hono<AppBindings>();

conversationRoutes.use("*", requireAuth);

// Declared before "/:id/..." so the literal path is not captured as an id.
conversationRoutes.get("/unread-count", async (c) =>
  c.json({ unread: await totalUnread(c.get("db"), c.get("account").id) }),
);

conversationRoutes.get("/", async (c) => {
  const page = await listConversations(c.get("db"), c.get("account").id, {
    limit: pageLimit(c.req.query("limit")),
    cursor: c.req.query("cursor"),
  });
  return c.json(page);
});

conversationRoutes.post("/", async (c) => {
  const { listingId } = await parseBody(c, openSchema);
  const result = await openConversation(c.get("db"), c.get("account").id, listingId);
  return c.json(result, result.created ? 201 : 200);
});

conversationRoutes.get("/:id/messages", async (c) => {
  const page = await listMessages(c.get("db"), c.req.param("id"), c.get("account").id, {
    limit: pageLimit(c.req.query("limit")),
    cursor: c.req.query("cursor"),
  });
  return c.json(page);
});

conversationRoutes.post("/:id/messages", async (c) => {
  const { body } = await parseBody(c, sendSchema);
  const saved = await sendMessage(c.get("db"), c.req.param("id"), c.get("account").id, body);
  return c.json(saved, 201);
});

conversationRoutes.post("/:id/read", async (c) =>
  c.json(await markConversationRead(c.get("db"), c.req.param("id"), c.get("account").id)),
);

/**
 * Live messages over Server-Sent Events.
 *
 * SSE rather than WebSocket on purpose: it is plain HTTP, so it survives proxies
 * and serverless hosting that will not do an upgrade handshake, and the browser
 * and OkHttp both reconnect on their own. The app only needs server-to-client
 * push here — messages are sent over POST — so the extra duplex of a socket
 * would buy nothing.
 */
conversationRoutes.get("/:id/stream", async (c) => {
  const conversationId = c.req.param("id");
  // Authorise before opening the stream, so a stranger never gets a live feed.
  await assertParticipant(c.get("db"), conversationId, c.get("account").id);

  return streamSSE(c, async (stream) => {
    const queue: ConversationEvent[] = [];
    let wake: (() => void) | null = null;

    const unsubscribe = subscribe(conversationId, (event) => {
      queue.push(event);
      wake?.();
    });

    stream.onAbort(() => {
      unsubscribe();
      wake?.();
    });

    await stream.writeSSE({ event: "ready", data: JSON.stringify({ conversationId }) });

    try {
      while (!stream.aborted) {
        while (queue.length > 0) {
          const event = queue.shift()!;
          await stream.writeSSE({
            event: "message",
            id: event.messageId,
            data: JSON.stringify(event),
          });
        }
        if (stream.aborted) break;

        // Wait for the next event, but wake periodically to send a comment so
        // proxies do not treat the idle connection as dead.
        await new Promise<void>((resolve) => {
          wake = resolve;
          setTimeout(resolve, 25_000);
        });
        wake = null;
        if (!stream.aborted && queue.length === 0) {
          await stream.writeSSE({ event: "ping", data: "" });
        }
      }
    } finally {
      unsubscribe();
    }
  });
});
