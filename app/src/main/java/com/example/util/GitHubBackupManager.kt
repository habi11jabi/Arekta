package com.example.util

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.model.AppUser
import com.example.data.model.Employee
import com.example.data.model.Expense
import com.example.data.model.Product
import com.example.data.model.ProductStatus
import com.example.data.model.SalaryPayment
import com.example.data.model.Sale
import com.example.data.model.ShopSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GitHubBackupConfig(
    val token: String = "",
    val owner: String = "",
    val repo: String = "",
    val path: String = "backup/apple_area_shop_backup.json",
    val branch: String = "main",
    val autoSyncEnabled: Boolean = true
)

data class GitHubSyncResult(
    val success: Boolean,
    val message: String,
    val commitSha: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object GitHubBackupManager {
    private const val TAG = "GitHubBackupManager"
    private const val PREFS_NAME = "github_backup_prefs"
    private const val KEY_TOKEN = "github_token"
    private const val KEY_OWNER = "github_owner"
    private const val KEY_REPO = "github_repo"
    private const val KEY_PATH = "github_path"
    private const val KEY_BRANCH = "github_branch"
    private const val KEY_AUTO_SYNC = "github_auto_sync"
    private const val KEY_LAST_SYNC_TIME = "github_last_sync_time"
    private const val KEY_LAST_COMMIT_SHA = "github_last_commit_sha"
    private const val KEY_LAST_SYNC_STATUS = "github_last_sync_status"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun loadConfig(context: Context): GitHubBackupConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return GitHubBackupConfig(
            token = prefs.getString(KEY_TOKEN, "") ?: "",
            owner = prefs.getString(KEY_OWNER, "") ?: "",
            repo = prefs.getString(KEY_REPO, "") ?: "",
            path = prefs.getString(KEY_PATH, "backup/apple_area_shop_backup.json") ?: "backup/apple_area_shop_backup.json",
            branch = prefs.getString(KEY_BRANCH, "main") ?: "main",
            autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true)
        )
    }

    fun saveConfig(context: Context, config: GitHubBackupConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TOKEN, config.token.trim())
            .putString(KEY_OWNER, config.owner.trim())
            .putString(KEY_REPO, config.repo.trim())
            .putString(KEY_PATH, config.path.trim().ifEmpty { "backup/apple_area_shop_backup.json" })
            .putString(KEY_BRANCH, config.branch.trim().ifEmpty { "main" })
            .putBoolean(KEY_AUTO_SYNC, config.autoSyncEnabled)
            .apply()
    }

    fun getLastSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    fun getLastCommitSha(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_COMMIT_SHA, "") ?: ""
    }

    fun getLastSyncStatus(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_SYNC_STATUS, "") ?: ""
    }

    fun saveSyncResult(context: Context, result: GitHubSyncResult) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_SYNC_TIME, result.timestamp)
            .putString(KEY_LAST_COMMIT_SHA, result.commitSha ?: "")
            .putString(KEY_LAST_SYNC_STATUS, result.message)
            .apply()
    }

    fun formatSyncTime(timestamp: Long, isBangla: Boolean = true): String {
        if (timestamp <= 0L) {
            return if (isBangla) "কখনও সিঙ্ক করা হয়নি" else "Never Synced"
        }
        val locale = if (isBangla) Locale("bn", "BD") else Locale.US
        val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", locale)
        return sdf.format(Date(timestamp))
    }

    /**
     * Tests GitHub credentials by verifying repo accessibility.
     */
    suspend fun testConnection(config: GitHubBackupConfig): Result<String> = withContext(Dispatchers.IO) {
        if (config.token.isBlank() || config.owner.isBlank() || config.repo.isBlank()) {
            return@withContext Result.failure(Exception("টোকেন, ইউজারনেম এবং রিপোসিটরি নাম অবশ্যই পূরণ করতে হবে।"))
        }

        try {
            val url = "https://api.github.com/repos/${config.owner}/${config.repo}"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${config.token}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "AppleAreaShop-App")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val repoFullName = json.optString("full_name", "${config.owner}/${config.repo}")
                val isPrivate = json.optBoolean("private", false)
                val typeStr = if (isPrivate) "প্রাইভেট (Private)" else "পাবলিক (Public)"
                Result.success("সফলভাবে সংযুক্ত হয়েছে: $repoFullName ($typeStr)")
            } else {
                val errorMsg = when (response.code) {
                    401 -> "GitHub টোকেনটি সঠিক নয় বা মেয়াদোত্তীর্ণ (401 Unauthorized)।"
                    404 -> "রিপোসিটরি '${config.owner}/${config.repo}' খুঁজে পাওয়া যায়নি (404 Not Found)।"
                    403 -> "অনুমতি নেই বা API লিমিট শেষ (403 Forbidden)।"
                    else -> "GitHub সংযোগ ব্যর্থ (HTTP ${response.code})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "testConnection error", e)
            Result.failure(Exception("নেটওয়ার্ক সমস্যা: ${e.localizedMessage ?: "সংযুক্ত হতে পারছে না"}"))
        }
    }

    /**
     * Commits and pushes the full database backup to GitHub.
     */
    suspend fun pushBackupToGitHub(
        context: Context,
        config: GitHubBackupConfig,
        backupJson: String
    ): GitHubSyncResult = withContext(Dispatchers.IO) {
        if (config.token.isBlank() || config.owner.isBlank() || config.repo.isBlank()) {
            return@withContext GitHubSyncResult(
                success = false,
                message = "GitHub কনফিগার করা হয়নি। সেটিংস থেকে টোকেন ও রিপোসিটরি সেট করুন।"
            )
        }

        // Always save offline snapshot to local storage
        saveAutoBackupSnapshot(context, backupJson)

        try {
            val safePath = config.path.trim().trimStart('/')
            val getUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/contents/$safePath?ref=${config.branch}"

            var existingSha: String? = null

            // 1. Get file SHA if it exists
            val getRequest = Request.Builder()
                .url(getUrl)
                .header("Authorization", "Bearer ${config.token}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "AppleAreaShop-App")
                .get()
                .build()

            val getResponse = httpClient.newCall(getRequest).execute()
            if (getResponse.isSuccessful) {
                val respStr = getResponse.body?.string().orEmpty()
                val getJson = JSONObject(respStr)
                existingSha = getJson.optString("sha", null)
            }
            getResponse.close()

            // 2. Prepare PUT payload
            val encodedContent = Base64.encodeToString(backupJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val timeStr = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(Date())

            val putPayload = JSONObject().apply {
                put("message", "Auto Backup - Apple Area Shop ($timeStr)")
                put("content", encodedContent)
                put("branch", config.branch)
                if (!existingSha.isNullOrEmpty()) {
                    put("sha", existingSha)
                }
            }

            val putUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/contents/$safePath"
            val requestBody = putPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val putRequest = Request.Builder()
                .url(putUrl)
                .header("Authorization", "Bearer ${config.token}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "AppleAreaShop-App")
                .put(requestBody)
                .build()

            val putResponse = httpClient.newCall(putRequest).execute()
            val putBody = putResponse.body?.string().orEmpty()

            if (putResponse.isSuccessful) {
                val respJson = JSONObject(putBody)
                val commitObj = respJson.optJSONObject("commit")
                val newSha = commitObj?.optString("sha") ?: respJson.optJSONObject("content")?.optString("sha")
                val shortSha = if (!newSha.isNullOrEmpty() && newSha.length >= 7) newSha.substring(0, 7) else newSha.orEmpty()

                val result = GitHubSyncResult(
                    success = true,
                    message = "সফলভাবে GitHub এ সেভ হয়েছে (Commit: $shortSha)",
                    commitSha = newSha,
                    timestamp = System.currentTimeMillis()
                )
                saveSyncResult(context, result)
                result
            } else {
                val err = when (putResponse.code) {
                    401 -> "GitHub টোকেন অনুমোদিত নয় (401 Unauthorized)।"
                    404 -> "রিপোসিটরি বা ব্রাঞ্চ পাওয়া যায়নি (404 Not Found)।"
                    409 -> "কনফ্লিক্ট হয়েছে (409 Conflict)।"
                    else -> "GitHub পুশ ব্যর্থ (HTTP ${putResponse.code})"
                }
                val result = GitHubSyncResult(success = false, message = err)
                saveSyncResult(context, result)
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "pushBackupToGitHub error", e)
            val result = GitHubSyncResult(
                success = false,
                message = "নেটওয়ার্ক এরর: ${e.localizedMessage ?: "GitHub এ সেভ করা সম্ভব হয়নি"}"
            )
            saveSyncResult(context, result)
            result
        }
    }

    /**
     * Downloads and parses backup data from GitHub.
     */
    suspend fun fetchBackupFromGitHub(config: GitHubBackupConfig): Result<ParsedBackupData> = withContext(Dispatchers.IO) {
        if (config.token.isBlank() || config.owner.isBlank() || config.repo.isBlank()) {
            return@withContext Result.failure(Exception("GitHub কনফিগারেশন অসম্পূর্ণ।"))
        }

        try {
            val safePath = config.path.trim().trimStart('/')
            val url = "https://api.github.com/repos/${config.owner}/${config.repo}/contents/$safePath?ref=${config.branch}"

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${config.token}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "AppleAreaShop-App")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("GitHub থেকে ফাইল ডাউনলোড করা যায়নি (HTTP ${response.code})"))
            }

            val json = JSONObject(body)
            val base64Content = json.optString("content", "").replace("\n", "").replace("\r", "")
            if (base64Content.isEmpty()) {
                return@withContext Result.failure(Exception("ব্যাকআপ ফাইলটিতে কোনো ডাটা পাওয়া যায়নি।"))
            }

            val decodedBytes = Base64.decode(base64Content, Base64.DEFAULT)
            val backupJsonString = String(decodedBytes, Charsets.UTF_8)

            val parsed = parseBackupJson(backupJsonString)
            if (parsed == null) {
                Result.failure(Exception("ব্যাকআপ ফাইলের ফরম্যাট সঠিক নয়।"))
            } else {
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchBackupFromGitHub error", e)
            Result.failure(Exception("GitHub রিস্টোর ব্যর্থ: ${e.localizedMessage}"))
        }
    }

    /**
     * Saves an offline backup snapshot JSON on the local filesystem.
     */
    fun saveAutoBackupSnapshot(context: Context, backupJson: String): Boolean {
        return try {
            val file = File(context.filesDir, "apple_area_shop_auto_backup.json")
            FileOutputStream(file).use { out ->
                out.write(backupJson.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save auto backup snapshot", e)
            false
        }
    }

    /**
     * Serializes all app data into JSON format for GitHub backup.
     */
    fun createFullBackupJson(
        products: List<Product>,
        sales: List<Sale>,
        expenses: List<Expense>,
        employees: List<Employee>,
        salaryPayments: List<SalaryPayment>,
        users: List<AppUser> = emptyList(),
        settings: ShopSettings
    ): String {
        val root = JSONObject()
        root.put("app", "Apple Area Shop")
        root.put("version", "1.0")
        root.put("backupTimestamp", System.currentTimeMillis())
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))

        // Shop Settings
        val settingsObj = JSONObject().apply {
            put("shopName", settings.shopName)
            put("shopTagline", settings.shopTagline)
            put("shopPhone", settings.shopPhone)
            put("shopAddress", settings.shopAddress)
            put("appLanguage", settings.appLanguage)
            put("warrantyReplacement", settings.warrantyReplacement)
            put("warrantyService", settings.warrantyService)
            put("warrantyExchangeDeduction", settings.warrantyExchangeDeduction)
            put("companyPresets", JSONArray(settings.companyPresets))
            put("variantPresets", JSONArray(settings.variantPresets))
            put("colorPresets", JSONArray(settings.colorPresets))
            put("batteryPresets", JSONArray(settings.batteryPresets))
            put("boxConditionPresets", JSONArray(settings.boxConditionPresets))
            put("supplierPresets", JSONArray(settings.supplierPresets))
        }
        root.put("settings", settingsObj)

        // Products
        val productsArr = JSONArray()
        products.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("company", p.company)
                put("model", p.model)
                put("variant", p.variant)
                put("color", p.color)
                put("batteryHealth", p.batteryHealth)
                put("boxCondition", p.boxCondition)
                put("supplier", p.supplier)
                put("purchasePrice", p.purchasePrice)
                put("targetSalePrice", p.targetSalePrice)
                put("imei", p.imei)
                put("status", p.status)
                put("notes", p.notes)
                put("createdAt", p.createdAt)
                put("returnReason", p.returnReason)
                put("returnedAt", p.returnedAt ?: JSONObject.NULL)
                put("returnRefundAmount", p.returnRefundAmount)
                put("returnNotes", p.returnNotes)
            }
            productsArr.put(obj)
        }
        root.put("products", productsArr)

        // Sales
        val salesArr = JSONArray()
        sales.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("productId", s.productId)
                put("invoiceNumber", s.invoiceNumber)
                put("productDetails", s.productDetails)
                put("imei", s.imei)
                put("batteryHealth", s.batteryHealth)
                put("boxCondition", s.boxCondition)
                put("customerName", s.customerName)
                put("customerPhone", s.customerPhone)
                put("customerAddress", s.customerAddress)
                put("costPrice", s.costPrice)
                put("salePrice", s.salePrice)
                put("paidAmount", s.paidAmount)
                put("dueAmount", s.dueAmount)
                put("paymentMethod", s.paymentMethod)
                put("warrantyInfo", s.warrantyInfo)
                put("soldAt", s.soldAt)
                put("remarks", s.remarks)
            }
            salesArr.put(obj)
        }
        root.put("sales", salesArr)

        // Expenses
        val expensesArr = JSONArray()
        expenses.forEach { e ->
            val obj = JSONObject().apply {
                put("id", e.id)
                put("type", e.type)
                put("category", e.category)
                put("amount", e.amount)
                put("note", e.note)
                put("dateMillis", e.dateMillis)
                put("paymentMethod", e.paymentMethod)
                put("referenceId", e.referenceId ?: JSONObject.NULL)
            }
            expensesArr.put(obj)
        }
        root.put("expenses", expensesArr)

        // Employees
        val employeesArr = JSONArray()
        employees.forEach { emp ->
            val obj = JSONObject().apply {
                put("id", emp.id)
                put("name", emp.name)
                put("designation", emp.designation)
                put("phone", emp.phone)
                put("monthlySalary", emp.monthlySalary)
                put("joiningDateMillis", emp.joiningDateMillis)
                put("isActive", emp.isActive)
                put("address", emp.address)
                put("nidNumber", emp.nidNumber)
                put("notes", emp.notes)
            }
            employeesArr.put(obj)
        }
        root.put("employees", employeesArr)

        // Salary Payments
        val salaryArr = JSONArray()
        salaryPayments.forEach { sp ->
            val obj = JSONObject().apply {
                put("id", sp.id)
                put("employeeId", sp.employeeId)
                put("employeeName", sp.employeeName)
                put("monthYear", sp.monthYear)
                put("amountPaid", sp.amountPaid)
                put("paymentDateMillis", sp.paymentDateMillis)
                put("paymentMethod", sp.paymentMethod)
                put("paymentType", sp.paymentType)
                put("note", sp.note)
            }
            salaryArr.put(obj)
        }
        root.put("salaryPayments", salaryArr)

        // Users
        val usersArr = JSONArray()
        users.forEach { u ->
            val obj = JSONObject().apply {
                put("id", u.id)
                put("username", u.username)
                put("password", u.password)
                put("fullName", u.fullName)
                put("role", u.role)
                put("phone", u.phone)
                put("isActive", u.isActive)
                put("createdAt", u.createdAt)
            }
            usersArr.put(obj)
        }
        root.put("users", usersArr)

        return root.toString(2)
    }

    /**
     * Parses a JSON backup string into structured domain objects.
     */
    fun parseBackupJson(jsonString: String): ParsedBackupData? {
        return try {
            val root = JSONObject(jsonString)

            // Settings
            val settings = root.optJSONObject("settings")?.let { obj ->
                ShopSettings(
                    shopName = obj.optString("shopName", "APPLE AREA BD"),
                    shopTagline = obj.optString("shopTagline", "Buy Sell & Exchange"),
                    shopPhone = obj.optString("shopPhone", "01930833375, 01758596640"),
                    shopAddress = obj.optString("shopAddress", "Shop No # 100, Basement - 1, Bashundhara City Shopping Mall, Panthapath, Dhaka- 1205"),
                    appLanguage = obj.optString("appLanguage", "BN"),
                    warrantyReplacement = obj.optString("warrantyReplacement", "7 Days Replacement"),
                    warrantyService = obj.optString("warrantyService", "2 Years Free Service Warranty"),
                    warrantyExchangeDeduction = obj.optString("warrantyExchangeDeduction", "20% - 25%"),
                    companyPresets = parseStringList(obj.optJSONArray("companyPresets")),
                    variantPresets = parseStringList(obj.optJSONArray("variantPresets")),
                    colorPresets = parseStringList(obj.optJSONArray("colorPresets")),
                    batteryPresets = parseStringList(obj.optJSONArray("batteryPresets")),
                    boxConditionPresets = parseStringList(obj.optJSONArray("boxConditionPresets")),
                    supplierPresets = parseStringList(obj.optJSONArray("supplierPresets"))
                )
            }

            // Products
            val productsList = mutableListOf<Product>()
            val productsArr = root.optJSONArray("products")
            if (productsArr != null) {
                for (i in 0 until productsArr.length()) {
                    val p = productsArr.getJSONObject(i)
                    productsList.add(
                        Product(
                            id = p.optLong("id", 0L),
                            company = p.optString("company", "Apple"),
                            model = p.optString("model", ""),
                            variant = p.optString("variant", "128GB"),
                            color = p.optString("color", "Black"),
                            batteryHealth = p.optString("batteryHealth", "100%"),
                            boxCondition = p.optString("boxCondition", "বক্স সহ"),
                            supplier = p.optString("supplier", "Stock"),
                            purchasePrice = p.optDouble("purchasePrice", 0.0),
                            targetSalePrice = p.optDouble("targetSalePrice", 0.0),
                            imei = p.optString("imei", ""),
                            status = p.optString("status", ProductStatus.IN_STOCK.name),
                            notes = p.optString("notes", ""),
                            returnReason = p.optString("returnReason", ""),
                            returnedAt = if (p.has("returnedAt") && !p.isNull("returnedAt") && p.optLong("returnedAt", 0L) > 0L) p.optLong("returnedAt") else null,
                            returnRefundAmount = p.optDouble("returnRefundAmount", 0.0),
                            returnNotes = p.optString("returnNotes", ""),
                            createdAt = p.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Sales
            val salesList = mutableListOf<Sale>()
            val salesArr = root.optJSONArray("sales")
            if (salesArr != null) {
                for (i in 0 until salesArr.length()) {
                    val s = salesArr.getJSONObject(i)
                    salesList.add(
                        Sale(
                            id = s.optLong("id", 0L),
                            productId = s.optLong("productId", 0L),
                            invoiceNumber = s.optString("invoiceNumber", "AABD-${System.currentTimeMillis() % 1000000}"),
                            productDetails = s.optString("productDetails", ""),
                            imei = s.optString("imei", ""),
                            batteryHealth = s.optString("batteryHealth", ""),
                            boxCondition = s.optString("boxCondition", ""),
                            customerName = s.optString("customerName", ""),
                            customerPhone = s.optString("customerPhone", ""),
                            customerAddress = s.optString("customerAddress", ""),
                            costPrice = s.optDouble("costPrice", 0.0),
                            salePrice = s.optDouble("salePrice", 0.0),
                            paidAmount = s.optDouble("paidAmount", 0.0),
                            dueAmount = s.optDouble("dueAmount", 0.0),
                            paymentMethod = s.optString("paymentMethod", "Cash"),
                            warrantyInfo = s.optString("warrantyInfo", ""),
                            soldAt = s.optLong("soldAt", System.currentTimeMillis()),
                            remarks = s.optString("remarks", "")
                        )
                    )
                }
            }

            // Expenses
            val expensesList = mutableListOf<Expense>()
            val expensesArr = root.optJSONArray("expenses")
            if (expensesArr != null) {
                for (i in 0 until expensesArr.length()) {
                    val e = expensesArr.getJSONObject(i)
                    expensesList.add(
                        Expense(
                            id = e.optLong("id", 0L),
                            type = e.optString("type", "EXPENSE"),
                            category = e.optString("category", "General"),
                            amount = e.optDouble("amount", 0.0),
                            note = e.optString("note", ""),
                            dateMillis = e.optLong("dateMillis", System.currentTimeMillis()),
                            paymentMethod = e.optString("paymentMethod", "Cash"),
                            referenceId = if (e.isNull("referenceId")) null else e.optString("referenceId")
                        )
                    )
                }
            }

            // Employees
            val employeesList = mutableListOf<Employee>()
            val empArr = root.optJSONArray("employees")
            if (empArr != null) {
                for (i in 0 until empArr.length()) {
                    val emp = empArr.getJSONObject(i)
                    employeesList.add(
                        Employee(
                            id = emp.optLong("id", 0L),
                            name = emp.optString("name", ""),
                            designation = emp.optString("designation", "Staff"),
                            phone = emp.optString("phone", ""),
                            monthlySalary = emp.optDouble("monthlySalary", 0.0),
                            joiningDateMillis = emp.optLong("joiningDateMillis", System.currentTimeMillis()),
                            isActive = emp.optBoolean("isActive", true),
                            address = emp.optString("address", ""),
                            nidNumber = emp.optString("nidNumber", ""),
                            notes = emp.optString("notes", "")
                        )
                    )
                }
            }

            // Salary Payments
            val salaryList = mutableListOf<SalaryPayment>()
            val salArr = root.optJSONArray("salaryPayments")
            if (salArr != null) {
                for (i in 0 until salArr.length()) {
                    val sp = salArr.getJSONObject(i)
                    salaryList.add(
                        SalaryPayment(
                            id = sp.optLong("id", 0L),
                            employeeId = sp.optLong("employeeId", 0L),
                            employeeName = sp.optString("employeeName", ""),
                            monthYear = sp.optString("monthYear", ""),
                            amountPaid = sp.optDouble("amountPaid", 0.0),
                            paymentDateMillis = sp.optLong("paymentDateMillis", System.currentTimeMillis()),
                            paymentMethod = sp.optString("paymentMethod", "Cash"),
                            paymentType = sp.optString("paymentType", "FULL"),
                            note = sp.optString("note", "")
                        )
                    )
                }
            }

            // Users
            val usersList = mutableListOf<AppUser>()
            val usersArr = root.optJSONArray("users")
            if (usersArr != null) {
                for (i in 0 until usersArr.length()) {
                    val u = usersArr.getJSONObject(i)
                    usersList.add(
                        AppUser(
                            id = u.optLong("id", 0L),
                            username = u.optString("username", ""),
                            password = u.optString("password", ""),
                            fullName = u.optString("fullName", ""),
                            role = u.optString("role", "Staff"),
                            phone = u.optString("phone", ""),
                            isActive = u.optBoolean("isActive", true),
                            createdAt = u.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            ParsedBackupData(
                shopSettings = settings,
                products = productsList,
                sales = salesList,
                expenses = expensesList,
                employees = employeesList,
                salaryPayments = salaryList,
                users = usersList
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseBackupJson error", e)
            null
        }
    }

    private fun parseStringList(jsonArray: JSONArray?): List<String> {
        if (jsonArray == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }
}
