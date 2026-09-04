import { useCallback, useState } from "react";
import { useFocusEffect } from "expo-router";

/**
 * A boolean "is this screen the focused tab" — expo-router's supported way, via
 * useFocusEffect. Importing @react-navigation/native directly is blocked from
 * SDK 56 onward, so this is the seam every screen uses to pause its video when
 * the user swaps tabs.
 */
export function useFocused(): boolean {
  const [focused, setFocused] = useState(true);
  useFocusEffect(
    useCallback(() => {
      setFocused(true);
      return () => setFocused(false);
    }, []),
  );
  return focused;
}
