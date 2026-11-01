package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Employee
import com.example.data.model.Expense
import com.example.data.model.Product
import com.example.data.model.SalaryPayment
import com.example.data.model.Sale
import com.example.data.model.ShopSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SyncResult(
    val success: Boolean,
    val message: String,
    val itemsCount: Int = 0
)

object GoogleDriveSyncManager {
    private const val PREFS_NAME = "google_drive_sync_prefs"
    private const val KEY_LAST_SYNC = "last_google_drive_sync_time"

    fun getLastSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNC, 0L)
    }

    fun saveLastSyncTime(context: Context, timestamp: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
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
     * Serializes all app data (Products, Sales, Expenses, Employees, SalaryPayments, Settings)
     * into a JSON string for Google Drive synchronization.
     */
    fun createFullBackupJson(
        products: List<Product>,
        sales: List<Sale>,
        expenses: List<Expense>,
        employees: List<Employee>,
        salaryPayments: List<SalaryPayment>,
        settings: ShopSettings
    ): String {
        val root = JSONObject()
        root.put("app", "APPLE AREA BD")
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
                put("notes", p.notes)
                put("status", p.status)
                put("createdAt", p.createdAt)
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
                put("referenceId", e.referenceId ?: "")
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
        salaryPayments.forEach { sal ->
            val obj = JSONObject().apply {
                put("id", sal.id)
                put("employeeId", sal.employeeId)
                put("employeeName", sal.employeeName)
                put("monthYear", sal.monthYear)
                put("amountPaid", sal.amountPaid)
                put("paymentDateMillis", sal.paymentDateMillis)
                put("paymentMethod", sal.paymentMethod)
                put("paymentType", sal.paymentType)
                put("note", sal.note)
            }
            salaryArr.put(obj)
        }
        root.put("salaryPayments", salaryArr)

        return root.toString(2)
    }

    /**
     * Exports and shares the backup file directly to Google Drive (or any cloud drive app).
     */
    fun shareBackupFileToDrive(context: Context, backupJson: String): Boolean {
        return try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val filename = "apple_area_bd_drive_backup_$dateStr.json"
            val file = File(context.cacheDir, filename)
            FileOutputStream(file).use { out ->
                out.write(backupJson.toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "APPLE AREA BD - Google Drive Cloud Backup")
                putExtra(Intent.EXTRA_TEXT, "APPLE AREA BD সম্পূর্ণ ডাটা ব্যাকআপ ফাইল (Google Drive এ সেভ করার জন্য)।")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Google Drive এ ব্যাকআপ সেভ করুন")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            saveLastSyncTime(context)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Parses the JSON backup string back into entity objects.
     */
    fun parseBackupJson(jsonString: String): ParsedBackupData? {
        return try {
            val root = JSONObject(jsonString)

            // Settings
            var shopSettings: ShopSettings? = null
            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                shopSettings = ShopSettings(
                    shopName = sObj.optString("shopName", "APPLE AREA BD"),
                    shopTagline = sObj.optString("shopTagline", "Buy Sell & Exchange"),
                    shopPhone = sObj.optString("shopPhone", "01930833375, 01758596640"),
                    shopAddress = sObj.optString("shopAddress", "Shop No # 100, Basement - 1, Bashundhara City Shopping Mall, Panthapath, Dhaka- 1205"),
                    appLanguage = sObj.optString("appLanguage", "BN"),
                    warrantyReplacement = sObj.optString("warrantyReplacement", "১০ দিনের রিপ্লেসমেন্ট গ্যারান্টি"),
                    warrantyService = sObj.optString("warrantyService", "১ বছর ফ্রি সার্ভিস ওয়ারেন্টি (ডিসপ্লে ও মাদারবোর্ড ব্যতীত)"),
                    warrantyExchangeDeduction = sObj.optString("warrantyExchangeDeduction", "১৫ দিনে এক্সচেঞ্জে ১০% এবং বিক্রয়ে ১৫% কর্তন")
                )
            }

            // Products
            val products = mutableListOf<Product>()
            val productsArr = root.optJSONArray("products")
            if (productsArr != null) {
                for (i in 0 until productsArr.length()) {
                    val p = productsArr.getJSONObject(i)
                    products.add(
                        Product(
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
                            notes = p.optString("notes", ""),
                            status = p.optString("status", "IN_STOCK"),
                            createdAt = p.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Sales
            val sales = mutableListOf<Sale>()
            val salesArr = root.optJSONArray("sales")
            if (salesArr != null) {
                for (i in 0 until salesArr.length()) {
                    val s = salesArr.getJSONObject(i)
                    sales.add(
                        Sale(
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
            val expenses = mutableListOf<Expense>()
            val expArr = root.optJSONArray("expenses")
            if (expArr != null) {
                for (i in 0 until expArr.length()) {
                    val e = expArr.getJSONObject(i)
                    expenses.add(
                        Expense(
                            type = e.optString("type", "EXPENSE"),
                            category = e.optString("category", "অন্যান্য খরচ"),
                            amount = e.optDouble("amount", 0.0),
                            note = e.optString("note", ""),
                            dateMillis = e.optLong("dateMillis", System.currentTimeMillis()),
                            paymentMethod = e.optString("paymentMethod", "Cash"),
                            referenceId = if (e.has("referenceId") && e.optString("referenceId").isNotBlank()) e.optString("referenceId") else null
                        )
                    )
                }
            }

            // Employees
            val employees = mutableListOf<Employee>()
            val empArr = root.optJSONArray("employees")
            if (empArr != null) {
                for (i in 0 until empArr.length()) {
                    val emp = empArr.getJSONObject(i)
                    employees.add(
                        Employee(
                            name = emp.optString("name", ""),
                            designation = emp.optString("designation", "সেলস এক্সিকিউটিভ"),
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
            val salaryPayments = mutableListOf<SalaryPayment>()
            val salArr = root.optJSONArray("salaryPayments")
            if (salArr != null) {
                for (i in 0 until salArr.length()) {
                    val sal = salArr.getJSONObject(i)
                    salaryPayments.add(
                        SalaryPayment(
                            employeeId = sal.optLong("employeeId", 0L),
                            employeeName = sal.optString("employeeName", ""),
                            monthYear = sal.optString("monthYear", ""),
                            amountPaid = sal.optDouble("amountPaid", 0.0),
                            paymentDateMillis = sal.optLong("paymentDateMillis", System.currentTimeMillis()),
                            paymentMethod = sal.optString("paymentMethod", "Cash"),
                            paymentType = sal.optString("paymentType", "FULL"),
                            note = sal.optString("note", "")
                        )
                    )
                }
            }

            ParsedBackupData(
                shopSettings = shopSettings,
                products = products,
                sales = sales,
                expenses = expenses,
                employees = employees,
                salaryPayments = salaryPayments
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class ParsedBackupData(
    val shopSettings: ShopSettings?,
    val products: List<Product>,
    val sales: List<Sale>,
    val expenses: List<Expense>,
    val employees: List<Employee>,
    val salaryPayments: List<SalaryPayment>
)
