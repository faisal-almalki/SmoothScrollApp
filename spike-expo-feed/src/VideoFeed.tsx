import { useCallback, useMemo, useRef, useState } from "react";
import {
  Dimensions,
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  View,
  type ViewToken,
} from "react-native";
import { VideoView, useVideoPlayer, type VideoPlayer } from "expo-video";
import { FEED, formatPrice, type FeedItem } from "./catalog";

/**
 * The spike. One question is being answered here: can Expo deliver a
 * TikTok-feel vertical feed — instant start on swipe, no flicker, no jank —
 * on a real phone, and especially a cheap Android one?
 *
 * The design mirrors what the Kotlin app's Media3 layer does by hand:
 *
 *  - A fixed pool of three players (previous / current / next), reused as the
 *    user scrolls, instead of a player per row. Creating a player is the
 *    expensive part on both platforms; three is the minimum that lets the
 *    neighbours be preloaded while the current one plays.
 *  - The neighbour players sit paused with the source attached, so the video
 *    starts buffering before it is ever on screen. That is the whole trick
 *    behind "it starts instantly when I swipe".
 *  - pagingEnabled + a full-screen row gives the one-video snap.
 *
 * What is deliberately NOT here: navigation, state management, the commerce
 * data layer, login. A spike that grows features stops measuring anything.
 */

const { height: SCREEN_HEIGHT } = Dimensions.get("window");
const POOL_SIZE = 3;

/** Which pool slot serves which feed index: plain round-robin. */
const slotFor = (index: number) => ((index % POOL_SIZE) + POOL_SIZE) % POOL_SIZE;

export default function VideoFeed() {
  const [activeIndex, setActiveIndex] = useState(0);
  const [muted, setMuted] = useState(true);

  // Three players for the whole feed, however long it gets. useVideoPlayer
  // ties each one's lifetime to this component, so there is nothing to
  // release by hand — the usual way player pools leak.
  const players: VideoPlayer[] = [
    useVideoPlayer(null, (p) => { p.loop = true; }),
    useVideoPlayer(null, (p) => { p.loop = true; }),
    useVideoPlayer(null, (p) => { p.loop = true; }),
  ];

  // Remembers what each slot last loaded so settling on the same index after
  // a half-swipe does not restart the video from zero.
  const slotSource = useRef<(string | null)[]>([null, null, null]);

  const syncPool = useCallback((index: number) => {
    for (let i = index - 1; i <= index + 1; i++) {
      if (i < 0 || i >= FEED.length) continue;
      const slot = slotFor(i);
      const player = players[slot];
      const url = FEED[i].videoUrl;

      if (slotSource.current[slot] !== url) {
        player.replace(url);          // attaching the source starts buffering
        slotSource.current[slot] = url;
      }
      if (i === index) {
        player.muted = muted;
        player.play();
      } else {
        player.pause();               // preloaded, silent, ready
      }
    }
    setActiveIndex(index);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [muted]);

  // First frame: start item 0 and preload item 1.
  const started = useRef(false);
  if (!started.current) {
    started.current = true;
    syncPool(0);
  }

  const onViewableItemsChanged = useRef(
    ({ viewableItems }: { viewableItems: ViewToken[] }) => {
      const visible = viewableItems.find((v) => v.isViewable);
      if (visible?.index != null) syncPoolRef.current(visible.index);
    },
  );
  // The FlatList callback identity must never change; the pool logic may.
  const syncPoolRef = useRef(syncPool);
  syncPoolRef.current = syncPool;

  const toggleMute = useCallback(() => {
    setMuted((m) => {
      players[slotFor(activeIndex)].muted = !m;
      return !m;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeIndex]);

  const renderItem = useCallback(
    ({ item, index }: { item: FeedItem; index: number }) => (
      <FeedRow
        item={item}
        player={players[slotFor(index)]}
        isActive={index === activeIndex}
        muted={muted}
        onToggleMute={toggleMute}
      />
    ),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [activeIndex, muted, toggleMute],
  );

  return (
    <FlatList
      data={FEED}
      renderItem={renderItem}
      keyExtractor={(item) => item.id}
      pagingEnabled
      showsVerticalScrollIndicator={false}
      onViewableItemsChanged={onViewableItemsChanged.current}
      viewabilityConfig={useMemo(() => ({ itemVisiblePercentThreshold: 60 }), [])}
      getItemLayout={(_, index) => ({
        length: SCREEN_HEIGHT,
        offset: SCREEN_HEIGHT * index,
        index,
      })}
      // The pool means off-screen rows hold a paused player or nothing, so the
      // window can stay small without paying for it on swipe.
      windowSize={3}
      maxToRenderPerBatch={2}
      initialNumToRender={1}
    />
  );
}

function FeedRow({
  item,
  player,
  isActive,
  muted,
  onToggleMute,
}: {
  item: FeedItem;
  player: VideoPlayer;
  isActive: boolean;
  muted: boolean;
  onToggleMute: () => void;
}) {
  return (
    <Pressable style={styles.page} onPress={onToggleMute}>
      {/* Every rendered row mounts its pool player's view, active or not. With
          windowSize=3 the rendered rows map to distinct slots, so no player is
          claimed by two views — and a neighbour mid-swipe shows its already
          buffered first frame instead of black, which is most of what makes a
          feed feel like TikTok rather than a video list. */}
      <VideoView
        player={player}
        style={StyleSheet.absoluteFill}
        contentFit="cover"
        nativeControls={false}
      />

      {/* The ad pill — the product's whole point, so the spike keeps it. */}
      <View style={styles.overlay}>
        <View style={styles.adCard}>
          <Text style={styles.adEmoji}>{item.seller.emoji}</Text>
          <View style={styles.adText}>
            <Text style={styles.adTitle} numberOfLines={1}>{item.listing.title}</Text>
            <Text style={styles.adMeta}>{formatPrice(item.listing.priceHalalas)} · {item.listing.city}</Text>
          </View>
          <View style={styles.adCta}><Text style={styles.adCtaText}>عرض</Text></View>
        </View>
        <Text style={styles.seller}>@{item.seller.handle}</Text>
        <Text style={styles.caption} numberOfLines={2}>{item.caption}</Text>
        {isActive && muted && <Text style={styles.muteHint}>اضغط للصوت 🔇</Text>}
      </View>
    </Pressable>
  );
}

const ACCENT = "#00B074";

const styles = StyleSheet.create({
  page: { height: SCREEN_HEIGHT, backgroundColor: "#000" },
  overlay: { position: "absolute", left: 16, right: 16, bottom: 48, gap: 8 },
  adCard: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "rgba(18,18,18,0.82)",
    borderRadius: 14,
    padding: 12,
    gap: 10,
  },
  adEmoji: { fontSize: 28 },
  adText: { flex: 1 },
  adTitle: { color: "#fff", fontWeight: "700", fontSize: 15 },
  adMeta: { color: ACCENT, fontWeight: "600", fontSize: 13, marginTop: 2 },
  adCta: { backgroundColor: ACCENT, borderRadius: 10, paddingHorizontal: 16, paddingVertical: 8 },
  adCtaText: { color: "#fff", fontWeight: "700" },
  seller: { color: "#fff", fontWeight: "700", fontSize: 14, textShadowColor: "#000", textShadowRadius: 4 },
  caption: { color: "#eee", fontSize: 13, textShadowColor: "#000", textShadowRadius: 4 },
  muteHint: { color: "#bbb", fontSize: 12, marginTop: 2 },
});
