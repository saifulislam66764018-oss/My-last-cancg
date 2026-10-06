package com.example.model

import java.net.URI
import kotlin.math.roundToInt

/**
 * Hardened security utilities to sanitize untrusted text, URLs, phone numbers,
 * and numeric values from Firestore or user inputs.
 */
object SecurityUtils {
    val ALLOWED_ADMIN_UIDS = setOf(
        "jvpnDzUBPqfEfGiGYBoxQLgPBFo2",
        "buYO5SjqICa5yuYz5P648cpb4mr1"
    )

    const val MAX_PRICE_BDT = 10_000_000.0
    const val MAX_DATA_IMAGE_LENGTH = 750_000 // Safe limit (~550KB compressed image) for Firestore 1MB doc limit

    fun isAuthorizedAdmin(uid: String?): Boolean {
        return uid != null && ALLOWED_ADMIN_UIDS.contains(uid.trim())
    }

    fun sanitizeText(input: String?, maxLength: Int = 2000): String {
        if (input.isNullOrBlank()) return ""
        return input
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("on[a-zA-Z]+\\s*=", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]+>"), "")
            .replace("javascript:", "", ignoreCase = true)
            .replace("vbscript:", "", ignoreCase = true)
            .replace("data:text/html", "", ignoreCase = true)
            .replace("data:", "", ignoreCase = true)
            .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")
            .trim()
            .take(maxLength)
    }

    fun sanitizeHttpsUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val trimmed = url.trim()
        if (trimmed.length > 2048) return ""
        if (trimmed.contains(Regex("[\\s<>\"'`]"))) return ""
        val lower = trimmed.lowercase()
        if (lower.startsWith("javascript:") ||
            lower.startsWith("data:") ||
            lower.startsWith("vbscript:") ||
            lower.startsWith("file:")
        ) {
            return ""
        }
        return try {
            val parsed = URI(trimmed)
            val scheme = parsed.scheme?.lowercase() ?: return ""
            val host = parsed.host ?: return ""
            if (scheme != "https" || host.isBlank() || !host.contains(".") || parsed.userInfo != null) {
                ""
            } else {
                trimmed
            }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Validates either a safe external HTTPS URL or a safe compressed Base64 raster image
     * (jpeg, png, webp) selected via the Android Photo Picker. Rejects SVG/HTML/script data URLs.
     */
    fun sanitizeImageSource(source: String?): String {
        if (source.isNullOrBlank()) return ""
        val trimmed = source.trim()
        if (trimmed.startsWith("data:image/", ignoreCase = true)) {
            if (trimmed.length > MAX_DATA_IMAGE_LENGTH) return ""
            val safeRasterPattern = Regex(
                "^data:image/(jpeg|jpg|png|webp);base64,[A-Za-z0-9+/=]+$",
                RegexOption.IGNORE_CASE
            )
            return if (safeRasterPattern.matches(trimmed)) trimmed else ""
        }
        return sanitizeHttpsUrl(trimmed)
    }

    fun sanitizePhone(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        return phone.replace(Regex("[^0-9+]"), "").trim().take(20)
    }

    fun isValidWhatsAppDigits(digitsOnly: String): Boolean {
        return digitsOnly.matches(Regex("^\\d{10,15}$"))
    }

    fun sanitizePrice(value: Any?): Double {
        val num = when (value) {
            is Number -> value.toDouble()
            is String -> value.trim().toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        if (num.isNaN() || num.isInfinite() || num < 0.0) return 0.0
        return num.coerceAtMost(MAX_PRICE_BDT)
    }

    /**
     * Evaluates Firestore Security Rules permissions for auditing and automated testing.
     */
    fun evaluateFirestoreRule(
        collection: String,
        operation: String, // "read", "create", "update", "delete"
        authUid: String?,
        price: Double = 100.0,
        imageUrl: String = "https://example.com/item.jpg"
    ): Boolean {
        val validCollection = collection in setOf("products", "categories", "settings/store")
        if (!validCollection) return false

        if (operation == "read") {
            return true
        }

        if (!isAuthorizedAdmin(authUid)) {
            return false
        }

        if (collection == "settings/store" && operation == "delete") {
            return false
        }

        if (collection == "products" && (operation == "create" || operation == "update")) {
            if (price < 0.0 || price > MAX_PRICE_BDT) return false
            if (sanitizeImageSource(imageUrl).isEmpty()) return false
        }

        return operation in setOf("create", "update", "delete")
    }
}

data class Product(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0,
    val oldPrice: Double? = null,
    val category: String = "",
    val subCategory: String = "",
    val description: String = "",
    val available: Boolean = true,
    val featured: Boolean = false,
    val newArrival: Boolean = false,
    val createdAt: Long = 0L
) {
    val discountPercentage: Int?
        get() {
            val old = oldPrice ?: return null
            if (old <= price || old <= 0.0 || price < 0.0) return null
            val pct = (((old - price) / old) * 100.0).roundToInt()
            return if (pct > 0) pct else null
        }
}

data class Category(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val subcategories: List<String> = emptyList(),
    val createdAt: Long = 0L
)

val JEWELRY_ACCESSORIES_SUBCATEGORIES = listOf(
    "ক্লিপ & ব্যান্ড",
    "এক্সেসরিজ",
    "বিউটি কেয়ার",
    "ন্যাচারাল কেয়ার"
)

val WOMENS_CLOTHING_SUBCATEGORIES = listOf(
    "রেডিমেড থ্রিপিস",
    "আনস্টিজ থ্রিপিস",
    "গাউন & কুর্তি",
    "লেহেঙ্গা & পার্টি",
    "ওয়েস্টার্ন ড্রেস",
    "টিশার্ট & স্ক্যার্ট",
    "শাড়ি",
    "বোরকা",
    "হিজাব & নিকাব",
    "সুন্নাতি ড্রেস"
)

val DEFAULT_MAIN_CATEGORIES = listOf(
    Category(
        id = "main_jewelry_accessories",
        name = "জুয়েলারি এন্ড এক্সেসরিজ",
        imageUrl = "",
        subcategories = JEWELRY_ACCESSORIES_SUBCATEGORIES,
        createdAt = 1L
    ),
    Category(
        id = "main_bag_collection",
        name = "ব্যাগ কালেকশন",
        imageUrl = "",
        subcategories = emptyList(),
        createdAt = 2L
    ),
    Category(
        id = "main_couple_combo",
        name = "কাপল এন্ড কম্বো",
        imageUrl = "",
        subcategories = emptyList(),
        createdAt = 3L
    ),
    Category(
        id = "main_womens_clothing",
        name = "মেয়েদের পোশাক",
        imageUrl = "",
        subcategories = WOMENS_CLOTHING_SUBCATEGORIES,
        createdAt = 4L
    ),
    Category(
        id = "main_baby_collection",
        name = "বেবি কালেকশন",
        imageUrl = "",
        subcategories = emptyList(),
        createdAt = 5L
    )
)

data class HeroProductIds(
    val left1: String = "",
    val left2: String = "",
    val right1: String = "",
    val right2: String = ""
)

data class StoreSettings(
    val websiteName: String = "Glowisticbd",
    val description: String = "Your Unlimited Glow. Premium cosmetics & skincare collection in Bangladesh.",
    val tagline: String = "আপনার সীমাহীন গ্লো। বাংলাদেশের প্রিমিয়াম কসমেটিকস ও স্কিনকেয়ার কালেকশন।",
    val whatsappNumber: String = "+8801779531040",
    val deliveryCharge: Double = 0.0,
    val logoUrl: String = "",
    val faviconUrl: String = "",
    val heroVideoUrl: String = "",
    val heroMediaType: String = "video", // "video" | "image"
    val heroProductIds: HeroProductIds = HeroProductIds()
)

data class CartItem(
    val productId: String,
    val title: String,
    val imageUrl: String,
    val price: Double,
    val quantity: Int
)

enum class SortOption(val label: String) {
    DEFAULT("Default"),
    PRICE_LOW_TO_HIGH("Price: Low to High"),
    PRICE_HIGH_TO_LOW("Price: High to Low"),
    NEWEST("Newest"),
    FEATURED("Featured")
}

enum class NavSection(val title: String) {
    HOME("Home"),
    SHOP("Shop"),
    CATEGORIES("Categories"),
    ADMIN("Admin")
}

fun formatBdt(amount: Double): String {
    val rounded = amount.roundToInt()
    return "৳$rounded"
}
