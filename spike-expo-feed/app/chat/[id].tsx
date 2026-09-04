import { useMemo, useState } from "react";
import { FlatList, KeyboardAvoidingView, Platform, Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { listingById, sellerById, sendMessage, useStore } from "../../src/data/store";
import { colors, radius, space } from "../../src/lib/theme";

export default function Chat() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const [text, setText] = useState("");

  const convo = useStore((s) => s.conversations.find((c) => c.id === id));
  const allMessages = useStore((s) => s.messages);
  const messages = useMemo(
    () => allMessages.filter((m) => m.conversationId === id).sort((a, b) => a.sentAt - b.sentAt),
    [allMessages, id],
  );
  const seller = convo ? sellerById(convo.sellerId) : null;
  const listing = convo ? listingById(convo.listingId) : null;

  const send = () => {
    const body = text.trim();
    if (!body) return;
    sendMessage(id!, body);
    setText("");
  };

  return (
    <KeyboardAvoidingView style={styles.root} behavior={Platform.OS === "ios" ? "padding" : undefined}>
      <View style={[styles.header, { paddingTop: insets.top + space.sm }]}>
        <Pressable onPress={() => router.back()} hitSlop={10}><Ionicons name="chevron-forward" size={26} color={colors.text} /></Pressable>
        <View style={{ flex: 1 }}>
          <Text style={styles.name}>{seller?.displayName ?? "بائع"}</Text>
          <Text style={styles.sub} numberOfLines={1}>{listing?.title ?? ""}</Text>
        </View>
        <Text style={{ fontSize: 24 }}>{seller?.emoji ?? "👤"}</Text>
      </View>

      <FlatList
        data={messages}
        keyExtractor={(m) => m.id}
        contentContainerStyle={{ padding: space.lg, gap: space.sm }}
        renderItem={({ item }) => (
          <View style={[styles.bubble, item.fromMe ? styles.mine : styles.theirs]}>
            <Text style={item.fromMe ? styles.mineText : styles.theirsText}>{item.body}</Text>
          </View>
        )}
      />

      <View style={[styles.inputBar, { paddingBottom: insets.bottom + space.sm }]}>
        <TextInput
          style={styles.input}
          placeholder="اكتب رسالة…"
          placeholderTextColor={colors.textFaint}
          value={text}
          onChangeText={setText}
          onSubmitEditing={send}
        />
        <Pressable style={styles.sendBtn} onPress={send}><Ionicons name="send" size={18} color="#fff" /></Pressable>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  header: { flexDirection: "row", alignItems: "center", gap: space.sm, paddingHorizontal: space.lg, paddingBottom: space.md, borderBottomColor: colors.border, borderBottomWidth: StyleSheet.hairlineWidth },
  name: { color: colors.text, fontWeight: "700", fontSize: 16 },
  sub: { color: colors.accent, fontSize: 12 },
  bubble: { maxWidth: "80%", borderRadius: radius.lg, paddingHorizontal: space.md, paddingVertical: space.sm },
  mine: { alignSelf: "flex-end", backgroundColor: colors.accent, borderBottomRightRadius: 4 },
  theirs: { alignSelf: "flex-start", backgroundColor: colors.surfaceHigh, borderBottomLeftRadius: 4 },
  mineText: { color: "#fff", fontSize: 15 },
  theirsText: { color: colors.text, fontSize: 15 },
  inputBar: { flexDirection: "row", alignItems: "center", gap: space.sm, padding: space.md, borderTopColor: colors.border, borderTopWidth: StyleSheet.hairlineWidth },
  input: { flex: 1, backgroundColor: colors.surface, borderRadius: radius.pill, paddingHorizontal: space.lg, paddingVertical: space.md, color: colors.text, textAlign: "right" },
  sendBtn: { width: 44, height: 44, borderRadius: 22, backgroundColor: colors.accent, alignItems: "center", justifyContent: "center" },
});
