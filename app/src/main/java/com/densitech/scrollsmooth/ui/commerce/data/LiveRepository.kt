package com.densitech.scrollsmooth.ui.commerce.data

import com.densitech.scrollsmooth.ui.commerce.model.LiveChatKind
import com.densitech.scrollsmooth.ui.commerce.model.LiveChatMessage
import com.densitech.scrollsmooth.ui.commerce.model.LiveStream
import kotlin.random.Random

/**
 * Drives the room around a live stream. The video is a looping source, but the viewer count and
 * the chat are generated here so a room feels alive while you look at what is being sold.
 */
object LiveRepository {

    private val handles = listOf(
        "abu_saud", "m.alharbi", "nouf", "salem77", "dana", "tariq", "hind", "yousef",
        "reem", "bandar", "ghada", "faisal.k", "lama", "omar", "sara", "ziyad",
    )

    private val avatars = listOf("🦊", "🐼", "🌙", "🦅", "🌵", "⚡", "🌊", "🍋", "🪐", "🎈", "🕊", "🐎")

    private val chatLines = listOf(
        "is it still available?",
        "last price?",
        "can you show the back please",
        "where in the city exactly",
        "any accidents on it?",
        "I sent you a message",
        "does the price include delivery",
        "can I see it tomorrow",
        "how long have you had it",
        "is the number in the ad correct",
        "please show the odometer",
        "I will take it if the price is right",
        "watching from Jeddah",
        "any warranty left?",
        "is it negotiable",
        "can you post more photos",
        "what year exactly",
        "reserved for me please 🙏",
    )

    fun liveStreams(): List<LiveStream> = CommerceCatalog.liveStreams

    fun liveStream(id: String?): LiveStream? = CommerceCatalog.liveStream(id)

    fun chatMessage(sequence: Long, random: Random = Random.Default): LiveChatMessage {
        val handle = handles[random.nextInt(handles.size)]
        val avatar = avatars[random.nextInt(avatars.size)]
        val roll = random.nextInt(100)
        return when {
            roll < 12 -> LiveChatMessage(
                id = sequence,
                author = handle,
                emoji = avatar,
                text = "joined",
                kind = LiveChatKind.JOIN,
            )

            roll < 24 -> LiveChatMessage(
                id = sequence,
                author = handle,
                emoji = avatar,
                text = "messaged the seller",
                kind = LiveChatKind.CONTACT,
            )

            else -> LiveChatMessage(
                id = sequence,
                author = handle,
                emoji = avatar,
                text = chatLines[random.nextInt(chatLines.size)],
                kind = LiveChatKind.CHAT,
            )
        }
    }

    /** Viewer counts drift rather than jump. */
    fun nextViewerCount(current: Int, random: Random = Random.Default): Int {
        val drift = random.nextInt(-18, 42)
        return (current + drift).coerceAtLeast(MIN_VIEWERS)
    }

    private const val MIN_VIEWERS = 24
}
