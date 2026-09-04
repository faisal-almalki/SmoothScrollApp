import { useMemo, useState } from "react";
import { FlatList, Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import ListingCard from "../../src/components/ListingCard";
import { useStore } from "../../src/data/store";
import { CATEGORIES, CITIES } from "../../src/data/types";
import { colors, radius, space } from "../../src/lib/theme";

export default function BrowseTab() {
  const insets = useSafeAreaInsets();
  const listings = useStore((s) => s.listings);
  const [q, setQ] = useState("");
  const [cat, setCat] = useState<string | null>(null);
  const [city, setCity] = useState<string | null>(null);

  const results = useMemo(() => {
    const needle = q.trim();
    return listings.filter((l) => {
      if (cat && l.category !== cat) return false;
      if (city && l.city !== city) return false;
      if (needle && !`${l.title} ${l.description}`.includes(needle)) return false;
      return true;
    });
  }, [listings, q, cat, city]);

  return (
    <View style={[styles.root, { paddingTop: insets.top + space.sm }]}>
      <View style={styles.searchBar}>
        <Ionicons name="search" size={18} color={colors.textDim} />
        <TextInput
          style={styles.input}
          placeholder="ابحث عن سيارة، جوال، أثاث…"
          placeholderTextColor={colors.textFaint}
          value={q}
          onChangeText={setQ}
        />
        {q.length > 0 && (
          <Pressable onPress={() => setQ("")}><Ionicons name="close-circle" size={18} color={colors.textDim} /></Pressable>
        )}
      </View>

      <View>
        <FlatList
          horizontal
          showsHorizontalScrollIndicator={false}
          data={["الكل", ...CATEGORIES]}
          keyExtractor={(c) => c}
          contentContainerStyle={styles.chips}
          renderItem={({ item }) => {
            const on = item === "الكل" ? cat === null : cat === item;
            return (
              <Pressable style={[styles.chip, on && styles.chipOn]}
                onPress={() => setCat(item === "الكل" ? null : item)}>
                <Text style={[styles.chipText, on && styles.chipTextOn]}>{item}</Text>
              </Pressable>
            );
          }}
        />
      </View>

      <FlatList
        data={results}
        keyExtractor={(l) => l.id}
        numColumns={2}
        contentContainerStyle={{ padding: space.sm, paddingBottom: 90 }}
        columnWrapperStyle={{ gap: 0 }}
        renderItem={({ item }) => <ListingCard listing={item} />}
        ListEmptyComponent={<Text style={styles.empty}>ما فيه نتائج مطابقة</Text>}
        ListHeaderComponent={
          <Text style={styles.count}>{results.length} إعلان</Text>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  searchBar: {
    flexDirection: "row", alignItems: "center", gap: space.sm,
    backgroundColor: colors.surface, borderRadius: radius.pill,
    marginHorizontal: space.lg, paddingHorizontal: space.md, height: 42,
  },
  input: { flex: 1, color: colors.text, fontSize: 15, textAlign: "right" },
  chips: { paddingHorizontal: space.lg, paddingVertical: space.md, gap: space.sm },
  chip: { backgroundColor: colors.surface, borderRadius: radius.pill, paddingHorizontal: space.md, paddingVertical: 7, marginLeft: space.sm },
  chipOn: { backgroundColor: colors.accent },
  chipText: { color: colors.textDim, fontWeight: "600", fontSize: 13 },
  chipTextOn: { color: "#fff" },
  count: { color: colors.textDim, fontSize: 12, paddingHorizontal: space.xs, paddingBottom: space.sm },
  empty: { color: colors.textDim, textAlign: "center", marginTop: 60 },
});
