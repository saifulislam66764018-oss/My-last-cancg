package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.Category
import com.example.model.DEFAULT_MAIN_CATEGORIES
import com.example.model.HeroProductIds
import com.example.model.Product
import com.example.model.SecurityUtils
import com.example.model.StoreSettings
import com.example.model.formatBdt
import com.example.ui.theme.GlowBgLight
import com.example.ui.theme.GlowBorderLight
import com.example.ui.theme.GlowDanger
import com.example.ui.theme.GlowDark
import com.example.ui.theme.GlowDarkPlum
import com.example.ui.theme.GlowPrimary
import com.example.ui.theme.GlowSecondary
import com.example.ui.theme.GlowSuccess
import com.example.ui.theme.GlowSurfaceLight
import com.example.ui.theme.GlowWhite
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Safely reads, downscales, and compresses an image picked from the Android Photo Picker
 * into a compact Base64 JPEG data URL suitable for Firestore documents.
 */
private suspend fun compressUriToDataUrl(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null
            // Reject raw files over 12MB
            if (bytes.isEmpty() || bytes.size > 12 * 1024 * 1024) return@withContext null

            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext null
            val maxDim = 800f
            val largest = max(original.width, original.height).toFloat()
            val scale = if (largest > maxDim) maxDim / largest else 1f
            val targetW = (original.width * scale).roundToInt().coerceAtLeast(1)
            val targetH = (original.height * scale).roundToInt().coerceAtLeast(1)

            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else {
                original
            }

            val out = ByteArrayOutputStream()
            var quality = 78
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            while (out.size() > 380_000 && quality > 35) {
                out.reset()
                quality -= 12
                resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (_: Exception) {
            null
        }
    }

@Composable
fun AdminPanelScreen(
    currentUid: String?,
    isBusy: Boolean,
    statusMessage: String?,
    products: List<Product>,
    categories: List<Category>,
    storeSettings: StoreSettings,
    onSignIn: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onResetPassword: (String) -> Unit = {},
    onUpdatePassword: (String, String, () -> Unit) -> Unit = { _, _, _ -> },
    onSaveProduct: (Product, () -> Unit) -> Unit,
    onToggleProductFlag: (String, String, Boolean) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onSaveCategory: (Category, () -> Unit) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveHeroSlots: (HeroProductIds) -> Unit,
    onSaveStoreSettings: (StoreSettings) -> Unit,
    onDismissStatus: () -> Unit,
    onBackToStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToStore()
    }

    val isAuthorized = SecurityUtils.isAuthorizedAdmin(currentUid)

    Column(
        modifier = modifier
            .widthIn(max = 1140.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .testTag("admin_panel_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Admin Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBackToStore,
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("admin_back_to_store_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Store",
                        tint = GlowDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Storefront", color = GlowDark, fontWeight = FontWeight.SemiBold)
                }

                Text(
                    text = "Admin Control Panel (/admin)",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = GlowDark,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (isAuthorized) {
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.testTag("admin_sign_out_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign Out",
                        tint = GlowDanger,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign Out", color = GlowDanger, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Status Banner
        if (!statusMessage.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = GlowSurfaceLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlowPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = GlowDark,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onDismissStatus,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Dismiss", fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (!isAuthorized) {
            // Firebase Email/Password Login Form for Admin
            AdminLoginCard(
                isBusy = isBusy,
                onSignIn = onSignIn,
                onResetPassword = onResetPassword
            )
        } else {
            // Authenticated Admin Tabs
            var selectedTab by remember { mutableIntStateOf(0) }
            val tabs = listOf(
                "Products (${products.size})",
                "Categories (${categories.size})",
                "Hero Product Slots",
                "Website Settings",
                "Security & Rules"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(title, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(999.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GlowPrimary,
                            selectedLabelColor = GlowWhite
                        ),
                        modifier = Modifier.testTag("admin_tab_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> AdminProductsSection(
                    products = products,
                    categories = categories,
                    isBusy = isBusy,
                    onSaveProduct = onSaveProduct,
                    onToggleFlag = onToggleProductFlag,
                    onDeleteProduct = onDeleteProduct
                )
                1 -> AdminCategoriesSection(
                    categories = categories,
                    isBusy = isBusy,
                    onSaveCategory = onSaveCategory,
                    onDeleteCategory = onDeleteCategory
                )
                2 -> AdminHeroSlotsSection(
                    products = products,
                    currentSlots = storeSettings.heroProductIds,
                    isBusy = isBusy,
                    onSaveHeroSlots = onSaveHeroSlots
                )
                3 -> AdminWebsiteSettingsSection(
                    storeSettings = storeSettings,
                    isBusy = isBusy,
                    onSaveSettings = onSaveStoreSettings
                )
                4 -> AdminFirestoreRulesSection(
                    isBusy = isBusy,
                    onUpdatePassword = onUpdatePassword
                )
            }
        }
    }
}

@Composable
private fun AdminLoginCard(
    isBusy: Boolean,
    onSignIn: (String, String) -> Unit,
    onResetPassword: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = GlowWhite,
        modifier = Modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .border(1.dp, GlowBorderLight, RoundedCornerShape(24.dp))
            .padding(24.dp)
            .testTag("admin_login_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(GlowSurfaceLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Admin Authentication",
                    tint = GlowPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Admin Authentication",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = GlowDark,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Firebase Email/Password Login (Restricted to Admin UID)",
                style = MaterialTheme.typography.bodyMedium.copy(color = GlowDarkPlum)
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Admin Email") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_email_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_password_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onSignIn(email, password) },
                enabled = !isBusy && email.isNotBlank() && password.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlowPrimary,
                    contentColor = GlowWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("admin_login_submit_btn")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        color = GlowWhite,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Sign In to Admin Panel", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { onResetPassword(email) },
                enabled = !isBusy && email.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_reset_password_btn")
            ) {
                Text(
                    text = "Forgot Password? Send Reset Email",
                    color = GlowPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AdminProductsSection(
    products: List<Product>,
    categories: List<Category>,
    isBusy: Boolean,
    onSaveProduct: (Product, () -> Unit) -> Unit,
    onToggleFlag: (String, String, Boolean) -> Unit,
    onDeleteProduct: (String) -> Unit
) {
    var editingProduct by remember { mutableStateOf<Product?>(Product()) }
    var isFormVisible by remember { mutableStateOf(true) }
    var productPendingDelete by remember { mutableStateOf<Product?>(null) }

    val availableCategories = remember(categories) {
        val merged = linkedMapOf<String, Category>()
        DEFAULT_MAIN_CATEGORIES.forEach { merged[it.name] = it }
        categories.forEach { cat ->
            val existing = merged[cat.name]
            if (existing != null) {
                val combinedSubs = (existing.subcategories + cat.subcategories).distinct()
                merged[cat.name] = cat.copy(subcategories = combinedSubs)
            } else {
                merged[cat.name] = cat
            }
        }
        merged.values.toList()
    }

    if (productPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { productPendingDelete = null },
            title = { Text("Confirm Product Deletion", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to permanently delete '${productPendingDelete?.title}' from Firestore?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = productPendingDelete?.id ?: ""
                        productPendingDelete = null
                        if (id.isNotBlank()) onDeleteProduct(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowDanger)
                ) {
                    Text("Delete", color = GlowWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { productPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Manage Products",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = GlowDark,
                    fontWeight = FontWeight.Bold
                )
            )
            Button(
                onClick = {
                    editingProduct = Product()
                    isFormVisible = true
                },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlowPrimary,
                    contentColor = GlowWhite
                ),
                modifier = Modifier.testTag("admin_add_product_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Product", fontWeight = FontWeight.Bold)
            }
        }

        if (isFormVisible && editingProduct != null) {
            ProductEditorCard(
                initial = editingProduct!!,
                categories = availableCategories,
                isBusy = isBusy,
                onCancel = {
                    isFormVisible = false
                    editingProduct = null
                },
                onSave = { updated ->
                    onSaveProduct(updated) {
                        editingProduct = Product()
                        isFormVisible = false
                    }
                }
            )
        }

        if (products.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = GlowWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlowBorderLight, RoundedCornerShape(18.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "কোনো পণ্য পাওয়া যায়নি — নিচের 'Add New Product' ফর্মটি পূরণ করে আপনার প্রথম প্রোডাক্ট যোগ করুন।",
                    style = MaterialTheme.typography.bodyLarge.copy(color = GlowDarkPlum)
                )
            }
        } else {
            products.forEach { product ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = GlowWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlowBorderLight, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = product.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GlowSurfaceLight)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = GlowDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                val catLabel = buildString {
                                    append(product.category.ifBlank { "Uncategorized" })
                                    if (product.subCategory.isNotBlank()) {
                                        append(" • ")
                                        append(product.subCategory)
                                    }
                                }
                                Text(
                                    text = "Category: $catLabel",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = GlowDarkPlum)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = formatBdt(product.price),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = GlowPrimary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                    product.oldPrice?.let { old ->
                                        Text(
                                            text = "Old: ${formatBdt(old)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = GlowDarkPlum)
                                        )
                                    }
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        editingProduct = product
                                        isFormVisible = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit product",
                                        tint = GlowPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { productPendingDelete = product }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete product",
                                        tint = GlowDanger
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = GlowBorderLight.copy(alpha = 0.6f))

                        // Real-time toggles for Available, Featured, New Arrival
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = product.available,
                                    onCheckedChange = { onToggleFlag(product.id, "available", it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = GlowWhite, checkedTrackColor = GlowSuccess)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Available", style = MaterialTheme.typography.labelLarge)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = product.featured,
                                    onCheckedChange = { onToggleFlag(product.id, "featured", it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = GlowWhite, checkedTrackColor = GlowPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Featured", style = MaterialTheme.typography.labelLarge)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = product.newArrival,
                                    onCheckedChange = { onToggleFlag(product.id, "newArrival", it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = GlowWhite, checkedTrackColor = GlowSecondary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Arrival", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductEditorCard(
    initial: Product,
    categories: List<Category>,
    isBusy: Boolean,
    onCancel: () -> Unit,
    onSave: (Product) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember(initial) { mutableStateOf(initial.title) }
    var category by remember(initial) { mutableStateOf(initial.category) }
    var subCategory by remember(initial) { mutableStateOf(initial.subCategory) }
    var priceText by remember(initial) {
        mutableStateOf(if (initial.price > 0.0) initial.price.toString() else "")
    }
    var oldPriceText by remember(initial) {
        mutableStateOf(initial.oldPrice?.toString() ?: "")
    }
    var description by remember(initial) { mutableStateOf(initial.description) }
    var imageUrl by remember(initial) { mutableStateOf(initial.imageUrl) }
    var available by remember(initial) { mutableStateOf(initial.available) }
    var featured by remember(initial) { mutableStateOf(initial.featured) }
    var newArrival by remember(initial) { mutableStateOf(initial.newArrival) }
    var isCompressingImage by remember { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isCompressingImage = true
            localValidationError = null
            coroutineScope.launch {
                val dataUrl = compressUriToDataUrl(context, uri)
                isCompressingImage = false
                if (dataUrl != null) {
                    imageUrl = dataUrl
                } else {
                    localValidationError = "Could not process selected photo. Please try another image or enter an HTTPS URL."
                }
            }
        }
    }

    val selectedCatObj = remember(categories, category) {
        categories.firstOrNull { it.name.equals(category, ignoreCase = true) }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GlowBgLight,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, GlowPrimary, RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("product_editor_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (initial.id.isBlank()) "Add New Product" else "Edit Product",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = GlowDark,
                    fontWeight = FontWeight.Bold
                )
            )

            if (!localValidationError.isNullOrBlank()) {
                Text(
                    text = localValidationError!!,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = GlowDanger,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    localValidationError = null
                },
                label = { Text("Product Title *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_title_input")
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Main Category Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_category_input")
            )

            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category.equals(cat.name, ignoreCase = true),
                            onClick = {
                                category = cat.name
                                if (cat.subcategories.none { it.equals(subCategory, ignoreCase = true) }) {
                                    subCategory = ""
                                }
                            },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = subCategory,
                onValueChange = { subCategory = it },
                label = { Text("Subcategory Name (Optional)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_subcategory_input")
            )

            if (selectedCatObj != null && selectedCatObj.subcategories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectedCatObj.subcategories.forEach { sub ->
                        FilterChip(
                            selected = subCategory.equals(sub, ignoreCase = true),
                            onClick = { subCategory = sub },
                            label = { Text(sub) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        localValidationError = null
                    },
                    label = { Text("Current Price (৳) *") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("product_price_input")
                )
                OutlinedTextField(
                    value = oldPriceText,
                    onValueChange = { oldPriceText = it },
                    label = { Text("Old Price (৳)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("product_old_price_input")
                )
            }

            // Image Upload via Gallery Photo Picker OR External HTTPS URL
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isCompressingImage && !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("product_pick_photo_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Pick Product Image from Gallery",
                        tint = GlowPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCompressingImage) "Compressing..." else "Upload Photo from Gallery",
                        color = GlowDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Selected Product Image Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, GlowPrimary, RoundedCornerShape(10.dp))
                            .background(GlowSurfaceLight)
                    )
                }
            }

            OutlinedTextField(
                value = if (imageUrl.startsWith("data:image/")) "[Gallery Image Attached]" else imageUrl,
                onValueChange = {
                    imageUrl = it
                    localValidationError = null
                },
                label = { Text("Or Paste External HTTPS Image URL *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_image_url_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Product Description") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_description_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = available, onCheckedChange = { available = it })
                    Text("Available")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = featured, onCheckedChange = { featured = it })
                    Text("Featured")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = newArrival, onCheckedChange = { newArrival = it })
                    Text("New Arrival")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        val cleanTitle = SecurityUtils.sanitizeText(title, 300)
                        val parsedPrice = priceText.trim().toDoubleOrNull()
                        val parsedOld = oldPriceText.trim().toDoubleOrNull()?.takeIf { it > 0.0 }
                        val validImage = SecurityUtils.sanitizeImageSource(imageUrl)

                        when {
                            cleanTitle.isBlank() -> {
                                localValidationError = "Product Title is required."
                            }
                            parsedPrice == null || parsedPrice < 0.0 -> {
                                localValidationError = "Please enter a valid Price (৳)."
                            }
                            validImage.isBlank() -> {
                                localValidationError = "Please upload a photo from Gallery or enter a valid https:// Image URL."
                            }
                            else -> {
                                localValidationError = null
                                onSave(
                                    initial.copy(
                                        title = cleanTitle,
                                        category = category.trim(),
                                        subCategory = subCategory.trim(),
                                        price = parsedPrice,
                                        oldPrice = parsedOld,
                                        description = description.trim(),
                                        imageUrl = validImage,
                                        available = available,
                                        featured = featured,
                                        newArrival = newArrival
                                    )
                                )
                            }
                        }
                    },
                    enabled = !isBusy && !isCompressingImage,
                    colors = ButtonDefaults.buttonColors(containerColor = GlowPrimary),
                    modifier = Modifier.testTag("product_save_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Product")
                }
            }
        }
    }
}

@Composable
private fun AdminCategoriesSection(
    categories: List<Category>,
    isBusy: Boolean,
    onSaveCategory: (Category, () -> Unit) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var editingCategory by remember { mutableStateOf(Category()) }
    var subcategoriesInput by remember(editingCategory.id) {
        mutableStateOf(editingCategory.subcategories.joinToString(", "))
    }
    var categoryPendingDelete by remember { mutableStateOf<Category?>(null) }

    if (categoryPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryPendingDelete = null },
            title = { Text("Confirm Category Deletion", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete category '${categoryPendingDelete?.name}'?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = categoryPendingDelete?.id ?: ""
                        categoryPendingDelete = null
                        if (id.isNotBlank()) onDeleteCategory(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowDanger)
                ) {
                    Text("Delete", color = GlowWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { categoryPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = GlowWhite,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlowBorderLight, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (editingCategory.id.isBlank()) "Add Category" else "Edit Category",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = GlowDark,
                        fontWeight = FontWeight.Bold
                    )
                )

                OutlinedTextField(
                    value = editingCategory.name,
                    onValueChange = { editingCategory = editingCategory.copy(name = it) },
                    label = { Text("Category Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editingCategory.imageUrl,
                    onValueChange = { editingCategory = editingCategory.copy(imageUrl = it) },
                    label = { Text("HTTPS Image URL (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subcategoriesInput,
                    onValueChange = { subcategoriesInput = it },
                    label = { Text("Subcategories (comma-separated, Optional)") },
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val parsedSubs = subcategoriesInput
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .distinct()
                            onSaveCategory(editingCategory.copy(subcategories = parsedSubs)) {
                                editingCategory = Category()
                                subcategoriesInput = ""
                            }
                        },
                        enabled = !isBusy && editingCategory.name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = GlowPrimary)
                    ) {
                        Text(if (editingCategory.id.isBlank()) "Add Category" else "Update Category")
                    }

                    if (editingCategory.id.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                editingCategory = Category()
                                subcategoriesInput = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        categories.forEach { cat ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GlowWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlowBorderLight, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (cat.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = cat.imageUrl,
                                contentDescription = cat.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GlowSurfaceLight)
                            )
                        }
                        Column {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = GlowDark,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (cat.subcategories.isNotEmpty()) {
                                Text(
                                    text = "Subcategories (${cat.subcategories.size}): ${cat.subcategories.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = GlowDarkPlum,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                editingCategory = cat
                                subcategoriesInput = cat.subcategories.joinToString(", ")
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit category", tint = GlowPrimary)
                        }
                        IconButton(onClick = { categoryPendingDelete = cat }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete category", tint = GlowDanger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminHeroSlotsSection(
    products: List<Product>,
    currentSlots: HeroProductIds,
    isBusy: Boolean,
    onSaveHeroSlots: (HeroProductIds) -> Unit
) {
    var left1 by remember(currentSlots) { mutableStateOf(currentSlots.left1) }
    var left2 by remember(currentSlots) { mutableStateOf(currentSlots.left2) }
    var right1 by remember(currentSlots) { mutableStateOf(currentSlots.right1) }
    var right2 by remember(currentSlots) { mutableStateOf(currentSlots.right2) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GlowWhite,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlowBorderLight, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Hero Product Slots (Real-Time Floating Cards)",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = GlowDark,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Select products from Firestore for the four floating Hero slots. Empty slots are hidden gracefully on the storefront.",
                style = MaterialTheme.typography.bodyMedium.copy(color = GlowDarkPlum)
            )

            HeroSlotSelectorRow(
                slotLabel = "Left Top (left1)",
                selectedProductId = left1,
                products = products,
                onSelect = { left1 = it }
            )
            HeroSlotSelectorRow(
                slotLabel = "Left Bottom (left2)",
                selectedProductId = left2,
                products = products,
                onSelect = { left2 = it }
            )
            HeroSlotSelectorRow(
                slotLabel = "Right Top (right1)",
                selectedProductId = right1,
                products = products,
                onSelect = { right1 = it }
            )
            HeroSlotSelectorRow(
                slotLabel = "Right Bottom (right2)",
                selectedProductId = right2,
                products = products,
                onSelect = { right2 = it }
            )

            Button(
                onClick = {
                    onSaveHeroSlots(
                        HeroProductIds(
                            left1 = left1,
                            left2 = left2,
                            right1 = right1,
                            right2 = right2
                        )
                    )
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = GlowPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Hero Product Slots", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HeroSlotSelectorRow(
    slotLabel: String,
    selectedProductId: String,
    products: List<Product>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedProduct = products.firstOrNull { it.id == selectedProductId }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlowBgLight)
            .border(1.dp, GlowBorderLight, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slotLabel,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = GlowPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = selectedProduct?.let { "${it.title} (${formatBdt(it.price)})" } ?: "Empty Slot (Hidden)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = GlowDark,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text("Select Product")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(GlowWhite)
            ) {
                DropdownMenuItem(
                    text = { Text("— Empty Slot (Hide Card) —", color = GlowDanger) },
                    onClick = {
                        onSelect("")
                        expanded = false
                    }
                )
                products.forEach { prod ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${prod.title} (${formatBdt(prod.price)})",
                                fontWeight = if (prod.id == selectedProductId) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSelect(prod.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminWebsiteSettingsSection(
    storeSettings: StoreSettings,
    isBusy: Boolean,
    onSaveSettings: (StoreSettings) -> Unit
) {
    var websiteName by remember(storeSettings) { mutableStateOf(storeSettings.websiteName) }
    var description by remember(storeSettings) { mutableStateOf(storeSettings.description) }
    var tagline by remember(storeSettings) { mutableStateOf(storeSettings.tagline) }
    var whatsappNumber by remember(storeSettings) { mutableStateOf(storeSettings.whatsappNumber) }
    var deliveryChargeText by remember(storeSettings) { mutableStateOf(storeSettings.deliveryCharge.toString()) }
    var logoUrl by remember(storeSettings) { mutableStateOf(storeSettings.logoUrl) }
    var faviconUrl by remember(storeSettings) { mutableStateOf(storeSettings.faviconUrl) }
    var heroVideoUrl by remember(storeSettings) { mutableStateOf(storeSettings.heroVideoUrl) }
    var heroMediaType by remember(storeSettings) { mutableStateOf(storeSettings.heroMediaType) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GlowWhite,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlowBorderLight, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Website & Store Settings (settings/store)",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = GlowDark,
                    fontWeight = FontWeight.Bold
                )
            )

            // Live Brand Logo & Banner Preview Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GlowBgLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlowBorderLight, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (logoUrl.isNotBlank()) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = "Active Logo Preview",
                            placeholder = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                            error = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, GlowPrimary, CircleShape)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                            contentDescription = "Default Glowisticbd Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, GlowPrimary, CircleShape)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, GlowBorderLight, RoundedCornerShape(12.dp))
                    ) {
                        if (heroMediaType == "image" && heroVideoUrl.isNotBlank()) {
                            AsyncImage(
                                model = heroVideoUrl,
                                contentDescription = "Active Banner Preview",
                                placeholder = painterResource(id = R.drawable.img_glowistic_store_banner_1791302798689),
                                error = painterResource(id = R.drawable.img_glowistic_store_banner_1791302798689),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_glowistic_store_banner_1791302798689),
                                contentDescription = "Default Glowisticbd Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = websiteName,
                onValueChange = { websiteName = it },
                label = { Text("Website Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tagline,
                onValueChange = { tagline = it },
                label = { Text("Bengali Tagline / Short Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = whatsappNumber,
                    onValueChange = { whatsappNumber = it },
                    label = { Text("WhatsApp Number") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = deliveryChargeText,
                    onValueChange = { deliveryChargeText = it },
                    label = { Text("Delivery Charge (৳)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = logoUrl,
                onValueChange = { logoUrl = it },
                label = { Text("Logo HTTPS URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = faviconUrl,
                onValueChange = { faviconUrl = it },
                label = { Text("Favicon HTTPS URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = heroVideoUrl,
                onValueChange = { heroVideoUrl = it },
                label = { Text("Hero Video / Image HTTPS URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Hero Media Type:", fontWeight = FontWeight.Bold, color = GlowDark)
                FilterChip(
                    selected = heroMediaType == "video",
                    onClick = { heroMediaType = "video" },
                    label = { Text("video") }
                )
                FilterChip(
                    selected = heroMediaType == "image",
                    onClick = { heroMediaType = "image" },
                    label = { Text("image") }
                )
            }

            Button(
                onClick = {
                    onSaveSettings(
                        storeSettings.copy(
                            websiteName = websiteName,
                            description = description,
                            tagline = tagline,
                            whatsappNumber = whatsappNumber,
                            deliveryCharge = deliveryChargeText.toDoubleOrNull() ?: 0.0,
                            logoUrl = logoUrl,
                            faviconUrl = faviconUrl,
                            heroVideoUrl = heroVideoUrl,
                            heroMediaType = heroMediaType
                        )
                    )
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = GlowPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Website Settings", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AdminFirestoreRulesSection(
    isBusy: Boolean = false,
    onUpdatePassword: (String, String, () -> Unit) -> Unit = { _, _, _ -> }
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    val rulesText = """
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isAdmin() {
      return request.auth != null
        && request.auth.token.firebase.sign_in_provider == 'password'
        && (
          request.auth.uid == "jvpnDzUBPqfEfGiGYBoxQLgPBFo2" ||
          request.auth.uid == "buYO5SjqICa5yuYz5P648cpb4mr1"
        );
    }

    function isValidString(val, minLen, maxLen) {
      return val is string && val.size() >= minLen && val.size() <= maxLen
        && !val.matches('.*<script.*') && !val.matches('.*javascript:.*');
    }

    function isValidImageSource(url) {
      return url is string && url.size() >= 10 && url.size() <= 750000
        && (
          url.matches('^https://[^\\s<>"\']+$') ||
          url.matches('^data:image/(jpeg|jpg|png|webp);base64,[A-Za-z0-9+/=]+$')
        );
    }

    function isValidPrice(val) {
      return val is number && val >= 0 && val <= 10000000;
    }

    match /products/{productId} {
      allow read: if true;
      allow create, update: if isAdmin()
        && request.resource.data.keys().hasOnly([
          'title', 'imageUrl', 'price', 'oldPrice', 'category', 'subCategory',
          'description', 'available', 'featured', 'newArrival', 'createdAt'
        ])
        && isValidString(request.resource.data.title, 1, 300)
        && isValidImageSource(request.resource.data.imageUrl)
        && isValidPrice(request.resource.data.price)
        && request.resource.data.available is bool
        && request.resource.data.featured is bool
        && request.resource.data.createdAt is number;
      allow delete: if isAdmin();
    }

    match /categories/{categoryId} {
      allow read: if true;
      allow create, update: if isAdmin()
        && request.resource.data.keys().hasOnly(['name', 'imageUrl', 'subcategories', 'createdAt'])
        && isValidString(request.resource.data.name, 1, 120)
        && request.resource.data.createdAt is number;
      allow delete: if isAdmin();
    }

    match /settings/store {
      allow read: if true;
      allow create, update: if isAdmin()
        && request.resource.data.keys().hasOnly([
          'websiteName', 'description', 'tagline', 'whatsappNumber',
          'deliveryCharge', 'logoUrl', 'faviconUrl', 'heroVideoUrl',
          'heroMediaType', 'heroProductIds'
        ]);
      allow delete: if false;
    }

    match /{document=**} {
      allow read, write: if false;
    }
  }
}
    """.trimIndent()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Secure Client-Side Admin Password Update Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = GlowWhite,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlowBorderLight, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .testTag("admin_change_password_card")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = GlowPrimary)
                    Text(
                        text = "Change Admin Password",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = GlowDark,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Text(
                    text = "Securely update your Firebase Admin password using re-authenticated client credentials (never embed firebase-admin service account keys in client code).",
                    style = MaterialTheme.typography.bodyMedium.copy(color = GlowDarkPlum)
                )

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_current_password_input")
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Secure Password (min 8 chars)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_new_password_input")
                )

                Button(
                    onClick = {
                        onUpdatePassword(currentPassword, newPassword) {
                            currentPassword = ""
                            newPassword = ""
                        }
                    },
                    enabled = !isBusy && currentPassword.isNotBlank() && newPassword.length >= 8,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlowPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_update_password_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update Admin Password", fontWeight = FontWeight.Bold)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = GlowWhite,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlowBorderLight, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = GlowPrimary)
                    Text(
                        text = "Production Firestore Security Rules",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = GlowDark,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Text(
                    text = "Public visitors can read products, categories, and store settings. Only the authenticated Admin UID can create, update, or delete.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = GlowDarkPlum)
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = GlowDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = rulesText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = GlowBgLight,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
