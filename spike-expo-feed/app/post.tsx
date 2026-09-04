import { useState } from "react";
import { Pressable, ScrollView, StyleSheet, Switch, Text, TextInput, View } from "react-native";
import { useRouter } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { postListing } from "../src/data/store";
import { CATEGORIES, CITIES, type Category, type City, type Condition } from "../src/data/types";
import { colors, radius, space } from "../src/lib/theme";

const EMOJIS = ["🚗", "📱", "💻", "🛋️", "👗", "🎮", "⌚", "🏠", "📷", "🚲", "🧸", "📦"];
const CONDITIONS: { key: Condition; label: string }[] = [
  { key: "NEW", label: "جديد" }, { key: "LIKE_NEW", label: "شبه جديد" }, { key: "USED", label: "مستعمل" },
];

export default function PostAd() {
  const insets = useSafeAreaInsets();
  const router = useRouter();

  const [title, setTitle] = useState("");
  const [price, setPrice] = useState("");
  const [desc, setDesc] = useState("");
  const [category, setCategory] = useState<Category>(CATEGORIES[0]);
  const [city, setCity] = useState<City>(CITIES[0]);
  const [condition, setCondition] = useState<Condition>("USED");
  const [negotiable, setNegotiable] = useState(true);
  const [emoji, setEmoji] = useState(EMOJIS[0]);

  const valid = title.trim().length > 2 && Number(price) > 0;

  const submit = () => {
    if (!valid) return;
    postListing({
      title: title.trim(),
      priceHalalas: Math.round(Number(price) * 100),
      description: desc.trim(),
      category, city, condition, isNegotiable: negotiable, emoji,
    });
    router.back();
  };

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} hitSlop={10}><Ionicons name="close" size={26} color={colors.text} /></Pressable>
        <Text style={styles.title}>إعلان جديد</Text>
        <View style={{ width: 26 }} />
      </View>

      <ScrollView contentContainerStyle={{ padding: space.lg, gap: space.lg, paddingBottom: 40 }}>
        <Field label="عنوان الإعلان">
          <TextInput style={styles.input} placeholder="مثال: آيفون 15 برو ماكس" placeholderTextColor={colors.textFaint} value={title} onChangeText={setTitle} />
        </Field>

        <Field label="السعر (ريال)">
          <TextInput style={styles.input} placeholder="0" placeholderTextColor={colors.textFaint} keyboardType="number-pad" value={price} onChangeText={setPrice} />
        </Field>

        <View style={styles.rowBetween}>
          <Text style={styles.label}>قابل للتفاوض</Text>
          <Switch value={negotiable} onValueChange={setNegotiable} trackColor={{ true: colors.accent, false: colors.border }} />
        </View>

        <Field label="التصنيف">
          <Chips options={CATEGORIES as readonly string[]} value={category} onChange={(v) => setCategory(v as Category)} />
        </Field>

        <Field label="المدينة">
          <Chips options={CITIES as readonly string[]} value={city} onChange={(v) => setCity(v as City)} />
        </Field>

        <Field label="الحالة">
          <Chips options={CONDITIONS.map((c) => c.label)} value={CONDITIONS.find((c) => c.key === condition)!.label}
            onChange={(label) => setCondition(CONDITIONS.find((c) => c.label === label)!.key)} />
        </Field>

        <Field label="أيقونة الإعلان">
          <View style={styles.emojiRow}>
            {EMOJIS.map((e) => (
              <Pressable key={e} style={[styles.emojiBtn, emoji === e && styles.emojiOn]} onPress={() => setEmoji(e)}>
                <Text style={{ fontSize: 24 }}>{e}</Text>
              </Pressable>
            ))}
          </View>
        </Field>

        <Field label="الوصف">
          <TextInput style={[styles.input, styles.textarea]} placeholder="تفاصيل إضافية…" placeholderTextColor={colors.textFaint} value={desc} onChangeText={setDesc} multiline />
        </Field>
      </ScrollView>

      <Pressable style={[styles.submit, !valid && styles.submitOff, { marginBottom: insets.bottom + space.md }]} onPress={submit} disabled={!valid}>
        <Text style={styles.submitText}>نشر الإعلان</Text>
      </Pressable>
    </View>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return <View style={{ gap: space.sm }}><Text style={styles.label}>{label}</Text>{children}</View>;
}

function Chips({ options, value, onChange }: { options: readonly string[]; value: string; onChange: (v: string) => void }) {
  return (
    <View style={styles.chips}>
      {options.map((o) => (
        <Pressable key={o} style={[styles.chip, value === o && styles.chipOn]} onPress={() => onChange(o)}>
          <Text style={[styles.chipText, value === o && styles.chipTextOn]}>{o}</Text>
        </Pressable>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  header: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingHorizontal: space.lg, paddingVertical: space.md },
  title: { color: colors.text, fontSize: 17, fontWeight: "700" },
  label: { color: colors.text, fontSize: 14, fontWeight: "700" },
  input: { backgroundColor: colors.surface, borderRadius: radius.md, padding: space.md, color: colors.text, fontSize: 15, textAlign: "right" },
  textarea: { minHeight: 90, textAlignVertical: "top" },
  rowBetween: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  chips: { flexDirection: "row", flexWrap: "wrap", gap: space.sm },
  chip: { backgroundColor: colors.surface, borderRadius: radius.pill, paddingHorizontal: space.md, paddingVertical: 8 },
  chipOn: { backgroundColor: colors.accent },
  chipText: { color: colors.textDim, fontWeight: "600", fontSize: 13 },
  chipTextOn: { color: "#fff" },
  emojiRow: { flexDirection: "row", flexWrap: "wrap", gap: space.sm },
  emojiBtn: { width: 46, height: 46, borderRadius: radius.md, backgroundColor: colors.surface, alignItems: "center", justifyContent: "center", borderWidth: 1, borderColor: "transparent" },
  emojiOn: { borderColor: colors.accent },
  submit: { backgroundColor: colors.accent, marginHorizontal: space.lg, borderRadius: radius.pill, paddingVertical: space.lg, alignItems: "center" },
  submitOff: { backgroundColor: colors.surfaceHigh },
  submitText: { color: "#fff", fontWeight: "800", fontSize: 16 },
});
