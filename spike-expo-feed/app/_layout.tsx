import { useEffect, useState } from "react";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { View, ActivityIndicator } from "react-native";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { hydrate } from "../src/data/store";
import { colors } from "../src/lib/theme";

export default function RootLayout() {
  const [ready, setReady] = useState(false);
  useEffect(() => {
    hydrate().finally(() => setReady(true));
  }, []);

  if (!ready) {
    return (
      <View style={{ flex: 1, backgroundColor: colors.bg, justifyContent: "center" }}>
        <ActivityIndicator color={colors.accent} />
      </View>
    );
  }

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <StatusBar style="light" />
        <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.bg } }}>
          <Stack.Screen name="(tabs)" />
          <Stack.Screen name="listing/[id]" options={{ presentation: "modal" }} />
          <Stack.Screen name="chat/[id]" />
          <Stack.Screen name="seller/[id]" />
          <Stack.Screen name="preview" options={{ presentation: "modal" }} />
          <Stack.Screen name="post" options={{ presentation: "modal" }} />
          <Stack.Screen name="login" options={{ presentation: "modal" }} />
        </Stack>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
