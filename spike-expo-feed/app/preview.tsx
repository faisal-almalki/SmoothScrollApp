import { useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";
import { useVideoPlayer, VideoView } from "expo-video";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { postVideo, useStore } from "../src/data/store";
import { colors, radius, space } from "../src/lib/theme";

export default function Preview() {
  const { uri } = useLocalSearchParams<{ uri: string }>();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const me = useStore((s) => s.me);
  const listings = useStore((s) => s.listings);
  const myListings = listings.filter((l) => l.sellerId === (me?.id ?? "me") && l.status === "ACTIVE");

  const [caption, setCaption] = useState("");
  const [attached, setAttached] = useState<string | null>(null);

  const player = useVideoPlayer(uri ?? null, (p) => { p.loop = true; p.muted = false; p.play(); });

  const publish = () => {
    postVideo({ videoUrl: uri!, caption: caption.trim() || "مقطع جديد", listingId: attached });
    // Back to the feed; the new clip is on top.
    router.dismissAll();
    router.replace("/(tabs)");
  };

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} hitSlop={10}><Ionicons name="close" size={26} color={colors.text} /></Pressable>
        <Text style={styles.title}>معاينة المقطع</Text>
        <View style={{ width: 26 }} />
      </View>

      <View style={styles.videoWrap}>
        {uri ? <VideoView player={player} style={StyleSheet.absoluteFill} contentFit="cover" nativeControls={false} /> : null}
      </View>

      <ScrollView contentContainerStyle={{ padding: space.lg, gap: space.md }}>
        <TextInput
          style={styles.caption}
          placeholder="اكتب وصفاً للمقطع…"
          placeholderTextColor={colors.textFaint}
          value={caption}
          onChangeText={setCaption}
          multiline
        />

        <Text style={styles.label}>اربط إعلاناً بالمقطع (اختياري)</Text>
        {myListings.length === 0 ? (
          <Text style={styles.note}>ما عندك إعلانات نشطة تربطها. تقدر تنشر المقطع بدون إعلان.</Text>
        ) : (
          <View style={{ gap: space.sm }}>
            {myListings.map((l) => (
              <Pressable key={l.id} style={[styles.pick, attached === l.id && styles.pickOn]}
                onPress={() => setAttached(attached === l.id ? null : l.id)}>
                <Text style={{ fontSize: 20 }}>{l.emoji}</Text>
                <Text style={styles.pickText} numberOfLines={1}>{l.title}</Text>
                {attached === l.id && <Ionicons name="checkmark-circle" size={20} color={colors.accent} />}
              </Pressable>
            ))}
          </View>
        )}
      </ScrollView>

      <Pressable style={[styles.publish, { marginBottom: insets.bottom + space.md }]} onPress={publish}>
        <Text style={styles.publishText}>نشر المقطع</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  header: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingHorizontal: space.lg, paddingVertical: space.md },
  title: { color: colors.text, fontSize: 17, fontWeight: "700" },
  videoWrap: { height: 260, backgroundColor: "#000", marginHorizontal: space.lg, borderRadius: radius.lg, overflow: "hidden" },
  caption: { backgroundColor: colors.surface, borderRadius: radius.md, padding: space.md, color: colors.text, fontSize: 15, minHeight: 70, textAlign: "right", textAlignVertical: "top" },
  label: { color: colors.text, fontSize: 14, fontWeight: "700" },
  note: { color: colors.textDim, fontSize: 13 },
  pick: { flexDirection: "row", alignItems: "center", gap: space.sm, backgroundColor: colors.surface, borderRadius: radius.md, padding: space.md, borderWidth: 1, borderColor: "transparent" },
  pickOn: { borderColor: colors.accent },
  pickText: { color: colors.text, flex: 1, fontSize: 14 },
  publish: { backgroundColor: colors.accent, marginHorizontal: space.lg, borderRadius: radius.pill, paddingVertical: space.lg, alignItems: "center" },
  publishText: { color: "#fff", fontWeight: "800", fontSize: 16 },
});
