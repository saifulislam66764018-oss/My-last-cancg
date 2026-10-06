package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.Category
import com.example.model.Product
import com.example.model.SortOption
import com.example.model.formatBdt
import com.example.ui.theme.GlowBgLight
import com.example.ui.theme.GlowBorderLight
import com.example.ui.theme.GlowDark
import com.example.ui.theme.GlowDarkPlum
import com.example.ui.theme.GlowPrimary
import com.example.ui.theme.GlowSecondary
import com.example.ui.theme.GlowSurfaceLight
import com.example.ui.theme.GlowWhite

enum class GalleryViewMode {
    GRID,
    SHOWCASE
}

/**
 * Helper to count how many products belong to a given main category
 * (including products matched via any of its subcategories).
 */
fun countProductsForCategory(category: Category, allProducts: List<Product>): Int {
    val validSubs = category.subcategories.map { it.lowercase() }.toSet()
    return allProducts.count { product ->
        product.category.equals(category.name, ignoreCase = true) ||
            (validSubs.isNotEmpty() && (
                product.category.lowercase() in validSubs ||
                    product.subCategory.lowercase() in validSubs
                ))
    }
}

/**
 * Helper to count how many products belong to a specific sub-category.
 */
fun countProductsForSubCategory(subCategoryName: String, allProducts: List<Product>): Int {
    return allProducts.count { product ->
        product.subCategory.equals(subCategoryName, ignoreCase = true) ||
            product.category.equals(subCategoryName, ignoreCase = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesSection(
    categories: List<Category>,
    allProducts: List<Product> = emptyList(),
    selectedCategory: String?,
    selectedSubCategory: String? = null,
    onSelectCategory: (String?) -> Unit,
    onSelectSubCategory: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoriesWithSubsToShow = remember(categories, selectedCategory) {
        val selected = categories.firstOrNull { it.name.equals(selectedCategory, ignoreCase = true) }
        if (selected != null) {
            if (selected.subcategories.isNotEmpty()) listOf(selected) else emptyList()
        } else {
            categories.filter { it.subcategories.isNotEmpty() }
        }
    }

    Column(
        modifier = modifier
            .widthIn(max = 1140.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .testTag("categories_section"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Shop by Category",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = GlowDark,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "মেইন ক্যাটাগরি এবং সাব-ক্যাটাগরি থেকে পছন্দের পণ্য বেছে নিন",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = GlowDarkPlum
                    )
                )
            }

            if (selectedCategory != null || selectedSubCategory != null) {
                OutlinedButton(
                    onClick = {
                        onSelectCategory(null)
                        onSelectSubCategory(null)
                    },
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("clear_category_filter_btn")
                ) {
                    Text(
                        text = "Reset Filter",
                        style = MaterialTheme.typography.labelLarge.copy(color = GlowPrimary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (categories.isEmpty()) {
            // Clean empty state when Firestore has no categories
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = GlowWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlowBorderLight, RoundedCornerShape(20.dp))
                    .padding(24.dp)
                    .testTag("empty_categories_state")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GlowSurfaceLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = GlowPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.empty_categories),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = GlowDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "ক্যাটাগরি অ্যাডমিন প্যানেল থেকে যুক্ত করলে এখানে প্রদর্শিত হবে।",
                        style = MaterialTheme.typography.bodyMedium.copy(color = GlowDarkPlum),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory.equals(category.name, ignoreCase = true)
                    val productCount = remember(category, allProducts) {
                        countProductsForCategory(category, allProducts)
                    }
                    CategoryCardItem(
                        category = category,
                        productCount = productCount,
                        isSelected = isSelected,
                        onClick = { onSelectCategory(category.name) }
                    )
                }
            }

            // Subcategories Showcase Box(es)
            categoriesWithSubsToShow.forEach { catObj ->
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = GlowWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlowBorderLight, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                        .testTag("subcategories_showcase_box_${catObj.id}")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${catObj.name} — সাব-ক্যাটাগরি সমূহ",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = GlowDark,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "${catObj.subcategories.size}টি সাব-ক্যাটাগরি",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GlowPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            catObj.subcategories.forEach { subName ->
                                val isSubSelected = selectedSubCategory.equals(subName, ignoreCase = true)
                                val subCount = remember(subName, allProducts) {
                                    countProductsForSubCategory(subName, allProducts)
                                }
                                FilterChip(
                                    selected = isSubSelected,
                                    onClick = {
                                        if (!selectedCategory.equals(catObj.name, ignoreCase = true)) {
                                            onSelectCategory(catObj.name)
                                        }
                                        onSelectSubCategory(subName)
                                    },
                                    label = {
                                        Text(
                                            text = if (subCount > 0) "$subName ($subCount)" else subName,
                                            fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GlowPrimary,
                                        selectedLabelColor = GlowWhite,
                                        containerColor = GlowBgLight,
                                        labelColor = GlowDark
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCardItem(
    category: Category,
    productCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else if (isSelected) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "category_card_scale"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) GlowSurfaceLight else GlowWhite,
        modifier = Modifier
            .width(154.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) GlowPrimary else GlowBorderLight,
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("category_card_${category.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(GlowSurfaceLight, GlowBgLight)
                        )
                    )
                    .border(1.dp, GlowBorderLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (category.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = category.imageUrl,
                        contentDescription = category.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = category.name,
                        tint = GlowPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (isSelected) GlowPrimary else GlowDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            val metaBadgeText = when {
                productCount > 0 && category.subcategories.isNotEmpty() ->
                    "$productCount পণ্য • ${category.subcategories.size} সাব"
                productCount > 0 ->
                    "${productCount}টি পণ্য"
                category.subcategories.isNotEmpty() ->
                    "${category.subcategories.size}টি সাব-ক্যাটাগরি"
                else ->
                    "সব দেখুন"
            }

            Text(
                text = metaBadgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = GlowPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainShopSection(
    products: List<Product>,
    allProducts: List<Product> = products,
    categories: List<Category>,
    searchQuery: String,
    selectedCategory: String?,
    selectedSubCategory: String? = null,
    sortOption: SortOption,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectSubCategory: (String?) -> Unit = {},
    onSortChange: (SortOption) -> Unit,
    onProductDetailsClick: (Product) -> Unit,
    onProductOrderClick: (Product) -> Unit,
    onAddToCartClick: (Product) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var sortDropdownExpanded by remember { mutableStateOf(false) }
    var galleryViewMode by remember { mutableStateOf(GalleryViewMode.GRID) }

    val activeCatForShop = remember(categories, selectedCategory) {
        categories.firstOrNull { it.name.equals(selectedCategory, ignoreCase = true) }
    }

    // Collect all unique subcategories retrieved from Firestore categories so customers can also filter directly by sub-category
    val allAvailableSubcategories = remember(categories, activeCatForShop) {
        if (activeCatForShop != null) {
            activeCatForShop.subcategories
        } else {
            categories.flatMap { it.subcategories }.distinct()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .widthIn(max = 1140.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag("main_shop_section")
    ) {
        val columns = when (galleryViewMode) {
            GalleryViewMode.SHOWCASE -> if (maxWidth >= 760.dp) 2 else 1
            GalleryViewMode.GRID -> when {
                maxWidth >= 960.dp -> 4
                maxWidth >= 680.dp -> 3
                maxWidth >= 420.dp -> 2
                else -> 1
            }
        }
        val spacing = 14.dp
        val cardWidth = (maxWidth - (spacing * (columns - 1))) / columns

        Column(modifier = Modifier.fillMaxWidth()) {
            // Gallery Section Header & View Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Product Gallery",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = GlowDark,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "ক্যাটাগরি এবং সাব-ক্যাটাগরি অনুযায়ী রিয়েল-টাইম প্রোডাক্ট গ্যালারি",
                        style = MaterialTheme.typography.bodyMedium.copy(color = GlowDarkPlum)
                    )
                }

                // Gallery View Mode Toggle (Grid vs Showcase)
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = GlowWhite,
                    modifier = Modifier
                        .border(1.dp, GlowBorderLight, RoundedCornerShape(999.dp))
                        .testTag("gallery_view_mode_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { galleryViewMode = GalleryViewMode.GRID },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (galleryViewMode == GalleryViewMode.GRID) GlowPrimary else GlowWhite
                                )
                                .testTag("gallery_mode_grid_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Grid Gallery View",
                                tint = if (galleryViewMode == GalleryViewMode.GRID) GlowWhite else GlowDarkPlum,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { galleryViewMode = GalleryViewMode.SHOWCASE },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (galleryViewMode == GalleryViewMode.SHOWCASE) GlowPrimary else GlowWhite
                                )
                                .testTag("gallery_mode_showcase_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewAgenda,
                                contentDescription = "Editorial Showcase View",
                                tint = if (galleryViewMode == GalleryViewMode.SHOWCASE) GlowWhite else GlowDarkPlum,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search & Sort Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium.copy(color = GlowDarkPlum.copy(alpha = 0.65f))
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search products",
                            tint = GlowPrimary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = GlowDarkPlum
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(999.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowPrimary,
                        unfocusedBorderColor = GlowBorderLight,
                        focusedContainerColor = GlowWhite,
                        unfocusedContainerColor = GlowWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shop_search_input")
                )

                // Sort Dropdown
                Box {
                    OutlinedButton(
                        onClick = { sortDropdownExpanded = true },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = GlowWhite,
                            contentColor = GlowDark
                        ),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("shop_sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Sort options",
                            tint = GlowPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sortOption.label,
                            style = MaterialTheme.typography.labelLarge.copy(color = GlowDark),
                            maxLines = 1
                        )
                    }

                    DropdownMenu(
                        expanded = sortDropdownExpanded,
                        onDismissRequest = { sortDropdownExpanded = false },
                        modifier = Modifier.background(GlowWhite)
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal,
                                        color = if (option == sortOption) GlowPrimary else GlowDark
                                    )
                                },
                                onClick = {
                                    onSortChange(option)
                                    sortDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // 1. Main Category Filter Chips (Retrieved from Firestore)
            if (categories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "মেইন ক্যাটাগরি (Main Categories):",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = GlowDarkPlum,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("gallery_category_filter_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null && selectedSubCategory == null,
                        onClick = {
                            onSelectCategory(null)
                            onSelectSubCategory(null)
                        },
                        label = {
                            Text(
                                text = if (allProducts.isNotEmpty()) "সব পণ্য (${allProducts.size})" else "সব পণ্য (All)",
                                fontWeight = if (selectedCategory == null && selectedSubCategory == null) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GlowPrimary,
                            selectedLabelColor = GlowWhite,
                            containerColor = GlowWhite,
                            labelColor = GlowDark
                        ),
                        modifier = Modifier.testTag("gallery_filter_all")
                    )
                    categories.forEach { cat ->
                        val selected = selectedCategory.equals(cat.name, ignoreCase = true)
                        val count = remember(cat, allProducts) {
                            countProductsForCategory(cat, allProducts)
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCategory(cat.name) },
                            label = {
                                Text(
                                    text = if (count > 0) "${cat.name} ($count)" else cat.name,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GlowPrimary,
                                selectedLabelColor = GlowWhite,
                                containerColor = GlowWhite,
                                labelColor = GlowDark
                            ),
                            modifier = Modifier.testTag("gallery_filter_cat_${cat.id}")
                        )
                    }
                }

                // 2. Sub-Category Filter Chips (Dynamic based on selected Category or all Firestore subcategories)
                if (allAvailableSubcategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (activeCatForShop != null) {
                            "${activeCatForShop.name} — সাব-ক্যাটাগরি ফিল্টার:"
                        } else {
                            "সাব-ক্যাটাগরি কুইক ফিল্টার (Sub-Categories):"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = GlowSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("gallery_subcategory_filter_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeCatForShop != null) {
                            FilterChip(
                                selected = selectedSubCategory == null,
                                onClick = { onSelectSubCategory(null) },
                                label = { Text("সব (${activeCatForShop.name})") },
                                shape = RoundedCornerShape(999.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GlowSecondary,
                                    selectedLabelColor = GlowWhite,
                                    containerColor = GlowWhite,
                                    labelColor = GlowDark
                                )
                            )
                        }
                        allAvailableSubcategories.forEach { sub ->
                            val isSubSelected = selectedSubCategory.equals(sub, ignoreCase = true)
                            val subCount = remember(sub, allProducts) {
                                countProductsForSubCategory(sub, allProducts)
                            }
                            FilterChip(
                                selected = isSubSelected,
                                onClick = {
                                    // If user selects a subcategory when no main category is selected, auto-match parent category if found
                                    if (selectedCategory == null) {
                                        val parentCat = categories.firstOrNull { c ->
                                            c.subcategories.any { it.equals(sub, ignoreCase = true) }
                                        }
                                        if (parentCat != null) {
                                            onSelectCategory(parentCat.name)
                                        }
                                    }
                                    onSelectSubCategory(sub)
                                },
                                label = {
                                    Text(
                                        text = if (subCount > 0) "$sub ($subCount)" else sub,
                                        fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                shape = RoundedCornerShape(999.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GlowSecondary,
                                    selectedLabelColor = GlowWhite,
                                    containerColor = GlowWhite,
                                    labelColor = GlowDark
                                )
                            )
                        }
                    }
                }
            }

            // 3. Active Filter Breadcrumb & Live Result Summary Bar
            AnimatedVisibility(
                visible = selectedCategory != null || selectedSubCategory != null || searchQuery.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GlowSurfaceLight.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .border(1.dp, GlowBorderLight, RoundedCornerShape(16.dp))
                        .testTag("gallery_active_filter_breadcrumb")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ফিল্টার:",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = GlowDarkPlum,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = selectedCategory ?: "সব ক্যাটাগরি",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = GlowPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (!selectedSubCategory.isNullOrBlank()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = GlowDarkPlum,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = selectedSubCategory,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = GlowSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Text(
                                text = " • ${products.size}টি পণ্য",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = GlowDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onSelectCategory(null)
                                onSelectSubCategory(null)
                                onSearchChange("")
                            },
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("gallery_clear_all_filters_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear filters",
                                tint = GlowPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "সব মুছুন",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GlowPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Product Gallery Grid or Empty State ("কোনো পণ্য পাওয়া যায়নি")
            if (products.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = GlowWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlowBorderLight, RoundedCornerShape(24.dp))
                        .padding(vertical = 36.dp, horizontal = 20.dp)
                        .testTag("empty_products_state")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(GlowSurfaceLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = GlowPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.empty_products),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = GlowDark,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedCategory != null || selectedSubCategory != null || searchQuery.isNotBlank()) {
                                "নির্বাচিত ক্যাটাগরি বা সাব-ক্যাটাগরিতে বর্তমানে কোনো পণ্য নেই। অন্য ক্যাটাগরি বেছে নিন অথবা ফিল্টার রিসেট করুন।"
                            } else {
                                "অ্যাডমিন প্যানেল (/admin) থেকে নতুন পণ্য যুক্ত করলে তা রিয়েল-টাইমে এখানে দেখা যাবে।"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(color = GlowDarkPlum),
                            textAlign = TextAlign.Center
                        )
                        if (selectedCategory != null || selectedSubCategory != null || searchQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    onSelectCategory(null)
                                    onSelectSubCategory(null)
                                    onSearchChange("")
                                },
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text("সব পণ্য দেখুন (Show All Products)", color = GlowPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_gallery_grid"),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    maxItemsInEachRow = columns
                ) {
                    products.forEach { product ->
                        ProductCardItem(
                            product = product,
                            isShowcaseMode = galleryViewMode == GalleryViewMode.SHOWCASE,
                            onDetailsClick = { onProductDetailsClick(product) },
                            onOrderClick = { onProductOrderClick(product) },
                            onAddToCartClick = { onAddToCartClick(product) },
                            onCategoryTagClick = { catName ->
                                if (catName.isNotBlank()) onSelectCategory(catName)
                            },
                            onSubCategoryTagClick = { subName ->
                                if (subName.isNotBlank()) onSelectSubCategory(subName)
                            },
                            modifier = Modifier
                                .width(cardWidth)
                                .testTag("product_card_${product.id}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCardItem(
    product: Product,
    isShowcaseMode: Boolean = false,
    onDetailsClick: () -> Unit,
    onOrderClick: () -> Unit,
    onAddToCartClick: () -> Unit = {},
    onCategoryTagClick: (String) -> Unit = {},
    onSubCategoryTagClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val active = isHovered || isPressed

    val cardScale by animateFloatAsState(
        targetValue = if (active) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "product_card_scale"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GlowWhite,
        tonalElevation = if (active) 3.dp else 0.dp,
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .border(
                width = 1.dp,
                color = if (active) GlowPrimary.copy(alpha = 0.7f) else GlowBorderLight,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onDetailsClick
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Product Image + Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isShowcaseMode) 1.25f else 1f)
                    .background(GlowSurfaceLight)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-left discount / new arrival badges
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    product.discountPercentage?.let { pct ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(GlowSecondary)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "-$pct%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GlowWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    if (product.newArrival) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(GlowDark)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "NEW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GlowWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Quick Add to Cart floating icon button (top-right)
                if (product.available) {
                    IconButton(
                        onClick = onAddToCartClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GlowWhite.copy(alpha = 0.9f))
                            .border(1.dp, GlowBorderLight, CircleShape)
                            .testTag("btn_quick_cart_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "Add to Cart",
                            tint = GlowPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Out of stock badge
                if (!product.available) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(GlowDark.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "স্টক আউট",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GlowWhite,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Product Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Clickable Category & Sub-Category Filter Badges
                if (product.category.isNotBlank() || product.subCategory.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (product.category.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(GlowSurfaceLight)
                                    .clickable { onCategoryTagClick(product.category) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = product.category,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GlowPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        if (product.subCategory.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(GlowBgLight)
                                    .border(1.dp, GlowBorderLight, RoundedCornerShape(999.dp))
                                    .clickable { onSubCategoryTagClick(product.subCategory) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = product.subCategory,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GlowSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = GlowDark,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    minLines = if (isShowcaseMode) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (isShowcaseMode && product.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = GlowDarkPlum,
                            fontSize = 13.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current Price & Old Price
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatBdt(product.price),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = GlowPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    if (product.oldPrice != null && product.oldPrice > product.price) {
                        Text(
                            text = formatBdt(product.oldPrice),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = GlowDarkPlum.copy(alpha = 0.65f),
                                textDecoration = TextDecoration.LineThrough
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons: "বিস্তারিত" and "অর্ডার"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDetailsClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_details_${product.id}")
                    ) {
                        Text(
                            text = stringResource(R.string.btn_details),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = GlowDark,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onOrderClick,
                        enabled = product.available,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlowPrimary,
                            contentColor = GlowWhite,
                            disabledContainerColor = GlowBorderLight,
                            disabledContentColor = GlowDarkPlum.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_order_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.btn_order),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
