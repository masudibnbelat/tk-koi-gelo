// FILE: app/src/main/java/com/masud/moneymanager/ui/SettingsScreen.kt
package com.masud.moneymanager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masud.moneymanager.data.AppDatabase

private val AppRed = Color(0xFFB71C1C) // aro dark red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    db: AppDatabase,
    currency: String = "BDT",
    onCurrencyChange: (String) -> Unit = {},
    darkTheme: Boolean = true,
    onDarkThemeChange: (Boolean) -> Unit = {},
    onExportBackup: () -> Unit = {},
    onExportExcel: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    onClearAllData: () -> Unit = {},
    onContactDeveloper: () -> Unit = {},
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }
    var showCategoryManager by remember { mutableStateOf(false) }

    val currencyLabel = AllCurrencies.find { it.code == currency.uppercase() }
        ?.let { "${it.code} — ${it.name} (${it.symbol})" } ?: currency

    // Dynamic count — DB theke live
    val expenseCats by db.categoryDao().observeByType("EXPENSE").collectAsState(initial = emptyList())
    val incomeCats by db.categoryDao().observeByType("INCOME").collectAsState(initial = emptyList())
    val categoryCount = expenseCats.size + incomeCats.size

    Scaffold { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .offset(y = (-12).dp),
        ) {
            // Branded header — centered full-round logo + app name
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App logo — full round dark-red bg, boro white ৳ (optically centered)
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(AppRed)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "৳",
                        color = Color.White,
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.offset(y = 3.dp) // glyph ke exactly middle e boshalo
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "TK Koi Gelo",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            SectionLabel("Appearance")
            SettingsGroup {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsIcon(Icons.Default.DarkMode)
                        Text("Dark theme", fontWeight = FontWeight.Medium)
                    }
                    Switch(checked = darkTheme, onCheckedChange = onDarkThemeChange)
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Preferences")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.AttachMoney,
                    title = "Currency",
                    subtitle = currencyLabel,
                    onClick = { showCurrencySheet = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon = Icons.Default.Category,
                    title = "Categories",
                    subtitle = "$categoryCount income & expense categories",
                    onClick = { showCategoryManager = true },
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Data & Storage")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.Backup,
                    title = "Backup app data",
                    subtitle = "Export as Excel (.xlsx) or JSON",
                    onClick = { showBackupSheet = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon = Icons.Default.Download,
                    title = "Import Data (Excel or JSON)",
                    subtitle = "Restore complete dataset from an exported .xlsx or .json file",
                    onClick = onImportBackup,
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Data management")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear all data",
                    subtitle = "Permanently delete all transactions, loans and repayments",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearDialog = true },
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("App updates")
            UpdateCard()

            Spacer(Modifier.height(20.dp))
            SectionLabel("Developer Contact")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.Email,
                    title = "Masud ibn Belat",
                    subtitle = "masudibnbelat@gmail.com",
                    onClick = onContactDeveloper,
                )
            }




            Spacer(Modifier.height(32.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Delete all data?") },
            text = {
                Text(
                    "This permanently deletes every transaction, loan and " +
                        "repayment from this device. Export a backup first if needed.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClearAllData()
                }) { Text("Delete everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showCurrencySheet) {
        CurrencyPickerSheet(
            current = currency,
            onDismiss = { showCurrencySheet = false },
            onSelect = {
                onCurrencyChange(it)
                showCurrencySheet = false
            }
        )
    }

    if (showBackupSheet) {
        BackupChooserSheet(
            onDismiss = { showBackupSheet = false },
            onExcel = { showBackupSheet = false; onExportExcel() },
            onJson = { showBackupSheet = false; onExportBackup() }
        )
    }

    // Dynamic category manager — add/edit/delete animated
    if (showCategoryManager) {
        CategoryManagementSheet(
            db = db,
            onDismiss = { showCategoryManager = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerSheet(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) AllCurrencies
        else AllCurrencies.filter {
            it.code.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Select currency", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "${AllCurrencies.size} currencies",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search currency") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.code }) { item ->
                    val selected = item.code == current.uppercase()
                    Surface(
                        onClick = { onSelect(item.code) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    item.symbol,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.code, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    item.name,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupChooserSheet(
    onDismiss: () -> Unit,
    onExcel: () -> Unit,
    onJson: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Backup app data", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Choose a format to export",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            BackupBigCard(
                icon = Icons.Default.TableChart,
                title = "Excel",
                subtitle = ".xlsx workbook",
                desc = "Transactions, loans & categories in separate sheets",
                accent = Color(0xFF1ED760),
                onClick = onExcel
            )
            Spacer(Modifier.height(10.dp))
            BackupBigCard(
                icon = Icons.Default.DataObject,
                title = "JSON",
                subtitle = ".json file",
                desc = "Full local backup — best for restore",
                accent = MaterialTheme.colorScheme.primary,
                onClick = onJson
            )
        }
    }
}

@Composable
private fun BackupBigCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    desc: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(52.dp).background(accent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector) {
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsIcon(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = titleColor)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
