import { useCallback, useMemo, useRef, useState } from "react";
import {
  Dimensions, FlatList, Pressable, StyleSheet, Text, View, type ViewToken,
} from "react-native";
import { VideoView, useVideoPlayer, type VideoPlayer } from "expo-video";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { colors, radius, space } from "../lib/theme";
import { formatSar } from "../lib/money";
import { listingById, sellerById, toggleLike, useStore } from "../data/store";
import type { VideoPost } from "../data/types";

/**
 * The shoppable vertical feed. Grown from the spike's three-player pool — the
 * part that proved smooth on device — with the commerce overlay, like button,
 * and taps that open the ad and the seller.
 *
 * `active` lets a parent (the tab bar) pause every player when the feed is not
 * the visible tab, so audio never plays under another screen.
 */
const { height: SCREEN_H } = Dimensions.get("window");
const POOL = 3;
const slotFor = (i: number) => ((i % POOL) + POOL) % POOL;

export default function Feed({ active = true }: { active?: boolean }) {
  const videos = useStore((s) => s.videos);
  const [index, setIndex] = useState(0);
  const [muted, setMuted] = useState(true);

  const players: VideoPlayer[] = [
    useVideoPlayer(null, (p) => { p.loop = true; }),
    useVideoPlayer(null, (p) => { p.loop = true; }),
    useVideoPlayer(null, (p) => { p.loop = true; }),
  ];
  const slotSource = useRef<(string | null)[]>([null, null, null]);

  const sync = useCallback((i: number) => {
    for (let j = i - 1; j <= i + 1; j++) {
      if (j < 0 || j >= videos.length) continue;
      const slot = slotFor(j);
      const url = videos[j].videoUrl;
      if (slotSource.current[slot] !== url) {
        players[slot].replace(url);
        slotSource.current[slot] = url;
      }
      if (j === i && active) {
        players[slot].muted = muted;
        players[slot].play();
      } else {
        players[slot].pause();
      }
    }
    setIndex(i);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [muted, active, videos]);

  const syncRef = useRef(sync);
  syncRef.current = sync;

  const started = useRef(false);
  if (!started.current && videos.length > 0) {
    started.current = true;
    sync(0);
  }
  // When the tab loses focus, silence everything.
  if (!active) players.forEach((p) => p.pause());

  const onViewable = useRef(({ viewableItems }: { viewableItems: ViewToken[] }) => {
    const v = viewableItems.find((x) => x.isViewable);
    if (v?.index != null) syncRef.current(v.index);
  });

  const toggleMute = useCallback(() => {
    setMuted((m) => {
      players[slotFor(index)].muted = !m;
      return !m;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [index]);

  const renderItem = useCallback(
    ({ item, index: i }: { item: VideoPost; index: number }) => (
      <Row item={item} player={players[slotFor(i)]} isActive={i === index && active}
        muted={muted} onToggleMute={toggleMute} />
    ),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [index, muted, active, toggleMute],
  );

  return (
    <FlatList
      data={videos}
      renderItem={renderItem}
      keyExtractor={(v) => v.id}
      pagingEnabled
      showsVerticalScrollIndicator={false}
      onViewableItemsChanged={onViewable.current}
      viewabilityConfig={useMemo(() => ({ itemVisiblePercentThreshold: 60 }), [])}
      getItemLayout={(_, i) => ({ length: SCREEN_H, offset: SCREEN_H * i, index: i })}
      windowSize={3}
      maxToRenderPerBatch={2}
      initialNumToRender={1}
    />
  );
}

function Row({
  item, player, isActive, muted, onToggleMute,
}: {
  item: VideoPost; player: VideoPlayer; isActive: boolean; muted: boolean; onToggleMute: () => void;
}) {
  const router = useRouter();
  const seller = sellerById(item.sellerId);
  const listing = item.listingId ? listingById(item.listingId) : null;
  const liked = useStore((s) => !!s.likes[item.id]);

  return (
    <Pressable style={styles.page} onPress={onToggleMute}>
      <VideoView player={player} style={StyleSheet.absoluteFill} contentFit="cover" nativeControls={false} />

      {/* right-hand action rail */}
      <View style={styles.rail}>
        <RailButton icon={liked ? "heart" : "heart-outline"}
          tint={liked ? colors.live : colors.text}
          label={String(item.likes + (liked ? 1 : 0))}
          onPress={() => toggleLike(item.id)} />
        {listing && (
          <RailButton icon="chatbubble-ellipses" label="سؤال"
            onPress={() => router.push(`/listing/${listing.id}`)} />
        )}
        <RailButton icon={muted ? "volume-mute" : "volume-high"} label={muted ? "صامت" : "صوت"}
          onPress={onToggleMute} />
      </View>

      <View style={styles.overlay}>
        {seller && (
          <Pressable onPress={() => router.push(`/seller/${seller.id}`)}>
            <Text style={styles.seller}>{seller.emoji} @{seller.handle}{seller.isVerified ? " ✓" : ""}</Text>
          </Pressable>
        )}
        <Text style={styles.caption} numberOfLines={2}>{item.caption}</Text>

        {listing && (
          <Pressable style={styles.adCard} onPress={() => router.push(`/listing/${listing.id}`)}>
            <Text style={styles.adEmoji}>{listing.emoji}</Text>
            <View style={{ flex: 1 }}>
              <Text style={styles.adTitle} numberOfLines={1}>{listing.title}</Text>
              <Text style={styles.adMeta}>{formatSar(listing.priceHalalas)} · {listing.city}</Text>
            </View>
            <View style={styles.adCta}><Text style={styles.adCtaText}>عرض الإعلان</Text></View>
          </Pressable>
        )}
        {isActive && muted && <Text style={styles.hint}>اضغط لتشغيل الصوت</Text>}
      </View>
    </Pressable>
  );
}

function RailButton({ icon, label, tint = colors.text, onPress }: {
  icon: keyof typeof Ionicons.glyphMap; label: string; tint?: string; onPress: () => void;
}) {
  return (
    <Pressable style={styles.railBtn} onPress={onPress} hitSlop={8}>
      <Ionicons name={icon} size={30} color={tint} />
      <Text style={styles.railLabel}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  page: { height: SCREEN_H, backgroundColor: colors.bg },
  rail: { position: "absolute", right: space.md, bottom: 160, alignItems: "center", gap: space.lg },
  railBtn: { alignItems: "center", gap: 3 },
  railLabel: { color: colors.text, fontSize: 11, fontWeight: "600", textShadowColor: "#000", textShadowRadius: 4 },
  overlay: { position: "absolute", left: space.lg, right: 76, bottom: 90, gap: space.sm },
  seller: { color: colors.text, fontWeight: "700", fontSize: 15, textShadowColor: "#000", textShadowRadius: 4 },
  caption: { color: "#EEE", fontSize: 13, textShadowColor: "#000", textShadowRadius: 4 },
  adCard: {
    flexDirection: "row", alignItems: "center", gap: space.sm,
    backgroundColor: "rgba(18,18,20,0.85)", borderRadius: radius.md, padding: space.md, marginTop: space.xs,
  },
  adEmoji: { fontSize: 26 },
  adTitle: { color: colors.text, fontWeight: "700", fontSize: 14 },
  adMeta: { color: colors.accent, fontWeight: "700", fontSize: 13, marginTop: 2 },
  adCta: { backgroundColor: colors.accent, borderRadius: radius.sm, paddingHorizontal: space.md, paddingVertical: space.sm },
  adCtaText: { color: "#fff", fontWeight: "700", fontSize: 12 },
  hint: { color: colors.textDim, fontSize: 12 },
});
