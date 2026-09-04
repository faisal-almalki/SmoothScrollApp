import { Linking, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { gradientFor } from "../../src/data/seed";
import { listingById, openConversation, sellerById, toggleFollow, useStore } from "../../src/data/store";
import { formatSar, timeAgo } from "../../src/lib/money";
import { colors, radius, space } from "../../src/lib/theme";

const CONDITION: Record<string, string> = { NEW: "جديد", LIKE_NEW: "شبه جديد", USED: "مستعمل" };

export default function ListingDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const listing = listingById(id!);
  const following = useStore((s) => (listing ? !!s.follows[listing.sellerId] : false));

  if (!listing) {
    return <View style={styles.root}><Text style={styles.missing}>الإعلان غير موجود</Text></View>;
  }
  const seller = sellerById(listing.sellerId);
  const [a, b] = gradientFor(listing.id);

  const message = () => {
    const cid = openConversation(listing.id);
    router.replace(`/chat/${cid}`);
  };
  const call = () => {
    if (seller?.publicPhone) Linking.openURL(`tel:${seller.publicPhone}`);
  };

  return (
    <View style={styles.root}>
      <ScrollView contentContainerStyle={{ paddingBottom: 120 }}>
        <LinearGradient colors={[a, b]} style={[styles.hero, { paddingTop: insets.top + space.sm }]}>
          <Pressable style={styles.close} onPress={() => router.back()} hitSlop={10}>
            <Ionicons name="chevron-down" size={26} color="#fff" />
          </Pressable>
          <Text style={styles.heroEmoji}>{listing.emoji}</Text>
        </LinearGradient>

        <View style={styles.body}>
          <Text style={styles.price}>{formatSar(listing.priceHalalas)}{listing.isNegotiable ? " · قابل للتفاوض" : ""}</Text>
          <Text style={styles.title}>{listing.title}</Text>
          <Text style={styles.meta}>{listing.city} · {CONDITION[listing.condition]} · {timeAgo(listing.createdAt)}</Text>

          <View style={styles.tags}>
            <Tag text={listing.category} />
            <Tag text={`${listing.views} مشاهدة`} />
          </View>

          <Text style={styles.desc}>{listing.description}</Text>

          {seller && (
            <Pressable style={styles.sellerRow} onPress={() => router.push(`/seller/${seller.id}`)}>
              <View style={styles.sellerAvatar}><Text style={{ fontSize: 22 }}>{seller.emoji}</Text></View>
              <View style={{ flex: 1 }}>
                <Text style={styles.sellerName}>{seller.displayName}{seller.isVerified ? " ✓" : ""}</Text>
                <Text style={styles.sellerMeta}>⭐ {seller.rating} · {seller.followerCount} متابع</Text>
              </View>
              <Pressable style={[styles.follow, following && styles.followOn]} onPress={() => toggleFollow(seller.id)}>
                <Text style={[styles.followText, following && styles.followTextOn]}>{following ? "متابَع" : "متابعة"}</Text>
              </Pressable>
            </Pressable>
          )}
        </View>
      </ScrollView>

      {/* Contact bar — message always; call only when the seller published a number. */}
      <View style={[styles.bar, { paddingBottom: insets.bottom + space.sm }]}>
        {seller?.allowMessages !== false && (
          <Pressable style={styles.msgBtn} onPress={message}>
            <Ionicons name="chatbubble-ellipses" size={18} color="#fff" />
            <Text style={styles.msgText}>راسل البائع</Text>
          </Pressable>
        )}
        {seller?.allowCalls && seller.publicPhone && (
          <Pressable style={styles.callBtn} onPress={call}>
            <Ionicons name="call" size={18} color="#fff" />
            <Text style={styles.msgText}>اتصال</Text>
          </Pressable>
        )}
      </View>
    </View>
  );
}

function Tag({ text }: { text: string }) {
  return <View style={styles.tag}><Text style={styles.tagText}>{text}</Text></View>;
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  missing: { color: colors.textDim, textAlign: "center", marginTop: 80 },
  hero: { height: 240, alignItems: "center", justifyContent: "center" },
  close: { position: "absolute", top: 44, right: space.lg, backgroundColor: "rgba(0,0,0,0.3)", borderRadius: radius.pill, padding: 6 },
  heroEmoji: { fontSize: 96 },
  body: { padding: space.lg, gap: space.sm },
  price: { color: colors.accent, fontSize: 24, fontWeight: "800" },
  title: { color: colors.text, fontSize: 19, fontWeight: "700" },
  meta: { color: colors.textDim, fontSize: 13 },
  tags: { flexDirection: "row", gap: space.sm, marginTop: space.xs },
  tag: { backgroundColor: colors.surface, borderRadius: radius.sm, paddingHorizontal: space.md, paddingVertical: 6 },
  tagText: { color: colors.textDim, fontSize: 12, fontWeight: "600" },
  desc: { color: "#DDD", fontSize: 15, lineHeight: 24, marginTop: space.sm },
  sellerRow: { flexDirection: "row", alignItems: "center", gap: space.md, backgroundColor: colors.surface, borderRadius: radius.md, padding: space.md, marginTop: space.lg },
  sellerAvatar: { width: 46, height: 46, borderRadius: radius.pill, backgroundColor: colors.surfaceHigh, alignItems: "center", justifyContent: "center" },
  sellerName: { color: colors.text, fontWeight: "700", fontSize: 15 },
  sellerMeta: { color: colors.textDim, fontSize: 12, marginTop: 2 },
  follow: { borderColor: colors.accent, borderWidth: 1, borderRadius: radius.pill, paddingHorizontal: space.md, paddingVertical: 6 },
  followOn: { backgroundColor: colors.accent, borderColor: colors.accent },
  followText: { color: colors.accent, fontWeight: "700", fontSize: 12 },
  followTextOn: { color: "#fff" },
  bar: { position: "absolute", left: 0, right: 0, bottom: 0, flexDirection: "row", gap: space.sm, padding: space.lg, backgroundColor: colors.surface, borderTopColor: colors.border, borderTopWidth: StyleSheet.hairlineWidth },
  msgBtn: { flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: space.sm, backgroundColor: colors.accent, borderRadius: radius.pill, paddingVertical: space.md },
  callBtn: { flexDirection: "row", alignItems: "center", justifyContent: "center", gap: space.sm, backgroundColor: colors.call, borderRadius: radius.pill, paddingVertical: space.md, paddingHorizontal: space.xl },
  msgText: { color: "#fff", fontWeight: "700", fontSize: 15 },
});
