/**
 * The app's data shapes. These deliberately match the backend's JSON responses
 * (server/src/services and server/src/db/schema) field for field, so that when
 * the server is deployed, the seeded store below can be swapped for fetch()
 * calls with no change to any screen.
 */

export type Condition = "NEW" | "LIKE_NEW" | "USED";

export const CATEGORIES = [
  "سيارات",
  "جوالات",
  "إلكترونيات",
  "أثاث",
  "عقار",
  "ألعاب",
  "أزياء",
  "خدمات",
  "أخرى",
] as const;
export type Category = (typeof CATEGORIES)[number];

export const CITIES = [
  "الرياض",
  "جدة",
  "الدمام",
  "مكة",
  "المدينة",
  "الخبر",
  "الطائف",
  "تبوك",
  "أبها",
] as const;
export type City = (typeof CITIES)[number];

export interface Seller {
  id: string;
  handle: string;
  displayName: string;
  emoji: string;
  city: City;
  bio: string;
  rating: number;
  ratingCount: number;
  followerCount: number;
  isVerified: boolean;
  /** Only present when the seller chose to publish it (allowCalls). */
  publicPhone: string | null;
  allowCalls: boolean;
  allowMessages: boolean;
}

export interface Listing {
  id: string;
  sellerId: string;
  title: string;
  description: string;
  priceHalalas: number;
  isNegotiable: boolean;
  category: Category;
  condition: Condition;
  city: City;
  /** Emoji + gradient stand in for a photo, so the app renders with no assets. */
  emoji: string;
  createdAt: number;
  status: "ACTIVE" | "SOLD";
  views: number;
  enquiries: number;
}

export interface VideoPost {
  id: string;
  sellerId: string;
  /** Remote sample URL, or a local file:// from the camera. */
  videoUrl: string;
  caption: string;
  createdAt: number;
  /** The ad pinned on the video, if any. */
  listingId: string | null;
  likes: number;
  /** True for clips the user recorded on this device. */
  mine?: boolean;
}

export interface Message {
  id: string;
  conversationId: string;
  fromMe: boolean;
  body: string;
  sentAt: number;
}

export interface Conversation {
  id: string;
  listingId: string;
  sellerId: string;
  lastMessage: string;
  lastAt: number;
  unread: number;
}
