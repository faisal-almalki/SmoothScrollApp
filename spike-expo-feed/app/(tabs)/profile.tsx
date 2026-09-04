import { FlatList, Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import ListingCard from "../../src/components/ListingCard";
import { signOut, useStore } from "../../src/data/store";
import { colors, radius, space } from "../../src/lib/theme";

export default function ProfileTab() {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const me = useStore((s) => s.me);
  const listings = useStore((s) => s.listings);
  const videos = useStore((s) => s.videos);

  const myId = me?.id ?? "me";
  const myListings = listings.filter((l) => l.sellerId === myId);
  const myVideos = videos.filter((v) => v.mine);

  return (
    <View style={[styles.root, { paddingTop: insets.top + space.md }]}>
      <View style={styles.head}>
        <View style={styles.avatar}><Text style={{ fontSize: 34 }}>{me?.emoji ?? "🙂"}</Text></View>
        <Text style={styles.name}>{me?.displayName ?? "زائر"}</Text>
        <Text style={styles.handle}>{me ? `@${me.handle} · ${me.city}` : "غير مسجّل دخول"}</Text>

        {me ? (
          <View style={styles.actions}>
            <Pressable style={styles.btnGhost} onPress={() => router.push("/post")}>
              <Ionicons name="add" size={16} color={colors.text} />
              <Text style={styles.btnGhostText}>أضف إعلان</Text>
            </Pressable>
            <Pressable style={styles.btnGhost} onPress={() => signOut()}>
              <Text style={styles.btnGhostText}>تسجيل خروج</Text>
            </Pressable>
          </View>
        ) : (
          <Pressable style={styles.btnPrimary} onPress={() => router.push("/login")}>
            <Text style={styles.btnPrimaryText}>تسجيل الدخول</Text>
          </Pressable>
        )}

        <View style={styles.stats}>
          <Stat n={myListings.length} label="إعلاناتي" />
          <Stat n={myVideos.length} label="مقاطعي" />
          <Stat n={0} label="متابِعون" />
        </View>
      </View>

      <Text style={styles.section}>إعلاناتي</Text>
      <FlatList
        data={myListings}
        keyExtractor={(l) => l.id}
        numColumns={2}
        contentContainerStyle={{ padding: space.sm, paddingBottom: 90 }}
        renderItem={({ item }) => <ListingCard listing={item} />}
        ListEmptyComponent={<Text style={styles.empty}>ما نشرت إعلانات بعد — اضغط «أضف إعلان» أو صوّر مقطعاً.</Text>}
      />
    </View>
  );
}

function Stat({ n, label }: { n: number; label: string }) {
  return (
    <View style={{ alignItems: "center" }}>
      <Text style={styles.statN}>{n}</Text>
      <Text style={styles.statL}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  head: { alignItems: "center", paddingHorizontal: space.lg, gap: space.xs },
  avatar: { width: 84, height: 84, borderRadius: radius.pill, backgroundColor: colors.surfaceHigh, alignItems: "center", justifyContent: "center" },
  name: { color: colors.text, fontSize: 20, fontWeight: "800", marginTop: space.sm },
  handle: { color: colors.textDim, fontSize: 13 },
  actions: { flexDirection: "row", gap: space.sm, marginTop: space.md },
  btnGhost: { flexDirection: "row", alignItems: "center", gap: 4, backgroundColor: colors.surface, borderRadius: radius.pill, paddingHorizontal: space.lg, paddingVertical: space.sm },
  btnGhostText: { color: colors.text, fontWeight: "600", fontSize: 13 },
  btnPrimary: { backgroundColor: colors.accent, borderRadius: radius.pill, paddingHorizontal: space.xxl, paddingVertical: space.md, marginTop: space.md },
  btnPrimaryText: { color: "#fff", fontWeight: "700" },
  stats: { flexDirection: "row", gap: space.xxl, marginTop: space.lg },
  statN: { color: colors.text, fontSize: 18, fontWeight: "800" },
  statL: { color: colors.textDim, fontSize: 12 },
  section: { color: colors.text, fontSize: 16, fontWeight: "700", paddingHorizontal: space.lg, marginTop: space.lg, marginBottom: space.xs },
  empty: { color: colors.textDim, textAlign: "center", marginTop: 40, paddingHorizontal: space.xl, lineHeight: 22 },
});
