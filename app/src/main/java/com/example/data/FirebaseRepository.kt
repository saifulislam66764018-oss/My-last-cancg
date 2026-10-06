package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.Category
import com.example.model.HeroProductIds
import com.example.model.Product
import com.example.model.SecurityUtils
import com.example.model.StoreSettings
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Real-time Firebase Firestore & Firebase Auth Repository connected directly
 * to the configured project "glowistic-f42ef".
 * Zero fake/demo data is ever injected here.
 */
class FirebaseRepository(context: Context) {

    companion object {
        private const val TAG = "FirebaseRepository"
        private const val APP_NAME = "GlowisticFirebaseApp"

        // Configured Firebase credentials from specification
        private const val API_KEY = "AIzaSyCqkLKzQEa09qT23tMEzrkVkjoazMQC_Vo"
        private const val APPLICATION_ID = "1:71356849992:web:6ef3e3454e9e9e914f9d7d"
        private const val PROJECT_ID = "glowistic-f42ef"
        private const val STORAGE_BUCKET = "glowistic-f42ef.firebasestorage.app"
        private const val GCM_SENDER_ID = "71356849992"
    }

    private val firebaseApp: FirebaseApp = getOrInitFirebaseApp(context.applicationContext)
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(firebaseApp)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(firebaseApp)

    private fun getOrInitFirebaseApp(appContext: Context): FirebaseApp {
        val existingApps = FirebaseApp.getApps(appContext)
        existingApps.firstOrNull { it.name == APP_NAME }?.let { return it }

        val options = FirebaseOptions.Builder()
            .setApiKey(API_KEY)
            .setApplicationId(APPLICATION_ID)
            .setProjectId(PROJECT_ID)
            .setStorageBucket(STORAGE_BUCKET)
            .setGcmSenderId(GCM_SENDER_ID)
            .build()

        return try {
            FirebaseApp.initializeApp(appContext, options, APP_NAME)
        } catch (e: Exception) {
            Log.w(TAG, "Fallback to existing FirebaseApp instance", e)
            FirebaseApp.getInstance(APP_NAME)
        }
    }

    fun observeAuthUid(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            val uid = currentUser?.uid
            if (uid != null && !SecurityUtils.isAuthorizedAdmin(uid)) {
                firebaseAuth.signOut()
                trySend(null)
            } else {
                trySend(uid)
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun observeProducts(): Flow<List<Product>> = callbackFlow {
        val registration: ListenerRegistration = firestore.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to products", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    parseProduct(doc)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeCategories(): Flow<List<Category>> = callbackFlow {
        val registration: ListenerRegistration = firestore.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to categories", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    parseCategory(doc)
                }?.sortedBy { it.createdAt } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeStoreSettings(): Flow<StoreSettings> = callbackFlow {
        val registration: ListenerRegistration = firestore.collection("settings")
            .document("store")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to settings/store", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(parseStoreSettings(snapshot))
                } else {
                    trySend(StoreSettings())
                }
            }
        awaitClose { registration.remove() }
    }

    private fun parseProduct(doc: DocumentSnapshot): Product? {
        return try {
            val title = SecurityUtils.sanitizeText(doc.getString("title"), 300)
            if (title.isBlank()) return null
            val imageUrl = SecurityUtils.sanitizeImageSource(doc.getString("imageUrl"))
            val price = SecurityUtils.sanitizePrice(doc.get("price"))
            val rawOldPrice = doc.get("oldPrice")
            val oldPrice = if (rawOldPrice != null) {
                val parsed = SecurityUtils.sanitizePrice(rawOldPrice)
                if (parsed > 0.0) parsed else null
            } else null
            val category = SecurityUtils.sanitizeText(doc.getString("category"), 120)
            val subCategory = SecurityUtils.sanitizeText(doc.getString("subCategory"), 120)
            val description = SecurityUtils.sanitizeText(doc.getString("description"), 4000)
            val available = doc.getBoolean("available") ?: true
            val featured = doc.getBoolean("featured") ?: false
            val newArrival = doc.getBoolean("newArrival") ?: false
            val createdAt = when (val rawCreated = doc.get("createdAt")) {
                is Number -> rawCreated.toLong()
                else -> 0L
            }
            Product(
                id = doc.id,
                title = title,
                imageUrl = imageUrl,
                price = price,
                oldPrice = oldPrice,
                category = category,
                subCategory = subCategory,
                description = description,
                available = available,
                featured = featured,
                newArrival = newArrival,
                createdAt = createdAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse product ${doc.id}", e)
            null
        }
    }

    private fun parseCategory(doc: DocumentSnapshot): Category? {
        return try {
            val name = SecurityUtils.sanitizeText(doc.getString("name"), 120)
            if (name.isBlank()) return null
            val imageUrl = SecurityUtils.sanitizeImageSource(doc.getString("imageUrl"))
            val rawSubs = doc.get("subcategories") as? List<*>
            val subcategories = rawSubs?.mapNotNull { item ->
                SecurityUtils.sanitizeText(item as? String, 120).takeIf { it.isNotBlank() }
            } ?: emptyList()
            val createdAt = when (val rawCreated = doc.get("createdAt")) {
                is Number -> rawCreated.toLong()
                else -> 0L
            }
            Category(
                id = doc.id,
                name = name,
                imageUrl = imageUrl,
                subcategories = subcategories,
                createdAt = createdAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse category ${doc.id}", e)
            null
        }
    }

    private fun parseStoreSettings(doc: DocumentSnapshot): StoreSettings {
        val websiteName = SecurityUtils.sanitizeText(doc.getString("websiteName"), 120)
            .ifBlank { "Glowisticbd" }
        val description = SecurityUtils.sanitizeText(doc.getString("description"), 600)
            .ifBlank { "Your Unlimited Glow. Premium cosmetics & skincare collection in Bangladesh." }
        val tagline = SecurityUtils.sanitizeText(doc.getString("tagline"), 300)
            .ifBlank { "আপনার সীমাহীন গ্লো। বাংলাদেশের প্রিমিয়াম কসমেটিকস ও স্কিনকেয়ার কালেকশন।" }
        val whatsappNumber = SecurityUtils.sanitizePhone(doc.getString("whatsappNumber"))
            .ifBlank { "+8801779531040" }
        val deliveryCharge = SecurityUtils.sanitizePrice(doc.get("deliveryCharge"))
        val logoUrl = SecurityUtils.sanitizeImageSource(doc.getString("logoUrl"))
        val faviconUrl = SecurityUtils.sanitizeImageSource(doc.getString("faviconUrl"))
        val heroVideoUrl = SecurityUtils.sanitizeHttpsUrl(doc.getString("heroVideoUrl"))
        val rawMediaType = SecurityUtils.sanitizeText(doc.getString("heroMediaType"), 20).lowercase()
        val heroMediaType = if (rawMediaType == "image") "image" else "video"

        val heroMap = doc.get("heroProductIds") as? Map<*, *>
        val heroProductIds = HeroProductIds(
            left1 = SecurityUtils.sanitizeText(heroMap?.get("left1") as? String, 120),
            left2 = SecurityUtils.sanitizeText(heroMap?.get("left2") as? String, 120),
            right1 = SecurityUtils.sanitizeText(heroMap?.get("right1") as? String, 120),
            right2 = SecurityUtils.sanitizeText(heroMap?.get("right2") as? String, 120)
        )

        return StoreSettings(
            websiteName = websiteName,
            description = description,
            tagline = tagline,
            whatsappNumber = whatsappNumber,
            deliveryCharge = deliveryCharge,
            logoUrl = logoUrl,
            faviconUrl = faviconUrl,
            heroVideoUrl = heroVideoUrl,
            heroMediaType = heroMediaType,
            heroProductIds = heroProductIds
        )
    }

    // ==================== ADMIN AUTHENTICATION ====================

    private fun toSafeUserMessage(e: Throwable, fallback: String): String {
        val msg = e.message ?: ""
        return when {
            e is IllegalArgumentException -> msg.ifBlank { fallback }
            e is IllegalAccessException -> msg.ifBlank { "Access denied. Admin authorization required." }
            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                msg.contains("password is invalid", ignoreCase = true) ||
                msg.contains("no user record", ignoreCase = true) ->
                "Invalid admin email or password."
            msg.contains("network", ignoreCase = true) ->
                "Network error. Please check your internet connection."
            msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Permission denied by Firestore Security Rules."
            else -> fallback
        }
    }

    suspend fun signInAdmin(email: String, password: String): Result<String> {
        return try {
            val cleanEmail = email.trim()
            require(cleanEmail.contains("@") && cleanEmail.length <= 150) {
                "Please enter a valid admin email address."
            }
            require(password.isNotEmpty() && password.length <= 128) {
                "Please enter a valid password."
            }
            val result = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val user = result.user
            val uid = user?.uid ?: ""
            if (!SecurityUtils.isAuthorizedAdmin(uid)) {
                auth.signOut()
                Result.failure(
                    IllegalAccessException(
                        "Access denied: Only the configured Admin UID is authorized."
                    )
                )
            } else {
                user?.getIdToken(true)?.await()
                Result.success(uid)
            }
        } catch (e: Exception) {
            Result.failure(Exception(toSafeUserMessage(e, "Admin sign-in failed. Please verify your credentials.")))
        }
    }

    fun signOutAdmin() {
        auth.signOut()
    }

    suspend fun sendAdminPasswordResetEmail(email: String): Result<Unit> {
        return try {
            val cleanEmail = email.trim()
            require(cleanEmail.contains("@") && cleanEmail.length <= 150) {
                "Please enter your registered Admin email address first."
            }
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    toSafeUserMessage(
                        e,
                        "Failed to send password reset email. Please check the email address."
                    )
                )
            )
        }
    }

    suspend fun updateAdminPassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            requireAdminAuth()
            val user = auth.currentUser
                ?: throw IllegalAccessException("Admin session expired. Please sign in again.")
            val email = user.email
                ?: throw IllegalAccessException("No email associated with current admin session.")

            require(currentPassword.isNotEmpty() && currentPassword.length <= 128) {
                "Please enter your current password."
            }
            require(newPassword.length >= 8 && newPassword.length <= 128) {
                "New password must be between 8 and 128 characters."
            }

            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            user.getIdToken(true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    toSafeUserMessage(
                        e,
                        "Failed to update password. Please verify your current password."
                    )
                )
            )
        }
    }

    private suspend fun requireAdminAuth() {
        val user = auth.currentUser
            ?: throw IllegalAccessException("Admin session expired. Please sign in again.")
        if (!SecurityUtils.isAuthorizedAdmin(user.uid)) {
            auth.signOut()
            throw IllegalAccessException("Unauthorized admin operation blocked.")
        }
        try {
            user.getIdToken(false).await()
        } catch (_: Exception) {
            auth.signOut()
            throw IllegalAccessException("Admin session token expired. Please sign in again.")
        }
    }

    // ==================== ADMIN CRUD OPERATIONS ====================

    suspend fun saveProduct(product: Product): Result<Unit> {
        return try {
            requireAdminAuth()
            val cleanTitle = SecurityUtils.sanitizeText(product.title, 300)
            require(cleanTitle.isNotBlank()) { "Product title is required." }
            require(product.price >= 0.0 && product.price <= SecurityUtils.MAX_PRICE_BDT) {
                "Price must be between ৳0 and ৳10,000,000."
            }
            val cleanPrice = SecurityUtils.sanitizePrice(product.price)
            if (product.oldPrice != null) {
                require(product.oldPrice >= 0.0 && product.oldPrice <= SecurityUtils.MAX_PRICE_BDT) {
                    "Old price must be a valid non-negative number."
                }
            }
            val cleanImageUrl = SecurityUtils.sanitizeImageSource(product.imageUrl)
            require(cleanImageUrl.isNotBlank()) {
                "A valid HTTPS Image URL or Gallery Photo is required."
            }

            val data = hashMapOf<String, Any?>(
                "title" to cleanTitle,
                "imageUrl" to cleanImageUrl,
                "price" to cleanPrice,
                "oldPrice" to product.oldPrice?.let { SecurityUtils.sanitizePrice(it) }?.takeIf { it > 0.0 },
                "category" to SecurityUtils.sanitizeText(product.category, 120),
                "subCategory" to SecurityUtils.sanitizeText(product.subCategory, 120),
                "description" to SecurityUtils.sanitizeText(product.description, 4000),
                "available" to product.available,
                "featured" to product.featured,
                "newArrival" to product.newArrival,
                "createdAt" to if (product.createdAt > 0L) product.createdAt else System.currentTimeMillis()
            )

            if (product.id.isBlank()) {
                firestore.collection("products").add(data).await()
            } else {
                firestore.collection("products").document(product.id).set(data).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(toSafeUserMessage(e, "Failed to save product.")))
        }
    }

    suspend fun toggleProductFlag(productId: String, field: String, value: Boolean): Result<Unit> {
        return try {
            requireAdminAuth()
            require(field in setOf("available", "featured", "newArrival")) { "Invalid product field." }
            firestore.collection("products").document(productId)
                .update(field, value)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(toSafeUserMessage(e, "Failed to update product status.")))
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            requireAdminAuth()
            require(productId.isNotBlank()) { "Invalid product ID." }
            firestore.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(toSafeUserMessage(e, "Failed to delete product.")))
        }
    }

    suspend fun saveCategory(category: Category): Result<Unit> {
        return try {
            requireAdminAuth()
            val cleanName = SecurityUtils.sanitizeText(category.name, 120)
            require(cleanName.isNotBlank()) { "Category name is required." }
            if (category.imageUrl.isNotBlank()) {
                require(SecurityUtils.sanitizeImageSource(category.imageUrl).isNotBlank()) {
                    "Category image must be a valid HTTPS URL or Gallery Photo."
                }
            }
            val cleanImageUrl = SecurityUtils.sanitizeImageSource(category.imageUrl)

            val cleanSubs = category.subcategories.mapNotNull {
                SecurityUtils.sanitizeText(it, 120).takeIf { s -> s.isNotBlank() }
            }.distinct()

            val data = hashMapOf<String, Any?>(
                "name" to cleanName,
                "imageUrl" to cleanImageUrl,
                "subcategories" to cleanSubs,
                "createdAt" to if (category.createdAt > 0L) category.createdAt else System.currentTimeMillis()
            )

            if (category.id.isBlank()) {
                firestore.collection("categories").add(data).await()
            } else {
                firestore.collection("categories").document(category.id).set(data).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(toSafeUserMessage(e, "Failed to save category.")))
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            requireAdminAuth()
            require(categoryId.isNotBlank()) { "Invalid category ID." }
            firestore.collection("categories").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(toSafeUserMessage(e, "Failed to delete category.")))
        }
    }

    suspend fun saveHeroProductSlots(slots: HeroProductIds): Result<Unit> {
        return try {
            requireAdminAuth()
            val data = mapOf(
                "heroProductIds" to mapOf(
                    "left1" to SecurityUtils.sanitizeText(slots.left1, 120),
                    "left2" to SecurityUtils.sanitizeText(slots.left2, 120),
                    "right1" to SecurityUtils.sanitizeText(slots.right1, 120),
                    "right2" to SecurityUtils.sanitizeText(slots.right2, 120)
                )
            )
            firestore.collection("settings").document("store")
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(toSafeUserMessage(e, "Failed to update Hero Slots.")))
        }
    }

    suspend fun saveStoreSettings(settings: StoreSettings): Result<Unit> {
        return try {
            requireAdminAuth()
            val cleanName = SecurityUtils.sanitizeText(settings.websiteName, 120)
            require(cleanName.isNotBlank()) { "Website name is required." }
            require(settings.deliveryCharge >= 0.0 && settings.deliveryCharge <= 50_000.0) {
                "Delivery charge must be between ৳0 and ৳50,000."
            }
            if (settings.logoUrl.isNotBlank()) {
                require(SecurityUtils.sanitizeImageSource(settings.logoUrl).isNotBlank()) {
                    "Logo URL must use HTTPS or be a valid uploaded image."
                }
            }
            if (settings.faviconUrl.isNotBlank()) {
                require(SecurityUtils.sanitizeImageSource(settings.faviconUrl).isNotBlank()) {
                    "Favicon URL must use HTTPS or be a valid uploaded image."
                }
            }
            if (settings.heroVideoUrl.isNotBlank()) {
                require(SecurityUtils.sanitizeHttpsUrl(settings.heroVideoUrl).isNotBlank()) {
                    "Hero Video/Image URL must use HTTPS."
                }
            }
            val cleanPhone = SecurityUtils.sanitizePhone(settings.whatsappNumber).ifBlank { "+8801779531040" }
            val digitsOnly = cleanPhone.replace(Regex("[^0-9]"), "")
            require(SecurityUtils.isValidWhatsAppDigits(digitsOnly)) {
                "WhatsApp number must contain 10 to 15 digits."
            }

            val data = mapOf(
                "websiteName" to cleanName,
                "description" to SecurityUtils.sanitizeText(settings.description, 600),
                "tagline" to SecurityUtils.sanitizeText(settings.tagline, 300),
                "whatsappNumber" to cleanPhone,
                "deliveryCharge" to SecurityUtils.sanitizePrice(settings.deliveryCharge),
                "logoUrl" to SecurityUtils.sanitizeImageSource(settings.logoUrl),
                "faviconUrl" to SecurityUtils.sanitizeImageSource(settings.faviconUrl),
                "heroVideoUrl" to SecurityUtils.sanitizeHttpsUrl(settings.heroVideoUrl),
                "heroMediaType" to if (settings.heroMediaType == "image") "image" else "video",
                "heroProductIds" to mapOf(
                    "left1" to SecurityUtils.sanitizeText(settings.heroProductIds.left1, 120),
                    "left2" to SecurityUtils.sanitizeText(settings.heroProductIds.left2, 120),
                    "right1" to SecurityUtils.sanitizeText(settings.heroProductIds.right1, 120),
                    "right2" to SecurityUtils.sanitizeText(settings.heroProductIds.right2, 120)
                )
            )
            firestore.collection("settings").document("store")
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(toSafeUserMessage(e, "Failed to save store settings.")))
        }
    }
}
