/**
 * One place for colour, spacing and type. A dark app by default — a
 * full-screen video feed lives in the dark, and every other screen matching
 * it means no white flash between tabs.
 *
 * The accent green and the call blue are carried over from the Kotlin app on
 * purpose: this is the same product, so it should feel like the same product.
 */
export const colors = {
  bg: "#000000",
  surface: "#121214",
  surfaceHigh: "#1E1E22",
  border: "#2A2A30",
  text: "#FFFFFF",
  textDim: "#9A9AA2",
  textFaint: "#6A6A72",
  accent: "#00B074", // brand / price / primary action
  accentInk: "#00160E",
  call: "#2E8BF0", // phone-call action
  live: "#FF2D55", // live badge
  danger: "#FF453A",
} as const;

export const space = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
} as const;

export const radius = {
  sm: 8,
  md: 12,
  lg: 18,
  pill: 999,
} as const;

export const font = {
  // Right-to-left friendly: the system font renders Arabic well on both
  // platforms, so no custom face to bundle.
  h1: { fontSize: 26, fontWeight: "800" },
  h2: { fontSize: 20, fontWeight: "700" },
  title: { fontSize: 16, fontWeight: "700" },
  body: { fontSize: 15, fontWeight: "500" },
  label: { fontSize: 13, fontWeight: "600" },
  tiny: { fontSize: 11, fontWeight: "600" },
} as const;
