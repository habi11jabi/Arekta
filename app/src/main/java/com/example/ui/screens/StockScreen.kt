package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.ProductStatus
import com.example.ui.theme.AppleBlue
import com.example.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StockScreen(
    products: List<Product>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedBrandFilter: String,
    onBrandFilterChange: (String) -> Unit,
    onAddProductClick: () -> Unit,
    onBulkAddClick: () -> Unit = {},
    onGitHubSyncClick: () -> Unit = {},
    lastSyncTime: Long = 0L,
    onOpenScanner: () -> Unit,
    onSellProduct: (Product) -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onReturnProduct: (Product) -> Unit = {},
    onRestoreProduct: (Product) -> Unit = {},
    selectedStatusFilter: String = "IN_STOCK",
    onStatusFilterChange: (String) -> Unit = {}
) {
    val strings = com.example.ui.LocalAppStrings.current
    val isEnglish = strings.tabDashboard == "Dashboard"
    val lang = if (isEnglish) "EN" else "BN"

    val brands = listOf("ALL", "Apple", "Samsung", "Google", "Xiaomi", "OnePlus")
    val inStockCount = products.count { it.status == ProductStatus.IN_STOCK.name }
    val inStockValue = products.filter { it.status == ProductStatus.IN_STOCK.name }.sumOf { it.purchasePrice }

    Scaffold(
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bulk Stock FAB
                FloatingActionButton(
                    onClick = onBulkAddClick,
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("stock_fab_bulk_add_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Layers, contentDescription = "Bulk Add", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEnglish) "Bulk Add" else "বাল্ক স্টক যোগ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Single Stock FAB
                FloatingActionButton(
                    onClick = onAddProductClick,
                    containerColor = AppleBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("stock_fab_add_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.actionAddStock, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // Search & Scanner Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text(strings.searchPlaceholder, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = AppleBlue)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stock_search_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    IconButton(
                        onClick = onOpenScanner,
                        modifier = Modifier
                            .size(52.dp)
                            .background(AppleBlue.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                            .testTag("stock_scanner_icon_button")
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = AppleBlue)
                    }
                }
            }

            // Quick Dual Add & Google Drive Sync Action Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Single Stock Add
                    Button(
                        onClick = onAddProductClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("stock_add_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEnglish) "Add Single" else "স্টক যোগ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Bulk Stock Add
                    Button(
                        onClick = onBulkAddClick,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("stock_bulk_add_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEnglish) "Bulk Add (Multiple)" else "বাল্ক স্টক যোগ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // GitHub.com Backup Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF24292F).copy(alpha = 0.12f),
                        modifier = Modifier.height(44.dp)
                    ) {
                        IconButton(
                            onClick = onGitHubSyncClick,
                            modifier = Modifier.testTag("stock_github_sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "GitHub Backup",
                                tint = Color(0xFF24292F),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // In-stock active badge
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "Available In-Stock Devices ($inStockCount)" else "দোকানের সক্রিয় স্টক পণ্য ($inStockCount টি)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                        Text(
                            text = if (isEnglish) "Sold items go to Sales page" else "বিক্রি হলে অটো সেলস পেইজে যাবে",
                            fontSize = 11.sp,
                            color = Color(0xFF047857).copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Status Filter Chips (স্টক vs রিটার্নড)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusOptions = listOf(
                        "IN_STOCK" to if (isEnglish) "In Stock" else "স্টকে সক্রিয়",
                        "RETURNED" to if (isEnglish) "Returned" else "রিটার্নড ডিভাইস",
                        "ALL" to if (isEnglish) "All" else "সকল পণ্য"
                    )
                    statusOptions.forEach { (key, label) ->
                        val isSelected = selectedStatusFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { onStatusFilterChange(key) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (key == "RETURNED") Color(0xFFD97706) else AppleBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Brand Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(brands) { brand ->
                        val isSelected = selectedBrandFilter == brand
                        val label = if (brand == "ALL") strings.allBrandsLabel else brand
                        FilterChip(
                            selected = isSelected,
                            onClick = { onBrandFilterChange(brand) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Stock Summary Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.totalUnitsLabel} $inStockCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${strings.currentStockValueLabel} ${strings.currencySymbol} ${InventoryViewModel.formatAmount(inStockValue, lang)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleBlue
                        )
                    }
                }
            }

            // Empty state or Items
            if (products.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = "Empty",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = strings.noProducts,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onAddProductClick,
                                colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(strings.actionAddStock)
                            }
                        }
                    }
                }
            } else {
                items(products, key = { it.id }) { product ->
                    ProductItemCard(
                        product = product,
                        onSellClick = { onSellProduct(product) },
                        onEditClick = { onEditProduct(product) },
                        onDeleteClick = { onDeleteProduct(product) },
                        onReturnClick = { onReturnProduct(product) },
                        onRestoreClick = { onRestoreProduct(product) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
