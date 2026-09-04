import { View, StyleSheet, Text } from "react-native";
import Feed from "../../src/components/Feed";
import { colors } from "../../src/lib/theme";
import { useFocused } from "../../src/lib/useFocused";

export default function FeedTab() {
  const focused = useFocused();
  return (
    <View style={styles.root}>
      <View style={styles.header} pointerEvents="none">
        <Text style={styles.brand}>حراج فيديو</Text>
      </View>
      <Feed active={focused} />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  header: { position: "absolute", top: 54, left: 0, right: 0, zIndex: 10, alignItems: "center" },
  brand: { color: colors.text, fontSize: 17, fontWeight: "800", textShadowColor: "#000", textShadowRadius: 6 },
});
