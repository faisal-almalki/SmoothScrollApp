package com.densitech.scrollsmooth.ui.commerce.data

import com.densitech.scrollsmooth.ui.commerce.model.Listing
import com.densitech.scrollsmooth.ui.commerce.model.ListingCategory
import com.densitech.scrollsmooth.ui.commerce.model.ListingCondition
import com.densitech.scrollsmooth.ui.commerce.model.LiveStream
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import com.densitech.scrollsmooth.ui.commerce.model.VideoListingTag
import com.densitech.scrollsmooth.ui.commerce.model.stableHash

/**
 * The seeded marketplace. There is no backend behind this build, so the ads live here and
 * anything the signed in user posts is layered on top by [ListingRepository].
 *
 * Video to listing tagging is derived from the video id rather than stored, so the feed shows the
 * same ads for the same video across launches.
 */
object CommerceCatalog {

    private const val VIDEO_BASE_URL =
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos"

    /** Ad ages are seeded relative to app start so the feed never shows "posted in the future". */
    private val startedAtMillis: Long = System.currentTimeMillis()

    private fun hoursAgo(hours: Long): Long = startedAtMillis - hours * 60L * 60L * 1_000L

    val cities: List<String> = listOf(
        "Riyadh", "Jeddah", "Dammam", "Khobar", "Mecca", "Medina", "Abha", "Taif",
    )

    val sellers: List<Seller> = listOf(
        Seller(
            id = "s_faris",
            handle = "faris.motors",
            displayName = "Faris",
            bio = "Cars only. Everything I post I have driven myself. Inspection welcome.",
            emoji = "🚗",
            rating = 4.9f,
            ratingCount = 214,
            followers = 48_200,
            isVerified = true,
            city = "Riyadh",
            phoneNumber = "+966501234567",
            allowCalls = true,
            memberSinceMillis = hoursAgo(24 * 800),
        ),
        Seller(
            id = "s_noura",
            handle = "noura.home",
            displayName = "Noura",
            bio = "Moving abroad, selling the whole apartment piece by piece.",
            emoji = "🛋",
            rating = 5.0f,
            ratingCount = 61,
            followers = 9_400,
            city = "Jeddah",
            phoneNumber = "+966555987654",
            allowCalls = true,
            memberSinceMillis = hoursAgo(24 * 120),
        ),
        Seller(
            id = "s_techsouq",
            handle = "tech.souq",
            displayName = "Tech Souq",
            bio = "Used phones and laptops, all tested on camera before posting.",
            emoji = "📱",
            rating = 4.7f,
            ratingCount = 1_842,
            followers = 132_600,
            isVerified = true,
            city = "Riyadh",
            allowCalls = false,
            memberSinceMillis = hoursAgo(24 * 1_400),
        ),
        Seller(
            id = "s_maha",
            handle = "maha.closet",
            displayName = "Maha",
            bio = "Preloved abayas and bags. Worn a few times, kept perfectly.",
            emoji = "👜",
            rating = 4.8f,
            ratingCount = 133,
            followers = 21_800,
            city = "Khobar",
            allowCalls = false,
            memberSinceMillis = hoursAgo(24 * 300),
        ),
        Seller(
            id = "s_abdullah",
            handle = "abu.saad",
            displayName = "Abdullah",
            bio = "Apartments and land in north Riyadh. Licensed broker.",
            emoji = "🏠",
            rating = 4.6f,
            ratingCount = 88,
            followers = 6_100,
            isVerified = true,
            city = "Riyadh",
            phoneNumber = "+966533112244",
            allowCalls = true,
            memberSinceMillis = hoursAgo(24 * 900),
        ),
        Seller(
            id = "s_saraf",
            handle = "saraf.pets",
            displayName = "Saraf",
            bio = "Cats and everything they need. Home visit before any rehoming.",
            emoji = "🐈",
            rating = 4.9f,
            ratingCount = 47,
            followers = 33_900,
            city = "Jeddah",
            phoneNumber = "+966544778899",
            allowCalls = true,
            memberSinceMillis = hoursAgo(24 * 200),
        ),
        Seller(
            id = "s_khalid",
            handle = "khalid.gear",
            displayName = "Khalid",
            bio = "Camping and desert gear. Weekend pickup from Exit 5.",
            emoji = "⛺",
            rating = 4.7f,
            ratingCount = 156,
            followers = 15_300,
            city = "Riyadh",
            allowCalls = false,
            memberSinceMillis = hoursAgo(24 * 420),
        ),
        Seller(
            id = "s_layla",
            handle = "layla.finds",
            displayName = "Layla",
            bio = "One of a kind pieces I find and restore. No returns, ask before you buy.",
            emoji = "🪞",
            rating = 4.9f,
            ratingCount = 72,
            followers = 11_700,
            city = "Dammam",
            phoneNumber = "+966566443322",
            allowCalls = true,
            memberSinceMillis = hoursAgo(24 * 260),
        ),
    )

    private val seedListings: List<Listing> = listOf(
        // Cars
        Listing(
            id = "l_landcruiser",
            sellerId = "s_faris",
            title = "Toyota Land Cruiser GXR 2019",
            description = "One owner, 148,000 km, full agency service history. New tyres last " +
                "month. Small scratch on the rear bumper, shown in the video. Inspection at any " +
                "workshop you choose.",
            priceCents = 18_500_000,
            category = ListingCategory.CARS.label,
            emoji = "🚙",
            city = "Riyadh",
            condition = ListingCondition.USED,
            postedAtMillis = hoursAgo(3),
            viewCount = 4_128,
        ),
        Listing(
            id = "l_camry",
            sellerId = "s_faris",
            title = "Toyota Camry 2021, low mileage",
            description = "62,000 km, never had an accident. Second key included. Price is a " +
                "little negotiable for a serious buyer.",
            priceCents = 7_900_000,
            category = ListingCategory.CARS.label,
            emoji = "🚗",
            city = "Riyadh",
            postedAtMillis = hoursAgo(19),
            viewCount = 2_640,
        ),
        Listing(
            id = "l_hilux",
            sellerId = "s_faris",
            title = "Hilux 2017 double cab, diesel",
            description = "Work truck, honest condition. Engine and gearbox perfect, body has " +
                "the marks you would expect. Sold as is.",
            priceCents = 6_200_000,
            isNegotiable = false,
            category = ListingCategory.CARS.label,
            emoji = "🛻",
            city = "Riyadh",
            postedAtMillis = hoursAgo(52),
            viewCount = 1_880,
        ),

        // Electronics
        Listing(
            id = "l_iphone",
            sellerId = "s_techsouq",
            title = "iPhone 15 Pro Max 256GB",
            description = "Battery health 94%. Screen has no scratches, back is clean. Box and " +
                "cable included, no charger. Tested live in the video.",
            priceCents = 340_000,
            category = ListingCategory.ELECTRONICS.label,
            emoji = "📱",
            city = "Riyadh",
            condition = ListingCondition.LIKE_NEW,
            postedAtMillis = hoursAgo(1),
            viewCount = 6_902,
        ),
        Listing(
            id = "l_macbook",
            sellerId = "s_techsouq",
            title = "MacBook Air M2, 16GB / 512GB",
            description = "38 charge cycles. Bought for university, barely used. Original box, " +
                "AppleCare until next year.",
            priceCents = 380_000,
            category = ListingCategory.ELECTRONICS.label,
            emoji = "💻",
            city = "Riyadh",
            condition = ListingCondition.LIKE_NEW,
            postedAtMillis = hoursAgo(8),
            viewCount = 3_410,
        ),
        Listing(
            id = "l_ps5",
            sellerId = "s_techsouq",
            title = "PlayStation 5 Slim + 2 controllers",
            description = "Disc edition. Two controllers, one still sealed. Four games included, " +
                "list is in the description on the video.",
            priceCents = 165_000,
            category = ListingCategory.ELECTRONICS.label,
            emoji = "🎮",
            city = "Riyadh",
            postedAtMillis = hoursAgo(30),
            viewCount = 5_240,
        ),
        Listing(
            id = "l_tv",
            sellerId = "s_techsouq",
            title = "Samsung 65\" 4K TV",
            description = "Two years old, works perfectly. Wall mount included. Pickup only, it " +
                "will not fit in a sedan.",
            priceCents = 130_000,
            category = ListingCategory.ELECTRONICS.label,
            emoji = "📺",
            city = "Riyadh",
            postedAtMillis = hoursAgo(74),
            viewCount = 1_120,
        ),

        // Furniture
        Listing(
            id = "l_sofa",
            sellerId = "s_noura",
            title = "L shaped sofa, beige fabric",
            description = "Three years old, no stains, no smoking in the house. Cushion covers " +
                "are removable and washable. Must go before the 30th.",
            priceCents = 180_000,
            category = ListingCategory.FURNITURE.label,
            emoji = "🛋",
            city = "Jeddah",
            postedAtMillis = hoursAgo(5),
            viewCount = 980,
        ),
        Listing(
            id = "l_diningtable",
            sellerId = "s_noura",
            title = "Dining table with 6 chairs, solid oak",
            description = "Heavy real wood, not veneer. Two chairs have light marks. You will " +
                "need two people and a van.",
            priceCents = 220_000,
            category = ListingCategory.FURNITURE.label,
            emoji = "🍽",
            city = "Jeddah",
            postedAtMillis = hoursAgo(29),
            viewCount = 640,
        ),
        Listing(
            id = "l_fridge",
            sellerId = "s_noura",
            title = "LG side by side fridge",
            description = "Working perfectly, ice maker included. Small dent on the left door.",
            priceCents = 140_000,
            category = ListingCategory.FURNITURE.label,
            emoji = "🧊",
            city = "Jeddah",
            postedAtMillis = hoursAgo(46),
            viewCount = 522,
        ),
        Listing(
            id = "l_desk",
            sellerId = "s_layla",
            title = "Restored 1970s writing desk",
            description = "Stripped, repaired and re-oiled by hand. One of a kind, the marks in " +
                "the wood are part of it.",
            priceCents = 95_000,
            isNegotiable = false,
            category = ListingCategory.FURNITURE.label,
            emoji = "🪑",
            city = "Dammam",
            postedAtMillis = hoursAgo(11),
            viewCount = 415,
        ),

        // Fashion
        Listing(
            id = "l_abaya",
            sellerId = "s_maha",
            title = "Embroidered abaya, size M",
            description = "Worn twice to events. Dry cleaned, comes on the original hanger.",
            priceCents = 45_000,
            category = ListingCategory.FASHION.label,
            emoji = "🧥",
            city = "Khobar",
            condition = ListingCondition.LIKE_NEW,
            postedAtMillis = hoursAgo(6),
            viewCount = 1_302,
        ),
        Listing(
            id = "l_bag",
            sellerId = "s_maha",
            title = "Leather shoulder bag, authentic",
            description = "Receipt and dust bag included. Corners are perfect, strap has light " +
                "wear shown close up in the video.",
            priceCents = 210_000,
            category = ListingCategory.FASHION.label,
            emoji = "👜",
            city = "Khobar",
            postedAtMillis = hoursAgo(22),
            viewCount = 2_044,
        ),
        Listing(
            id = "l_watch",
            sellerId = "s_maha",
            title = "Seiko automatic, 40mm",
            description = "Keeps good time, serviced last year. Two straps included.",
            priceCents = 130_000,
            category = ListingCategory.FASHION.label,
            emoji = "⌚",
            city = "Khobar",
            postedAtMillis = hoursAgo(65),
            viewCount = 890,
        ),

        // Property
        Listing(
            id = "l_apartment",
            sellerId = "s_abdullah",
            title = "3 bedroom apartment, north Riyadh",
            description = "165 sqm, third floor with lift, two parking spots. Ready to move in. " +
                "Annual rent, negotiable for two years up front.",
            priceCents = 6_500_000,
            category = ListingCategory.PROPERTY.label,
            emoji = "🏢",
            city = "Riyadh",
            condition = ListingCondition.NEW,
            postedAtMillis = hoursAgo(4),
            viewCount = 3_760,
        ),
        Listing(
            id = "l_villa",
            sellerId = "s_abdullah",
            title = "Villa for sale, 400 sqm corner plot",
            description = "Six bedrooms, driver's room, separate entrance. Walking distance to " +
                "two schools. Serious buyers only please.",
            priceCents = 220_000_000,
            category = ListingCategory.PROPERTY.label,
            emoji = "🏡",
            city = "Riyadh",
            postedAtMillis = hoursAgo(40),
            viewCount = 5_190,
        ),
        Listing(
            id = "l_shop",
            sellerId = "s_abdullah",
            title = "Retail shop for rent, main street",
            description = "72 sqm, glass front, ready for fit out. Municipality licence in place.",
            priceCents = 9_000_000,
            category = ListingCategory.PROPERTY.label,
            emoji = "🏬",
            city = "Riyadh",
            postedAtMillis = hoursAgo(88),
            viewCount = 1_440,
        ),

        // Pets
        Listing(
            id = "l_kittens",
            sellerId = "s_saraf",
            title = "Persian kittens, 9 weeks",
            description = "Vaccinated, litter trained, eating dry food. I visit the home before " +
                "rehoming, no exceptions.",
            priceCents = 90_000,
            category = ListingCategory.PETS.label,
            emoji = "🐈",
            city = "Jeddah",
            condition = ListingCondition.NEW,
            postedAtMillis = hoursAgo(2),
            viewCount = 7_640,
        ),
        Listing(
            id = "l_cattree",
            sellerId = "s_saraf",
            title = "Cat tree, 1.8 m",
            description = "Six months old, rope is intact. Free if you take the two litter boxes " +
                "with it.",
            priceCents = 22_000,
            category = ListingCategory.PETS.label,
            emoji = "🪵",
            city = "Jeddah",
            postedAtMillis = hoursAgo(33),
            viewCount = 512,
        ),

        // Other / outdoor
        Listing(
            id = "l_tent",
            sellerId = "s_khalid",
            title = "4 season tent, sleeps 6",
            description = "Used three trips. Poles all straight, no tears. Includes footprint " +
                "and a repair kit.",
            priceCents = 78_000,
            category = ListingCategory.OTHER.label,
            emoji = "⛺",
            city = "Riyadh",
            postedAtMillis = hoursAgo(9),
            viewCount = 1_180,
        ),
        Listing(
            id = "l_fridgebox",
            sellerId = "s_khalid",
            title = "Portable car fridge 40L",
            description = "12V and 220V. Holds minus 18 in summer. Cable and bag included.",
            priceCents = 95_000,
            category = ListingCategory.OTHER.label,
            emoji = "🧊",
            city = "Riyadh",
            postedAtMillis = hoursAgo(27),
            viewCount = 760,
        ),
        Listing(
            id = "l_bikes",
            sellerId = "s_khalid",
            title = "Two mountain bikes, aluminium",
            description = "Both size L, hydraulic brakes. Selling as a pair only, not separately.",
            priceCents = 160_000,
            isNegotiable = false,
            category = ListingCategory.OTHER.label,
            emoji = "🚲",
            city = "Riyadh",
            postedAtMillis = hoursAgo(57),
            viewCount = 990,
        ),
        Listing(
            id = "l_mirror",
            sellerId = "s_layla",
            title = "Antique brass mirror",
            description = "Cleaned and re-silvered. Heavy, needs a proper wall fixing.",
            priceCents = 68_000,
            isNegotiable = false,
            category = ListingCategory.OTHER.label,
            emoji = "🪞",
            city = "Dammam",
            postedAtMillis = hoursAgo(14),
            viewCount = 388,
        ),
    )

    val liveStreams: List<LiveStream> = listOf(
        LiveStream(
            id = "live_faris",
            sellerId = "s_faris",
            title = "Walking around the Land Cruiser, ask me anything",
            videoUrl = "$VIDEO_BASE_URL/video_1.mp4",
            topic = "Cars",
            listingIds = listOf("l_landcruiser", "l_camry", "l_hilux"),
            pinnedListingId = "l_landcruiser",
            startingViewerCount = 3_480,
        ),
        LiveStream(
            id = "live_techsouq",
            sellerId = "s_techsouq",
            title = "Testing every phone before I post it 📱",
            videoUrl = "$VIDEO_BASE_URL/video_2.mp4",
            topic = "Electronics",
            listingIds = listOf("l_iphone", "l_macbook", "l_ps5", "l_tv"),
            pinnedListingId = "l_iphone",
            startingViewerCount = 12_940,
        ),
        LiveStream(
            id = "live_noura",
            sellerId = "s_noura",
            title = "Whole apartment going, room by room",
            videoUrl = "$VIDEO_BASE_URL/video_3.mp4",
            topic = "Furniture",
            listingIds = listOf("l_sofa", "l_diningtable", "l_fridge"),
            pinnedListingId = "l_sofa",
            startingViewerCount = 1_210,
        ),
        LiveStream(
            id = "live_saraf",
            sellerId = "s_saraf",
            title = "Kittens are awake 🐈 come say hi",
            videoUrl = "$VIDEO_BASE_URL/video_4.mp4",
            topic = "Pets",
            listingIds = listOf("l_kittens", "l_cattree"),
            pinnedListingId = "l_kittens",
            startingViewerCount = 8_360,
        ),
        LiveStream(
            id = "live_abdullah",
            sellerId = "s_abdullah",
            title = "Live tour of the north Riyadh apartment",
            videoUrl = "$VIDEO_BASE_URL/video_5.mp4",
            topic = "Property",
            listingIds = listOf("l_apartment", "l_villa", "l_shop"),
            pinnedListingId = "l_apartment",
            startingViewerCount = 2_070,
        ),
        LiveStream(
            id = "live_maha",
            sellerId = "s_maha",
            title = "Closet clear out, everything on camera",
            videoUrl = "$VIDEO_BASE_URL/video_6.mp4",
            topic = "Fashion",
            listingIds = listOf("l_abaya", "l_bag", "l_watch"),
            pinnedListingId = "l_bag",
            startingViewerCount = 4_530,
        ),
    )

    private val sellersById: Map<String, Seller> = sellers.associateBy { it.id }
    private val seedListingsById: Map<String, Listing> = seedListings.associateBy { it.id }

    val categories: List<ListingCategory> = ListingCategory.entries.toList()

    fun seedListings(): List<Listing> = seedListings

    fun seller(id: String?): Seller? = id?.let { sellersById[it] }

    fun seedListing(id: String): Listing? = seedListingsById[id]

    fun liveStream(id: String?): LiveStream? =
        id?.let { wanted -> liveStreams.firstOrNull { it.id == wanted } }

    fun liveStreamForSeller(sellerId: String): LiveStream? =
        liveStreams.firstOrNull { it.sellerId == sellerId }

    /**
     * Deterministically attaches one to three ads to a feed video. The same video id always
     * produces the same tag, which keeps the feed stable across scrolls and relaunches.
     */
    fun tagForVideo(videoId: String, available: List<Listing>): VideoListingTag {
        if (available.isEmpty()) return VideoListingTag(videoId, emptyList())
        val seed = stableHash(videoId)
        val count = 1 + seed % 3
        val ids = ArrayList<String>(count)
        var cursor = seed % available.size
        val step = 1 + (seed / 7) % (available.size - 1).coerceAtLeast(1)
        repeat(count.coerceAtMost(available.size)) {
            val candidate = available[cursor % available.size].id
            if (!ids.contains(candidate)) ids.add(candidate)
            cursor += step
        }
        return VideoListingTag(videoId, ids)
    }
}
