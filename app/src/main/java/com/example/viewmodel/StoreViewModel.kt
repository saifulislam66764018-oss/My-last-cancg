package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirebaseRepository
import com.example.model.CartItem
import com.example.model.Category
import com.example.model.DEFAULT_MAIN_CATEGORIES
import com.example.model.HeroProductIds
import com.example.model.NavSection
import com.example.model.Product
import com.example.model.SecurityUtils
import com.example.model.SortOption
import com.example.model.StoreSettings
import com.example.model.formatBdt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirebaseRepository(application)
    private val cartPrefs = application.getSharedPreferences("glowistic_cart_prefs", Context.MODE_PRIVATE)

    // Real-time Firestore state flows
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(DEFAULT_MAIN_CATEGORIES)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _storeSettings = MutableStateFlow(StoreSettings())
    val storeSettings: StateFlow<StoreSettings> = _storeSettings.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Navigation & UI state
    private val _activeSection = MutableStateFlow(NavSection.HOME)
    val activeSection: StateFlow<NavSection> = _activeSection.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedSubCategory = MutableStateFlow<String?>(null)
    val selectedSubCategory: StateFlow<String?> = _selectedSubCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DEFAULT)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Modals & Drawers
    private val _selectedProductForModal = MutableStateFlow<Product?>(null)
    val selectedProductForModal: StateFlow<Product?> = _selectedProductForModal.asStateFlow()

    private val _isCartOpen = MutableStateFlow(false)
    val isCartOpen: StateFlow<Boolean> = _isCartOpen.asStateFlow()

    private val _isCheckoutOpen = MutableStateFlow(false)
    val isCheckoutOpen: StateFlow<Boolean> = _isCheckoutOpen.asStateFlow()

    // Cart items (persisted locally — no personal customer info stored)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    // Admin Auth state
    private val _currentAuthUid = MutableStateFlow<String?>(null)
    val currentAuthUid: StateFlow<String?> = _currentAuthUid.asStateFlow()

    private val _adminStatusMessage = MutableStateFlow<String?>(null)
    val adminStatusMessage: StateFlow<String?> = _adminStatusMessage.asStateFlow()

    private val _isAdminBusy = MutableStateFlow(false)
    val isAdminBusy: StateFlow<Boolean> = _isAdminBusy.asStateFlow()

    // Derived filtered and sorted products
    val filteredProducts: StateFlow<List<Product>> = combine(
        _products,
        _categories,
        _searchQuery,
        _selectedCategory,
        _selectedSubCategory
    ) { allProducts, allCategories, query, category, subCategory ->
        val trimmedQuery = query.trim().lowercase()
        val activeCatObj = allCategories.firstOrNull {
            it.name.equals(category, ignoreCase = true)
        }
        val validSubs = activeCatObj?.subcategories?.map { it.lowercase() }?.toSet() ?: emptySet()

        allProducts.filter { product ->
            val matchesQuery = trimmedQuery.isEmpty() ||
                product.title.lowercase().contains(trimmedQuery)

            val matchesCategory = if (category.isNullOrBlank()) {
                true
            } else {
                product.category.equals(category, ignoreCase = true) ||
                    (validSubs.isNotEmpty() && (
                        product.category.lowercase() in validSubs ||
                            product.subCategory.lowercase() in validSubs
                        ))
            }

            val matchesSubCategory = if (subCategory.isNullOrBlank()) {
                true
            } else {
                product.subCategory.equals(subCategory, ignoreCase = true) ||
                    product.category.equals(subCategory, ignoreCase = true)
            }

            matchesQuery && matchesCategory && matchesSubCategory
        }
    }.combine(_sortOption) { filtered, sort ->
        when (sort) {
            SortOption.DEFAULT -> filtered
            SortOption.PRICE_LOW_TO_HIGH -> filtered.sortedBy { it.price }
            SortOption.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.price }
            SortOption.NEWEST -> filtered.sortedWith(
                compareByDescending<Product> { it.newArrival }.thenByDescending { it.createdAt }
            )
            SortOption.FEATURED -> filtered.sortedWith(
                compareByDescending<Product> { it.featured }.thenByDescending { it.createdAt }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadCartFromStorage()

        viewModelScope.launch {
            repository.observeProducts().collect { list ->
                _products.value = list
                _isLoading.value = false
            }
        }

        viewModelScope.launch {
            repository.observeCategories().collect { firestoreCategories ->
                val merged = mutableListOf<Category>()
                // Ensure the 5 main categories appear first, merging subcategories from DEFAULT_MAIN_CATEGORIES
                DEFAULT_MAIN_CATEGORIES.forEach { defaultCat ->
                    val existing = firestoreCategories.firstOrNull {
                        it.id == defaultCat.id || it.name.trim() == defaultCat.name.trim()
                    }
                    if (existing != null) {
                        val combinedSubs = (defaultCat.subcategories + existing.subcategories).distinct()
                        merged.add(existing.copy(subcategories = combinedSubs))
                    } else {
                        merged.add(defaultCat)
                    }
                }
                // Append any additional custom categories from Firestore
                firestoreCategories.forEach { fsCat ->
                    val alreadyIncluded = merged.any {
                        it.id == fsCat.id || it.name.trim() == fsCat.name.trim()
                    }
                    if (!alreadyIncluded) {
                        merged.add(fsCat)
                    }
                }
                _categories.value = merged
            }
        }

        viewModelScope.launch {
            repository.observeStoreSettings().collect { settings ->
                _storeSettings.value = settings
            }
        }

        viewModelScope.launch {
            repository.observeAuthUid().collect { uid ->
                _currentAuthUid.value = uid
                if (SecurityUtils.isAuthorizedAdmin(uid)) {
                    // Automatically ensure the 5 main categories are saved in Firestore
                    DEFAULT_MAIN_CATEGORIES.forEach { mainCat ->
                        repository.saveCategory(mainCat)
                    }
                }
            }
        }
    }

    // ==================== NAVIGATION & FILTERING ====================

    fun setActiveSection(section: NavSection) {
        _activeSection.value = section
    }

    fun setSearchQuery(query: String) {
        val trimmed = query.trim().lowercase()
        if (trimmed == "/admin" ||
            trimmed == "#/admin" ||
            trimmed == "/admin panel" ||
            trimmed == "#/admin panel" ||
            trimmed == "admin panel"
        ) {
            _searchQuery.value = ""
            _activeSection.value = NavSection.ADMIN
        } else {
            _searchQuery.value = query
        }
    }

    fun setSelectedCategory(categoryName: String?) {
        val next = if (_selectedCategory.value == categoryName) null else categoryName
        _selectedCategory.value = next
        _selectedSubCategory.value = null
    }

    fun setSelectedSubCategory(subCategoryName: String?) {
        _selectedSubCategory.value =
            if (_selectedSubCategory.value == subCategoryName) null else subCategoryName
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun openProductDetails(product: Product) {
        _selectedProductForModal.value = product
    }

    fun closeProductDetails() {
        _selectedProductForModal.value = null
    }

    fun setCartOpen(open: Boolean) {
        _isCartOpen.value = open
    }

    fun setCheckoutOpen(open: Boolean) {
        _isCheckoutOpen.value = open
    }

    // ==================== CART MANAGEMENT ====================

    fun addToCart(product: Product, quantity: Int = 1) {
        if (!product.available || quantity <= 0) return
        val current = _cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.productId == product.id }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            current[existingIndex] = existing.copy(
                title = product.title,
                imageUrl = product.imageUrl,
                price = product.price,
                quantity = (existing.quantity + quantity).coerceAtMost(99)
            )
        } else {
            current.add(
                CartItem(
                    productId = product.id,
                    title = product.title,
                    imageUrl = product.imageUrl,
                    price = product.price,
                    quantity = quantity.coerceIn(1, 99)
                )
            )
        }
        _cartItems.value = current
        saveCartToStorage(current)
    }

    fun updateCartQuantity(productId: String, newQuantity: Int) {
        val current = _cartItems.value.toMutableList()
        if (newQuantity <= 0) {
            current.removeAll { it.productId == productId }
        } else {
            val idx = current.indexOfFirst { it.productId == productId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(quantity = newQuantity.coerceAtMost(99))
            }
        }
        _cartItems.value = current
        saveCartToStorage(current)
    }

    fun removeFromCart(productId: String) {
        val updated = _cartItems.value.filterNot { it.productId == productId }
        _cartItems.value = updated
        saveCartToStorage(updated)
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        saveCartToStorage(emptyList())
    }

    private fun saveCartToStorage(items: List<CartItem>) {
        try {
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("productId", item.productId)
                    put("title", item.title)
                    put("imageUrl", item.imageUrl)
                    put("price", item.price)
                    put("quantity", item.quantity)
                }
                array.put(obj)
            }
            cartPrefs.edit().putString("cart_items_json", array.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun loadCartFromStorage() {
        try {
            val raw = cartPrefs.getString("cart_items_json", null) ?: return
            val array = JSONArray(raw)
            val loaded = mutableListOf<CartItem>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = SecurityUtils.sanitizeText(obj.optString("productId"), 120)
                val title = SecurityUtils.sanitizeText(obj.optString("title"), 300)
                val imageUrl = SecurityUtils.sanitizeHttpsUrl(obj.optString("imageUrl"))
                val price = SecurityUtils.sanitizePrice(obj.optDouble("price", 0.0))
                val qty = obj.optInt("quantity", 1).coerceIn(1, 99)
                if (id.isNotBlank() && title.isNotBlank()) {
                    loaded.add(CartItem(id, title, imageUrl, price, qty))
                }
            }
            _cartItems.value = loaded
        } catch (_: Exception) {
        }
    }

    // ==================== WHATSAPP ORDER LAUNCHER ====================

    private fun getFormattedWhatsAppPhone(): String {
        val raw = _storeSettings.value.whatsappNumber.ifBlank { "+8801779531040" }
        val digitsOnly = raw.replace(Regex("[^0-9]"), "")
        val candidate = when {
            digitsOnly.startsWith("880") -> digitsOnly
            digitsOnly.startsWith("01") -> "88$digitsOnly"
            digitsOnly.isNotBlank() -> digitsOnly
            else -> "8801779531040"
        }
        return if (SecurityUtils.isValidWhatsAppDigits(candidate)) {
            candidate
        } else {
            "8801779531040"
        }
    }

    fun launchSingleProductWhatsAppOrder(context: Context, product: Product, quantity: Int = 1) {
        if (!product.available) return
        val storeName = _storeSettings.value.websiteName.ifBlank { "Glowisticbd" }
        val delivery = _storeSettings.value.deliveryCharge
        val subtotal = product.price * quantity
        val total = subtotal + delivery

        val message = buildString {
            appendLine("✨ *New Order — $storeName*")
            appendLine("────────────────────")
            appendLine("📦 *Product:* ${product.title}")
            if (product.category.isNotBlank()) {
                appendLine("🏷️ *Category:* ${product.category}")
            }
            appendLine("🔢 *Quantity:* $quantity")
            appendLine("💰 *Unit Price:* ${formatBdt(product.price)}")
            appendLine("💵 *Subtotal:* ${formatBdt(subtotal)}")
            appendLine("🚚 *Delivery Charge:* ${formatBdt(delivery)}")
            appendLine("💎 *Total Payable:* ${formatBdt(total)}")
            appendLine("────────────────────")
            append("আমি এই পণ্যটি অর্ডার করতে চাই।")
        }

        openWhatsAppUrl(context, message)
    }

    fun launchCheckoutWhatsAppOrder(
        context: Context,
        customerName: String,
        phone: String,
        address: String,
        note: String
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) return

        val cleanName = SecurityUtils.sanitizeText(customerName, 120)
        val cleanPhone = SecurityUtils.sanitizeText(phone, 40)
        val cleanAddress = SecurityUtils.sanitizeText(address, 500)
        val cleanNote = SecurityUtils.sanitizeText(note, 500)

        val storeName = _storeSettings.value.websiteName.ifBlank { "Glowisticbd" }
        val delivery = _storeSettings.value.deliveryCharge
        val subtotal = items.sumOf { it.price * it.quantity }
        val total = subtotal + delivery

        val message = buildString {
            appendLine("✨ *New Order — $storeName*")
            appendLine("────────────────────")
            appendLine("👤 *Customer Name:* $cleanName")
            appendLine("📞 *Phone:* $cleanPhone")
            appendLine("📍 *Delivery Address:* $cleanAddress")
            appendLine("────────────────────")
            appendLine("🛍️ *Ordered Products:*")
            items.forEachIndexed { index, item ->
                val lineTotal = item.price * item.quantity
                appendLine(
                    "${index + 1}. ${item.title} — Qty: ${item.quantity} × ${formatBdt(item.price)} = ${formatBdt(lineTotal)}"
                )
            }
            appendLine("────────────────────")
            appendLine("💵 *Subtotal:* ${formatBdt(subtotal)}")
            appendLine("🚚 *Delivery Charge:* ${formatBdt(delivery)}")
            appendLine("💎 *Total:* ${formatBdt(total)}")
            if (cleanNote.isNotBlank()) {
                appendLine("📝 *Note:* $cleanNote")
            }
        }

        openWhatsAppUrl(context, message)
        clearCart()
        _isCheckoutOpen.value = false
        _isCartOpen.value = false
    }

    private fun openWhatsAppUrl(context: Context, message: String) {
        val phone = getFormattedWhatsAppPhone()
        val encoded = Uri.encode(message)
        val url = "https://wa.me/$phone?text=$encoded"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    // ==================== ADMIN OPERATIONS ====================

    fun clearAdminStatusMessage() {
        _adminStatusMessage.value = null
    }

    fun signInAdmin(email: String, password: String) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            _adminStatusMessage.value = null
            val res = repository.signInAdmin(email, password)
            _isAdminBusy.value = false
            res.onSuccess {
                _adminStatusMessage.value = "Admin authenticated successfully."
            }.onFailure { err ->
                _adminStatusMessage.value = err.message ?: "Authentication failed."
            }
        }
    }

    fun signOutAdmin() {
        repository.signOutAdmin()
        _adminStatusMessage.value = "Signed out of Admin Panel."
    }

    fun sendAdminPasswordResetEmail(email: String) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            _adminStatusMessage.value = null
            val res = repository.sendAdminPasswordResetEmail(email)
            _isAdminBusy.value = false
            res.onSuccess {
                _adminStatusMessage.value =
                    "Password reset email sent! Please check your inbox (and spam folder)."
            }.onFailure { err ->
                _adminStatusMessage.value = err.message ?: "Failed to send password reset email."
            }
        }
    }

    fun updateAdminPassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit = {}
    ) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            _adminStatusMessage.value = null
            val res = repository.updateAdminPassword(currentPassword, newPassword)
            _isAdminBusy.value = false
            res.onSuccess {
                _adminStatusMessage.value = "Admin password updated successfully."
                onSuccess()
            }.onFailure { err ->
                _adminStatusMessage.value = err.message ?: "Failed to update password."
            }
        }
    }

    fun saveProduct(product: Product, onSuccess: () -> Unit = {}) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            val res = repository.saveProduct(product)
            _isAdminBusy.value = false
            res.onSuccess {
                _adminStatusMessage.value = "Product saved to Firestore."
                onSuccess()
            }.onFailure { err ->
                _adminStatusMessage.value = err.message ?: "Failed to save product."
            }
        }
    }

    fun toggleProductFlag(productId: String, field: String, value: Boolean) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            val res = repository.toggleProductFlag(productId, field, value)
            res.onFailure { err ->
                _adminStatusMessage.value = err.message ?: "Failed to update product."
            }
        }
    }

    fun deleteProduct(productId: String) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            val res = repository.deleteProduct(productId)
            _isAdminBusy.value = false
            res.onSuccess {
                _adminStatusMessage.value = "Product deleted from Firestore."
            }.onFailure { err ->
                _adminStatusMessage.value = err.message ?: "Failed to delete product."
            }
        }
    }

    fun saveCategory(category: Category, onSuccess: () -> Unit = {}) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            val res = repository.saveCategory(category)
            _isAdminBusy.value = false
            res.onSuccess {
                _adminStatusMessage.value = "Category saved to Firestore."
                onSuccess()
            }.onFailure { err ->
                _adminStatusMessage.value = err.message ?: "Failed to save category."
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            val res = repository.deleteCategory(categoryId)
            _isAdminBusy.value = false
            res.onSuccess {
                _adminStatusMessage.value = "Category deleted."
            }.onFailure { err ->
                _adminStatusMessage.value = err.message ?: "Failed to delete category."
            }
        }
    }

    fun saveHeroSlots(slots: HeroProductIds) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            val res = repository.saveHeroProductSlots(slots)
            _isAdminBusy.value = false
            res.onSuccess {
                _adminStatusMessage.value = "Hero Product Slots updated in real time."
            }.onFailure { err ->
                _adminStatusMessage.value = err.message ?: "Failed to update Hero Slots."
            }
        }
    }

    fun saveStoreSettings(settings: StoreSettings) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            val res = repository.saveStoreSettings(settings)
            _isAdminBusy.value = false
            res.onSuccess {
                _adminStatusMessage.value = "Store settings saved to Firestore."
            }.onFailure { err ->
                _adminStatusMessage.value = err.message ?: "Failed to save settings."
            }
        }
    }
}
