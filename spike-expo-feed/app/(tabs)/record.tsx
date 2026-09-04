import { useRef, useState } from "react";
import { Pressable, StyleSheet, Text, View, ActivityIndicator } from "react-native";
import { CameraView, useCameraPermissions, useMicrophonePermissions, type CameraType } from "expo-camera";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import * as Haptics from "expo-haptics";
import { colors, radius, space } from "../../src/lib/theme";
import { useFocused } from "../../src/lib/useFocused";

/**
 * Record a clip, TikTok/Snap style: hold-to-record (or tap start/stop), flip
 * the camera, then hand the file to the preview screen to caption and post.
 *
 * Permissions are requested on first use rather than at launch, which is what
 * both stores expect and what a first-time user finds least alarming.
 */
const MAX_SECONDS = 60;

export default function RecordTab() {
  const router = useRouter();
  const focused = useFocused();
  const [camPerm, requestCam] = useCameraPermissions();
  const [micPerm, requestMic] = useMicrophonePermissions();
  const [facing, setFacing] = useState<CameraType>("back");
  const [recording, setRecording] = useState(false);
  const [elapsed, setElapsed] = useState(0);
  const cameraRef = useRef<CameraView>(null);
  const timer = useRef<ReturnType<typeof setInterval> | null>(null);

  if (!focused) return <View style={styles.root} />;

  // Permission gate.
  if (!camPerm || !micPerm) {
    return <View style={styles.root}><ActivityIndicator color={colors.accent} /></View>;
  }
  if (!camPerm.granted || !micPerm.granted) {
    return (
      <View style={[styles.root, styles.center]}>
        <Ionicons name="videocam-outline" size={56} color={colors.textDim} />
        <Text style={styles.permTitle}>صوّر مقطعك</Text>
        <Text style={styles.permBody}>
          نحتاج إذن الكاميرا والمايك لتصوير مقاطع لإعلاناتك.
        </Text>
        <Pressable style={styles.permBtn} onPress={async () => { await requestCam(); await requestMic(); }}>
          <Text style={styles.permBtnText}>السماح</Text>
        </Pressable>
      </View>
    );
  }

  const stopTimer = () => {
    if (timer.current) clearInterval(timer.current);
    timer.current = null;
  };

  const start = async () => {
    if (recording || !cameraRef.current) return;
    setRecording(true);
    setElapsed(0);
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium).catch(() => {});
    timer.current = setInterval(() => {
      setElapsed((e) => {
        if (e + 1 >= MAX_SECONDS) stop();
        return e + 1;
      });
    }, 1000);
    try {
      const video = await cameraRef.current.recordAsync({ maxDuration: MAX_SECONDS });
      if (video?.uri) router.push({ pathname: "/preview", params: { uri: video.uri } });
    } catch {
      // Recording cancelled or failed; nothing to post.
    } finally {
      setRecording(false);
      setElapsed(0);
      stopTimer();
    }
  };

  const stop = () => {
    stopTimer();
    cameraRef.current?.stopRecording();
  };

  return (
    <View style={styles.root}>
      <CameraView ref={cameraRef} style={StyleSheet.absoluteFill} facing={facing} mode="video" />

      {recording && (
        <View style={styles.timer}>
          <View style={styles.dot} />
          <Text style={styles.timerText}>{String(Math.floor(elapsed / 60)).padStart(2, "0")}:{String(elapsed % 60).padStart(2, "0")}</Text>
        </View>
      )}

      <View style={styles.controls}>
        <Pressable style={styles.side} onPress={() => setFacing((f) => (f === "back" ? "front" : "back"))} disabled={recording}>
          <Ionicons name="camera-reverse-outline" size={30} color={recording ? colors.textFaint : colors.text} />
        </Pressable>

        <Pressable onPress={recording ? stop : start}>
          <View style={[styles.shutterOuter, recording && styles.shutterOuterRec]}>
            <View style={[styles.shutterInner, recording && styles.shutterInnerRec]} />
          </View>
        </Pressable>

        <View style={styles.side} />
      </View>

      <Text style={styles.hint}>{recording ? "اضغط للإيقاف" : "اضغط للتصوير"}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  center: { alignItems: "center", justifyContent: "center", padding: space.xl, gap: space.md },
  permTitle: { color: colors.text, fontSize: 20, fontWeight: "800" },
  permBody: { color: colors.textDim, textAlign: "center", lineHeight: 22 },
  permBtn: { backgroundColor: colors.accent, borderRadius: radius.pill, paddingHorizontal: space.xxl, paddingVertical: space.md, marginTop: space.sm },
  permBtnText: { color: "#fff", fontWeight: "700" },
  timer: { position: "absolute", top: 60, alignSelf: "center", flexDirection: "row", alignItems: "center", gap: 6, backgroundColor: "rgba(0,0,0,0.5)", borderRadius: radius.pill, paddingHorizontal: space.md, paddingVertical: 6 },
  dot: { width: 10, height: 10, borderRadius: 5, backgroundColor: colors.live },
  timerText: { color: colors.text, fontWeight: "700", fontVariant: ["tabular-nums"] },
  controls: { position: "absolute", bottom: 96, left: 0, right: 0, flexDirection: "row", alignItems: "center", justifyContent: "space-around", paddingHorizontal: space.xxl },
  side: { width: 56, height: 56, alignItems: "center", justifyContent: "center" },
  shutterOuter: { width: 84, height: 84, borderRadius: 42, borderWidth: 5, borderColor: "#fff", alignItems: "center", justifyContent: "center" },
  shutterOuterRec: { borderColor: colors.live },
  shutterInner: { width: 66, height: 66, borderRadius: 33, backgroundColor: "#fff" },
  shutterInnerRec: { width: 32, height: 32, borderRadius: 8, backgroundColor: colors.live },
  hint: { position: "absolute", bottom: 70, alignSelf: "center", color: colors.textDim, fontSize: 12 },
});
