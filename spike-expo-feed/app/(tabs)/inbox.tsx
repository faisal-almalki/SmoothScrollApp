import { FlatList, Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useRouter } from "expo-router";
import { listingById, sellerById, useStore } from "../../src/data/store";
import { timeAgo } from "../../src/lib/money";
import { colors, radius, space } from "../../src/lib/theme";

export default function InboxTab() {
  const insets = useSafeAreaInsets();
  const conversations = useStore((s) => s.conversations);
  const router = useRouter();

  return (
    <View style={[styles.root, { paddingTop: insets.top + space.md }]}>
      <Text style={styles.h1}>الرسائل</Text>
      <FlatList
        data={[...conversations].sort((a, b) => b.lastAt - a.lastAt)}
        keyExtractor={(c) => c.id}
        contentContainerStyle={{ paddingBottom: 90 }}
        renderItem={({ item }) => {
          const seller = sellerById(item.sellerId);
          const listing = listingById(item.listingId);
          return (
            <Pressable style={styles.row} onPress={() => router.push(`/chat/${item.id}`)}>
              <View style={styles.avatar}><Text style={{ fontSize: 22 }}>{seller?.emoji ?? "👤"}</Text></View>
              <View style={{ flex: 1 }}>
                <Text style={styles.name} numberOfLines={1}>{seller?.displayName ?? "بائع"}</Text>
                <Text style={styles.sub} numberOfLines={1}>{listing?.title ?? ""}</Text>
                <Text style={styles.last} numberOfLines={1}>{item.lastMessage || "— لا رسائل بعد —"}</Text>
              </View>
              <View style={{ alignItems: "flex-end", gap: 6 }}>
                <Text style={styles.time}>{timeAgo(item.lastAt)}</Text>
                {item.unread > 0 && <View style={styles.badge}><Text style={styles.badgeText}>{item.unread}</Text></View>}
              </View>
            </Pressable>
          );
        }}
        ListEmptyComponent={<Text style={styles.empty}>ما عندك محادثات بعد.{"\n"}افتح إعلاناً وراسل البائع.</Text>}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg, paddingHorizontal: space.lg },
  h1: { color: colors.text, fontSize: 24, fontWeight: "800", marginBottom: space.md },
  row: { flexDirection: "row", alignItems: "center", gap: space.md, paddingVertical: space.md, borderBottomColor: colors.border, borderBottomWidth: StyleSheet.hairlineWidth },
  avatar: { width: 48, height: 48, borderRadius: radius.pill, backgroundColor: colors.surfaceHigh, alignItems: "center", justifyContent: "center" },
  name: { color: colors.text, fontWeight: "700", fontSize: 15 },
  sub: { color: colors.accent, fontSize: 12, marginTop: 1 },
  last: { color: colors.textDim, fontSize: 13, marginTop: 2 },
  time: { color: colors.textFaint, fontSize: 11 },
  badge: { backgroundColor: colors.accent, borderRadius: radius.pill, minWidth: 20, height: 20, paddingHorizontal: 6, alignItems: "center", justifyContent: "center" },
  badgeText: { color: "#fff", fontSize: 11, fontWeight: "700" },
  empty: { color: colors.textDim, textAlign: "center", marginTop: 80, lineHeight: 22 },
});
