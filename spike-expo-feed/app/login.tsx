import { useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { useRouter } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { signIn } from "../src/data/store";
import { colors, radius, space } from "../src/lib/theme";

export default function Login() {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const [step, setStep] = useState<"phone" | "code">("phone");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [devCode, setDevCode] = useState("");

  const requestCode = () => {
    if (phone.trim().length < 9) return;
    // No SMS provider yet: a local 4-digit code stands in, shown on screen.
    const c = String(Math.floor(1000 + Math.random() * 9000));
    setDevCode(c);
    setStep("code");
  };

  const verify = () => {
    if (code !== devCode) return;
    const tail = phone.replace(/\D/g, "").slice(-4);
    signIn({
      id: "me",
      handle: `user_${tail}`,
      displayName: "حسابي",
      emoji: "🙂",
      city: "الرياض",
      publicPhone: null,
      allowCalls: false,
    });
    router.back();
  };

  return (
    <View style={[styles.root, { paddingTop: insets.top + space.xl }]}>
      <Pressable style={styles.close} onPress={() => router.back()} hitSlop={10}><Ionicons name="close" size={26} color={colors.text} /></Pressable>
      <Text style={styles.brand}>حراج فيديو</Text>
      <Text style={styles.sub}>سجّل دخولك برقم جوالك</Text>

      {step === "phone" ? (
        <>
          <TextInput
            style={styles.input}
            placeholder="05xxxxxxxx"
            placeholderTextColor={colors.textFaint}
            keyboardType="phone-pad"
            value={phone}
            onChangeText={setPhone}
          />
          <Pressable style={styles.btn} onPress={requestCode}><Text style={styles.btnText}>إرسال الرمز</Text></Pressable>
        </>
      ) : (
        <>
          <Text style={styles.devNote}>رمزك التجريبي: <Text style={styles.devCode}>{devCode}</Text></Text>
          <TextInput
            style={styles.input}
            placeholder="أدخل الرمز"
            placeholderTextColor={colors.textFaint}
            keyboardType="number-pad"
            value={code}
            onChangeText={setCode}
            maxLength={4}
          />
          <Pressable style={styles.btn} onPress={verify}><Text style={styles.btnText}>دخول</Text></Pressable>
          <Pressable onPress={() => setStep("phone")}><Text style={styles.back}>تغيير الرقم</Text></Pressable>
        </>
      )}
      <Text style={styles.disclaimer}>النسخة التجريبية بدون رسائل SMS — الرمز يظهر هنا مباشرة.</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg, paddingHorizontal: space.xl, alignItems: "center", gap: space.md },
  close: { position: "absolute", top: 44, right: space.lg },
  brand: { color: colors.accent, fontSize: 28, fontWeight: "900", marginTop: space.xxl },
  sub: { color: colors.textDim, fontSize: 15 },
  input: { width: "100%", backgroundColor: colors.surface, borderRadius: radius.md, padding: space.lg, color: colors.text, fontSize: 18, textAlign: "center", marginTop: space.md },
  btn: { width: "100%", backgroundColor: colors.accent, borderRadius: radius.pill, paddingVertical: space.lg, alignItems: "center", marginTop: space.sm },
  btnText: { color: "#fff", fontWeight: "800", fontSize: 16 },
  back: { color: colors.textDim, marginTop: space.md },
  devNote: { color: colors.textDim, fontSize: 14, marginTop: space.sm },
  devCode: { color: colors.accent, fontWeight: "800", fontSize: 18 },
  disclaimer: { color: colors.textFaint, fontSize: 12, textAlign: "center", marginTop: space.xxl, paddingHorizontal: space.lg },
});
