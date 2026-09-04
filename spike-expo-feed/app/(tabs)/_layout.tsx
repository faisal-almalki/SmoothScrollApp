import { Tabs } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { View, StyleSheet } from "react-native";
import { colors } from "../../src/lib/theme";

/**
 * Five tabs, TikTok-style: the middle one is the record button, raised and
 * filled, because recording a clip is the thing the app most wants you to do.
 */
export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarStyle: styles.bar,
        tabBarActiveTintColor: colors.text,
        tabBarInactiveTintColor: colors.textFaint,
        tabBarLabelStyle: { fontSize: 10, fontWeight: "600" },
      }}
    >
      <Tabs.Screen name="index" options={{
        title: "الرئيسية",
        tabBarIcon: ({ color, size }) => <Ionicons name="play-circle" size={size} color={color} />,
      }} />
      <Tabs.Screen name="browse" options={{
        title: "تصفّح",
        tabBarIcon: ({ color, size }) => <Ionicons name="search" size={size} color={color} />,
      }} />
      <Tabs.Screen name="record" options={{
        title: "",
        tabBarIcon: () => (
          <View style={styles.record}>
            <Ionicons name="add" size={30} color="#fff" />
          </View>
        ),
      }} />
      <Tabs.Screen name="inbox" options={{
        title: "الرسائل",
        tabBarIcon: ({ color, size }) => <Ionicons name="chatbubble-ellipses-outline" size={size} color={color} />,
      }} />
      <Tabs.Screen name="profile" options={{
        title: "حسابي",
        tabBarIcon: ({ color, size }) => <Ionicons name="person-outline" size={size} color={color} />,
      }} />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  bar: {
    backgroundColor: colors.bg,
    borderTopColor: colors.border,
    height: 62,
    paddingBottom: 8,
    paddingTop: 6,
  },
  record: {
    width: 46, height: 32, borderRadius: 10,
    backgroundColor: colors.accent, alignItems: "center", justifyContent: "center",
    marginTop: 2,
  },
});
