import type { Conversation, Listing, Message, Seller, VideoPost } from "./types";

/**
 * The offline catalogue. It is what makes the app fully explorable on a phone
 * with no backend and no account — browse, open an ad, watch the feed, start a
 * chat — exactly the way the Kotlin app's seeded data did.
 *
 * Sample videos are Google's public bucket: permanent, free, and unauthenticated.
 */
const GTV = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample";

export const SELLERS: Seller[] = [
  {
    id: "s1", handle: "abu_faisal_cars", displayName: "أبو فيصل للسيارات", emoji: "🚗",
    city: "الرياض", bio: "معرض سيارات مستعملة — ضمان وفحص", rating: 4.8, ratingCount: 214,
    followerCount: 1820, isVerified: true, publicPhone: "0555123456", allowCalls: true, allowMessages: true,
  },
  {
    id: "s2", handle: "tech_haven", displayName: "عالم التقنية", emoji: "📱",
    city: "جدة", bio: "جوالات ولابتوبات جديدة ومستعملة", rating: 4.6, ratingCount: 98,
    followerCount: 640, isVerified: true, publicPhone: "0533987654", allowCalls: true, allowMessages: true,
  },
  {
    id: "s3", handle: "beit_alathath", displayName: "بيت الأثاث", emoji: "🛋️",
    city: "الدمام", bio: "أثاث منزلي بأسعار منافسة", rating: 4.4, ratingCount: 51,
    followerCount: 305, isVerified: false, publicPhone: null, allowCalls: false, allowMessages: true,
  },
  {
    id: "s4", handle: "sara_style", displayName: "سارة ستايل", emoji: "👗",
    city: "الرياض", bio: "أزياء وإكسسوارات نسائية", rating: 4.9, ratingCount: 176,
    followerCount: 2410, isVerified: true, publicPhone: "0509001122", allowCalls: true, allowMessages: true,
  },
  {
    id: "s5", handle: "gamer_zone", displayName: "منطقة الألعاب", emoji: "🎮",
    city: "الخبر", bio: "أجهزة وألعاب — بيع وشراء وتبديل", rating: 4.5, ratingCount: 63,
    followerCount: 512, isVerified: false, publicPhone: "0561234567", allowCalls: true, allowMessages: true,
  },
];

let t = Date.now();
const ago = (mins: number) => t - mins * 60_000;

export const LISTINGS: Listing[] = [
  { id: "l1", sellerId: "s1", title: "كامري 2021 فل كامل", description: "ماشية 60 ألف، فحص كامل، بصمة وشاشة.", priceHalalas: 8900000, isNegotiable: true, category: "سيارات", condition: "USED", city: "الرياض", emoji: "🚗", createdAt: ago(35), status: "ACTIVE", views: 1240, enquiries: 37 },
  { id: "l2", sellerId: "s2", title: "آيفون 15 برو ماكس 256", description: "جديد مسكر، ضمان الوكيل سنة.", priceHalalas: 470000, isNegotiable: false, category: "جوالات", condition: "NEW", city: "جدة", emoji: "📱", createdAt: ago(80), status: "ACTIVE", views: 860, enquiries: 22 },
  { id: "l3", sellerId: "s3", title: "طقم كنب 7 مقاعد", description: "قماش فخم، استعمال شهرين بس.", priceHalalas: 320000, isNegotiable: true, category: "أثاث", condition: "LIKE_NEW", city: "الدمام", emoji: "🛋️", createdAt: ago(160), status: "ACTIVE", views: 410, enquiries: 9 },
  { id: "l4", sellerId: "s4", title: "عباية سوداء مطرزة", description: "خامة كريب ياباني، مقاس حر.", priceHalalas: 45000, isNegotiable: false, category: "أزياء", condition: "NEW", city: "الرياض", emoji: "👗", createdAt: ago(20), status: "ACTIVE", views: 220, enquiries: 14 },
  { id: "l5", sellerId: "s5", title: "بلايستيشن 5 + يدين", description: "نسخة القرص، معاها 3 ألعاب.", priceHalalas: 195000, isNegotiable: true, category: "ألعاب", condition: "LIKE_NEW", city: "الخبر", emoji: "🎮", createdAt: ago(300), status: "ACTIVE", views: 730, enquiries: 41 },
  { id: "l6", sellerId: "s2", title: "لابتوب ماك برو M3", description: "14 إنش، 18 جيجا، استخدام خفيف.", priceHalalas: 780000, isNegotiable: true, category: "إلكترونيات", condition: "LIKE_NEW", city: "جدة", emoji: "💻", createdAt: ago(500), status: "ACTIVE", views: 512, enquiries: 18 },
  { id: "l7", sellerId: "s1", title: "لكزس ES 2019", description: "وكالة، صيانة منتظمة، لون أبيض.", priceHalalas: 11500000, isNegotiable: true, category: "سيارات", condition: "USED", city: "الرياض", emoji: "🚙", createdAt: ago(720), status: "ACTIVE", views: 990, enquiries: 28 },
  { id: "l8", sellerId: "s4", title: "ساعة نسائية فضية", description: "ستانلس ستيل، ضد الماء.", priceHalalas: 68000, isNegotiable: false, category: "أزياء", condition: "NEW", city: "الرياض", emoji: "⌚", createdAt: ago(90), status: "SOLD", views: 150, enquiries: 6 },
];

export const VIDEOS: VideoPost[] = [
  { id: "v1", sellerId: "s1", videoUrl: `${GTV}/BigBuckBunny.mp4`, caption: "كامري 2021 وصلت المعرض 🔥 فحص كامل", createdAt: ago(35), listingId: "l1", likes: 342 },
  { id: "v2", sellerId: "s4", videoUrl: `${GTV}/ElephantsDream.mp4`, caption: "تشكيلة العبايات الجديدة وصلت ✨", createdAt: ago(20), listingId: "l4", likes: 1280 },
  { id: "v3", sellerId: "s5", videoUrl: `${GTV}/ForBiggerBlazes.mp4`, caption: "بلايستيشن 5 بسعر ما يتكرر 🎮", createdAt: ago(300), listingId: "l5", likes: 890 },
  { id: "v4", sellerId: "s2", videoUrl: `${GTV}/ForBiggerEscapes.mp4`, caption: "آيفون 15 برو ماكس — جديد مسكر 📦", createdAt: ago(80), listingId: "l2", likes: 455 },
  { id: "v5", sellerId: "s3", videoUrl: `${GTV}/ForBiggerFun.mp4`, caption: "كنب فخم يغيّر مجلسك بالكامل 🛋️", createdAt: ago(160), listingId: "l3", likes: 210 },
];

export const CONVERSATIONS: Conversation[] = [
  { id: "c1", listingId: "l1", sellerId: "s1", lastMessage: "السيارة متوفرة، تفضل تشوفها بالمعرض", lastAt: ago(15), unread: 1 },
  { id: "c2", listingId: "l5", sellerId: "s5", lastMessage: "أوكي، خلها 1800 وتم", lastAt: ago(240), unread: 0 },
];

export const MESSAGES: Message[] = [
  { id: "m1", conversationId: "c1", fromMe: true, body: "السلام عليكم، الكامري متوفرة؟", sentAt: ago(30) },
  { id: "m2", conversationId: "c1", fromMe: false, body: "وعليكم السلام، نعم متوفرة", sentAt: ago(22) },
  { id: "m3", conversationId: "c1", fromMe: false, body: "السيارة متوفرة، تفضل تشوفها بالمعرض", sentAt: ago(15) },
  { id: "m4", conversationId: "c2", fromMe: true, body: "آخر سعر للبلايستيشن؟", sentAt: ago(260) },
  { id: "m5", conversationId: "c2", fromMe: false, body: "أوكي، خلها 1800 وتم", sentAt: ago(240) },
];

/** Stable gradient for a listing/seller, derived from its id — no image assets. */
export function gradientFor(id: string): [string, string] {
  let h = 0;
  for (let i = 0; i < id.length; i++) h = (h * 31 + id.charCodeAt(i)) & 0xffffff;
  const hue = h % 360;
  return [`hsl(${hue}, 55%, 32%)`, `hsl(${(hue + 40) % 360}, 60%, 18%)`];
}
