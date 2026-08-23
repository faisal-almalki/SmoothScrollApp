/**
 * The spike's data. Shaped like the real backend's feed response
 * (GET /feed in server/) so swapping this file for a fetch is the
 * whole integration.
 *
 * Videos are Google's public sample bucket — permanent, free, and the
 * same files every video player demo on earth uses. The original
 * Kotlin app's Firebase source is dead (the project fell off the free
 * storage plan), so these are more reliable than what it shipped with.
 */

export interface FeedListing {
  id: string;
  title: string;
  priceHalalas: number;
  city: string;
}

export interface FeedItem {
  id: string;
  videoUrl: string;
  seller: { handle: string; displayName: string; emoji: string };
  caption: string;
  listing: FeedListing;
}

const GTV = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample";

export const FEED: FeedItem[] = [
  {
    id: "v1",
    videoUrl: `${GTV}/ForBiggerBlazes.mp4`,
    seller: { handle: "abu_khalid_tech", displayName: "أبو خالد للتقنية", emoji: "📱" },
    caption: "آيفون ١٥ برو ماكس، استخدام ٣ أشهر بس",
    listing: { id: "l1", title: "iPhone 15 Pro Max 256GB", priceHalalas: 385_000, city: "الرياض" },
  },
  {
    id: "v2",
    videoUrl: `${GTV}/ForBiggerEscapes.mp4`,
    seller: { handle: "riyadh_motors", displayName: "معرض الرياض", emoji: "🚗" },
    caption: "لكزس LX570 موديل ٢٠٢١، فحص كامل",
    listing: { id: "l2", title: "Lexus LX570 2021", priceHalalas: 32_500_000, city: "الرياض" },
  },
  {
    id: "v3",
    videoUrl: `${GTV}/ForBiggerFun.mp4`,
    seller: { handle: "um_sara_abaya", displayName: "أم سارة", emoji: "🧥" },
    caption: "عبايات جديدة وصلت، شغل يدوي",
    listing: { id: "l3", title: "عباية مطرزة يدوي", priceHalalas: 45_000, city: "جدة" },
  },
  {
    id: "v4",
    videoUrl: `${GTV}/ForBiggerJoyrides.mp4`,
    seller: { handle: "gaming_ksa", displayName: "قيمنق ستور", emoji: "🎮" },
    caption: "بلايستيشن ٥ مع يدين وثلاث ألعاب",
    listing: { id: "l4", title: "PS5 + يدّين + ٣ ألعاب", priceHalalas: 175_000, city: "الدمام" },
  },
  {
    id: "v5",
    videoUrl: `${GTV}/ForBiggerMeltdowns.mp4`,
    seller: { handle: "furniture_house", displayName: "بيت الأثاث", emoji: "🛋️" },
    caption: "كنب مودرن ٧ مقاعد، شبه جديد",
    listing: { id: "l5", title: "طقم كنب ٧ مقاعد", priceHalalas: 230_000, city: "الرياض" },
  },
  {
    id: "v6",
    videoUrl: `${GTV}/BigBuckBunny.mp4`,
    seller: { handle: "abu_fahad_cam", displayName: "أبو فهد للكاميرات", emoji: "📷" },
    caption: "كانون R6 مع عدسة ٢٤-١٠٥",
    listing: { id: "l6", title: "Canon R6 + 24-105mm", priceHalalas: 890_000, city: "جدة" },
  },
  {
    id: "v7",
    videoUrl: `${GTV}/ElephantsDream.mp4`,
    seller: { handle: "saif_watches", displayName: "سيف للساعات", emoji: "⌚" },
    caption: "ساعة أوميغا أصلية مع الصندوق والأوراق",
    listing: { id: "l7", title: "Omega Seamaster أصلية", priceHalalas: 1_450_000, city: "الرياض" },
  },
  {
    id: "v8",
    videoUrl: `${GTV}/Sintel.mp4`,
    seller: { handle: "dates_alqassim", displayName: "تمور القصيم", emoji: "🌴" },
    caption: "سكري ملكي فاخر، الكرتون ٥ كيلو",
    listing: { id: "l8", title: "تمر سكري ملكي ٥ كيلو", priceHalalas: 12_000, city: "بريدة" },
  },
];

export function formatPrice(halalas: number): string {
  const riyals = halalas / 100;
  return `${riyals.toLocaleString("en-US", { maximumFractionDigits: 0 })} ﷼`;
}
