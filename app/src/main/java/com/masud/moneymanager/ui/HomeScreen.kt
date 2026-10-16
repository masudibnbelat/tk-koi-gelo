// FILE: app/src/main/java/com/masud/moneymanager/ui/HomeScreen.kt
package com.masud.moneymanager.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masud.moneymanager.data.AccountEntity
import com.masud.moneymanager.data.AccountEntryEntity
import com.masud.moneymanager.data.AppDatabase
import com.masud.moneymanager.data.TransactionEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val IncomeGreen = Color(0xFF1ED760)
private val FabDarkRed = Color(0xFFD32F2F) // + button dark red
private val AccountFabRed = Color(0xFFB71C1C) // Accounts tab er FAB — aro dark red

@Composable
fun HomeScreen(
    db: AppDatabase,
    currency: String,
    onAddTransaction: () -> Unit = {},
) {
    val dao = db.transactionDao()
    val transactions by dao.observeAll().collectAsState(initial = emptyList())

    val accent = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()

    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selectedView by remember { mutableStateOf(0) } // 0 = Monthly, 1 = Calendar, 2 = Accounts
    var editTarget by remember { mutableStateOf<TransactionEntity?>(null) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showAddAccountSheet by remember { mutableStateOf(false) }

    // Search row animation toggle
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    // Month full names, choto font e dekhabo
    val monthNames =
        remember {
            listOf(
                "January",
                "February",
                "March",
                "April",
                "May",
                "June",
                "July",
                "August",
                "September",
                "October",
                "November",
                "December",
            )
        }

    // Month base calendar
    val baseCal =
        remember(selectedYear, selectedMonth) {
            Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

    // Calendar view er jonno selected day
    var selectedDay by remember { mutableStateOf(1) }
    LaunchedEffect(selectedYear, selectedMonth) {
        val now = Calendar.getInstance()
        selectedDay =
            if (now.get(Calendar.YEAR) == selectedYear && now.get(Calendar.MONTH) == selectedMonth) {
                now.get(Calendar.DAY_OF_MONTH)
            } else {
                1
            }
    }

    // Ei month-er sob transaction (filter optional search)
    val monthTransactions =
        remember(transactions, selectedYear, selectedMonth, searchQuery) {
            transactions.filter { tx ->
                val c = Calendar.getInstance().apply { timeInMillis = tx.createdAt }
                val inMonth =
                    c.get(Calendar.YEAR) == selectedYear && c.get(Calendar.MONTH) == selectedMonth
                val matches =
                    if (searchQuery.isBlank()) {
                        true
                    } else {
                        tx.title.contains(searchQuery, true) ||
                            tx.category.contains(searchQuery, true) ||
                            tx.note.contains(searchQuery, true)
                    }
                inMonth && matches
            }
        }

    // Summary (month-wise, search chara)
    val monthAll =
        remember(transactions, selectedYear, selectedMonth) {
            transactions.filter { tx ->
                val c = Calendar.getInstance().apply { timeInMillis = tx.createdAt }
                c.get(Calendar.YEAR) == selectedYear && c.get(Calendar.MONTH) == selectedMonth
            }
        }
    val totalIncome = monthAll.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = monthAll.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val net = totalIncome - totalExpense

    // Group by day (descending)
    fun groupByDay(list: List<TransactionEntity>): Map<String, List<TransactionEntity>> =
        list
            .sortedByDescending { it.createdAt }
            .groupBy { tx ->
                val c = Calendar.getInstance().apply { timeInMillis = tx.createdAt }
                "${c.get(Calendar.DAY_OF_YEAR)}-${c.get(Calendar.YEAR)}"
            }

    val grouped = remember(monthTransactions) { groupByDay(monthTransactions) }

    // Calendar-strip er jonno: oi month-er kon kon dine tx ache (search chara — stable)
    val daysWithTx =
        remember(monthAll) {
            monthAll
                .map {
                    Calendar
                        .getInstance()
                        .apply { timeInMillis = it.createdAt }
                        .get(Calendar.DAY_OF_MONTH)
                }.toSet()
        }

    // Select kora diner tx
    val dayTransactions =
        remember(monthTransactions, selectedDay) {
            monthTransactions
                .filter {
                    Calendar
                        .getInstance()
                        .apply { timeInMillis = it.createdAt }
                        .get(Calendar.DAY_OF_MONTH) == selectedDay
                }.sortedByDescending { it.createdAt }
        }
    val dayIncome = dayTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val dayExpense = dayTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    LaunchedEffect(isSearching) { if (isSearching) searchFocusRequester.requestFocus() }

    Scaffold(
        // MainActivity er Scaffold already system inset handle kore — ekhane double hobe na
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            // Accounts tab e darker red FAB — click korle new account sheet khulbe
            val fabColor by animateColorAsState(
                targetValue = if (selectedView == 2) AccountFabRed else FabDarkRed,
                animationSpec = tween(300),
                label = "fabColor",
            )
            FloatingActionButton(
                onClick = {
                    if (selectedView == 2) showAddAccountSheet = true else onAddTransaction()
                },
                shape = RoundedCornerShape(18.dp),
                containerColor = fabColor,
                contentColor = Color.White,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            // Top row: month/year nav OR search bar
            AnimatedContent(targetState = isSearching) { searching ->
                if (searching) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        IconButton(
                            onClick = {
                                isSearching = false
                                searchQuery = ""
                            },
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Close search",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f).focusRequester(searchFocusRequester),
                            placeholder = { Text("Search transactions") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accent,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedMonth == 0) {
                                    selectedMonth = 11
                                    selectedYear--
                                } else {
                                    selectedMonth--
                                }
                            },
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Previous month",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(40.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                monthNames[selectedMonth],
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.clickable { showMonthPicker = true },
                            )
                            Text(
                                selectedYear.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.clickable { showYearPicker = true },
                            )
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    if (selectedMonth == 11) {
                                        selectedMonth = 0
                                        selectedYear++
                                    } else {
                                        selectedMonth++
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Next month",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                            IconButton(onClick = { isSearching = true }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    }
                }
            }

            // Tabs
            Row(modifier = Modifier.fillMaxWidth()) {
                TabHeaderItem(
                    text = "Monthly",
                    selected = selectedView == 0,
                    modifier = Modifier.weight(1f),
                ) {
                    selectedView = 0
                }
                TabHeaderItem(
                    text = "Calendar",
                    selected = selectedView == 1,
                    modifier = Modifier.weight(1f),
                ) {
                    selectedView = 1
                }
                TabHeaderItem(
                    text = "Accounts",
                    selected = selectedView == 2,
                    modifier = Modifier.weight(1f),
                ) {
                    selectedView = 2
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Summary — Accounts tab e hide thakbe
            AnimatedVisibility(visible = selectedView != 2) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SummaryMiniCard(
                        title = "Income",
                        value = formatMoney(totalIncome, currency),
                        valueColor = IncomeGreen,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMiniCard(
                        title = "Expenses",
                        value = formatMoney(totalExpense, currency),
                        valueColor = accent,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMiniCard(
                        title = "Total",
                        value =
                            (if (net < 0) "- " else "") + formatMoney(kotlin.math.abs(net), currency),
                        valueColor = if (net < 0) accent else IncomeGreen,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Content
            Crossfade(targetState = selectedView) { view ->
                if (view == 0) {
                    if (grouped.isEmpty()) {
                        EmptyState(
                            title = "No transactions this month",
                            message = "Tap the + button to add an income or expense.",
                        )
                    } else {
                        TransactionList(
                            grouped = grouped,
                            currency = currency,
                            onItemClick = { editTarget = it },
                        )
                    }
                } else if (view == 1) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        CalendarStrip(
                            year = selectedYear,
                            month = selectedMonth,
                            selectedDay = selectedDay,
                            daysWithTx = daysWithTx,
                            onSelect = { selectedDay = it },
                        )
                        Spacer(Modifier.height(10.dp))

                        DayHeaderForDay(
                            year = selectedYear,
                            month = selectedMonth,
                            day = selectedDay,
                            income = dayIncome,
                            expense = dayExpense,
                            currency = currency,
                        )

                        Spacer(Modifier.height(6.dp))

                        if (dayTransactions.isEmpty()) {
                            EmptyState(
                                title = "No transactions on this day",
                                message = "Pick another date from the strip above.",
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 90.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                itemsIndexed(dayTransactions, key = { _, tx -> tx.id }) { index, tx ->
                                    TransactionRow(
                                        transaction = tx,
                                        currency = currency,
                                        onClick = { editTarget = tx },
                                    )
                                    if (index != dayTransactions.lastIndex) {
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    AccountsTab(
                        db = db,
                        currency = currency,
                        showAddSheet = showAddAccountSheet,
                        onShowAddSheetChange = { showAddAccountSheet = it },
                    )
                }
            }
        }
    }

    if (showYearPicker) {
        YearPickerSheet(
            currentYear = selectedYear,
            onDismiss = { showYearPicker = false },
            onYearSelected = {
                selectedYear = it
                showYearPicker = false
            },
        )
    }

    if (showMonthPicker) {
        MonthPickerSheet(
            currentMonth = selectedMonth,
            onDismiss = { showMonthPicker = false },
            onMonthSelected = {
                selectedMonth = it
                showMonthPicker = false
            },
        )
    }

    editTarget?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            currency = currency,
            onDismiss = { editTarget = null },
            onUpdate = { updated ->
                scope.launch { db.transactionDao().insert(updated) }
                editTarget = null
            },
            onDelete = {
                scope.launch { db.transactionDao().deleteById(tx.id) }
                editTarget = null
            },
        )
    }
}

// ---------------- Accounts ----------------

@Composable
private fun AccountsTab(
    db: AppDatabase,
    currency: String,
    showAddSheet: Boolean,
    onShowAddSheetChange: (Boolean) -> Unit,
) {
    val dao = db.accountDao()
    val accounts by dao.observeAccounts().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (accounts.isEmpty()) {
            EmptyState(
                title = "No accounts yet",
                message = "Tap the + button to create an account."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
                    AccountCard(
                        db = db,
                        account = account,
                        currency = currency,
                        onClick = { selectedAccount = account }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddAccountSheet(
            onDismiss = { onShowAddSheetChange(false) },
            onSave = { name ->
                scope.launch { dao.insertAccount(AccountEntity(name = name)) }
                onShowAddSheetChange(false)
            }
        )
    }

    selectedAccount?.let { account ->
        AccountDetailSheet(
            db = db,
            account = account,
            currency = currency,
            onDismiss = { selectedAccount = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountSheet(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
                .animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text("New account", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Track money separately for a person",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                placeholder = { Text("Account name") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            AnimatedVisibility(visible = error != null) {
                Text(
                    error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        error = "Enter a name."
                    } else {
                        onSave(trimmed)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Create account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun AccountCard(
    db: AppDatabase,
    account: AccountEntity,
    currency: String,
    onClick: () -> Unit
) {
    val total by db.accountDao().observeTotal(account.id).collectAsState(initial = 0.0)
    val entries by db.accountDao().observeEntries(account.id).collectAsState(initial = emptyList())
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    account.name.firstOrNull()?.uppercase() ?: "?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = accent
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "${entries.size} entries",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatMoney(total, currency),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDetailSheet(
    db: AppDatabase,
    account: AccountEntity,
    currency: String,
    onDismiss: () -> Unit
) {
    val dao = db.accountDao()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val entries by dao.observeEntries(account.id).collectAsState(initial = emptyList())
    val total by dao.observeTotal(account.id).collectAsState(initial = 0.0)

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var entryDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddForm by remember { mutableStateOf(false) }
    var deleteEntryTarget by remember { mutableStateOf<AccountEntryEntity?>(null) }
    var showDeleteAccount by remember { mutableStateOf(false) }

    val dateLabel = remember(entryDate) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(java.util.Date(entryDate))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            account.name.firstOrNull()?.uppercase() ?: "?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(account.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${entries.size} entries",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Total balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatMoney(total, currency),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { showAddForm = !showAddForm },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (showAddForm) "Hide" else "Add money", fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(
                visible = showAddForm,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = sanitizeAmountInput(it); error = null },
                        placeholder = { Text("Amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("Note (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(dateLabel, fontSize = 14.sp)
                        }
                    }
                    AnimatedVisibility(visible = error != null) {
                        Text(
                            error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val v = amount.trim().toDoubleOrNull()
                            when {
                                v == null || !v.isFinite() || v <= 0.0 -> error = "Enter a valid amount."
                                else -> {
                                    scope.launch {
                                        dao.insertEntry(
                                            AccountEntryEntity(
                                                accountId = account.id,
                                                amount = v,
                                                note = note.trim(),
                                                createdAt = entryDate
                                            )
                                        )
                                    }
                                    amount = ""; note = ""; error = null
                                    entryDate = System.currentTimeMillis()
                                    showAddForm = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Entries", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))

            if (entries.isEmpty()) {
                Text(
                    "No entries yet.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (entry.note.isNotBlank()) entry.note else "Entry",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                            .format(java.util.Date(entry.createdAt)),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    formatMoney(entry.amount, currency),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                IconButton(
                                    onClick = { deleteEntryTarget = entry },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete entry",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { showDeleteAccount = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text("Delete account", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDatePicker) {
        AccountDatePicker(
            initialMillis = entryDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { entryDate = it }
        )
    }

    deleteEntryTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteEntryTarget = null },
            title = { Text("Delete entry?") },
            text = { Text("Delete this entry of ${formatMoney(entry.amount, currency)}?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.deleteEntry(entry.id) }
                    deleteEntryTarget = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteEntryTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteAccount) {
        AlertDialog(
            onDismissRequest = { showDeleteAccount = false },
            title = { Text("Delete account?") },
            text = { Text("Delete \"${account.name}\" and all its entries?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.deleteAccount(account.id) }
                    showDeleteAccount = false
                    onDismiss()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccount = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDatePicker(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let(onConfirm)
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = state)
    }
}

// ---------------- existing components (unchanged) ----------------

@Composable
private fun SummaryMiniCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(
                value,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TransactionList(
    grouped: Map<String, List<TransactionEntity>>,
    currency: String,
    onItemClick: (TransactionEntity) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        grouped.forEach { (key, items) ->
            val dayIncome = items.filter { it.type == "INCOME" }.sumOf { it.amount }
            val dayExpense = items.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val first = items.first()

            item(key = "header-$key") {
                DayHeader(
                    timestamp = first.createdAt,
                    income = dayIncome,
                    expense = dayExpense,
                    currency = currency,
                )
            }

            itemsIndexed(items, key = { _, tx -> tx.id }) { index, tx ->
                Column {
                    TransactionRow(
                        transaction = tx,
                        currency = currency,
                        onClick = { onItemClick(tx) },
                    )
                    if (index != items.lastIndex) {
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            item(key = "spacer-$key") { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun TabHeaderItem(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .height(3.dp)
                        .fillMaxWidth(0.6f)
                        .clip(RoundedCornerShape(50))
                        .background(accent),
            )
        } else {
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun DayHeader(
    timestamp: Long,
    income: Double,
    expense: Double,
    currency: String,
) {
    val accent = MaterialTheme.colorScheme.primary
    val cal = remember(timestamp) { Calendar.getInstance().apply { timeInMillis = timestamp } }
    val day = cal.get(Calendar.DAY_OF_MONTH).toString()
    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
    val monthYear = SimpleDateFormat("MM.yyyy", Locale.getDefault()).format(cal.time)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    day,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Column {
                    Text(
                        dayName,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        monthYear,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (income > 0) {
                    Text(
                        formatMoney(income, currency),
                        color = IncomeGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
                if (expense > 0) {
                    Text(
                        formatMoney(expense, currency),
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHeaderForDay(
    year: Int,
    month: Int,
    day: Int,
    income: Double,
    expense: Double,
    currency: String,
) {
    val accent = MaterialTheme.colorScheme.primary
    val cal =
        remember(year, month, day) {
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
            }
        }
    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
    val monthYear = SimpleDateFormat("MM.yyyy", Locale.getDefault()).format(cal.time)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    day.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Column {
                    Text(
                        dayName,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        monthYear,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (income > 0) {
                    Text(
                        formatMoney(income, currency),
                        color = IncomeGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
                if (expense > 0) {
                    Text(
                        formatMoney(expense, currency),
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionEntity,
    currency: String,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val isIncome = transaction.type == "INCOME"
    val tint = if (isIncome) IncomeGreen else accent
    val bg = tint.copy(alpha = 0.14f)

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector =
                        if (isIncome) Icons.Default.Storefront else Icons.Default.Category,
                    contentDescription = null,
                    tint = tint,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    transaction.category,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }

            Text(
                text = (if (isIncome) "+ " else "- ") + formatMoney(transaction.amount, currency),
                color = tint,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun CalendarStrip(
    year: Int,
    month: Int, // 0..11
    selectedDay: Int,
    daysWithTx: Set<Int>,
    onSelect: (Int) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val daysInMonth =
        remember(year, month) {
            Calendar
                .getInstance()
                .apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, 1)
                }.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val listState = rememberLazyListState()

    // Active date samne niye aso - today/current date auto visible thakbe
    LaunchedEffect(year, month, selectedDay) {
        val index = (selectedDay - 1).coerceIn(0, (daysInMonth - 1).coerceAtLeast(0))
        listState.scrollToItem(index)
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(daysInMonth) { index ->
            val day = index + 1
            val cal =
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
            val dayName = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val selected = day == selectedDay

            Surface(
                modifier = Modifier.clickable { onSelect(day) },
                shape = RoundedCornerShape(16.dp),
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    },
                border =
                    BorderStroke(
                        1.dp,
                        if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                    ),
            ) {
                Column(
                    modifier =
                        Modifier.widthIn(min = 56.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        dayName,
                        fontSize = 11.sp,
                        color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        day.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (daysWithTx.contains(day)) {
                        Box(
                            modifier =
                                Modifier
                                    .padding(top = 2.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(accent),
                        )
                    } else {
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
