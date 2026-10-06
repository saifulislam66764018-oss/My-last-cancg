package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.theme.GlowGoldAccent
import com.example.ui.theme.GlowHeroDeep
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.model.Product
import com.example.model.StoreSettings
import com.example.model.formatBdt
import com.example.ui.theme.GlowBgLight
import com.example.ui.theme.GlowBorderLight
import com.example.ui.theme.GlowDark
import com.example.ui.theme.GlowDarkPlum
import com.example.ui.theme.GlowPrimary
import com.example.ui.theme.GlowSecondary
import com.example.ui.theme.GlowSurfaceLight
import com.example.ui.theme.GlowWhite
import kotlin.math.roundToInt

@Composable
fun HeroSection(
    storeSettings: StoreSettings,
    products: List<Product>,
    onExploreShop: () -> Unit,
    onExploreCategories: () -> Unit,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    // Resolve the 4 dynamic Hero Product Slots from real Firestore products
    val productMap = remember(products) { products.associateBy { it.id } }
    val slotLeft1 = productMap[storeSettings.heroProductIds.left1]
    val slotLeft2 = productMap[storeSettings.heroProductIds.left2]
    val slotRight1 = productMap[storeSettings.heroProductIds.right1]
    val slotRight2 = productMap[storeSettings.heroProductIds.right2]

    // Entrance animation state
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
    }

    val entranceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "hero_entrance_alpha"
    )
    val entranceTranslateY by animateFloatAsState(
        targetValue = if (entered) 0f else 28f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "hero_entrance_y"
    )
    val entranceScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.97f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "hero_entrance_scale"
    )

    // Interactive timeline scrubbing & parallax state (using primitive state to avoid heavy recompositions)
    var heroSize by remember { mutableStateOf(IntSize(1, 1)) }
    var scrubProgress by remember { mutableFloatStateOf(0.25f) }
    var pointerNormX by remember { mutableFloatStateOf(0f) } // -1f..1f
    var pointerNormY by remember { mutableFloatStateOf(0f) } // -1f..1f
    var videoDurationMs by remember { mutableIntStateOf(0) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var lastSeekTimestampMs by remember { mutableLongStateOf(0L) }

    // Frame-aligned video seek loop when user scrubs timeline
    LaunchedEffect(scrubProgress, videoDurationMs) {
        val player = mediaPlayerRef ?: return@LaunchedEffect
        if (videoDurationMs <= 0) return@LaunchedEffect
        withFrameNanos { frameTimeNanos ->
            val nowMs = frameTimeNanos / 1_000_000L
            // Throttle hardware video seeks to ~12fps (every 80ms) for smooth scrubbing
            if (nowMs - lastSeekTimestampMs >= 80L) {
                lastSeekTimestampMs = nowMs
                val targetMs = (scrubProgress.coerceIn(0f, 1f) * videoDurationMs).roundToInt()
                try {
                    player.seekTo(targetMs)
                } catch (_: Exception) {
                }
            }
        }
    }

    val animatedParallaxX by animateFloatAsState(
        targetValue = pointerNormX,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "parallax_x"
    )
    val animatedParallaxY by animateFloatAsState(
        targetValue = pointerNormY,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "parallax_y"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { heroSize = it }
            // Desktop cursor / pointer position tracking for timeline scrubbing + parallax
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Move) {
                            val pos = event.changes.firstOrNull()?.position ?: continue
                            val width = heroSize.width.coerceAtLeast(1).toFloat()
                            val height = heroSize.height.coerceAtLeast(1).toFloat()
                            val normX = (pos.x / width).coerceIn(0f, 1f)
                            val normY = (pos.y / height).coerceIn(0f, 1f)
                            scrubProgress = normX
                            pointerNormX = (normX - 0.5f) * 2f
                            pointerNormY = (normY - 0.5f) * 2f
                        }
                    }
                }
            }
            // Mobile horizontal swipe interaction (Swipe left -> timeline forward, Swipe right -> backward)
            // Vertical swipes pass through naturally to scroll the page!
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val width = heroSize.width.coerceAtLeast(1).toFloat()
                    // Swipe left (negative dragAmount) moves video timeline forward
                    val delta = (-dragAmount / width) * 1.15f
                    scrubProgress = (scrubProgress + delta).coerceIn(0f, 1f)
                    pointerNormX = (scrubProgress - 0.5f) * 2f
                }
            }
            .testTag("hero_section")
    ) {
        val isWideScreen = maxWidth >= 820.dp
        val heroMinHeight = if (isWideScreen) 620.dp else 540.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroMinHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            GlowHeroDeep,
                            GlowDark,
                            GlowDarkPlum,
                            GlowBgLight
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // 1. Luxury Rose-Gold & Pearl Silk Editorial Background Artwork with Parallax
            Image(
                painter = painterResource(id = R.drawable.img_hero_luxury_bg_1791302493790),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.08f
                        scaleY = 1.08f
                        translationX = -animatedParallaxX * 16f + (scrubProgress - 0.5f) * 24f
                        translationY = -animatedParallaxY * 10f
                        alpha = 0.58f
                    }
            )

            // 2. Ambient Luxury Botanical & Champagne Gold Glow Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val scrubShift = (scrubProgress - 0.5f) * w * 0.28f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlowPrimary.copy(alpha = 0.44f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.28f + scrubShift, h * 0.34f),
                        radius = w * 0.55f
                    ),
                    center = Offset(w * 0.28f + scrubShift, h * 0.34f),
                    radius = w * 0.55f
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlowGoldAccent.copy(alpha = 0.30f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.52f - scrubShift * 0.5f, h * 0.26f),
                        radius = w * 0.45f
                    ),
                    center = Offset(w * 0.52f - scrubShift * 0.5f, h * 0.26f),
                    radius = w * 0.45f
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlowSecondary.copy(alpha = 0.36f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.74f - scrubShift, h * 0.56f),
                        radius = w * 0.5f
                    ),
                    center = Offset(w * 0.74f - scrubShift, h * 0.56f),
                    radius = w * 0.5f
                )
            }

            // 2. Configured Hero Background Media (Video or Image from Firestore settings/store)
            val mediaUrl = storeSettings.heroVideoUrl
            if (mediaUrl.isNotBlank()) {
                if (storeSettings.heroMediaType == "image") {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = "Hero Background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1.05f
                                scaleY = 1.05f
                                translationX = -animatedParallaxX * 14f
                                translationY = -animatedParallaxY * 10f
                                alpha = 0.45f
                            }
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(Uri.parse(mediaUrl))
                                setOnPreparedListener { mp ->
                                    mediaPlayerRef = mp
                                    videoDurationMs = mp.duration.coerceAtLeast(0)
                                    mp.isLooping = true
                                    mp.setVolume(0f, 0f)
                                    mp.start()
                                }
                                setOnErrorListener { _, _, _ -> true }
                            }
                        },
                        update = { view ->
                            if (view.tag != mediaUrl) {
                                view.tag = mediaUrl
                                try {
                                    view.setVideoURI(Uri.parse(mediaUrl))
                                } catch (_: Exception) {
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 0.42f }
                    )
                }
            }

            // Soft vignette overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                GlowDark.copy(alpha = 0.58f),
                                GlowDark.copy(alpha = 0.38f),
                                GlowBgLight.copy(alpha = 0.96f)
                            )
                        )
                    )
            )

            // 3. Desktop / Wide Screen Four Floating Product Cards Around Hero
            if (isWideScreen) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 1200.dp)
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 76.dp)
                ) {
                    // Left Top (left1)
                    if (slotLeft1 != null) {
                        FloatingHeroProductCard(
                            product = slotLeft1,
                            durationMillis = 2800,
                            delayMillis = 0,
                            floatAmplitudeDp = 8f,
                            parallaxX = animatedParallaxX * 10f,
                            parallaxY = animatedParallaxY * 8f,
                            onClick = { onProductClick(slotLeft1) },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .testTag("hero_slot_left1")
                        )
                    }

                    // Left Bottom (left2)
                    if (slotLeft2 != null) {
                        FloatingHeroProductCard(
                            product = slotLeft2,
                            durationMillis = 3400,
                            delayMillis = 420,
                            floatAmplitudeDp = 10f,
                            parallaxX = animatedParallaxX * -8f,
                            parallaxY = animatedParallaxY * 10f,
                            onClick = { onProductClick(slotLeft2) },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(bottom = 28.dp)
                                .testTag("hero_slot_left2")
                        )
                    }

                    // Right Top (right1)
                    if (slotRight1 != null) {
                        FloatingHeroProductCard(
                            product = slotRight1,
                            durationMillis = 3100,
                            delayMillis = 720,
                            floatAmplitudeDp = 9f,
                            parallaxX = animatedParallaxX * 12f,
                            parallaxY = animatedParallaxY * -8f,
                            onClick = { onProductClick(slotRight1) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .testTag("hero_slot_right1")
                        )
                    }

                    // Right Bottom (right2)
                    if (slotRight2 != null) {
                        FloatingHeroProductCard(
                            product = slotRight2,
                            durationMillis = 3600,
                            delayMillis = 260,
                            floatAmplitudeDp = 7f,
                            parallaxX = animatedParallaxX * -10f,
                            parallaxY = animatedParallaxY * -9f,
                            onClick = { onProductClick(slotRight2) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 28.dp)
                                .testTag("hero_slot_right2")
                        )
                    }
                }
            }

            // 4. Center Hero Content with Smooth Entrance Animation
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 74.dp, bottom = 24.dp)
                    .graphicsLayer {
                        alpha = entranceAlpha
                        translationY = entranceTranslateY
                        scaleX = entranceScale
                        scaleY = entranceScale
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pill badge with Brand Logo
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = GlowWhite.copy(alpha = 0.16f),
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = GlowWhite.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(999.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (storeSettings.logoUrl.isNotBlank()) {
                            AsyncImage(
                                model = storeSettings.logoUrl,
                                contentDescription = "${storeSettings.websiteName} logo",
                                placeholder = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                                error = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, GlowGoldAccent, CircleShape)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                                contentDescription = "Glowisticbd logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, GlowGoldAccent, CircleShape)
                            )
                        }
                        Text(
                            text = storeSettings.websiteName.ifBlank { "Glowisticbd" },
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = GlowWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = storeSettings.description.ifBlank {
                        "Your Unlimited Glow. Premium cosmetics & skincare collection in Bangladesh."
                    },
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = GlowWhite,
                        fontSize = if (isWideScreen) 38.sp else 28.sp,
                        lineHeight = if (isWideScreen) 46.sp else 36.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = storeSettings.tagline.ifBlank {
                        "আপনার সীমাহীন গ্লো। বাংলাদেশের প্রিমিয়াম কসমেটিকস ও স্কিনকেয়ার কালেকশন।"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = GlowSurfaceLight,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // CTA Buttons with scale feedback
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val shopBtnInteraction = remember { MutableInteractionSource() }
                    val shopPressed by shopBtnInteraction.collectIsPressedAsState()
                    val shopScale by animateFloatAsState(
                        targetValue = if (shopPressed) 0.96f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "hero_shop_btn_scale"
                    )

                    Button(
                        onClick = onExploreShop,
                        interactionSource = shopBtnInteraction,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlowPrimary,
                            contentColor = GlowWhite
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .graphicsLayer {
                                scaleX = shopScale
                                scaleY = shopScale
                            }
                            .testTag("hero_explore_shop_button")
                    ) {
                        Text(
                            text = "Shop Collection",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onExploreCategories,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = GlowWhite.copy(alpha = 0.14f),
                            contentColor = GlowWhite
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("hero_categories_button")
                    ) {
                        Text(
                            text = "Categories",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 5. Compact Horizontal Floating Product Carousel on Mobile
                val activeHeroProducts = remember(slotLeft1, slotLeft2, slotRight1, slotRight2) {
                    listOfNotNull(slotLeft1, slotLeft2, slotRight1, slotRight2)
                }

                if (!isWideScreen && activeHeroProducts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(22.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        activeHeroProducts.forEachIndexed { idx, heroProduct ->
                            FloatingHeroProductCard(
                                product = heroProduct,
                                durationMillis = 2700 + (idx * 300),
                                delayMillis = idx * 220,
                                floatAmplitudeDp = 5f,
                                parallaxX = animatedParallaxX * 4f,
                                parallaxY = 0f,
                                onClick = { onProductClick(heroProduct) },
                                modifier = Modifier.testTag("hero_mobile_card_$idx")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive Timeline Scrubber Indicator Pill
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = GlowDark.copy(alpha = 0.55f),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = GlowWhite.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .testTag("hero_timeline_indicator")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Swipe,
                            contentDescription = "Interactive video timeline",
                            tint = GlowSurfaceLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(GlowWhite.copy(alpha = 0.25f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(scrubProgress.coerceIn(0.05f, 1f))
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(GlowPrimary, GlowSecondary)
                                        )
                                    )
                            )
                        }
                        Text(
                            text = "${(scrubProgress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GlowSurfaceLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingHeroProductCard(
    product: Product,
    durationMillis: Int,
    delayMillis: Int,
    floatAmplitudeDp: Float,
    parallaxX: Float,
    parallaxY: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_card_float_${product.id}")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -floatAmplitudeDp,
        targetValue = floatAmplitudeDp,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis)
        ),
        label = "floating_y_offset"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val active = isHovered || isPressed

    val cardScale by animateFloatAsState(
        targetValue = if (active) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "hero_card_scale"
    )
    val liftY by animateFloatAsState(
        targetValue = if (active) -6f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "hero_card_lift"
    )
    val imageScale by animateFloatAsState(
        targetValue = if (active) 1.08f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "hero_card_img_zoom"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = GlowWhite.copy(alpha = if (active) 0.96f else 0.88f),
        tonalElevation = if (active) 6.dp else 2.dp,
        modifier = modifier
            .width(196.dp)
            .graphicsLayer {
                translationX = parallaxX
                translationY = floatOffset + parallaxY + liftY
                scaleX = cardScale
                scaleY = cardScale
            }
            .border(
                width = if (active) 1.5.dp else 1.dp,
                color = if (active) GlowPrimary else GlowBorderLight.copy(alpha = 0.8f),
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlowSurfaceLight)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = imageScale
                            scaleY = imageScale
                        }
                )
                product.discountPercentage?.let { pct ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(3.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GlowSecondary)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "-$pct%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GlowWhite,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = GlowDark,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatBdt(product.price),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = GlowPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    if (product.oldPrice != null && product.oldPrice > product.price) {
                        Text(
                            text = formatBdt(product.oldPrice),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GlowDarkPlum.copy(alpha = 0.65f),
                                textDecoration = TextDecoration.LineThrough
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StorePromoBannerSection(
    storeSettings: StoreSettings,
    onShopNowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = 1140.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("store_promo_banner_section")
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = GlowDark,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, GlowBorderLight, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                // Store Banner Image (uses custom Firestore image URL if set to image mode, else official Glowisticbd banner)
                if (storeSettings.heroMediaType == "image" && storeSettings.heroVideoUrl.isNotBlank()) {
                    AsyncImage(
                        model = storeSettings.heroVideoUrl,
                        contentDescription = "${storeSettings.websiteName} Promotional Banner",
                        placeholder = painterResource(id = R.drawable.img_glowistic_store_banner_1791302798689),
                        error = painterResource(id = R.drawable.img_glowistic_store_banner_1791302798689),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_glowistic_store_banner_1791302798689),
                        contentDescription = "${storeSettings.websiteName.ifBlank { "Glowisticbd" }} Official Store Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Luxury Gradient Overlay for readable text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    GlowHeroDeep.copy(alpha = 0.88f),
                                    GlowDark.copy(alpha = 0.68f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (storeSettings.logoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = storeSettings.logoUrl,
                                    contentDescription = "${storeSettings.websiteName} Logo",
                                    placeholder = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                                    error = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, GlowGoldAccent, CircleShape)
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_glowistic_brand_logo_1791302785474),
                                    contentDescription = "Glowisticbd Official Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, GlowGoldAccent, CircleShape)
                                )
                            }

                            Column {
                                Text(
                                    text = storeSettings.websiteName.ifBlank { "Glowisticbd" },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = GlowWhite,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                                Text(
                                    text = "OFFICIAL FLAGSHIP STORE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GlowGoldAccent,
                                        letterSpacing = 1.1.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = storeSettings.tagline.ifBlank {
                                "আপনার সীমাহীন গ্লো। বাংলাদেশের প্রিমিয়াম কসমেটিকস ও স্কিনকেয়ার কালেকশন।"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = GlowSurfaceLight,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onShopNowClick,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlowPrimary,
                                contentColor = GlowWhite
                            ),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("promo_banner_shop_btn")
                        ) {
                            Text(
                                text = "Explore Collection",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

