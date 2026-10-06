package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.NavSection
import com.example.model.StoreSettings
import com.example.ui.components.AdminPanelScreen
import com.example.ui.components.CategoriesSection
import com.example.ui.components.CheckoutModal
import com.example.ui.components.GlassHeader
import com.example.ui.components.HeroSection
import com.example.ui.components.MainShopSection
import com.example.ui.components.ProductDetailsModal
import com.example.ui.components.SlideOverCartDrawer
import com.example.ui.components.StorePromoBannerSection
import com.example.ui.theme.GlowBgChampagne
import com.example.ui.theme.GlowBgLight
import com.example.ui.theme.GlowBgPearl
import com.example.ui.theme.GlowBgRoseMist
import com.example.ui.theme.GlowBorderLight
import com.example.ui.theme.GlowDark
import com.example.ui.theme.GlowDarkPlum
import com.example.ui.theme.GlowGoldAccent
import com.example.ui.theme.GlowPrimary
import com.example.ui.theme.GlowSecondary
import com.example.ui.theme.GlowSurfaceLight
import com.example.ui.theme.GlowWhite
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.StoreViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GlowisticStoreApp()
            }
        }
    }
}

@Composable
fun GlowisticStoreApp(
    storeViewModel: StoreViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val products by storeViewModel.products.collectAsStateWithLifecycle()
    val filteredProducts by storeViewModel.filteredProducts.collectAsStateWithLifecycle()
    val categories by storeViewModel.categories.collectAsStateWithLifecycle()
    val storeSettings by storeViewModel.storeSettings.collectAsStateWithLifecycle()
    val isLoading by storeViewModel.isLoading.collectAsStateWithLifecycle()

    val activeSection by storeViewModel.activeSection.collectAsStateWithLifecycle()
    val searchQuery by storeViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by storeViewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedSubCategory by storeViewModel.selectedSubCategory.collectAsStateWithLifecycle()
    val sortOption by storeViewModel.sortOption.collectAsStateWithLifecycle()

    val selectedProductForModal by storeViewModel.selectedProductForModal.collectAsStateWithLifecycle()
    val isCartOpen by storeViewModel.isCartOpen.collectAsStateWithLifecycle()
    val isCheckoutOpen by storeViewModel.isCheckoutOpen.collectAsStateWithLifecycle()
    val cartItems by storeViewModel.cartItems.collectAsStateWithLifecycle()

    val currentAuthUid by storeViewModel.currentAuthUid.collectAsStateWithLifecycle()
    val adminStatusMessage by storeViewModel.adminStatusMessage.collectAsStateWithLifecycle()
    val isAdminBusy by storeViewModel.isAdminBusy.collectAsStateWithLifecycle()

    // Handle Back navigation for sub-states
    BackHandler(enabled = isCartOpen || activeSection != NavSection.HOME) {
        when {
            isCartOpen -> storeViewModel.setCartOpen(false)
            activeSection != NavSection.HOME -> storeViewModel.setActiveSection(NavSection.HOME)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                val bgBrush = Brush.verticalGradient(
                    colors = listOf(
                        GlowBgPearl,
                        GlowBgLight,
                        GlowBgChampagne,
                        GlowBgRoseMist,
                        GlowBgPearl
                    )
                )
                val c1Center = Offset(w * 0.15f, h * 0.28f)
                val c1Radius = (w * 0.65f).coerceAtLeast(1f)
                val c1Brush = Brush.radialGradient(
                    colors = listOf(GlowPrimary.copy(alpha = 0.08f), Color.Transparent),
                    center = c1Center,
                    radius = c1Radius
                )
                val c2Center = Offset(w * 0.88f, h * 0.45f)
                val c2Radius = (w * 0.6f).coerceAtLeast(1f)
                val c2Brush = Brush.radialGradient(
                    colors = listOf(GlowGoldAccent.copy(alpha = 0.14f), Color.Transparent),
                    center = c2Center,
                    radius = c2Radius
                )
                val c3Center = Offset(w * 0.25f, h * 0.78f)
                val c3Radius = (w * 0.7f).coerceAtLeast(1f)
                val c3Brush = Brush.radialGradient(
                    colors = listOf(GlowSecondary.copy(alpha = 0.07f), Color.Transparent),
                    center = c3Center,
                    radius = c3Radius
                )
                onDrawBehind {
                    drawRect(brush = bgBrush)
                    drawCircle(brush = c1Brush, center = c1Center, radius = c1Radius)
                    drawCircle(brush = c2Brush, center = c2Center, radius = c2Radius)
                    drawCircle(brush = c3Brush, center = c3Center, radius = c3Radius)
                }
            }
    ) {
        // Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (activeSection == NavSection.ADMIN) {
                // Spacer for fixed glass header
                Spacer(modifier = Modifier.height(88.dp))

                AdminPanelScreen(
                    currentUid = currentAuthUid,
                    isBusy = isAdminBusy,
                    statusMessage = adminStatusMessage,
                    products = products,
                    categories = categories,
                    storeSettings = storeSettings,
                    onSignIn = { email, pass -> storeViewModel.signInAdmin(email, pass) },
                    onSignOut = { storeViewModel.signOutAdmin() },
                    onResetPassword = { email -> storeViewModel.sendAdminPasswordResetEmail(email) },
                    onUpdatePassword = { currentPass, newPass, onDone ->
                        storeViewModel.updateAdminPassword(currentPass, newPass, onDone)
                    },
                    onSaveProduct = { prod, onDone -> storeViewModel.saveProduct(prod, onDone) },
                    onToggleProductFlag = { id, field, value ->
                        storeViewModel.toggleProductFlag(id, field, value)
                    },
                    onDeleteProduct = { id -> storeViewModel.deleteProduct(id) },
                    onSaveCategory = { cat, onDone -> storeViewModel.saveCategory(cat, onDone) },
                    onDeleteCategory = { id -> storeViewModel.deleteCategory(id) },
                    onSaveHeroSlots = { slots -> storeViewModel.saveHeroSlots(slots) },
                    onSaveStoreSettings = { settings -> storeViewModel.saveStoreSettings(settings) },
                    onDismissStatus = { storeViewModel.clearAdminStatusMessage() },
                    onBackToStore = { storeViewModel.setActiveSection(NavSection.HOME) }
                )
            } else {
                // 1. Interactive Hero Section (shown in HOME view)
                if (activeSection == NavSection.HOME) {
                    HeroSection(
                        storeSettings = storeSettings,
                        products = products,
                        onExploreShop = {
                            storeViewModel.setActiveSection(NavSection.SHOP)
                            coroutineScope.launch { scrollState.animateScrollTo(0) }
                        },
                        onExploreCategories = {
                            storeViewModel.setActiveSection(NavSection.CATEGORIES)
                            coroutineScope.launch { scrollState.animateScrollTo(0) }
                        },
                        onProductClick = { product ->
                            storeViewModel.openProductDetails(product)
                        }
                    )
                } else {
                    // Spacer for fixed glass header when in SHOP or CATEGORIES view
                    Spacer(modifier = Modifier.height(92.dp))
                }

                // Loading indicator while initial Firestore snapshot connects
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GlowPrimary)
                    }
                }

                // 1B. Official Brand Logo & Store Promotional Banner Showcase
                StorePromoBannerSection(
                    storeSettings = storeSettings,
                    onShopNowClick = {
                        storeViewModel.setActiveSection(NavSection.SHOP)
                        coroutineScope.launch { scrollState.animateScrollTo(0) }
                    }
                )

                // 2. Categories Section (shown on HOME and CATEGORIES)
                if (activeSection == NavSection.HOME || activeSection == NavSection.CATEGORIES) {
                    CategoriesSection(
                        categories = categories,
                        allProducts = products,
                        selectedCategory = selectedCategory,
                        selectedSubCategory = selectedSubCategory,
                        onSelectCategory = { catName ->
                            storeViewModel.setSelectedCategory(catName)
                        },
                        onSelectSubCategory = { subName ->
                            storeViewModel.setSelectedSubCategory(subName)
                            if (activeSection == NavSection.CATEGORIES && subName != null) {
                                storeViewModel.setActiveSection(NavSection.SHOP)
                            }
                        }
                    )
                }

                // 3. Main Shop & Customer Product Gallery Section (shown on HOME, SHOP, and CATEGORIES)
                MainShopSection(
                    products = filteredProducts,
                    allProducts = products,
                    categories = categories,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    selectedSubCategory = selectedSubCategory,
                    sortOption = sortOption,
                    onSearchChange = { storeViewModel.setSearchQuery(it) },
                    onSelectCategory = { storeViewModel.setSelectedCategory(it) },
                    onSelectSubCategory = { storeViewModel.setSelectedSubCategory(it) },
                    onSortChange = { storeViewModel.setSortOption(it) },
                    onProductDetailsClick = { product ->
                        storeViewModel.openProductDetails(product)
                    },
                    onProductOrderClick = { product ->
                        storeViewModel.launchSingleProductWhatsAppOrder(context, product, 1)
                    },
                    onAddToCartClick = { product ->
                        storeViewModel.addToCart(product, 1)
                        storeViewModel.setCartOpen(true)
                    }
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // 4. Store Footer
            StoreFooter(
                storeSettings = storeSettings,
                onNavigate = { section ->
                    storeViewModel.setActiveSection(section)
                    coroutineScope.launch { scrollState.animateScrollTo(0) }
                }
            )
        }

        // Fixed Top Glass Navbar
        GlassHeader(
            storeSettings = storeSettings,
            activeSection = activeSection,
            cartCount = cartItems.sumOf { it.quantity },
            onSelectSection = { section ->
                storeViewModel.setActiveSection(section)
                coroutineScope.launch { scrollState.animateScrollTo(0) }
            },
            onOpenCart = { storeViewModel.setCartOpen(true) },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Slide-Over Right Cart Drawer
        SlideOverCartDrawer(
            visible = isCartOpen,
            cartItems = cartItems,
            deliveryCharge = storeSettings.deliveryCharge,
            onClose = { storeViewModel.setCartOpen(false) },
            onUpdateQuantity = { prodId, qty ->
                storeViewModel.updateCartQuantity(prodId, qty)
            },
            onRemoveItem = { prodId ->
                storeViewModel.removeFromCart(prodId)
            },
            onClearCart = { storeViewModel.clearCart() },
            onProceedToCheckout = {
                storeViewModel.setCartOpen(false)
                storeViewModel.setCheckoutOpen(true)
            }
        )

        // Product Details Modal
        if (selectedProductForModal != null) {
            ProductDetailsModal(
                product = selectedProductForModal!!,
                onDismiss = { storeViewModel.closeProductDetails() },
                onAddToCart = { product, qty ->
                    storeViewModel.addToCart(product, qty)
                    storeViewModel.setCartOpen(true)
                },
                onWhatsAppOrder = { product, qty ->
                    storeViewModel.launchSingleProductWhatsAppOrder(context, product, qty)
                }
            )
        }

        // Checkout Modal
        if (isCheckoutOpen) {
            CheckoutModal(
                cartItems = cartItems,
                deliveryCharge = storeSettings.deliveryCharge,
                onDismiss = { storeViewModel.setCheckoutOpen(false) },
                onConfirmWhatsAppOrder = { name, phone, address, note ->
                    storeViewModel.launchCheckoutWhatsAppOrder(
                        context = context,
                        customerName = name,
                        phone = phone,
                        address = address,
                        note = note
                    )
                }
            )
        }
    }
}

@Composable
private fun StoreFooter(
    storeSettings: StoreSettings,
    onNavigate: (NavSection) -> Unit
) {
    Surface(
        color = GlowDark,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("store_footer")
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 1140.dp)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (storeSettings.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = storeSettings.logoUrl,
                        contentDescription = "${storeSettings.websiteName} logo",
                        placeholder = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                        error = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, GlowGoldAccent, CircleShape)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                        contentDescription = "Glowisticbd logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, GlowGoldAccent, CircleShape)
                    )
                }
                Text(
                    text = storeSettings.websiteName.ifBlank { "Glowisticbd" },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = GlowWhite,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = storeSettings.tagline.ifBlank {
                    "আপনার সীমাহীন গ্লো। বাংলাদেশের প্রিমিয়াম কসমেটিকস ও স্কিনকেয়ার কালেকশন।"
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = GlowSurfaceLight),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "WhatsApp Number",
                    tint = GlowPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "WhatsApp: ${storeSettings.whatsappNumber.ifBlank { "+8801779531040" }}",
                    style = MaterialTheme.typography.labelLarge.copy(color = GlowWhite)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    NavSection.HOME to "Home",
                    NavSection.SHOP to "Shop",
                    NavSection.CATEGORIES to "Categories",
                    NavSection.ADMIN to "Admin (/admin)"
                ).forEach { (section, label) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = GlowSurfaceLight,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNavigate(section) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = GlowDarkPlum)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "© ${storeSettings.websiteName.ifBlank { "Glowisticbd" }} — Powered by Real-Time Firebase Firestore",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = GlowSurfaceLight.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
