package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.DEFAULT_MAIN_CATEGORIES
import com.example.model.Product
import com.example.model.SecurityUtils
import com.example.ui.components.countProductsForCategory
import com.example.ui.components.countProductsForSubCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Glowisticbd", appName)
    }

    @Test
    fun `verify security sanitization and discount calculation`() {
        // 1. Admin UID enforcement
        assertTrue(SecurityUtils.isAuthorizedAdmin("jvpnDzUBPqfEfGiGYBoxQLgPBFo2"))
        assertTrue(SecurityUtils.isAuthorizedAdmin("buYO5SjqICa5yuYz5P648cpb4mr1"))
        assertFalse(SecurityUtils.isAuthorizedAdmin("random_visitor_uid"))
        assertFalse(SecurityUtils.isAuthorizedAdmin(null))
        assertFalse(SecurityUtils.isAuthorizedAdmin(""))

        // 2. Malicious URL and XSS rejection
        assertEquals("", SecurityUtils.sanitizeHttpsUrl("javascript:alert(1)"))
        assertEquals("", SecurityUtils.sanitizeHttpsUrl("data:text/html,<script>alert(1)</script>"))
        assertEquals("", SecurityUtils.sanitizeHttpsUrl("http://insecure.example.com/img.jpg"))
        assertEquals("https://example.com/img.jpg", SecurityUtils.sanitizeHttpsUrl("https://example.com/img.jpg"))

        // 3. Script and tag stripping
        val sanitizedText = SecurityUtils.sanitizeText("<script>evil()</script>Glow Serum onclick=alert(1)")
        assertFalse(sanitizedText.contains("<script"))
        assertFalse(sanitizedText.contains("onclick="))

        // 4. Price sanitization & discount percentage
        assertEquals(0.0, SecurityUtils.sanitizePrice(-500.0), 0.001)
        val product = Product(price = 800.0, oldPrice = 1000.0)
        assertEquals(20, product.discountPercentage)

        // 5. Firestore rule evaluation audit
        assertTrue(SecurityUtils.evaluateFirestoreRule("products", "read", null))
        assertFalse(SecurityUtils.evaluateFirestoreRule("products", "create", null))
        assertFalse(SecurityUtils.evaluateFirestoreRule("products", "create", "wrong_uid"))
        assertTrue(
            SecurityUtils.evaluateFirestoreRule(
                collection = "products",
                operation = "create",
                authUid = "buYO5SjqICa5yuYz5P648cpb4mr1",
                price = 1250.0,
                imageUrl = "https://example.com/serum.jpg"
            )
        )
        assertFalse(
            SecurityUtils.evaluateFirestoreRule(
                collection = "products",
                operation = "create",
                authUid = "buYO5SjqICa5yuYz5P648cpb4mr1",
                price = -10.0,
                imageUrl = "https://example.com/serum.jpg"
            )
        )
        assertFalse(
            SecurityUtils.evaluateFirestoreRule(
                collection = "settings/store",
                operation = "delete",
                authUid = "buYO5SjqICa5yuYz5P648cpb4mr1"
            )
        )
    }

    @Test
    fun `verify product gallery category and subcategory filtering`() {
        val sampleProducts = listOf(
            Product(
                id = "p1",
                title = "Silk Saree",
                category = "মেয়েদের পোশাক",
                subCategory = "শাড়ি",
                price = 2500.0
            ),
            Product(
                id = "p2",
                title = "Party Gown",
                category = "মেয়েদের পোশাক",
                subCategory = "গাউন & কুর্তি",
                price = 3200.0
            ),
            Product(
                id = "p3",
                title = "Pearl Hair Clip",
                category = "জুয়েলারি এন্ড এক্সেসরিজ",
                subCategory = "ক্লিপ & ব্যান্ড",
                price = 350.0
            )
        )

        val womensCategory = DEFAULT_MAIN_CATEGORIES.first { it.name == "মেয়েদের পোশাক" }
        val jewelryCategory = DEFAULT_MAIN_CATEGORIES.first { it.name == "জুয়েলারি এন্ড এক্সেসরিজ" }

        assertEquals(2, countProductsForCategory(womensCategory, sampleProducts))
        assertEquals(1, countProductsForCategory(jewelryCategory, sampleProducts))
        assertEquals(1, countProductsForSubCategory("শাড়ি", sampleProducts))
        assertEquals(1, countProductsForSubCategory("ক্লিপ & ব্যান্ড", sampleProducts))
    }
}
