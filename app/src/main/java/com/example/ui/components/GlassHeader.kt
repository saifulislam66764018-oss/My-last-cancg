package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.NavSection
import com.example.model.StoreSettings
import com.example.ui.theme.GlowBorderLight
import com.example.ui.theme.GlowDark
import com.example.ui.theme.GlowDarkPlum
import com.example.ui.theme.GlowPrimary
import com.example.ui.theme.GlowSecondary
import com.example.ui.theme.GlowSurfaceLight
import com.example.ui.theme.GlowWhite

@Composable
fun GlassHeader(
    storeSettings: StoreSettings,
    activeSection: NavSection,
    cartCount: Int,
    onSelectSection: (NavSection) -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mobileMenuOpen by remember { mutableStateOf(false) }
    val navItems = remember {
        listOf(NavSection.HOME, NavSection.SHOP, NavSection.CATEGORIES, NavSection.ADMIN)
    }
    var hoveredNavIndex by remember { mutableIntStateOf(-1) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val isCompact = maxWidth < 640.dp

        Column(
            modifier = Modifier
                .widthIn(max = 1080.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Floating pill-style glass container
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = GlowWhite.copy(alpha = 0.88f),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = GlowBorderLight.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .testTag("glass_header_navbar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // LEFT: Dynamic Logo & Website Name from Firestore settings
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                onSelectSection(NavSection.HOME)
                                mobileMenuOpen = false
                            }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                            .testTag("header_brand_logo")
                    ) {
                        if (storeSettings.logoUrl.isNotBlank()) {
                            AsyncImage(
                                model = storeSettings.logoUrl,
                                contentDescription = "${storeSettings.websiteName} logo",
                                placeholder = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                                error = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, GlowPrimary.copy(alpha = 0.6f), CircleShape)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                                contentDescription = "${storeSettings.websiteName.ifBlank { "Glowisticbd" }} logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, GlowPrimary.copy(alpha = 0.6f), CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = storeSettings.websiteName.ifBlank { "Glowisticbd" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = GlowDark,
                                letterSpacing = (-0.2).sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // CENTER: Desktop/Tablet Navigation with spring cursor-following indicator
                    if (!isCompact) {
                        val activeIndex = navItems.indexOf(activeSection).coerceAtLeast(0)
                        val targetIndex = if (hoveredNavIndex >= 0) hoveredNavIndex else activeIndex
                        val itemWidth = 104.dp
                        val indicatorOffset by animateDpAsState(
                            targetValue = itemWidth * targetIndex,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "nav_indicator_offset"
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(GlowSurfaceLight.copy(alpha = 0.55f))
                                .border(
                                    width = 1.dp,
                                    color = GlowBorderLight.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .padding(4.dp)
                        ) {
                            // Spring-animated sliding pill indicator
                            Box(
                                modifier = Modifier
                                    .offset(x = indicatorOffset)
                                    .width(itemWidth)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                GlowPrimary.copy(alpha = 0.16f),
                                                GlowSecondary.copy(alpha = 0.14f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = GlowPrimary.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                navItems.forEachIndexed { index, item ->
                                    val isSelected = activeSection == item
                                    val isHovered = hoveredNavIndex == index
                                    val scale by animateFloatAsState(
                                        targetValue = if (isHovered || isSelected) 1.05f else 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "nav_item_scale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .width(itemWidth)
                                            .height(36.dp)
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .pointerInput(index) {
                                                awaitPointerEventScope {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        when (event.type) {
                                                            PointerEventType.Enter,
                                                            PointerEventType.Move -> hoveredNavIndex = index
                                                            PointerEventType.Exit -> {
                                                                if (hoveredNavIndex == index) {
                                                                    hoveredNavIndex = -1
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable { onSelectSection(item) }
                                            .testTag("nav_item_${item.name.lowercase()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) GlowPrimary else GlowDarkPlum
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // RIGHT: Admin shortcut + Cart + Mobile Hamburger Menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Visible Admin Panel pill button (/admin)
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (activeSection == NavSection.ADMIN) {
                                GlowPrimary
                            } else {
                                GlowSurfaceLight
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(999.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (activeSection == NavSection.ADMIN) GlowPrimary else GlowBorderLight,
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .clickable {
                                    onSelectSection(NavSection.ADMIN)
                                    mobileMenuOpen = false
                                }
                                .testTag("header_admin_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Panel",
                                    tint = if (activeSection == NavSection.ADMIN) GlowWhite else GlowPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Admin",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeSection == NavSection.ADMIN) GlowWhite else GlowDark
                                    )
                                )
                            }
                        }

                        // Cart button with badge
                        IconButton(
                            onClick = onOpenCart,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(GlowSurfaceLight)
                                .border(1.dp, GlowBorderLight, CircleShape)
                                .testTag("header_cart_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (cartCount > 0) {
                                        Badge(
                                            containerColor = GlowPrimary,
                                            contentColor = GlowWhite
                                        ) {
                                            Text(
                                                text = cartCount.toString(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = "Shopping Cart",
                                    tint = GlowDark
                                )
                            }
                        }

                        // Mobile hamburger menu button
                        if (isCompact) {
                            IconButton(
                                onClick = { mobileMenuOpen = !mobileMenuOpen },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(CircleShape)
                                    .background(GlowSurfaceLight.copy(alpha = 0.7f))
                                    .border(1.dp, GlowBorderLight, CircleShape)
                                    .testTag("header_mobile_menu_button")
                            ) {
                                Icon(
                                    imageVector = if (mobileMenuOpen) Icons.Default.Close else Icons.Default.Menu,
                                    contentDescription = if (mobileMenuOpen) "Close menu" else "Open menu",
                                    tint = GlowDark
                                )
                            }
                        }
                    }
                }
            }

            // Mobile Dropdown Menu
            AnimatedVisibility(
                visible = isCompact && mobileMenuOpen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = GlowWhite.copy(alpha = 0.96f),
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .border(1.dp, GlowBorderLight, RoundedCornerShape(24.dp))
                        .testTag("mobile_nav_dropdown")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val allMobileItems = listOf(
                            NavSection.HOME,
                            NavSection.SHOP,
                            NavSection.CATEGORIES,
                            NavSection.ADMIN
                        )
                        allMobileItems.forEach { section ->
                            val isSelected = activeSection == section
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) GlowSurfaceLight else Color.Transparent
                                    )
                                    .clickable {
                                        onSelectSection(section)
                                        mobileMenuOpen = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("mobile_nav_${section.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (section == NavSection.ADMIN) "Admin Panel (/admin)" else section.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) GlowPrimary else GlowDark
                                    )
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(GlowPrimary)
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
