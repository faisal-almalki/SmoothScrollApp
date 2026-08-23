/**
 * Fan-out for Server-Sent Events.
 *
 * IMPORTANT — this registry lives in the memory of one process. It is correct
 * for a single instance and wrong the moment there are two: a message sent on
 * instance A will not reach a listener connected to instance B.
 *
 * Before scaling past one instance, replace the body of `publish` and
 * `subscribe` with Postgres LISTEN/NOTIFY (no new infrastructure, fine to a few
 * thousand listeners) or Redis pub/sub (better beyond that). Nothing outside
 * this file needs to change — that is the reason it is isolated here.
 */

export interface ConversationEvent {
  type: "message";
  conversationId: string;
  messageId: string;
  senderId: string;
  body: string;
  sentAt: string;
}

type Listener = (event: ConversationEvent) => void;

const listeners = new Map<string, Set<Listener>>();

/** Returns the unsubscribe function; callers must invoke it when the request ends. */
export function subscribe(conversationId: string, listener: Listener): () => void {
  let set = listeners.get(conversationId);
  if (!set) {
    set = new Set();
    listeners.set(conversationId, set);
  }
  set.add(listener);

  return () => {
    const current = listeners.get(conversationId);
    if (!current) return;
    current.delete(listener);
    // Drop the key entirely, otherwise the map grows without bound as
    // conversations come and go.
    if (current.size === 0) listeners.delete(conversationId);
  };
}

export function publish(event: ConversationEvent): void {
  const set = listeners.get(event.conversationId);
  if (!set) return;
  for (const listener of set) {
    // One bad listener must not stop the others from being notified.
    try {
      listener(event);
    } catch (error) {
      console.error("[events] listener failed", error);
    }
  }
}

/** Exposed for the verifier and for a health endpoint. */
export function listenerCount(conversationId?: string): number {
  if (conversationId) return listeners.get(conversationId)?.size ?? 0;
  let total = 0;
  for (const set of listeners.values()) total += set.size;
  return total;
}
