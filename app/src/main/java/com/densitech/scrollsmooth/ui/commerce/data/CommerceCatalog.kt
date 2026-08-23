package com.densitech.scrollsmooth.ui.commerce.data

import com.densitech.scrollsmooth.ui.commerce.model.LiveStream
import com.densitech.scrollsmooth.ui.commerce.model.Product
import com.densitech.scrollsmooth.ui.commerce.model.ProductCategory
import com.densitech.scrollsmooth.ui.commerce.model.ProductOption
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import com.densitech.scrollsmooth.ui.commerce.model.VideoProductTag
import com.densitech.scrollsmooth.ui.commerce.model.stableHash

/**
 * The seeded storefront. There is no commerce backend behind this build, so the catalogue lives
 * here and creator-listed products are layered on top of it by [SellerRepository].
 *
 * Video to product tagging is derived from the video id rather than stored, so the feed always
 * shows the same products for the same video across launches.
 */
object CommerceCatalog {

    private const val VIDEO_BASE_URL = "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos"

    val sellers: List<Seller> = listOf(
        Seller(
            id = "s_glowlab",
            handle = "glowlab",
            displayName = "Glow Lab",
            bio = "Clean skincare made in small batches. Live every Tuesday 7pm PT.",
            emoji = "🧴",
            rating = 4.9f,
            followers = 482_000,
            isVerified = true,
            shipsFrom = "Los Angeles, CA",
        ),
        Seller(
            id = "s_thrifted",
            handle = "thrifted.by.mia",
            displayName = "Thrifted by Mia",
            bio = "One-of-one vintage drops. If you see it, it is the only one.",
            emoji = "🧥",
            rating = 4.8f,
            followers = 219_400,
            isVerified = true,
            shipsFrom = "Brooklyn, NY",
        ),
        Seller(
            id = "s_gearpost",
            handle = "gearpost",
            displayName = "GearPost",
            bio = "Audio and desk gear, tested on camera before it is listed.",
            emoji = "🎧",
            rating = 4.7f,
            followers = 903_100,
            isVerified = true,
            shipsFrom = "Austin, TX",
        ),
        Seller(
            id = "s_hearthhome",
            handle = "hearth.home",
            displayName = "Hearth & Home",
            bio = "Handmade ceramics and slow kitchen things.",
            emoji = "🏺",
            rating = 4.9f,
            followers = 156_700,
            isVerified = false,
            shipsFrom = "Portland, OR",
        ),
        Seller(
            id = "s_kettlebella",
            handle = "kettlebella",
            displayName = "Kettlebella",
            bio = "Coach Ana. Home gym kit that actually fits in a closet.",
            emoji = "🏋",
            rating = 4.6f,
            followers = 331_200,
            isVerified = true,
            shipsFrom = "Miami, FL",
        ),
        Seller(
            id = "s_snackrun",
            handle = "snackrun",
            displayName = "Snack Run",
            bio = "Imported snacks, restocked weekly. Chaos boxes sell out fast.",
            emoji = "🍫",
            rating = 4.5f,
            followers = 612_800,
            isVerified = false,
            shipsFrom = "Chicago, IL",
        ),
        Seller(
            id = "s_studioleaf",
            handle = "studioleaf",
            displayName = "Studio Leaf",
            bio = "Plants, pots and the boring stuff that keeps them alive.",
            emoji = "🪴",
            rating = 4.8f,
            followers = 98_300,
            isVerified = false,
            shipsFrom = "Seattle, WA",
        ),
        Seller(
            id = "s_nightowl",
            handle = "nightowl.tech",
            displayName = "Night Owl Tech",
            bio = "Desk setups after dark. Cables, lights, keycaps.",
            emoji = "⌨",
            rating = 4.7f,
            followers = 274_500,
            isVerified = true,
            shipsFrom = "San Jose, CA",
        ),
    )

    private val seedProducts: List<Product> = listOf(
        // Glow Lab — beauty
        Product(
            id = "p_serum",
            sellerId = "s_glowlab",
            title = "Barrier Repair Serum 30ml",
            description = "Five percent niacinamide with ceramides. The one from the Tuesday live " +
                "that sold out twice. Fragrance free, safe to layer under sunscreen.",
            priceCents = 2_800,
            compareAtPriceCents = 4_200,
            category = ProductCategory.BEAUTY.label,
            emoji = "🧪",
            rating = 4.9f,
            ratingCount = 12_480,
            soldCount = 41_200,
            options = listOf(ProductOption("Size", listOf("30ml", "50ml"))),
            shipsInDays = 2,
        ),
        Product(
            id = "p_cleanser",
            sellerId = "s_glowlab",
            title = "Gel-to-Milk Cleanser",
            description = "Melts sunscreen without stripping. pH 5.5, 150ml pump bottle.",
            priceCents = 1_900,
            compareAtPriceCents = 2_600,
            category = ProductCategory.BEAUTY.label,
            emoji = "🫧",
            rating = 4.8f,
            ratingCount = 6_310,
            soldCount = 18_900,
        ),
        Product(
            id = "p_lipoil",
            sellerId = "s_glowlab",
            title = "Tinted Lip Oil",
            description = "Non sticky, four shades, doubles as a cheek tint.",
            priceCents = 1_400,
            category = ProductCategory.BEAUTY.label,
            emoji = "💋",
            rating = 4.7f,
            ratingCount = 9_002,
            soldCount = 27_450,
            options = listOf(ProductOption("Shade", listOf("Peach", "Rose", "Berry", "Clear"))),
        ),
        Product(
            id = "p_spf",
            sellerId = "s_glowlab",
            title = "Invisible SPF 50 Fluid",
            description = "No white cast, no pilling under makeup. 50ml.",
            priceCents = 2_400,
            compareAtPriceCents = 3_000,
            category = ProductCategory.BEAUTY.label,
            emoji = "☀",
            rating = 4.9f,
            ratingCount = 15_780,
            soldCount = 52_300,
        ),

        // Thrifted by Mia — fashion
        Product(
            id = "p_leatherjacket",
            sellerId = "s_thrifted",
            title = "90s Cropped Leather Jacket",
            description = "True vintage, real leather, size M. One available. Measurements in the " +
                "last photo of the video.",
            priceCents = 12_800,
            compareAtPriceCents = 19_000,
            category = ProductCategory.FASHION.label,
            emoji = "🧥",
            rating = 5.0f,
            ratingCount = 214,
            soldCount = 198,
            stock = 1,
            shipsInDays = 4,
        ),
        Product(
            id = "p_denim",
            sellerId = "s_thrifted",
            title = "Straight Leg Vintage Denim",
            description = "Broken in properly. Waist 26 to 34, pick yours below.",
            priceCents = 6_400,
            category = ProductCategory.FASHION.label,
            emoji = "👖",
            rating = 4.8f,
            ratingCount = 1_842,
            soldCount = 4_010,
            options = listOf(ProductOption("Waist", listOf("26", "28", "30", "32", "34"))),
        ),
        Product(
            id = "p_knit",
            sellerId = "s_thrifted",
            title = "Oversized Wool Knit",
            description = "Heavy lambswool, unisex fit. Three colourways left from the last drop.",
            priceCents = 5_900,
            compareAtPriceCents = 8_500,
            category = ProductCategory.FASHION.label,
            emoji = "🧶",
            rating = 4.7f,
            ratingCount = 903,
            soldCount = 2_140,
            options = listOf(
                ProductOption("Size", listOf("S", "M", "L", "XL")),
                ProductOption("Colour", listOf("Oat", "Moss", "Charcoal")),
            ),
        ),
        Product(
            id = "p_tote",
            sellerId = "s_thrifted",
            title = "Waxed Canvas Tote",
            description = "Fits a 16 inch laptop. Gets better looking as it ages.",
            priceCents = 4_200,
            category = ProductCategory.FASHION.label,
            emoji = "👜",
            rating = 4.6f,
            ratingCount = 512,
            soldCount = 1_320,
        ),

        // GearPost — tech
        Product(
            id = "p_earbuds",
            sellerId = "s_gearpost",
            title = "ANC Earbuds Gen 3",
            description = "Eight hours a charge, 28 with the case. The pair used in every video " +
                "on this account.",
            priceCents = 7_900,
            compareAtPriceCents = 12_900,
            category = ProductCategory.TECH.label,
            emoji = "🎧",
            rating = 4.7f,
            ratingCount = 22_140,
            soldCount = 88_600,
            options = listOf(ProductOption("Colour", listOf("Black", "Sand", "Slate"))),
            shipsInDays = 2,
        ),
        Product(
            id = "p_micarm",
            sellerId = "s_gearpost",
            title = "Low Profile Mic Arm",
            description = "Internal cable routing, clamps to a 60mm desk edge.",
            priceCents = 5_400,
            category = ProductCategory.TECH.label,
            emoji = "🎙",
            rating = 4.8f,
            ratingCount = 3_190,
            soldCount = 7_820,
        ),
        Product(
            id = "p_powerbank",
            sellerId = "s_gearpost",
            title = "20K Power Bank, 100W",
            description = "Charges a laptop. Airline safe. Reads out remaining watt hours.",
            priceCents = 4_900,
            compareAtPriceCents = 6_900,
            category = ProductCategory.TECH.label,
            emoji = "🔋",
            rating = 4.6f,
            ratingCount = 8_450,
            soldCount = 19_700,
        ),
        Product(
            id = "p_tripod",
            sellerId = "s_gearpost",
            title = "Creator Tripod + Remote",
            description = "Extends to 1.6m, folds to phone length. Bluetooth shutter included.",
            priceCents = 3_600,
            category = ProductCategory.TECH.label,
            emoji = "📷",
            rating = 4.5f,
            ratingCount = 4_006,
            soldCount = 11_240,
        ),

        // Hearth & Home
        Product(
            id = "p_mug",
            sellerId = "s_hearthhome",
            title = "Speckled Stoneware Mug",
            description = "Thrown and glazed by hand, 350ml. Every one is slightly different.",
            priceCents = 3_200,
            category = ProductCategory.HOME.label,
            emoji = "☕",
            rating = 4.9f,
            ratingCount = 1_204,
            soldCount = 3_980,
            options = listOf(ProductOption("Glaze", listOf("Sand", "Sea", "Ash"))),
            shipsInDays = 5,
        ),
        Product(
            id = "p_board",
            sellerId = "s_hearthhome",
            title = "End Grain Walnut Board",
            description = "Knife friendly end grain, 40x30cm, oiled and ready to use.",
            priceCents = 8_900,
            compareAtPriceCents = 11_000,
            category = ProductCategory.HOME.label,
            emoji = "🪵",
            rating = 4.9f,
            ratingCount = 640,
            soldCount = 1_510,
            shipsInDays = 6,
        ),
        Product(
            id = "p_linen",
            sellerId = "s_hearthhome",
            title = "Washed Linen Napkins, Set of 4",
            description = "Stonewashed European flax. Softens every wash.",
            priceCents = 4_400,
            category = ProductCategory.HOME.label,
            emoji = "🧺",
            rating = 4.7f,
            ratingCount = 388,
            soldCount = 902,
        ),

        // Kettlebella — fitness
        Product(
            id = "p_kettlebell",
            sellerId = "s_kettlebella",
            title = "Adjustable Kettlebell 5-20kg",
            description = "Six weights in one bell. The entire closet gym in a single purchase.",
            priceCents = 14_900,
            compareAtPriceCents = 21_900,
            category = ProductCategory.FITNESS.label,
            emoji = "🏋",
            rating = 4.8f,
            ratingCount = 5_620,
            soldCount = 12_800,
            shipsInDays = 4,
        ),
        Product(
            id = "p_bands",
            sellerId = "s_kettlebella",
            title = "Fabric Resistance Band Set",
            description = "Three tensions, does not roll up on squats.",
            priceCents = 2_200,
            compareAtPriceCents = 3_400,
            category = ProductCategory.FITNESS.label,
            emoji = "🎗",
            rating = 4.7f,
            ratingCount = 9_910,
            soldCount = 33_400,
        ),
        Product(
            id = "p_mat",
            sellerId = "s_kettlebella",
            title = "6mm Grip Mat",
            description = "Grips when your hands are wet. 183x68cm, strap included.",
            priceCents = 3_900,
            category = ProductCategory.FITNESS.label,
            emoji = "🧘",
            rating = 4.6f,
            ratingCount = 2_770,
            soldCount = 8_150,
            options = listOf(ProductOption("Colour", listOf("Clay", "Ink", "Fern"))),
        ),

        // Snack Run
        Product(
            id = "p_chaosbox",
            sellerId = "s_snackrun",
            title = "Chaos Box: 20 Imported Snacks",
            description = "Twenty snacks you cannot get locally. No two boxes are the same.",
            priceCents = 3_500,
            compareAtPriceCents = 4_800,
            category = ProductCategory.SNACKS.label,
            emoji = "📦",
            rating = 4.6f,
            ratingCount = 18_320,
            soldCount = 64_700,
            shipsInDays = 3,
        ),
        Product(
            id = "p_matcha",
            sellerId = "s_snackrun",
            title = "Ceremonial Matcha 40g",
            description = "First harvest, stone milled. Whisk sold separately, sorry.",
            priceCents = 2_600,
            category = ProductCategory.SNACKS.label,
            emoji = "🍵",
            rating = 4.8f,
            ratingCount = 4_120,
            soldCount = 9_880,
        ),
        Product(
            id = "p_chocolate",
            sellerId = "s_snackrun",
            title = "Single Origin Chocolate x6",
            description = "Six bars, six origins, blind tasting card in the box.",
            priceCents = 3_100,
            compareAtPriceCents = 3_900,
            category = ProductCategory.SNACKS.label,
            emoji = "🍫",
            rating = 4.7f,
            ratingCount = 2_040,
            soldCount = 6_130,
        ),

        // Studio Leaf
        Product(
            id = "p_monstera",
            sellerId = "s_studioleaf",
            title = "Monstera in Nursery Pot",
            description = "Established plant, four to six leaves. Shipped in 48h, packed damp.",
            priceCents = 4_500,
            category = ProductCategory.HOME.label,
            emoji = "🪴",
            rating = 4.8f,
            ratingCount = 1_460,
            soldCount = 3_220,
            options = listOf(ProductOption("Pot size", listOf("12cm", "17cm", "21cm"))),
            shipsInDays = 2,
        ),
        Product(
            id = "p_planter",
            sellerId = "s_studioleaf",
            title = "Self Watering Planter",
            description = "Two week reservoir. The reason my plants survive tour.",
            priceCents = 2_900,
            compareAtPriceCents = 3_800,
            category = ProductCategory.HOME.label,
            emoji = "🏺",
            rating = 4.7f,
            ratingCount = 812,
            soldCount = 2_450,
        ),

        // Night Owl Tech
        Product(
            id = "p_keycaps",
            sellerId = "s_nightowl",
            title = "PBT Keycap Set, 129 Keys",
            description = "Doubleshot PBT, shine through legends, fits most layouts.",
            priceCents = 6_800,
            compareAtPriceCents = 8_900,
            category = ProductCategory.TECH.label,
            emoji = "⌨",
            rating = 4.8f,
            ratingCount = 3_940,
            soldCount = 9_600,
            options = listOf(ProductOption("Colourway", listOf("Midnight", "Dawn", "Retro"))),
        ),
        Product(
            id = "p_lightbar",
            sellerId = "s_nightowl",
            title = "Monitor Light Bar",
            description = "No screen glare, warm to cool, auto dimming sensor.",
            priceCents = 5_200,
            category = ProductCategory.TECH.label,
            emoji = "💡",
            rating = 4.7f,
            ratingCount = 6_720,
            soldCount = 21_050,
        ),
        Product(
            id = "p_cablekit",
            sellerId = "s_nightowl",
            title = "Desk Cable Management Kit",
            description = "Trays, sleeves and clips. Twenty minutes to a clean desk.",
            priceCents = 2_700,
            compareAtPriceCents = 3_600,
            category = ProductCategory.TECH.label,
            emoji = "🔌",
            rating = 4.5f,
            ratingCount = 2_190,
            soldCount = 7_400,
        ),
    )

    val liveStreams: List<LiveStream> = listOf(
        LiveStream(
            id = "live_glowlab",
            sellerId = "s_glowlab",
            title = "Serum restock 🚨 first 200 get the bundle price",
            videoUrl = "$VIDEO_BASE_URL/video_1.mp4",
            topic = "Beauty",
            productIds = listOf("p_serum", "p_spf", "p_cleanser", "p_lipoil"),
            pinnedProductId = "p_serum",
            startingViewerCount = 12_480,
            flashSaleSeconds = 9 * 60,
            flashSaleDiscountPercent = 30,
        ),
        LiveStream(
            id = "live_thrifted",
            sellerId = "s_thrifted",
            title = "Vintage drop — one of each, first comment wins",
            videoUrl = "$VIDEO_BASE_URL/video_2.mp4",
            topic = "Fashion",
            productIds = listOf("p_leatherjacket", "p_denim", "p_knit", "p_tote"),
            pinnedProductId = "p_leatherjacket",
            startingViewerCount = 5_210,
            flashSaleSeconds = 5 * 60,
            flashSaleDiscountPercent = 20,
        ),
        LiveStream(
            id = "live_gearpost",
            sellerId = "s_gearpost",
            title = "Earbuds teardown + lowest price we have run",
            videoUrl = "$VIDEO_BASE_URL/video_3.mp4",
            topic = "Tech",
            productIds = listOf("p_earbuds", "p_powerbank", "p_micarm", "p_tripod"),
            pinnedProductId = "p_earbuds",
            startingViewerCount = 28_940,
            flashSaleSeconds = 12 * 60,
            flashSaleDiscountPercent = 25,
        ),
        LiveStream(
            id = "live_kettlebella",
            sellerId = "s_kettlebella",
            title = "Closet gym setup, bundle deal for the hour",
            videoUrl = "$VIDEO_BASE_URL/video_4.mp4",
            topic = "Fitness",
            productIds = listOf("p_kettlebell", "p_bands", "p_mat"),
            pinnedProductId = "p_kettlebell",
            startingViewerCount = 3_860,
            flashSaleSeconds = 7 * 60,
            flashSaleDiscountPercent = 15,
        ),
        LiveStream(
            id = "live_snackrun",
            sellerId = "s_snackrun",
            title = "Unboxing the new chaos box live 🍫",
            videoUrl = "$VIDEO_BASE_URL/video_5.mp4",
            topic = "Snacks",
            productIds = listOf("p_chaosbox", "p_chocolate", "p_matcha"),
            pinnedProductId = "p_chaosbox",
            startingViewerCount = 9_120,
            flashSaleSeconds = 4 * 60,
            flashSaleDiscountPercent = 20,
        ),
        LiveStream(
            id = "live_hearthhome",
            sellerId = "s_hearthhome",
            title = "Kiln opening — mugs going up as they cool",
            videoUrl = "$VIDEO_BASE_URL/video_6.mp4",
            topic = "Home",
            productIds = listOf("p_mug", "p_board", "p_linen"),
            pinnedProductId = "p_mug",
            startingViewerCount = 1_740,
            flashSaleSeconds = 15 * 60,
            flashSaleDiscountPercent = 10,
        ),
    )

    private val sellersById: Map<String, Seller> = sellers.associateBy { it.id }
    private val seedProductsById: Map<String, Product> = seedProducts.associateBy { it.id }

    val categories: List<ProductCategory> = ProductCategory.entries.toList()

    fun seedProducts(): List<Product> = seedProducts

    fun seller(id: String?): Seller? = id?.let { sellersById[it] }

    fun seedProduct(id: String): Product? = seedProductsById[id]

    fun liveStream(id: String?): LiveStream? = id?.let { stream -> liveStreams.firstOrNull { it.id == stream } }

    fun liveStreamForSeller(sellerId: String): LiveStream? = liveStreams.firstOrNull { it.sellerId == sellerId }

    /**
     * Deterministically tags a feed video with one to three products. The same video id always
     * produces the same tag, which keeps the feed stable across scrolls and relaunches.
     */
    fun tagForVideo(videoId: String, availableProducts: List<Product>): VideoProductTag {
        if (availableProducts.isEmpty()) return VideoProductTag(videoId, emptyList())
        val seed = stableHash(videoId)
        val count = 1 + seed % 3
        val ids = ArrayList<String>(count)
        var cursor = seed % availableProducts.size
        val step = 1 + (seed / 7) % (availableProducts.size - 1).coerceAtLeast(1)
        repeat(count.coerceAtMost(availableProducts.size)) {
            val candidate = availableProducts[cursor % availableProducts.size].id
            if (!ids.contains(candidate)) ids.add(candidate)
            cursor += step
        }
        return VideoProductTag(videoId, ids)
    }
}
