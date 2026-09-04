import { Pressable, StyleSheet, Text, View } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { useRouter } from "expo-router";
import { colors, radius, space } from "../lib/theme";
import { formatSar, timeAgo } from "../lib/money";
import { gradientFor } from "../data/seed";
import type { Listing } from "../data/types";

export default function ListingCard({ listing }: { listing: Listing }) {
  const router = useRouter();
  const [a, b] = gradientFor(listing.id);
  const sold = listing.status === "SOLD";
  return (
    <Pressable style={styles.card} onPress={() => router.push(`/listing/${listing.id}`)}>
      <LinearGradient colors={[a, b]} style={styles.thumb}>
        <Text style={styles.emoji}>{listing.emoji}</Text>
        {sold && <View style={styles.soldTag}><Text style={styles.soldText}>مباع</Text></View>}
      </LinearGradient>
      <View style={styles.body}>
        <Text style={styles.title} numberOfLines={1}>{listing.title}</Text>
        <Text style={styles.price}>{formatSar(listing.priceHalalas)}</Text>
        <Text style={styles.meta} numberOfLines={1}>{listing.city} · {timeAgo(listing.createdAt)}</Text>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: { flex: 1, backgroundColor: colors.surface, borderRadius: radius.md, overflow: "hidden", margin: space.xs },
  thumb: { height: 120, alignItems: "center", justifyContent: "center" },
  emoji: { fontSize: 44 },
  soldTag: { position: "absolute", top: space.sm, right: space.sm, backgroundColor: colors.danger, borderRadius: radius.sm, paddingHorizontal: space.sm, paddingVertical: 2 },
  soldText: { color: "#fff", fontSize: 11, fontWeight: "700" },
  body: { padding: space.sm, gap: 2 },
  title: { color: colors.text, fontSize: 14, fontWeight: "600" },
  price: { color: colors.accent, fontSize: 15, fontWeight: "800" },
  meta: { color: colors.textDim, fontSize: 11 },
});
