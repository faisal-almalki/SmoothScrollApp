import { FlatList, Pressable, StyleSheet, Text, View } from "react-native";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import ListingCard from "../../src/components/ListingCard";
import { sellerById, toggleFollow, useStore } from "../../src/data/store";
import { colors, radius, space } from "../../src/lib/theme";

export default function SellerPage() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const seller = sellerById(id!);
  const listings = useStore((s) => s.listings.filter((l) => l.sellerId === id));
  const following = useStore((s) => !!s.follows[id!]);

  if (!seller) return <View style={styles.root}><Text style={styles.missing}>البائع غير موجود</Text></View>;

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      <Pressable style={styles.back} onPress={() => router.back()} hitSlop={10}>
        <Ionicons name="chevron-forward" size={26} color={colors.text} />
      </Pressable>
      <View style={styles.head}>
        <View style={styles.avatar}><Text style={{ fontSize: 34 }}>{seller.emoji}</Text></View>
        <Text style={styles.name}>{seller.displayName}{seller.isVerified ? " ✓" : ""}</Text>
        <Text style={styles.handle}>@{seller.handle} · {seller.city}</Text>
        <Text style={styles.bio}>{seller.bio}</Text>
        <View style={styles.stats}>
          <Text style={styles.stat}>⭐ {seller.rating} ({seller.ratingCount})</Text>
          <Text style={styles.stat}>{seller.followerCount} متابع</Text>
          <Text style={styles.stat}>{listings.length} إعلان</Text>
        </View>
        <Pressable style={[styles.follow, following && styles.followOn]} onPress={() => toggleFollow(seller.id)}>
          <Text style={[styles.followText, following && styles.followTextOn]}>{following ? "متابَع" : "متابعة"}</Text>
        </Pressable>
      </View>
      <FlatList
        data={listings}
        keyExtractor={(l) => l.id}
        numColumns={2}
        contentContainerStyle={{ padding: space.sm, paddingBottom: 40 }}
        renderItem={({ item }) => <ListingCard listing={item} />}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  missing: { color: colors.textDim, textAlign: "center", marginTop: 80 },
  back: { position: "absolute", top: 44, right: space.lg, zIndex: 10 },
  head: { alignItems: "center", padding: space.lg, gap: space.xs },
  avatar: { width: 80, height: 80, borderRadius: radius.pill, backgroundColor: colors.surfaceHigh, alignItems: "center", justifyContent: "center" },
  name: { color: colors.text, fontSize: 20, fontWeight: "800", marginTop: space.sm },
  handle: { color: colors.textDim, fontSize: 13 },
  bio: { color: "#CCC", fontSize: 14, textAlign: "center", marginTop: space.xs },
  stats: { flexDirection: "row", gap: space.lg, marginTop: space.sm },
  stat: { color: colors.textDim, fontSize: 12 },
  follow: { borderColor: colors.accent, borderWidth: 1, borderRadius: radius.pill, paddingHorizontal: space.xxl, paddingVertical: space.sm, marginTop: space.md },
  followOn: { backgroundColor: colors.accent, borderColor: colors.accent },
  followText: { color: colors.accent, fontWeight: "700" },
  followTextOn: { color: "#fff" },
});
