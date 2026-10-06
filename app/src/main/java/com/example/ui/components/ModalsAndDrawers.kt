package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CartItem
import com.example.model.Product
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

@Composable
fun ProductDetailsModal(
    product: Product,
    onDismiss: () -> Unit,
    onAddToCart: (Product, Int) -> Unit,
    onWhatsAppOrder: (Product, Int) -> Unit
) {
    var quantity by remember(product.id) { mutableIntStateOf(1) }
    val allImages = remember(product) {
        (listOf(product.imageUrl) + product.galleryImages)
            .filter { it.isNotBlank() }
            .distinct()
    }
    var selectedImageUrl by remember(product.id) {
        mutableStateOf(allImages.firstOrNull() ?: product.imageUrl)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = GlowWhite,
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxWidth(0.94f)
                .border(1.dp, GlowBorderLight, RoundedCornerShape(28.dp))
                .testTag("product_details_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // High-Resolution Image Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .background(GlowSurfaceLight)
                ) {
                    AsyncImage(
                        model = selectedImageUrl,
                        contentDescription = product.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Discount Badge
                    product.discountPercentage?.let { pct ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(GlowSecondary)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "-$pct% OFF",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = GlowWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(CircleShape)
                            .background(GlowWhite.copy(alpha = 0.9f))
                            .border(1.dp, GlowBorderLight, CircleShape)
                            .testTag("close_product_modal_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close details",
                            tint = GlowDark
                        )
                    }
                }

                // Interactive Gallery Thumbnail Strip when product has multiple images
                if (allImages.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlowBgLight)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        allImages.forEach { imgUrl ->
                            val isSelected = imgUrl == selectedImageUrl
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) GlowPrimary else GlowBorderLight,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedImageUrl = imgUrl }
                            ) {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = "Product angle",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Details Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (product.category.isNotBlank() || product.subCategory.isNotBlank()) {
                            val badgeLabel = listOf(product.category, product.subCategory)
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = GlowSurfaceLight
                            ) {
                                Text(
                                    text = badgeLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = GlowPrimary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Stock status badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (product.available) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (product.available) GlowSuccess else GlowDanger,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (product.available) {
                                    stringResource(R.string.status_in_stock)
                                } else {
                                    stringResource(R.string.status_out_of_stock)
                                },
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (product.available) GlowSuccess else GlowDanger,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = GlowDark,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Price Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatBdt(product.price),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = GlowPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        if (product.oldPrice != null && product.oldPrice > product.price) {
                            Text(
                                text = formatBdt(product.oldPrice),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = GlowDarkPlum.copy(alpha = 0.6f),
                                    textDecoration = TextDecoration.LineThrough
                                )
                            )
                        }
                    }

                    if (product.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = GlowBorderLight.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = GlowDarkPlum
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Quantity Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Quantity",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = GlowDark,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(GlowSurfaceLight)
                                .border(1.dp, GlowBorderLight, RoundedCornerShape(999.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                enabled = product.available && quantity > 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease quantity",
                                    tint = GlowDark
                                )
                            }

                            Text(
                                text = quantity.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = GlowDark,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )

                            IconButton(
                                onClick = { if (quantity < 99) quantity++ },
                                enabled = product.available,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase quantity",
                                    tint = GlowDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons: Add to Cart & WhatsApp Order
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onAddToCart(product, quantity)
                                onDismiss()
                            },
                            enabled = product.available,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("modal_add_to_cart_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = null,
                                tint = GlowPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_add_to_cart),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = GlowPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Button(
                            onClick = {
                                onWhatsAppOrder(product, quantity)
                                onDismiss()
                            },
                            enabled = product.available,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlowPrimary,
                                contentColor = GlowWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("modal_whatsapp_order_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_whatsapp_order),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SlideOverCartDrawer(
    visible: Boolean,
    cartItems: List<CartItem>,
    deliveryCharge: Double,
    onClose: () -> Unit,
    onUpdateQuantity: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onProceedToCheckout: () -> Unit
) {
    val subtotal = remember(cartItems) { cartItems.sumOf { it.price * it.quantity } }
    val total = if (cartItems.isEmpty()) 0.0 else subtotal + deliveryCharge

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim overlay
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GlowDark.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
            )
        }

        // Right-side Drawer Panel
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                color = GlowWhite,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight()
                    .border(1.dp, GlowBorderLight)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .testTag("cart_slide_over_drawer")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Drawer Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = GlowPrimary
                            )
                            Text(
                                text = "Shopping Cart (${cartItems.sumOf { it.quantity }})",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = GlowDark,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (cartItems.isNotEmpty()) {
                                IconButton(
                                    onClick = onClearCart,
                                    modifier = Modifier.testTag("clear_cart_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear Cart",
                                        tint = GlowDanger
                                    )
                                }
                            }
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.testTag("close_cart_drawer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Cart",
                                    tint = GlowDark
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = GlowBorderLight)

                    // Cart Items List
                    if (cartItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = GlowBorderLight,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "আপনার কার্ট খালি",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = GlowDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "পছন্দের পণ্য কার্টে যুক্ত করুন।",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = GlowDarkPlum)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(cartItems, key = { it.productId }) { item ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = GlowBgLight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, GlowBorderLight, RoundedCornerShape(16.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AsyncImage(
                                            model = item.imageUrl,
                                            contentDescription = item.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(GlowSurfaceLight)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = GlowDark,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${formatBdt(item.price)} / unit",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = GlowPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Quantity controls
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = GlowWhite,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .border(1.dp, GlowBorderLight, CircleShape)
                                                        .clickable {
                                                            onUpdateQuantity(item.productId, item.quantity - 1)
                                                        }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Remove,
                                                            contentDescription = "Decrease",
                                                            tint = GlowDark,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = item.quantity.toString(),
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        color = GlowDark,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )

                                                Surface(
                                                    shape = CircleShape,
                                                    color = GlowWhite,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .border(1.dp, GlowBorderLight, CircleShape)
                                                        .clickable {
                                                            onUpdateQuantity(item.productId, item.quantity + 1)
                                                        }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = "Increase",
                                                            tint = GlowDark,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            IconButton(
                                                onClick = { onRemoveItem(item.productId) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove item",
                                                    tint = GlowDanger,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = formatBdt(item.price * item.quantity),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = GlowDark,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Cart Footer Summary
                        Surface(
                            color = GlowBgLight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlowBorderLight)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.label_subtotal),
                                        style = MaterialTheme.typography.bodyMedium.copy(color = GlowDarkPlum)
                                    )
                                    Text(
                                        text = formatBdt(subtotal),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = GlowDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.label_delivery_charge),
                                        style = MaterialTheme.typography.bodyMedium.copy(color = GlowDarkPlum)
                                    )
                                    Text(
                                        text = formatBdt(deliveryCharge),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = GlowDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                HorizontalDivider(color = GlowBorderLight)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.label_total),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = GlowDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = formatBdt(total),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            color = GlowPrimary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = onProceedToCheckout,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GlowPrimary,
                                        contentColor = GlowWhite
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("proceed_to_checkout_btn")
                                ) {
                                    Text(
                                        text = stringResource(R.string.btn_checkout),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
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
}

@Composable
fun CheckoutModal(
    cartItems: List<CartItem>,
    deliveryCharge: Double,
    onDismiss: () -> Unit,
    onConfirmWhatsAppOrder: (name: String, phone: String, address: String, note: String) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val subtotal = remember(cartItems) { cartItems.sumOf { it.price * it.quantity } }
    val total = subtotal + deliveryCharge

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = GlowWhite,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.94f)
                .border(1.dp, GlowBorderLight, RoundedCornerShape(26.dp))
                .testTag("checkout_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Checkout & Order",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = GlowDark,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close checkout",
                            tint = GlowDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Customer Details Form
                OutlinedTextField(
                    value = customerName,
                    onValueChange = {
                        customerName = it
                        validationError = null
                    },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        validationError = null
                    },
                    label = { Text("Active Phone Number *") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_phone_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        validationError = null
                    },
                    label = { Text("Delivery Address *") },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_address_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Optional Note") },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_note_input")
                )

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = GlowDanger,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Order Summary Box
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GlowBgLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlowBorderLight, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Order Summary",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = GlowDark,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        cartItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.title} (${item.quantity} × ${formatBdt(item.price)})",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = GlowDarkPlum),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formatBdt(item.price * item.quantity),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = GlowDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        HorizontalDivider(color = GlowBorderLight)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", color = GlowDarkPlum)
                            Text(formatBdt(subtotal), fontWeight = FontWeight.Bold, color = GlowDark)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Delivery Charge", color = GlowDarkPlum)
                            Text(formatBdt(deliveryCharge), fontWeight = FontWeight.Bold, color = GlowDark)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Payable",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = GlowDark,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = formatBdt(total),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = GlowPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        when {
                            customerName.trim().length < 2 -> {
                                validationError = "অনুগ্রহ করে আপনার নাম লিখুন (Please enter your name)."
                            }
                            phone.replace(Regex("[^0-9]"), "").length < 10 -> {
                                validationError = "অনুগ্রহ করে সঠিক সচল ফোন নম্বর দিন (Please enter a valid phone number)."
                            }
                            address.trim().length < 4 -> {
                                validationError = "অনুগ্রহ করে ডেলিভারি ঠিকানা লিখুন (Please enter delivery address)."
                            }
                            else -> {
                                onConfirmWhatsAppOrder(customerName, phone, address, note)
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlowPrimary,
                        contentColor = GlowWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_whatsapp_order_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_whatsapp_order),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
