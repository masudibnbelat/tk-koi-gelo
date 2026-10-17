// FILE: app/src/main/java/com/masud/moneymanager/MainActivity.kt
package com.masud.moneymanager

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.masud.moneymanager.data.AppDatabase
import com.masud.moneymanager.data.BackupManager
import com.masud.moneymanager.ui.AddTransactionDialog
import com.masud.moneymanager.ui.HomeScreen
import com.masud.moneymanager.ui.LoansScreen
import com.masud.moneymanager.ui.MoneyManagerTheme
import com.masud.moneymanager.ui.SettingsScreen
import com.masud.moneymanager.widget.EXTRA_ADD_TASK
import com.masud.moneymanager.widget.EXTRA_EDIT_TASK_ID
import com.masud.moneymanager.widget.EXTRA_OPEN_TASKS
import com.masud.moneymanager.widget.TaskDeepLink
import kotlinx.coroutines.launch

private data class NavItem(
        val route: String,
        val label: String,
        val icon: ImageVector,
)

class MainActivity : ComponentActivity() {

    private val taskDeepLinkState = mutableStateOf(TaskDeepLink())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskDeepLinkState.value = intent.toTaskDeepLink()
        enableEdgeToEdge() // status bar app background er sathe blend hobe
        setContent {
            val context = LocalContext.current
            val db = remember { AppDatabase.getInstance(context) }
            val scope = rememberCoroutineScope()

            var darkTheme by remember { mutableStateOf(true) }
            var currency by remember { mutableStateOf("BDT") }
            var currentRoute by remember { mutableStateOf("home") }
            var showAddDialog by remember { mutableStateOf(false) }
            var showImportConfirm by remember { mutableStateOf(false) }

            val backupDate = remember {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        .format(java.util.Date())
            }

            // Status bar icon color theme onujayi (dark theme e sada icon)
            SideEffect {
                val window = (context as Activity).window
                WindowCompat.getInsetsController(window, window.decorView)
                        .isAppearanceLightStatusBars = !darkTheme
            }

            // Dynamic categories — DB theke live asbe
            val expenseCats by
                    db.categoryDao().observeByType("EXPENSE").collectAsState(initial = emptyList())
            val incomeCats by
                    db.categoryDao().observeByType("INCOME").collectAsState(initial = emptyList())

            // Backup / import file pickers
            val jsonExportLauncher =
                    rememberLauncherForActivityResult(
                            ActivityResultContracts.CreateDocument("application/json")
                    ) { uri ->
                        uri?.let {
                            scope.launch {
                                try {
                                    BackupManager.exportJson(context, db, it)
                                    Toast.makeText(context, "JSON backup saved", Toast.LENGTH_SHORT)
                                            .show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Backup failed", Toast.LENGTH_LONG)
                                            .show()
                                }
                            }
                        }
                    }
            val excelExportLauncher =
                    rememberLauncherForActivityResult(
                            ActivityResultContracts.CreateDocument(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            )
                    ) { uri ->
                        uri?.let {
                            scope.launch {
                                try {
                                    BackupManager.exportExcel(context, db, it)
                                    Toast.makeText(
                                                    context,
                                                    "Excel backup saved",
                                                    Toast.LENGTH_SHORT
                                            )
                                            .show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Backup failed", Toast.LENGTH_LONG)
                                            .show()
                                }
                            }
                        }
                    }
            val importLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri
                        ->
                        uri?.let {
                            scope.launch {
                                try {
                                    val count = BackupManager.importFile(context, db, it)
                                    Toast.makeText(
                                                    context,
                                                    "Import complete: $count items",
                                                    Toast.LENGTH_SHORT
                                            )
                                            .show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Import failed", Toast.LENGTH_LONG)
                                            .show()
                                }
                            }
                        }
                    }

            val navItems =
                    listOf(
                            NavItem("home", "Home", Icons.Rounded.Home),
                            NavItem("loans", "Loans", Icons.Rounded.AccountBalanceWallet),
                            NavItem("settings", "Settings", Icons.Rounded.Settings),
                    )

            MoneyManagerTheme(darkTheme = darkTheme) {
                Scaffold(
                        bottomBar = {
                            FancyBottomNav(
                                    items = navItems,
                                    currentRoute = currentRoute,
                                    onSelect = { currentRoute = it }
                            )
                        }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        when (currentRoute) {
                            "home" ->
                                    HomeScreen(
                                            db = db,
                                            currency = currency,
                                            onAddTransaction = { showAddDialog = true },
                                            taskDeepLink = taskDeepLinkState.value,
                                            onTaskDeepLinkConsumed = {
                                                taskDeepLinkState.value = TaskDeepLink()
                                            },
                                    )
                            "loans" ->
                                    LoansScreen(
                                            db = db,
                                            currency = currency,
                                    )
                            "settings" ->
                                    SettingsScreen(
                                            db = db,
                                            currency = currency,
                                            onCurrencyChange = { currency = it },
                                            darkTheme = darkTheme,
                                            onDarkThemeChange = { darkTheme = it },
                                            onExportBackup = {
                                                jsonExportLauncher.launch(
                                                        "MoneyManager-Backup-$backupDate.json"
                                                )
                                            },
                                            onExportExcel = {
                                                excelExportLauncher.launch(
                                                        "MoneyManager-Backup-$backupDate.xlsx"
                                                )
                                            },
                                            onImportBackup = { showImportConfirm = true },
                                            onClearAllData = {
                                                scope.launch { db.transactionDao().deleteAll() }
                                            },
                                            onContactDeveloper = { /* TODO: open email */},
                                    )
                        }
                    }
                }

                if (showAddDialog) {
                    AddTransactionDialog(
                            currency = currency,
                            expenseCategories = expenseCats.map { it.name },
                            incomeCategories = incomeCats.map { it.name },
                            onDismiss = { showAddDialog = false },
                            onSave = { tx ->
                                scope.launch { db.transactionDao().insert(tx) }
                                showAddDialog = false
                            },
                    )
                }

                if (showImportConfirm) {
                    ImportConfirmSheet(
                            onDismiss = { showImportConfirm = false },
                            onChooseFile = {
                                showImportConfirm = false
                                importLauncher.launch(arrayOf("*/*"))
                            }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        taskDeepLinkState.value = intent.toTaskDeepLink()
    }

    private fun Intent.toTaskDeepLink(): TaskDeepLink =
            TaskDeepLink(
                    nonce = System.currentTimeMillis(),
                    openTasksTab = getBooleanExtra(EXTRA_OPEN_TASKS, false),
                    openAddSheet = getBooleanExtra(EXTRA_ADD_TASK, false),
                    editTaskId = getLongExtra(EXTRA_EDIT_TASK_ID, -1L).takeIf { it != -1L }
            )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportConfirmSheet(onDismiss: () -> Unit, onChooseFile: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val error = MaterialTheme.colorScheme.error
    val green = Color(0xFF1ED760)

    ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
                modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 28.dp)
        ) {
            // Warning icon — red tonal circle with border
            Box(
                    modifier =
                            Modifier.size(64.dp)
                                    .background(error.copy(alpha = 0.12f), CircleShape)
                                    .border(1.dp, error.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = error,
                        modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text("Import data?", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                    "Restore a backup file into MoneyManager. " +
                            "Excel (.xlsx) and JSON (.json) files are supported.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Supported formats card
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(
                                                    alpha = 0.6f
                                            ),
                                            RoundedCornerShape(18.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                ImportFormatRow(
                        icon = Icons.Filled.TableChart,
                        title = "Excel",
                        subtitle = ".xlsx — transactions, loans, accounts & tasks in sheets",
                        tint = green
                )
                HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
                ImportFormatRow(
                        icon = Icons.Filled.DataObject,
                        title = "JSON",
                        subtitle = ".json — full backup, best for restore",
                        tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Warning note — animated in
            AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(error.copy(alpha = 0.08f))
                                        .border(
                                                1.dp,
                                                error.copy(alpha = 0.3f),
                                                RoundedCornerShape(16.dp)
                                        )
                                        .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                            Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = error,
                            modifier = Modifier.size(22.dp)
                    )
                    Text(
                            "All current data on this device will be replaced by the backup.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                    onClick = onChooseFile,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Choose backup file", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(4.dp))

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ImportFormatRow(icon: ImageVector, title: String, subtitle: String, tint: Color) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
                modifier = Modifier.size(40.dp).background(tint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FancyBottomNav(items: List<NavItem>, currentRoute: String, onSelect: (String) -> Unit) {
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(26.dp)
                                )
                                .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                Column(
                        modifier =
                                Modifier.weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                                if (selected)
                                                        MaterialTheme.colorScheme.primary.copy(
                                                                alpha = 0.14f
                                                        )
                                                else Color.Transparent
                                        )
                                        .clickable { onSelect(item.route) }
                                        .padding(vertical = 8.dp)
                                        .animateContentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint =
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                    )
                    AnimatedVisibility(
                            visible = selected,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                                item.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
