import { StatusBar } from "expo-status-bar";
import { View, StyleSheet } from "react-native";
import VideoFeed from "./src/VideoFeed";

export default function App() {
  return (
    <View style={styles.root}>
      <StatusBar style="light" />
      <VideoFeed />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: "#000" },
});
