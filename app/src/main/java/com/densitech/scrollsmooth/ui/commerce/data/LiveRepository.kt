package com.densitech.scrollsmooth.ui.commerce.data

import com.densitech.scrollsmooth.ui.commerce.model.LiveChatKind
import com.densitech.scrollsmooth.ui.commerce.model.LiveChatMessage
import com.densitech.scrollsmooth.ui.commerce.model.LiveStream
import kotlin.random.Random

/**
 * Drives the room around a live stream. The video is a looping source, but the viewer count,
 * the chat and the purchase ticker are generated here so a room feels alive while you shop it.
 */
object LiveRepository {

    fun liveStreams(): List<LiveStream> = CommerceCatalog.liveStreams

    fun liveStream(id: String?): LiveStream? = CommerceCatalog.liveStream(id)

    private val handles = listOf(
        "mina.k", "dev_ari", "june22", "polly", "notyourbabe", "sam.exe", "kofi", "tinyplant",
        "rae", "buzzcut.bill", "yuki", "cass", "omar_h", "the.grape", "lo.fi.lu", "nadia",
        "pixel", "bee", "quietstorm", "mo", "harper", "tex", "veda", "zin",
    )

    private val avatars = listOf("🦊", "🐼", "🐝", "🦉", "🐙", "🌵", "🍄", "⚡", "🌊", "🍋", "🪐", "🎈")

    private val chatLines = listOf(
        "is this restocking??",
        "just copped 2 🙌",
        "does it ship to canada",
        "the packaging is so cute",
        "waited all week for this",
        "size up or true to size?",
        "bought last drop, worth it",
        "can you show the back",
        "PLEASE do a bundle",
        "how long does one last",
        "my third one lol",
        "link not working for me",
        "any code for first order",
        "watching from berlin 🇩🇪",
        "hi from the 6 🇨🇦",
        "does it work on sensitive skin",
        "sold out already?? 😭",
        "quality is unreal for the price",
        "can we get more colours",
        "you convinced me, adding to cart",
    )

    fun chatMessage(sequence: Long, random: Random = Random.Default): LiveChatMessage {
        val handle = handles[random.nextInt(handles.size)]
        val avatar = avatars[random.nextInt(avatars.size)]
        val roll = random.nextInt(100)
        return when {
            roll < 10 -> LiveChatMessage(
                id = sequence,
                author = handle,
                emoji = avatar,
                text = "joined",
                kind = LiveChatKind.JOIN,
            )

            roll < 22 -> LiveChatMessage(
                id = sequence,
                author = handle,
                emoji = avatar,
                text = "bought ${1 + random.nextInt(3)}",
                kind = LiveChatKind.PURCHASE,
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

    /** Viewer counts drift rather than jump, with a slight upward bias while a sale is running. */
    fun nextViewerCount(current: Int, random: Random = Random.Default): Int {
        val drift = random.nextInt(-18, 46)
        return (current + drift).coerceAtLeast(MIN_VIEWERS)
    }

    fun livePriceCents(basePriceCents: Long, discountPercent: Int): Long {
        if (discountPercent <= 0) return basePriceCents
        val discounted = basePriceCents - (basePriceCents * discountPercent / 100)
        return discounted.coerceAtLeast(100L)
    }

    private const val MIN_VIEWERS = 42
}
