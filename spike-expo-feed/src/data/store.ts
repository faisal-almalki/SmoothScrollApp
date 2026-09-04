import { useSyncExternalStore } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";
import {
  CONVERSATIONS, LISTINGS, MESSAGES, SELLERS, VIDEOS,
} from "./seed";
import type { Conversation, Listing, Message, Seller, VideoPost } from "./types";

/**
 * A tiny observable store.
 *
 * The screens read it through useStore() and re-render on change; writes go
 * through the action functions. It keeps the seeded catalogue and layers the
 * user's own activity on top — the clips they record, the ads they post, the
 * chats they open — persisting that layer to the device so it survives a
 * restart. Seed data is never persisted; only what the user made.
 *
 * This is deliberately the same shape a real API client would expose, so the
 * swap to the backend later is action-by-action, not a rewrite.
 */

const ME_KEY = "me.v1";
const PERSIST_KEY = "userdata.v1";

export interface Me {
  id: string;
  handle: string;
  displayName: string;
  emoji: string;
  city: string;
  publicPhone: string | null;
  allowCalls: boolean;
}

interface State {
  ready: boolean;
  me: Me | null;
  sellers: Seller[];
  listings: Listing[];
  videos: VideoPost[];
  conversations: Conversation[];
  messages: Message[];
  follows: Record<string, boolean>;
  likes: Record<string, boolean>;
}

let state: State = {
  ready: false,
  me: null,
  sellers: SELLERS,
  listings: LISTINGS,
  videos: VIDEOS,
  conversations: CONVERSATIONS,
  messages: MESSAGES,
  follows: {},
  likes: {},
};

const listeners = new Set<() => void>();
function emit() {
  for (const l of listeners) l();
}
function set(patch: Partial<State>) {
  state = { ...state, ...patch };
  emit();
}

// Only the user-made layer is persisted; the seed is code, not data.
interface Persisted {
  myListings: Listing[];
  myVideos: VideoPost[];
  conversations: Conversation[];
  messages: Message[];
  follows: Record<string, boolean>;
  likes: Record<string, boolean>;
}

async function persist() {
  const p: Persisted = {
    myListings: state.listings.filter((l) => !LISTINGS.some((s) => s.id === l.id)),
    myVideos: state.videos.filter((v) => v.mine),
    conversations: state.conversations.filter((c) => !CONVERSATIONS.some((s) => s.id === c.id)),
    messages: state.messages.filter((m) => !MESSAGES.some((s) => s.id === m.id)),
    follows: state.follows,
    likes: state.likes,
  };
  try {
    await AsyncStorage.setItem(PERSIST_KEY, JSON.stringify(p));
  } catch {
    // A failed write loses a draft, never crashes the app.
  }
}

export async function hydrate() {
  try {
    const [meRaw, dataRaw] = await Promise.all([
      AsyncStorage.getItem(ME_KEY),
      AsyncStorage.getItem(PERSIST_KEY),
    ]);
    const me: Me | null = meRaw ? JSON.parse(meRaw) : null;
    const p: Persisted | null = dataRaw ? JSON.parse(dataRaw) : null;
    if (p) {
      set({
        me,
        ready: true,
        listings: [...p.myListings, ...LISTINGS],
        videos: [...p.myVideos, ...VIDEOS],
        conversations: [...p.conversations, ...CONVERSATIONS],
        messages: [...MESSAGES, ...p.messages],
        follows: p.follows ?? {},
        likes: p.likes ?? {},
      });
    } else {
      set({ me, ready: true });
    }
  } catch {
    set({ ready: true });
  }
}

// ---- selectors ----
export function useStore<T>(selector: (s: State) => T): T {
  return useSyncExternalStore(
    (cb) => {
      listeners.add(cb);
      return () => listeners.delete(cb);
    },
    () => selector(state),
    () => selector(state),
  );
}

export const getState = () => state;
export const sellerById = (id: string) => state.sellers.find((s) => s.id === id);
export const listingById = (id: string) => state.listings.find((l) => l.id === id);

// ---- auth ----
export async function signIn(me: Me) {
  set({ me });
  await AsyncStorage.setItem(ME_KEY, JSON.stringify(me));
}
export async function signOut() {
  set({ me: null });
  await AsyncStorage.removeItem(ME_KEY);
}
export async function updateMe(patch: Partial<Me>) {
  if (!state.me) return;
  const me = { ...state.me, ...patch };
  set({ me });
  await AsyncStorage.setItem(ME_KEY, JSON.stringify(me));
}

// ---- actions ----
let seq = 0;
const newId = (p: string) => `${p}_${Date.now().toString(36)}_${seq++}`;

export function postListing(input: {
  title: string; priceHalalas: number; category: Listing["category"];
  condition: Listing["condition"]; city: Listing["city"]; description: string;
  isNegotiable: boolean; emoji: string;
}): Listing {
  const me = state.me;
  const listing: Listing = {
    id: newId("l"),
    sellerId: me?.id ?? "me",
    title: input.title,
    description: input.description,
    priceHalalas: input.priceHalalas,
    isNegotiable: input.isNegotiable,
    category: input.category,
    condition: input.condition,
    city: input.city,
    emoji: input.emoji,
    createdAt: Date.now(),
    status: "ACTIVE",
    views: 0,
    enquiries: 0,
  };
  set({ listings: [listing, ...state.listings] });
  void persist();
  return listing;
}

export function postVideo(input: { videoUrl: string; caption: string; listingId: string | null }): VideoPost {
  const me = state.me;
  const video: VideoPost = {
    id: newId("v"),
    sellerId: me?.id ?? "me",
    videoUrl: input.videoUrl,
    caption: input.caption,
    createdAt: Date.now(),
    listingId: input.listingId,
    likes: 0,
    mine: true,
  };
  set({ videos: [video, ...state.videos] });
  void persist();
  return video;
}

export function markSold(listingId: string, sold: boolean) {
  set({
    listings: state.listings.map((l) =>
      l.id === listingId ? { ...l, status: sold ? "SOLD" : "ACTIVE" } : l,
    ),
  });
  void persist();
}

export function toggleFollow(sellerId: string) {
  const follows = { ...state.follows, [sellerId]: !state.follows[sellerId] };
  set({ follows });
  void persist();
}

export function toggleLike(videoId: string) {
  const likes = { ...state.likes, [videoId]: !state.likes[videoId] };
  set({ likes });
  void persist();
}

/** Opens (or reuses) a thread with a listing's seller and returns its id. */
export function openConversation(listingId: string): string {
  const existing = state.conversations.find((c) => c.listingId === listingId);
  if (existing) return existing.id;
  const listing = listingById(listingId);
  const convo: Conversation = {
    id: newId("c"),
    listingId,
    sellerId: listing?.sellerId ?? "s1",
    lastMessage: "",
    lastAt: Date.now(),
    unread: 0,
  };
  set({ conversations: [convo, ...state.conversations] });
  void persist();
  return convo.id;
}

export function sendMessage(conversationId: string, body: string) {
  const msg: Message = {
    id: newId("m"),
    conversationId,
    fromMe: true,
    body,
    sentAt: Date.now(),
  };
  const conversations = state.conversations.map((c) =>
    c.id === conversationId ? { ...c, lastMessage: body, lastAt: Date.now() } : c,
  );
  set({ messages: [...state.messages, msg], conversations });
  void persist();

  // A one-off canned reply so a fresh thread is not a dead end while there is
  // no other person on the line. Clearly a demo convenience, not a fake seller.
  setTimeout(() => {
    const reply: Message = {
      id: newId("m"),
      conversationId,
      fromMe: false,
      body: "شكراً لتواصلك 🙏 (رد تلقائي — النسخة التجريبية بدون خادم)",
      sentAt: Date.now(),
    };
    set({
      messages: [...state.messages, reply],
      conversations: state.conversations.map((c) =>
        c.id === conversationId ? { ...c, lastMessage: reply.body, lastAt: Date.now() } : c,
      ),
    });
    void persist();
  }, 1400);
}
