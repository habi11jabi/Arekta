package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Employee
import com.example.data.model.Expense
import com.example.data.model.Product
import com.example.data.model.ProductStatus
import com.example.data.model.SalaryPayment
import com.example.data.model.Sale
import com.example.data.model.ShopSettings
import com.example.data.model.TransactionType
import android.content.Context
import com.example.data.repository.InventoryRepository
import com.example.data.repository.SettingsManager
import com.example.util.GoogleDriveSyncManager
import com.example.util.ParsedBackupData
import com.example.data.model.AppUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

enum class AppTab {
    DASHBOARD,
    STOCK,
    EXPENSES,
    EMPLOYEES,
    SALES,
    REPORTS,
    SETTINGS
}

data class DashboardMetrics(
    val inStockCount: Int = 0,
    val totalStockValue: Double = 0.0,
    val totalTargetValue: Double = 0.0,
    val totalSalesCount: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalDue: Double = 0.0,
    val todayNetBalance: Double = 0.0,
    val totalStaffCount: Int = 0
)

data class CashbookMetrics(
    val todayIncome: Double = 0.0,
    val todayExpense: Double = 0.0,
    val todayBalance: Double = 0.0,
    val periodIncome: Double = 0.0,
    val periodExpense: Double = 0.0,
    val periodBalance: Double = 0.0
)

data class MonthlyReport(
    val periodName: String,
    val unitsSold: Int,
    val totalRevenue: Double,
    val totalCost: Double,
    val totalProfit: Double,
    val totalPaid: Double,
    val totalDue: Double,
    val profitMarginPercent: Double,
    val topSellingModels: List<Pair<String, Int>>
)

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InventoryRepository
    private val settingsManager: SettingsManager = SettingsManager(application)

    val allProducts: StateFlow<List<Product>>
    val allSales: StateFlow<List<Sale>>
    val allExpenses: StateFlow<List<Expense>>
    val allEmployees: StateFlow<List<Employee>>
    val allSalaryPayments: StateFlow<List<SalaryPayment>>
    val allUsers: StateFlow<List<AppUser>>
    val returnedProducts: StateFlow<List<Product>>
    val shopSettings: StateFlow<ShopSettings> = settingsManager.settings

    // Authentication & User Session State
    private val loginPrefs = application.getSharedPreferences("apple_area_bd_login_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Navigation Tab
    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Search and Filters for Stock
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("IN_STOCK")
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    private val _selectedBrandFilter = MutableStateFlow("ALL")
    val selectedBrandFilter: StateFlow<String> = _selectedBrandFilter.asStateFlow()

    // Expense Period Filter: "TODAY", "THIS_WEEK", "THIS_MONTH", "ALL"
    private val _expenseFilterPeriod = MutableStateFlow("THIS_MONTH")
    val expenseFilterPeriod: StateFlow<String> = _expenseFilterPeriod.asStateFlow()

    // Add / Edit Product Dialog / Screen State
    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen.asStateFlow()

    private val _isBatchMode = MutableStateFlow(false)
    val isBatchMode: StateFlow<Boolean> = _isBatchMode.asStateFlow()

    private val _editingProduct = MutableStateFlow<Product?>(null)
    val editingProduct: StateFlow<Product?> = _editingProduct.asStateFlow()

    // Product Return Dialog State
    private val _productToReturn = MutableStateFlow<Product?>(null)
    val productToReturn: StateFlow<Product?> = _productToReturn.asStateFlow()

    // User Management Dialog State (for Admin)
    private val _isUserManagementOpen = MutableStateFlow(false)
    val isUserManagementOpen: StateFlow<Boolean> = _isUserManagementOpen.asStateFlow()

    // Google Drive Sync State
    private val _isDriveSyncing = MutableStateFlow(false)
    val isDriveSyncing: StateFlow<Boolean> = _isDriveSyncing.asStateFlow()

    private val _lastDriveSyncTime = MutableStateFlow(GoogleDriveSyncManager.getLastSyncTime(application))
    val lastDriveSyncTime: StateFlow<Long> = _lastDriveSyncTime.asStateFlow()

    // Sale Checkout Dialog State
    private val _productToSell = MutableStateFlow<Product?>(null)
    val productToSell: StateFlow<Product?> = _productToSell.asStateFlow()

    // Cash Memo / Invoice Dialog State
    private val _activeInvoiceSale = MutableStateFlow<Sale?>(null)
    val activeInvoiceSale: StateFlow<Sale?> = _activeInvoiceSale.asStateFlow()

    // Sale Edit Dialog State
    private val _editingSale = MutableStateFlow<Sale?>(null)
    val editingSale: StateFlow<Sale?> = _editingSale.asStateFlow()

    // Scanner Dialog State
    private val _isScannerOpen = MutableStateFlow(false)
    val isScannerOpen: StateFlow<Boolean> = _isScannerOpen.asStateFlow()

    // Due Payment Update Dialog
    private val _saleForDuePayment = MutableStateFlow<Sale?>(null)
    val saleForDuePayment: StateFlow<Sale?> = _saleForDuePayment.asStateFlow()

    // Report Period: "THIS_MONTH", "LAST_MONTH", "ALL_TIME"
    private val _reportPeriod = MutableStateFlow("THIS_MONTH")
    val reportPeriod: StateFlow<String> = _reportPeriod.asStateFlow()

    // User Feedback Message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = InventoryRepository(
            application,
            db.productDao(),
            db.saleDao(),
            db.expenseDao(),
            db.employeeDao(),
            db.userDao()
        )

        allProducts = repository.allProducts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        returnedProducts = repository.returnedProducts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allSales = repository.allSales.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allExpenses = repository.allExpenses.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allEmployees = repository.allEmployees.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allSalaryPayments = repository.allSalaryPayments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allUsers = repository.allUsers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.seedAdminUserIfEmpty()
            repository.seedInitialDataIfEmpty()

            // Restore login session if remembered
            val savedUser = loginPrefs.getString("logged_in_username", null)
            if (!savedUser.isNullOrBlank()) {
                val user = repository.getUserByUsername(savedUser)
                if (user != null && user.isActive) {
                    _currentUser.value = user
                    _isLoggedIn.value = true
                }
            }
        }
    }

    // Filtered Products (Supports In-Stock, Returned, and All with Search & Brand filter)
    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        _searchQuery,
        _selectedBrandFilter,
        _selectedStatusFilter
    ) { products, query, brand, statusFilter ->
        products.filter { product ->
            val matchesStatus = when (statusFilter) {
                "IN_STOCK" -> product.status == ProductStatus.IN_STOCK.name
                "RETURNED" -> product.status == ProductStatus.RETURNED.name
                "ALL" -> product.status != ProductStatus.SOLD.name
                else -> product.status == statusFilter
            }
            val matchesQuery = query.isBlank() ||
                    product.model.contains(query, ignoreCase = true) ||
                    product.company.contains(query, ignoreCase = true) ||
                    product.variant.contains(query, ignoreCase = true) ||
                    product.imei.contains(query, ignoreCase = true) ||
                    product.color.contains(query, ignoreCase = true) ||
                    product.supplier.contains(query, ignoreCase = true)

            val matchesBrand = if (brand == "ALL") true else product.company.equals(brand, ignoreCase = true)

            matchesStatus && matchesQuery && matchesBrand
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Expenses by selected period
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        allExpenses,
        _expenseFilterPeriod
    ) { expenses, period ->
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()

        when (period) {
            "TODAY" -> {
                cal.timeInMillis = now
                val currentDay = cal.get(Calendar.DAY_OF_YEAR)
                val currentYear = cal.get(Calendar.YEAR)
                expenses.filter {
                    cal.timeInMillis = it.dateMillis
                    cal.get(Calendar.DAY_OF_YEAR) == currentDay && cal.get(Calendar.YEAR) == currentYear
                }
            }
            "THIS_WEEK" -> {
                cal.timeInMillis = now
                val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
                val currentYear = cal.get(Calendar.YEAR)
                expenses.filter {
                    cal.timeInMillis = it.dateMillis
                    cal.get(Calendar.WEEK_OF_YEAR) == currentWeek && cal.get(Calendar.YEAR) == currentYear
                }
            }
            "THIS_MONTH" -> {
                cal.timeInMillis = now
                val currentMonth = cal.get(Calendar.MONTH)
                val currentYear = cal.get(Calendar.YEAR)
                expenses.filter {
                    cal.timeInMillis = it.dateMillis
                    cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                }
            }
            else -> expenses
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cashbook metrics calculation
    val cashbookMetrics: StateFlow<CashbookMetrics> = combine(
        allExpenses,
        allSales,
        _expenseFilterPeriod
    ) { expenses, sales, period ->
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        cal.timeInMillis = now
        val todayDay = cal.get(Calendar.DAY_OF_YEAR)
        val todayYear = cal.get(Calendar.YEAR)

        // Today calculations
        val todaySalesTotal = sales.filter {
            cal.timeInMillis = it.soldAt
            cal.get(Calendar.DAY_OF_YEAR) == todayDay && cal.get(Calendar.YEAR) == todayYear
        }.sumOf { it.paidAmount }

        val todayExtraIncome = expenses.filter {
            it.type == TransactionType.INCOME.name && run {
                cal.timeInMillis = it.dateMillis
                cal.get(Calendar.DAY_OF_YEAR) == todayDay && cal.get(Calendar.YEAR) == todayYear
            }
        }.sumOf { it.amount }

        val todayTotalIncome = todaySalesTotal + todayExtraIncome

        val todayTotalExpense = expenses.filter {
            it.type == TransactionType.EXPENSE.name && run {
                cal.timeInMillis = it.dateMillis
                cal.get(Calendar.DAY_OF_YEAR) == todayDay && cal.get(Calendar.YEAR) == todayYear
            }
        }.sumOf { it.amount }

        val todayBalance = todayTotalIncome - todayTotalExpense

        // Period calculations
        val periodExpenses = when (period) {
            "TODAY" -> expenses.filter {
                cal.timeInMillis = it.dateMillis
                cal.get(Calendar.DAY_OF_YEAR) == todayDay && cal.get(Calendar.YEAR) == todayYear
            }
            "THIS_WEEK" -> {
                cal.timeInMillis = now
                val week = cal.get(Calendar.WEEK_OF_YEAR)
                val year = cal.get(Calendar.YEAR)
                expenses.filter {
                    cal.timeInMillis = it.dateMillis
                    cal.get(Calendar.WEEK_OF_YEAR) == week && cal.get(Calendar.YEAR) == year
                }
            }
            "THIS_MONTH" -> {
                cal.timeInMillis = now
                val month = cal.get(Calendar.MONTH)
                val year = cal.get(Calendar.YEAR)
                expenses.filter {
                    cal.timeInMillis = it.dateMillis
                    cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
                }
            }
            else -> expenses
        }

        val periodSales = when (period) {
            "TODAY" -> sales.filter {
                cal.timeInMillis = it.soldAt
                cal.get(Calendar.DAY_OF_YEAR) == todayDay && cal.get(Calendar.YEAR) == todayYear
            }
            "THIS_WEEK" -> {
                cal.timeInMillis = now
                val week = cal.get(Calendar.WEEK_OF_YEAR)
                val year = cal.get(Calendar.YEAR)
                sales.filter {
                    cal.timeInMillis = it.soldAt
                    cal.get(Calendar.WEEK_OF_YEAR) == week && cal.get(Calendar.YEAR) == year
                }
            }
            "THIS_MONTH" -> {
                cal.timeInMillis = now
                val month = cal.get(Calendar.MONTH)
                val year = cal.get(Calendar.YEAR)
                sales.filter {
                    cal.timeInMillis = it.soldAt
                    cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
                }
            }
            else -> sales
        }

        val pIncome = periodSales.sumOf { it.paidAmount } + periodExpenses.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val pExpense = periodExpenses.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val pBalance = pIncome - pExpense

        CashbookMetrics(
            todayIncome = todayTotalIncome,
            todayExpense = todayTotalExpense,
            todayBalance = todayBalance,
            periodIncome = pIncome,
            periodExpense = pExpense,
            periodBalance = pBalance
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CashbookMetrics())

    // Metrics for Dashboard
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allProducts,
        allSales,
        cashbookMetrics,
        allEmployees
    ) { products, sales, cash, employees ->
        val inStock = products.filter { it.status == ProductStatus.IN_STOCK.name }
        val inStockCount = inStock.size
        val stockVal = inStock.sumOf { it.purchasePrice }
        val targetVal = inStock.sumOf { it.targetSalePrice }

        val totalSalesCount = sales.size
        val totalRevenue = sales.sumOf { it.salePrice }
        val totalProfit = sales.sumOf { it.profit }
        val totalDue = sales.sumOf { it.dueAmount }

        DashboardMetrics(
            inStockCount = inStockCount,
            totalStockValue = stockVal,
            totalTargetValue = targetVal,
            totalSalesCount = totalSalesCount,
            totalRevenue = totalRevenue,
            totalProfit = totalProfit,
            totalDue = totalDue,
            todayNetBalance = cash.todayBalance,
            totalStaffCount = employees.count { it.isActive }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Monthly Report calculation
    val reportData: StateFlow<MonthlyReport> = combine(
        allSales,
        _reportPeriod
    ) { sales, period ->
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()

        val filteredSales = when (period) {
            "THIS_MONTH" -> {
                cal.timeInMillis = now
                val currentMonth = cal.get(Calendar.MONTH)
                val currentYear = cal.get(Calendar.YEAR)
                sales.filter {
                    cal.timeInMillis = it.soldAt
                    cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                }
            }
            "LAST_MONTH" -> {
                cal.timeInMillis = now
                cal.add(Calendar.MONTH, -1)
                val targetMonth = cal.get(Calendar.MONTH)
                val targetYear = cal.get(Calendar.YEAR)
                sales.filter {
                    cal.timeInMillis = it.soldAt
                    cal.get(Calendar.MONTH) == targetMonth && cal.get(Calendar.YEAR) == targetYear
                }
            }
            else -> sales
        }

        val units = filteredSales.size
        val revenue = filteredSales.sumOf { it.salePrice }
        val cost = filteredSales.sumOf { it.costPrice }
        val profit = filteredSales.sumOf { it.profit }
        val paid = filteredSales.sumOf { it.paidAmount }
        val due = filteredSales.sumOf { it.dueAmount }
        val margin = if (revenue > 0) (profit / revenue) * 100 else 0.0

        val topModels = filteredSales
            .groupBy { it.productDetails.substringBefore("(") }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        val periodName = when (period) {
            "THIS_MONTH" -> "চলতি মাস"
            "LAST_MONTH" -> "গত মাস"
            else -> "সব সময়"
        }

        MonthlyReport(
            periodName = periodName,
            unitsSold = units,
            totalRevenue = revenue,
            totalCost = cost,
            totalProfit = profit,
            totalPaid = paid,
            totalDue = due,
            profitMarginPercent = margin,
            topSellingModels = topModels
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyReport("চলতি মাস", 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList())
    )

    // Navigation and Tab controls
    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    fun setBrandFilter(brand: String) {
        _selectedBrandFilter.value = brand
    }

    fun setReportPeriod(period: String) {
        _reportPeriod.value = period
    }

    fun setExpenseFilterPeriod(period: String) {
        _expenseFilterPeriod.value = period
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // Add / Edit Product Actions
    fun openAddProduct(batchMode: Boolean = false) {
        _editingProduct.value = null
        _isBatchMode.value = batchMode
        _isAddEditOpen.value = true
    }

    fun openEditProduct(product: Product) {
        _editingProduct.value = product
        _isBatchMode.value = false
        _isAddEditOpen.value = true
    }

    fun closeAddEdit() {
        _isAddEditOpen.value = false
        _isBatchMode.value = false
        _editingProduct.value = null
    }

    fun saveProduct(
        company: String,
        model: String,
        variant: String,
        color: String,
        batteryHealth: String,
        boxCondition: String,
        supplier: String,
        purchasePrice: Double,
        targetSalePrice: Double,
        imei: String,
        notes: String
    ) {
        viewModelScope.launch {
            val current = _editingProduct.value
            if (current != null) {
                val updated = current.copy(
                    company = company.trim(),
                    model = model.trim(),
                    variant = variant.trim(),
                    color = color.trim(),
                    batteryHealth = batteryHealth.trim(),
                    boxCondition = boxCondition.trim(),
                    supplier = supplier.trim(),
                    purchasePrice = purchasePrice,
                    targetSalePrice = targetSalePrice,
                    imei = imei.trim(),
                    notes = notes.trim()
                )
                repository.updateProduct(updated)
                triggerAutoCloudBackup()
                _snackbarMessage.value = "${updated.model} আপডেট করা হয়েছে!"
            } else {
                val newProduct = Product(
                    company = company.trim(),
                    model = model.trim(),
                    variant = variant.trim(),
                    color = color.trim(),
                    batteryHealth = batteryHealth.trim(),
                    boxCondition = boxCondition.trim(),
                    supplier = supplier.trim(),
                    purchasePrice = purchasePrice,
                    targetSalePrice = targetSalePrice,
                    imei = imei.trim(),
                    notes = notes.trim(),
                    status = ProductStatus.IN_STOCK.name
                )
                repository.addProduct(newProduct)
                triggerAutoCloudBackup()
                _snackbarMessage.value = "${newProduct.model} স্টকে সফলভাবে যুক্ত হয়েছে!"
            }
            closeAddEdit()
        }
    }

    fun saveBatchProducts(
        company: String,
        model: String,
        variant: String,
        color: String,
        batteryHealth: String,
        boxCondition: String,
        supplier: String,
        purchasePrice: Double,
        targetSalePrice: Double,
        imeis: List<String>,
        notes: String
    ) {
        viewModelScope.launch {
            val products = imeis.mapIndexed { index, imeiItem ->
                Product(
                    company = company.trim(),
                    model = model.trim(),
                    variant = variant.trim(),
                    color = color.trim(),
                    batteryHealth = batteryHealth.trim(),
                    boxCondition = boxCondition.trim(),
                    supplier = supplier.trim(),
                    purchasePrice = purchasePrice,
                    targetSalePrice = targetSalePrice,
                    imei = imeiItem.trim(),
                    notes = if (imeis.size > 1) "$notes (Item #${index + 1} of ${imeis.size})" else notes,
                    status = ProductStatus.IN_STOCK.name
                )
            }
            repository.addProducts(products)
            triggerAutoCloudBackup()
            _snackbarMessage.value = "একসাথে ${products.size}টি $model সফলভাবে স্টকে যুক্ত হয়েছে!"
            closeAddEdit()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            triggerAutoCloudBackup()
            _snackbarMessage.value = "${product.model} মুছে ফেলা হয়েছে।"
        }
    }

    // Sale & Checkout Actions
    fun openSaleCheckout(product: Product) {
        _productToSell.value = product
    }

    fun closeSaleCheckout() {
        _productToSell.value = null
    }

    fun completeSale(
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        salePrice: Double,
        paidAmount: Double,
        paymentMethod: String,
        warrantyInfo: String,
        remarks: String
    ) {
        val product = _productToSell.value ?: return
        viewModelScope.launch {
            val saleId = repository.recordSale(
                product = product,
                customerName = customerName.trim(),
                customerPhone = customerPhone.trim(),
                customerAddress = customerAddress.trim(),
                salePrice = salePrice,
                paidAmount = paidAmount,
                paymentMethod = paymentMethod,
                warrantyInfo = warrantyInfo,
                remarks = remarks
            )
            _productToSell.value = null
            _snackbarMessage.value = "${product.model} বিক্রি সফল হয়েছে! ক্যাশমেমো তৈরি হয়েছে।"

            val sale = Sale(
                id = saleId,
                productId = product.id,
                invoiceNumber = "AABD-" + (System.currentTimeMillis() % 1000000).toString(),
                productDetails = "${product.company} ${product.model} (${product.variant}, ${product.color})",
                imei = product.imei,
                batteryHealth = product.batteryHealth,
                boxCondition = product.boxCondition,
                customerName = customerName.trim(),
                customerPhone = customerPhone.trim(),
                customerAddress = customerAddress.trim(),
                costPrice = product.purchasePrice,
                salePrice = salePrice,
                paidAmount = paidAmount,
                dueAmount = (salePrice - paidAmount).coerceAtLeast(0.0),
                paymentMethod = paymentMethod,
                warrantyInfo = warrantyInfo,
                soldAt = System.currentTimeMillis(),
                remarks = remarks
            )
            _activeInvoiceSale.value = sale
            triggerAutoCloudBackup()
        }
    }

    // Invoice View / Dialog
    fun openInvoice(sale: Sale) {
        _activeInvoiceSale.value = sale
    }

    fun closeInvoice() {
        _activeInvoiceSale.value = null
    }

    // Edit Sale / Memo
    fun openEditSale(sale: Sale) {
        _editingSale.value = sale
    }

    fun closeEditSale() {
        _editingSale.value = null
    }

    fun saveEditedSale(sale: Sale) {
        viewModelScope.launch {
            repository.updateSale(sale)
            _editingSale.value = null
            if (_activeInvoiceSale.value?.id == sale.id) {
                _activeInvoiceSale.value = sale
            }
            _snackbarMessage.value = "বিক্রয় মেমো সফলভাবে আপডেট করা হয়েছে!"
        }
    }

    fun deleteSale(sale: Sale, restoreStock: Boolean) {
        viewModelScope.launch {
            repository.deleteSale(sale, restoreStock)
            _editingSale.value = null
            _activeInvoiceSale.value = null
            _snackbarMessage.value = if (restoreStock) "মেমো মুছে ডিভাইসটি স্টকে ফেরত আনা হয়েছে।" else "মেমো মুছে ফেলা হয়েছে।"
        }
    }

    // Due Payment Update
    fun openDuePayment(sale: Sale) {
        _saleForDuePayment.value = sale
    }

    fun closeDuePayment() {
        _saleForDuePayment.value = null
    }

    fun recordDuePayment(sale: Sale, additionalPayment: Double) {
        viewModelScope.launch {
            val newPaid = sale.paidAmount + additionalPayment
            val newDue = (sale.dueAmount - additionalPayment).coerceAtLeast(0.0)
            repository.updateDuePayment(sale.id, newPaid, newDue)
            _saleForDuePayment.value = null
            _snackbarMessage.value = "৳ ${formatBdt(additionalPayment)} বকেয়া জমা সফল হয়েছে!"
        }
    }

    // Expense & Income Actions
    fun addExpense(
        type: String,
        category: String,
        amount: Double,
        note: String,
        paymentMethod: String,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val item = Expense(
                type = type,
                category = category.trim(),
                amount = amount,
                note = note.trim(),
                paymentMethod = paymentMethod,
                dateMillis = dateMillis
            )
            repository.addExpense(item)
            val label = if (type == TransactionType.EXPENSE.name) "খরচ" else "আয়"
            _snackbarMessage.value = "৳ ${formatBdt(amount)} $label সফলভাবে যুক্ত হয়েছে!"
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _snackbarMessage.value = "হিসাব এন্ট্রি মুছে ফেলা হয়েছে।"
        }
    }

    // Employee & Salary Actions
    fun addEmployee(
        name: String,
        designation: String,
        phone: String,
        salary: Double,
        address: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val emp = Employee(
                name = name.trim(),
                designation = designation.trim(),
                phone = phone.trim(),
                monthlySalary = salary,
                address = address.trim(),
                notes = notes.trim(),
                joiningDateMillis = System.currentTimeMillis(),
                isActive = true
            )
            repository.addEmployee(emp)
            _snackbarMessage.value = "${emp.name} কে কর্মচারী হিসেবে যুক্ত করা হয়েছে!"
        }
    }

    fun updateEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.updateEmployee(employee)
            _snackbarMessage.value = "${employee.name} এর তথ্য আপডেট করা হয়েছে!"
        }
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.deleteEmployee(employee)
            _snackbarMessage.value = "${employee.name} কে মুছে ফেলা হয়েছে।"
        }
    }

    fun recordSalaryPayment(
        employee: Employee,
        monthYear: String,
        amountPaid: Double,
        paymentType: String,
        paymentMethod: String,
        note: String,
        recordAsShopExpense: Boolean = true
    ) {
        viewModelScope.launch {
            val payment = SalaryPayment(
                employeeId = employee.id,
                employeeName = employee.name,
                monthYear = monthYear.trim(),
                amountPaid = amountPaid,
                paymentDateMillis = System.currentTimeMillis(),
                paymentMethod = paymentMethod,
                paymentType = paymentType,
                note = note.trim()
            )
            repository.recordSalaryPayment(payment, recordAsShopExpense)
            _snackbarMessage.value = "${employee.name} কে ৳ ${formatBdt(amountPaid)} বেতন পরিশোধ সফল হয়েছে!"
        }
    }

    fun deleteSalaryPayment(payment: SalaryPayment) {
        viewModelScope.launch {
            repository.deleteSalaryPayment(payment)
            _snackbarMessage.value = "বেতন পরিশোধের রেকর্ড মুছে ফেলা হয়েছে।"
        }
    }

    // Settings Customization
    fun updateShopSettings(settings: ShopSettings) {
        settingsManager.saveSettings(settings)
        _snackbarMessage.value = "দোকান ও মেমো সেটিংস সংরক্ষিত হয়েছে!"
    }

    fun resetShopSettings() {
        settingsManager.resetToDefaults()
        _snackbarMessage.value = "ডিফল্ট সেটিংস সফলভাবে রিস্টোর হয়েছে!"
    }

    fun clearAllDatabaseData() {
        viewModelScope.launch {
            repository.clearAllData()
            _snackbarMessage.value = "সকল স্টক, বিক্রি, খরচ এবং কর্মচারীর তথ্য মুছে ফেলা হয়েছে।"
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            repository.seedDemoData()
            val current = shopSettings.value
            val demoSettings = current.copy(
                shopName = "APPLE AREA BD",
                shopTagline = "Buy Sell & Exchange",
                shopPhone = "01930833375, 01758596640",
                shopAddress = "Shop No # 100, Basement - 1, Bashundhara City Shopping Mall, Panthapath, Dhaka- 1205"
            )
            settingsManager.saveSettings(demoSettings)
            _snackbarMessage.value = "APPLE AREA BD এর ডেমো ডাটা এবং শপ ইনফো সফলভাবে লোড হয়েছে।"
        }
    }

    // Google Drive Synchronization & Automated Cloud Backup
    fun getFullBackupJson(): String {
        return GoogleDriveSyncManager.createFullBackupJson(
            products = allProducts.value,
            sales = allSales.value,
            expenses = allExpenses.value,
            employees = allEmployees.value,
            salaryPayments = allSalaryPayments.value,
            users = allUsers.value,
            settings = shopSettings.value
        )
    }

    /**
     * Automatically saves a complete snapshot of all shop data in the background.
     */
    fun triggerAutoCloudBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = getFullBackupJson()
                GoogleDriveSyncManager.saveAutoBackupSnapshot(getApplication(), json)
                _lastDriveSyncTime.value = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncAllDataToGoogleDrive(context: Context) {
        viewModelScope.launch {
            _isDriveSyncing.value = true
            val json = getFullBackupJson()
            val success = GoogleDriveSyncManager.shareBackupFileToDrive(context, json)
            _isDriveSyncing.value = false
            if (success) {
                val now = System.currentTimeMillis()
                _lastDriveSyncTime.value = now
                _snackbarMessage.value = "গুগল ড্রাইভ ব্যাকআপ ফাইল সফলভাবে তৈরি ও পাঠানো হয়েছে!"
            } else {
                _snackbarMessage.value = "গুগল ড্রাইভে সিঙ্ক করতে সমস্যা হয়েছে।"
            }
        }
    }

    fun restoreFromDriveBackup(backupData: ParsedBackupData) {
        viewModelScope.launch {
            repository.restoreDatabase(
                products = backupData.products,
                sales = backupData.sales,
                expenses = backupData.expenses,
                employees = backupData.employees,
                salaryPayments = backupData.salaryPayments,
                users = backupData.users
            )
            backupData.shopSettings?.let {
                settingsManager.saveSettings(it)
            }
            triggerAutoCloudBackup()
            _snackbarMessage.value = "গুগল ড্রাইভ ব্যাকআপ থেকে ${backupData.products.size}টি প্রোডাক্ট ও সকল ডাটা সফলভাবে রিস্টোর হয়েছে!"
        }
    }

    // Product Return Management (স্টক লিস্ট থেকে প্রোডাক্ট রিটার্ন)
    fun openReturnProduct(product: Product) {
        _productToReturn.value = product
    }

    fun closeReturnProduct() {
        _productToReturn.value = null
    }

    fun confirmReturnProduct(
        productId: Long,
        reason: String,
        refundAmount: Double = 0.0,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.returnProduct(productId, reason, refundAmount, notes)
            closeReturnProduct()
            triggerAutoCloudBackup()
            _snackbarMessage.value = "প্রোডাক্টটি সফলভাবে রিটার্ন হিসেবে রেকর্ড করা হয়েছে!"
        }
    }

    fun restoreReturnedProductToStock(product: Product) {
        viewModelScope.launch {
            val restored = product.copy(
                status = ProductStatus.IN_STOCK.name,
                returnReason = "",
                returnedAt = null,
                returnRefundAmount = 0.0,
                returnNotes = ""
            )
            repository.updateProduct(restored)
            triggerAutoCloudBackup()
            _snackbarMessage.value = "${product.model} পুনরায় স্টকে সক্রিয় করা হয়েছে!"
        }
    }

    // User Authentication & Management (Conock / 1219 default admin)
    fun login(username: String, password: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _loginError.value = null
            val trimmedUser = username.trim()
            val trimmedPass = password.trim()
            if (trimmedUser.isBlank() || trimmedPass.isBlank()) {
                _loginError.value = "ইউজার নেম ও পাসওয়ার্ড উভয়ই লিখুন।"
                onResult(false)
                return@launch
            }
            val user = repository.authenticate(trimmedUser, trimmedPass)
            if (user != null) {
                _currentUser.value = user
                _isLoggedIn.value = true
                loginPrefs.edit().putString("logged_in_username", user.username).apply()
                _snackbarMessage.value = "স্বাগতম, ${user.fullName}!"
                onResult(true)
            } else {
                _loginError.value = "ভুল ইউজার নেম বা পাসওয়ার্ড! পুনরায় চেক করুন।"
                onResult(false)
            }
        }
    }

    fun logout() {
        loginPrefs.edit().remove("logged_in_username").apply()
        _currentUser.value = null
        _isLoggedIn.value = false
        _snackbarMessage.value = "লগআউট সফল হয়েছে।"
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun openUserManagement() {
        _isUserManagementOpen.value = true
    }

    fun closeUserManagement() {
        _isUserManagementOpen.value = false
    }

    fun addUser(fullName: String, username: String, password: String, role: String, phone: String) {
        viewModelScope.launch {
            val trimmedUser = username.trim()
            val trimmedPass = password.trim()
            val trimmedName = fullName.trim()

            if (trimmedUser.isBlank() || trimmedPass.isBlank() || trimmedName.isBlank()) {
                _snackbarMessage.value = "ইউজার তৈরির জন্য নাম, ইউজারনেম ও পাসওয়ার্ড আবশ্যক।"
                return@launch
            }

            val existing = repository.getUserByUsername(trimmedUser)
            if (existing != null) {
                _snackbarMessage.value = "ইউজারনেম '$trimmedUser' ইতিমধ্যে ব্যবহৃত হয়েছে।"
                return@launch
            }

            val newUser = AppUser(
                username = trimmedUser,
                password = trimmedPass,
                fullName = trimmedName,
                role = role,
                phone = phone.trim()
            )
            repository.addUser(newUser)
            triggerAutoCloudBackup()
            _snackbarMessage.value = "নতুন ইউজার '$trimmedName' সফলভাবে তৈরি হয়েছে!"
        }
    }

    fun updateUser(user: AppUser) {
        viewModelScope.launch {
            repository.updateUser(user)
            triggerAutoCloudBackup()
            _snackbarMessage.value = "ইউজার '${user.fullName}' আপডেট করা হয়েছে।"
        }
    }

    fun deleteUser(user: AppUser) {
        viewModelScope.launch {
            if (user.username.equals("Conock", ignoreCase = true)) {
                _snackbarMessage.value = "মূল এডমিন ইউজার মুছে ফেলা সম্ভব নয়!"
                return@launch
            }
            if (_currentUser.value?.id == user.id) {
                _snackbarMessage.value = "বর্তমান লগইন করা একাউন্ট মুছে ফেলা যাবে না!"
                return@launch
            }
            repository.deleteUser(user)
            triggerAutoCloudBackup()
            _snackbarMessage.value = "ইউজার '${user.fullName}' সফলভাবে ডিলিট করা হয়েছে।"
        }
    }

    // Scanner
    fun openScanner() {
        _isScannerOpen.value = true
    }

    fun closeScanner() {
        _isScannerOpen.value = false
    }

    companion object {
        fun formatAmount(amount: Double, language: String = "BN"): String {
            val locale = if (language.equals("EN", ignoreCase = true)) Locale.US else Locale("bn", "BD")
            val format = NumberFormat.getNumberInstance(locale)
            return format.format(amount.toLong())
        }

        fun formatBdt(amount: Double): String {
            return formatAmount(amount, "BN")
        }
    }
}
