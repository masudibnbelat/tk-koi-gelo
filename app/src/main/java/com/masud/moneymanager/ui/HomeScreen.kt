// FILE: app/src/main/java/com/masud/moneymanager/ui/HomeScreen.kt
package com.masud.moneymanager.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.masud.moneymanager.data.AccountEntity
import com.masud.moneymanager.data.AccountEntryEntity
import com.masud.moneymanager.data.AppDatabase
import com.masud.moneymanager.data.TaskEntity
import com.masud.moneymanager.data.TransactionEntity
import com.masud.moneymanager.widget.TaskDeepLink
import com.masud.moneymanager.widget.refreshTaskWidgets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val IncomeGreen = Color(0xFF1ED760)
private val FabDarkRed = Color(0xFFD32F2F)
private val AccountFabRed = Color(0xFFB71C1C)

@Composable
fun HomeScreen(
        db: AppDatabase,
        currency: String,
        onAddTransaction: () -> Unit = {},
        taskDeepLink: TaskDeepLink = TaskDeepLink(),
        onTaskDeepLinkConsumed: () -> Unit = {},
) {
        val dao = db.transactionDao()
        val transactions by dao.observeAll().collectAsState(initial = emptyList())

        val accent = MaterialTheme.colorScheme.primary
        val scope = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current

        var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
        var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
        var selectedView by remember { mutableStateOf(0) }

        // Swipe navigation — pager + tab state duidik theke sync
        val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
        LaunchedEffect(selectedView) {
                if (pagerState.currentPage != selectedView) {
                        pagerState.animateScrollToPage(selectedView)
                }
        }
        LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                if (!pagerState.isScrollInProgress && selectedView != pagerState.currentPage) {
                        selectedView = pagerState.currentPage
                }
        }

        var editTarget by remember { mutableStateOf<TransactionEntity?>(null) }
        var showYearPicker by remember { mutableStateOf(false) }
        var showMonthPicker by remember { mutableStateOf(false) }
        var showAddAccountSheet by remember { mutableStateOf(false) }
        var showAddTaskSheet by remember { mutableStateOf(false) }

        LaunchedEffect(taskDeepLink) {
                if (!taskDeepLink.isEmpty) {
                        selectedView = 3
                        if (taskDeepLink.openAddSheet) showAddTaskSheet = true
                }
        }

        var summaryFilter by remember { mutableStateOf<String?>(null) }

        var isSearching by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        val searchFocusRequester = remember { FocusRequester() }

        val monthNames = remember {
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

        var selectedDay by remember { mutableStateOf(1) }
        LaunchedEffect(selectedYear, selectedMonth) {
                summaryFilter = null
                val now = Calendar.getInstance()
                selectedDay =
                        if (now.get(Calendar.YEAR) == selectedYear &&
                                        now.get(Calendar.MONTH) == selectedMonth
                        ) {
                                now.get(Calendar.DAY_OF_MONTH)
                        } else {
                                1
                        }
        }

        val monthTransactions =
                remember(transactions, selectedYear, selectedMonth, searchQuery, summaryFilter) {
                        transactions.filter { tx ->
                                val c = Calendar.getInstance().apply { timeInMillis = tx.createdAt }
                                val inMonth =
                                        c.get(Calendar.YEAR) == selectedYear &&
                                                c.get(Calendar.MONTH) == selectedMonth
                                val matches =
                                        if (searchQuery.isBlank()) {
                                                true
                                        } else {
                                                tx.title.contains(searchQuery, true) ||
                                                        tx.category.contains(searchQuery, true) ||
                                                        tx.note.contains(searchQuery, true)
                                        }
                                val matchesType = summaryFilter == null || tx.type == summaryFilter
                                inMonth && matches && matchesType
                        }
                }

        val monthAll =
                remember(transactions, selectedYear, selectedMonth) {
                        transactions.filter { tx ->
                                val c = Calendar.getInstance().apply { timeInMillis = tx.createdAt }
                                c.get(Calendar.YEAR) == selectedYear &&
                                        c.get(Calendar.MONTH) == selectedMonth
                        }
                }
        val totalIncome = monthAll.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = monthAll.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val net = totalIncome - totalExpense

        fun groupByDay(list: List<TransactionEntity>): Map<String, List<TransactionEntity>> =
                list.sortedByDescending { it.createdAt }.groupBy { tx ->
                        val c = Calendar.getInstance().apply { timeInMillis = tx.createdAt }
                        "${c.get(Calendar.DAY_OF_YEAR)}-${c.get(Calendar.YEAR)}"
                }

        val grouped = remember(monthTransactions) { groupByDay(monthTransactions) }

        val daysWithTx =
                remember(monthAll) {
                        monthAll
                                .map {
                                        Calendar.getInstance()
                                                .apply { timeInMillis = it.createdAt }
                                                .get(Calendar.DAY_OF_MONTH)
                                }
                                .toSet()
                }

        val dayTransactions =
                remember(monthTransactions, selectedDay) {
                        monthTransactions
                                .filter {
                                        Calendar.getInstance()
                                                .apply { timeInMillis = it.createdAt }
                                                .get(Calendar.DAY_OF_MONTH) == selectedDay
                                }
                                .sortedByDescending { it.createdAt }
                }
        val dayIncome = dayTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val dayExpense = dayTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        LaunchedEffect(isSearching) { if (isSearching) searchFocusRequester.requestFocus() }

        Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                floatingActionButton = {
                        AnimatedVisibility(
                                visible = selectedView != 1,
                                enter = fadeIn(),
                                exit = fadeOut()
                        ) {
                                val fabColor by
                                        animateColorAsState(
                                                targetValue =
                                                        if (selectedView >= 2) AccountFabRed
                                                        else FabDarkRed,
                                                animationSpec = tween(300),
                                                label = "fabColor",
                                        )
                                FloatingActionButton(
                                        onClick = {
                                                when (selectedView) {
                                                        2 -> showAddAccountSheet = true
                                                        3 -> showAddTaskSheet = true
                                                        else -> onAddTransaction()
                                                }
                                        },
                                        shape = RoundedCornerShape(18.dp),
                                        containerColor = fabColor,
                                        contentColor = Color.White,
                                        modifier = Modifier.size(56.dp),
                                ) { Icon(Icons.Default.Add, contentDescription = "Add") }
                        }
                },
        ) { padding ->
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(padding)
                                        .padding(horizontal = 16.dp)
                                        .pointerInput(isSearching) {
                                                detectTapGestures(
                                                        onTap = {
                                                                if (isSearching) {
                                                                        isSearching = false
                                                                        searchQuery = ""
                                                                        focusManager.clearFocus()
                                                                }
                                                        }
                                                )
                                        }
                ) {
                        AnimatedContent(targetState = isSearching) { searching ->
                                if (searching) {
                                        OutlinedTextField(
                                                value = searchQuery,
                                                onValueChange = { searchQuery = it },
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(top = 4.dp, bottom = 6.dp)
                                                                .height(48.dp)
                                                                .focusRequester(
                                                                        searchFocusRequester
                                                                ),
                                                placeholder = {
                                                        Text(
                                                                "Search transactions",
                                                                fontSize = 14.sp
                                                        )
                                                },
                                                textStyle = TextStyle(fontSize = 14.sp),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                keyboardOptions =
                                                        KeyboardOptions(
                                                                imeAction = ImeAction.Search
                                                        ),
                                                colors =
                                                        OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = accent,
                                                                unfocusedBorderColor =
                                                                        MaterialTheme.colorScheme
                                                                                .outline,
                                                        ),
                                                trailingIcon = {
                                                        if (searchQuery.isNotEmpty()) {
                                                                IconButton(
                                                                        onClick = {
                                                                                searchQuery = ""
                                                                        }
                                                                ) {
                                                                        Icon(
                                                                                Icons.Default.Close,
                                                                                contentDescription =
                                                                                        "Clear",
                                                                                tint =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurfaceVariant,
                                                                        )
                                                                }
                                                        }
                                                },
                                        )
                                } else {
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(top = 4.dp, bottom = 6.dp),
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
                                                                contentDescription =
                                                                        "Previous month",
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .onBackground,
                                                        )
                                                }

                                                Row(
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(40.dp),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically,
                                                ) {
                                                        Text(
                                                                monthNames[selectedMonth],
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onBackground,
                                                                modifier =
                                                                        Modifier.clickable {
                                                                                showMonthPicker =
                                                                                        true
                                                                        },
                                                        )
                                                        Text(
                                                                selectedYear.toString(),
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onBackground,
                                                                modifier =
                                                                        Modifier.clickable {
                                                                                showYearPicker =
                                                                                        true
                                                                        },
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
                                                                        contentDescription =
                                                                                "Next month",
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onBackground,
                                                                )
                                                        }
                                                        IconButton(
                                                                onClick = { isSearching = true }
                                                        ) {
                                                                Icon(
                                                                        Icons.Default.Search,
                                                                        contentDescription =
                                                                                "Search",
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onBackground,
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                                TabHeaderItem(
                                        text = "Monthly",
                                        selected = selectedView == 0,
                                        modifier = Modifier.weight(1f),
                                ) { selectedView = 0 }
                                TabHeaderItem(
                                        text = "Calendar",
                                        selected = selectedView == 1,
                                        modifier = Modifier.weight(1f),
                                ) { selectedView = 1 }
                                TabHeaderItem(
                                        text = "Accounts",
                                        selected = selectedView == 2,
                                        modifier = Modifier.weight(1f),
                                ) { selectedView = 2 }
                                TabHeaderItem(
                                        text = "Tasks",
                                        selected = selectedView == 3,
                                        modifier = Modifier.weight(1f),
                                ) { selectedView = 3 }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        AnimatedVisibility(visible = selectedView == 0) {
                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                ) {
                                        SummaryMiniCard(
                                                title = "Income",
                                                value = formatMoney(totalIncome, currency),
                                                valueColor = IncomeGreen,
                                                modifier = Modifier.weight(1f),
                                                selected = summaryFilter == "INCOME",
                                                onClick = {
                                                        summaryFilter =
                                                                if (summaryFilter == "INCOME") null
                                                                else "INCOME"
                                                },
                                        )
                                        SummaryMiniCard(
                                                title = "Expenses",
                                                value = formatMoney(totalExpense, currency),
                                                valueColor = accent,
                                                modifier = Modifier.weight(1f),
                                                selected = summaryFilter == "EXPENSE",
                                                onClick = {
                                                        summaryFilter =
                                                                if (summaryFilter == "EXPENSE") null
                                                                else "EXPENSE"
                                                },
                                        )
                                        SummaryMiniCard(
                                                title = "Total",
                                                value =
                                                        (if (net < 0) "- " else "") +
                                                                formatMoney(
                                                                        kotlin.math.abs(net),
                                                                        currency
                                                                ),
                                                valueColor = if (net < 0) accent else IncomeGreen,
                                                modifier = Modifier.weight(1f),
                                                selected = summaryFilter == null,
                                                onClick = { summaryFilter = null },
                                        )
                                }
                        }

                        HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                userScrollEnabled = true,
                                key = { it },
                        ) { page ->
                                if (page == 0) {
                                        if (grouped.isEmpty()) {
                                                EmptyState(
                                                        title = "No transactions this month",
                                                        message =
                                                                "Tap the + button to add an income or expense.",
                                                )
                                        } else {
                                                TransactionList(
                                                        grouped = grouped,
                                                        currency = currency,
                                                        onItemClick = { editTarget = it },
                                                )
                                        }
                                } else if (page == 1) {
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
                                                                title =
                                                                        "No transactions on this day",
                                                                message =
                                                                        "Pick another date from the strip above.",
                                                        )
                                                } else {
                                                        LazyColumn(
                                                                contentPadding =
                                                                        PaddingValues(
                                                                                bottom = 90.dp
                                                                        ),
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(6.dp),
                                                        ) {
                                                                itemsIndexed(
                                                                        dayTransactions,
                                                                        key = { _, tx -> tx.id }
                                                                ) { index, tx ->
                                                                        TransactionRow(
                                                                                transaction = tx,
                                                                                currency = currency,
                                                                                onClick = {
                                                                                        editTarget =
                                                                                                tx
                                                                                },
                                                                        )
                                                                        if (index !=
                                                                                        dayTransactions
                                                                                                .lastIndex
                                                                        ) {
                                                                                Spacer(
                                                                                        Modifier.height(
                                                                                                6.dp
                                                                                        )
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                } else if (page == 2) {
                                        AccountsTab(
                                                db = db,
                                                currency = currency,
                                                showAddSheet = showAddAccountSheet,
                                                onShowAddSheetChange = { showAddAccountSheet = it },
                                        )
                                } else {
                                        TasksTab(
                                                db = db,
                                                currency = currency,
                                                showAddSheet = showAddTaskSheet,
                                                onShowAddSheetChange = { showAddTaskSheet = it },
                                                taskDeepLink = taskDeepLink,
                                                onTaskDeepLinkConsumed = onTaskDeepLinkConsumed,
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

// ---------------- Tasks ----------------

@Composable
private fun TasksTab(
        db: AppDatabase,
        currency: String,
        showAddSheet: Boolean,
        onShowAddSheetChange: (Boolean) -> Unit,
        taskDeepLink: TaskDeepLink = TaskDeepLink(),
        onTaskDeepLinkConsumed: () -> Unit = {},
) {
        val dao = db.taskDao()
        val tasks by dao.observeAll().collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        var completedExpanded by remember { mutableStateOf(false) }
        var deleteTarget by remember { mutableStateOf<TaskEntity?>(null) }
        var editingTask by remember { mutableStateOf<TaskEntity?>(null) }

        val permissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

        fun ensureNotificationPermission() {
                if (Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
        }

        val active = tasks.filter { !it.isDone }
        val completed = tasks.filter { it.isDone }
        val totalAmount = tasks.sumOf { it.amount }

        LaunchedEffect(taskDeepLink) {
                if (!taskDeepLink.isEmpty) {
                        taskDeepLink.editTaskId?.let { id ->
                                dao.getById(id)?.let { editingTask = it }
                        }
                        onTaskDeepLinkConsumed()
                }
        }

        LaunchedEffect(tasks) { runCatching { refreshTaskWidgets(context) } }

        Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                                RoundedCornerShape(
                                        topStart = 0.dp,
                                        topEnd = 0.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 16.dp
                                ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border =
                                BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                ),
                ) {
                        Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text(
                                        "Total amount",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                        formatMoney(totalAmount, currency),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                )
                        }
                }
                Spacer(Modifier.height(10.dp))

                if (tasks.isEmpty()) {
                        EmptyState(
                                title = "No tasks yet",
                                message = "Tap the + button to add your first task."
                        )
                } else {
                        LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                                items(active, key = { it.id }) { task ->
                                        TaskRow(
                                                task = task,
                                                currency = currency,
                                                onToggle = {
                                                        scope.launch {
                                                                dao.setDone(task.id, true)
                                                                TaskAlarmScheduler.cancel(
                                                                        context,
                                                                        task.id
                                                                )
                                                        }
                                                },
                                                onDelete = { deleteTarget = task },
                                                onEdit = { editingTask = task }
                                        )
                                }

                                if (completed.isNotEmpty()) {
                                        item(key = "completed-folder") {
                                                CompletedFolder(
                                                        count = completed.size,
                                                        expanded = completedExpanded,
                                                        onToggleExpand = {
                                                                completedExpanded =
                                                                        !completedExpanded
                                                        }
                                                ) {
                                                        Column(
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(8.dp)
                                                        ) {
                                                                completed.forEach { task ->
                                                                        TaskRow(
                                                                                task = task,
                                                                                currency = currency,
                                                                                onToggle = {
                                                                                        scope
                                                                                                .launch {
                                                                                                        dao.setDone(
                                                                                                                task.id,
                                                                                                                false
                                                                                                        )
                                                                                                        TaskAlarmScheduler
                                                                                                                .schedule(
                                                                                                                        context,
                                                                                                                        task.copy(
                                                                                                                                isDone =
                                                                                                                                        false
                                                                                                                        )
                                                                                                                )
                                                                                                        refreshTaskWidgets(
                                                                                                                context
                                                                                                        )
                                                                                                }
                                                                                },
                                                                                onDelete = {
                                                                                        deleteTarget =
                                                                                                task
                                                                                },
                                                                                onEdit = {
                                                                                        editingTask =
                                                                                                task
                                                                                }
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }

        if (showAddSheet || editingTask != null) {
                AddTaskSheet(
                        editing = editingTask,
                        onDismiss = {
                                onShowAddSheetChange(false)
                                editingTask = null
                        },
                        onSave = { title, details, amount, dateMillis, exactMillis ->
                                ensureNotificationPermission()
                                val wasEditing = editingTask
                                scope.launch {
                                        if (wasEditing != null) {
                                                val updated =
                                                        wasEditing.copy(
                                                                title = title,
                                                                details = details,
                                                                amount = amount,
                                                                remindDate = dateMillis,
                                                                remindAt = exactMillis
                                                        )
                                                dao.update(updated)
                                                TaskAlarmScheduler.cancel(context, updated.id)
                                                if (!updated.isDone) {
                                                        TaskAlarmScheduler.schedule(
                                                                context,
                                                                updated
                                                        )
                                                }
                                        } else {
                                                val id =
                                                        dao.insert(
                                                                TaskEntity(
                                                                        title = title,
                                                                        details = details,
                                                                        amount = amount,
                                                                        remindDate = dateMillis,
                                                                        remindAt = exactMillis
                                                                )
                                                        )
                                                TaskAlarmScheduler.schedule(
                                                        context,
                                                        TaskEntity(
                                                                id = id,
                                                                title = title,
                                                                details = details,
                                                                amount = amount,
                                                                remindDate = dateMillis,
                                                                remindAt = exactMillis
                                                        )
                                                )
                                        }
                                }
                                onShowAddSheetChange(false)
                                editingTask = null
                        }
                )
        }

        deleteTarget?.let { task ->
                AlertDialog(
                        onDismissRequest = { deleteTarget = null },
                        title = { Text("Delete task?") },
                        text = { Text("Delete \"${task.title}\"?") },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                scope.launch {
                                                        TaskAlarmScheduler.cancel(context, task.id)
                                                        refreshTaskWidgets(context)
                                                        dao.deleteById(task.id)
                                                }
                                                deleteTarget = null
                                        }
                                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = {
                                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
                        }
                )
        }
}

@Composable
private fun TaskRow(
        task: TaskEntity,
        currency: String,
        onToggle: () -> Unit,
        onDelete: () -> Unit,
        onEdit: () -> Unit
) {
        val accent = MaterialTheme.colorScheme.primary
        val reminderLabel = taskReminderLabel(task)
        Surface(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
                Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        TaskCheckBox(done = task.isDone, onToggle = onToggle)

                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        task.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 2,
                                        textDecoration =
                                                if (task.isDone) TextDecoration.LineThrough
                                                else null,
                                        color =
                                                if (task.isDone)
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                else MaterialTheme.colorScheme.onSurface
                                )
                                if (task.details.isNotBlank()) {
                                        Text(
                                                task.details,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                modifier = Modifier.padding(top = 2.dp)
                                        )
                                }
                                if (task.amount > 0 || reminderLabel != null) {
                                        Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                                if (task.amount > 0) {
                                                        Text(
                                                                formatMoney(task.amount, currency),
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = accent
                                                        )
                                                }
                                                reminderLabel?.let { label ->
                                                        Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = accent.copy(alpha = 0.10f),
                                                                border =
                                                                        BorderStroke(
                                                                                1.dp,
                                                                                accent.copy(
                                                                                        alpha =
                                                                                                0.25f
                                                                                )
                                                                        ),
                                                        ) {
                                                                Row(
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        horizontal =
                                                                                                8.dp,
                                                                                        vertical =
                                                                                                4.dp
                                                                                ),
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically,
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .spacedBy(
                                                                                                4.dp
                                                                                        )
                                                                ) {
                                                                        Icon(
                                                                                Icons.Default
                                                                                        .Notifications,
                                                                                contentDescription =
                                                                                        null,
                                                                                tint = accent,
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                12.dp
                                                                                        )
                                                                        )
                                                                        Text(
                                                                                label,
                                                                                fontSize = 11.sp,
                                                                                color = accent,
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Medium
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }

                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete task",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                )
                        }
                }
        }
}

private fun taskReminderLabel(task: TaskEntity): String? {
        return when {
                task.remindAt != null ->
                        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                .format(Date(task.remindAt))
                task.remindDate != null ->
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                .format(Date(task.remindDate))
                else -> null
        }
}

@Composable
private fun TaskCheckBox(done: Boolean, onToggle: () -> Unit) {
        val scale by
                animateFloatAsState(
                        targetValue = if (done) 1f else 1f,
                        animationSpec = tween(200),
                        label = "checkScale"
                )
        Box(
                modifier =
                        Modifier.size(26.dp)
                                .scale(scale)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (done) IncomeGreen else Color.Transparent)
                                .border(
                                        1.5.dp,
                                        if (done) IncomeGreen
                                        else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                )
                                .clickable(onClick = onToggle)
                                .animateContentSize(),
                contentAlignment = Alignment.Center
        ) {
                AnimatedVisibility(
                        visible = done,
                        enter = fadeIn(tween(150)) + expandVertically(tween(150)),
                        exit = fadeOut(tween(100)) + shrinkVertically(tween(100))
                ) {
                        Icon(
                                Icons.Default.Check,
                                contentDescription = "Done",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                        )
                }
        }
}

@Composable
private fun CompletedFolder(
        count: Int,
        expanded: Boolean,
        onToggleExpand: () -> Unit,
        content: @Composable () -> Unit
) {
        val accent = MaterialTheme.colorScheme.primary
        Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
                Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(onClick = onToggleExpand)
                                                .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                                Box(
                                        modifier =
                                                Modifier.size(40.dp)
                                                        .background(
                                                                accent.copy(alpha = 0.12f),
                                                                CircleShape
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = accent,
                                                modifier = Modifier.size(20.dp)
                                        )
                                }
                                Text(
                                        "Completed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f)
                                )
                                Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accent.copy(alpha = 0.12f)
                                ) {
                                        Text(
                                                count.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accent,
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 10.dp,
                                                                vertical = 4.dp
                                                        )
                                        )
                                }
                                Icon(
                                        if (expanded) Icons.Default.ExpandLess
                                        else Icons.Default.ExpandMore,
                                        contentDescription = if (expanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }

                        AnimatedVisibility(
                                visible = expanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                        ) { Column(modifier = Modifier.padding(top = 8.dp)) { content() } }
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddTaskSheet(
        editing: TaskEntity? = null,
        onDismiss: () -> Unit,
        onSave:
                (
                        title: String,
                        details: String,
                        amount: Double,
                        dateMillis: Long?,
                        exactMillis: Long?) -> Unit
) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val isEditing = editing != null

        fun startOfDay(millis: Long): Long {
                val cal = Calendar.getInstance().apply { timeInMillis = millis }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return cal.timeInMillis
        }

        var title by remember(editing) { mutableStateOf(editing?.title.orEmpty()) }
        var details by remember(editing) { mutableStateOf(editing?.details.orEmpty()) }
        var amount by
                remember(editing) {
                        mutableStateOf(
                                editing?.amount?.let { a ->
                                        if (a > 0)
                                                (if (a % 1.0 == 0.0) a.toLong().toString()
                                                else a.toString())
                                        else ""
                                }
                                        ?: ""
                        )
                }
        var dateMillis by
                remember(editing) {
                        mutableStateOf<Long?>(
                                when {
                                        editing?.remindAt != null -> startOfDay(editing.remindAt)
                                        editing?.remindDate != null -> editing.remindDate
                                        else -> null
                                }
                        )
                }
        var timeHour by
                remember(editing) {
                        mutableStateOf<Int?>(
                                editing?.remindAt?.let {
                                        Calendar.getInstance()
                                                .apply { timeInMillis = it }
                                                .get(Calendar.HOUR_OF_DAY)
                                }
                        )
                }
        var timeMinute by
                remember(editing) {
                        mutableStateOf<Int?>(
                                editing?.remindAt?.let {
                                        Calendar.getInstance()
                                                .apply { timeInMillis = it }
                                                .get(Calendar.MINUTE)
                                }
                        )
                }
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }

        val dateLabel =
                dateMillis?.let {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
                }
        val timeLabel =
                if (timeHour != null && timeMinute != null) {
                        String.format(Locale.US, "%02d:%02d", timeHour, timeMinute)
                } else null

        val reminderNote =
                when {
                        dateMillis != null && timeHour != null -> "Notification at the set time"
                        dateMillis != null -> "3 reminders during the day (9 AM, 1 PM, 6 PM)"
                        else -> "No reminder will be sent"
                }

        val hasInput = title.isNotBlank()

        fun doSave() {
                val exactMillis =
                        if (dateMillis != null && timeHour != null && timeMinute != null) {
                                Calendar.getInstance()
                                        .apply {
                                                timeInMillis = dateMillis!!
                                                set(Calendar.HOUR_OF_DAY, timeHour!!)
                                                set(Calendar.MINUTE, timeMinute!!)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                        }
                                        .timeInMillis
                        } else null
                val finalDate = if (exactMillis != null) null else dateMillis
                onSave(
                        title.trim(),
                        details.trim(),
                        amount.trim().toDoubleOrNull() ?: 0.0,
                        finalDate,
                        exactMillis
                )
        }

        ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .imePadding()
                                        .padding(horizontal = 22.dp)
                                        .padding(bottom = 28.dp)
                                        .animateContentSize()
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        if (isEditing) "Edit task" else "New task",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                )
                                Button(
                                        onClick = { if (hasInput) doSave() else onDismiss() },
                                        shape =
                                                RoundedCornerShape(
                                                        topStart = 0.dp,
                                                        topEnd = 0.dp,
                                                        bottomStart = 14.dp,
                                                        bottomEnd = 14.dp
                                                ),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = AccountFabRed,
                                                        contentColor = Color.White
                                                ),
                                        contentPadding =
                                                PaddingValues(horizontal = 22.dp, vertical = 9.dp)
                                ) {
                                        Text(
                                                text =
                                                        if (hasInput) {
                                                                if (isEditing) "Update" else "Save"
                                                        } else {
                                                                "Cancel"
                                                        },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                        )
                                }
                        }

                        Spacer(Modifier.height(18.dp))

                        OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                placeholder = { Text("Title") },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor =
                                                        MaterialTheme.colorScheme.primary,
                                                unfocusedContainerColor =
                                                        MaterialTheme.colorScheme.surface,
                                                focusedContainerColor =
                                                        MaterialTheme.colorScheme.surface
                                        )
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                                value = details,
                                onValueChange = { details = it },
                                placeholder = { Text("Details (optional)") },
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor =
                                                        MaterialTheme.colorScheme.primary,
                                                unfocusedContainerColor =
                                                        MaterialTheme.colorScheme.surface,
                                                focusedContainerColor =
                                                        MaterialTheme.colorScheme.surface
                                        )
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                                value = amount,
                                onValueChange = { amount = sanitizeAmountInput(it) },
                                placeholder = { Text("Amount (optional)") },
                                singleLine = true,
                                keyboardOptions =
                                        KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor =
                                                        MaterialTheme.colorScheme.primary,
                                                unfocusedContainerColor =
                                                        MaterialTheme.colorScheme.surface,
                                                focusedContainerColor =
                                                        MaterialTheme.colorScheme.surface
                                        )
                        )

                        Spacer(Modifier.height(10.dp))

                        Surface(
                                onClick = { showDatePicker = true },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Row(
                                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                        Icon(
                                                Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                dateLabel ?: "Date (optional)",
                                                fontSize = 14.sp,
                                                color =
                                                        if (dateLabel != null)
                                                                MaterialTheme.colorScheme.onSurface
                                                        else
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                        )
                                        if (dateMillis != null) {
                                                IconButton(
                                                        onClick = {
                                                                dateMillis = null
                                                                timeHour = null
                                                                timeMinute = null
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                ) {
                                                        Icon(
                                                                Icons.Default.Close,
                                                                contentDescription = "Clear date",
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant,
                                                                modifier = Modifier.size(16.dp)
                                                        )
                                                }
                                        }
                                }
                        }

                        Spacer(Modifier.height(10.dp))

                        val timeEnabled = dateMillis != null
                        Surface(
                                onClick = { if (timeEnabled) showTimePicker = true },
                                shape = RoundedCornerShape(16.dp),
                                color =
                                        MaterialTheme.colorScheme.surface.copy(
                                                alpha = if (timeEnabled) 1f else 0.5f
                                        ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Row(
                                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                        Icon(
                                                Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                timeLabel ?: "Time (optional)",
                                                fontSize = 14.sp,
                                                color =
                                                        if (timeLabel != null)
                                                                MaterialTheme.colorScheme.onSurface
                                                        else
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                        )
                                        if (timeLabel != null) {
                                                IconButton(
                                                        onClick = {
                                                                timeHour = null
                                                                timeMinute = null
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                ) {
                                                        Icon(
                                                                Icons.Default.Close,
                                                                contentDescription = "Clear time",
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant,
                                                                modifier = Modifier.size(16.dp)
                                                        )
                                                }
                                        }
                                }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                Icon(
                                        Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                )
                                Text(
                                        reminderNote,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
        }

        if (showDatePicker) {
                AccountDatePicker(
                        initialMillis = dateMillis ?: System.currentTimeMillis(),
                        onDismiss = { showDatePicker = false },
                        onConfirm = { millis ->
                                dateMillis =
                                        Calendar.getInstance()
                                                .apply {
                                                        timeInMillis = millis
                                                        set(Calendar.HOUR_OF_DAY, 0)
                                                        set(Calendar.MINUTE, 0)
                                                        set(Calendar.SECOND, 0)
                                                        set(Calendar.MILLISECOND, 0)
                                                }
                                                .timeInMillis
                                timeHour = null
                                timeMinute = null
                        }
                )
        }

        if (showTimePicker) {
                TaskTimePickerDialog(
                        onDismiss = { showTimePicker = false },
                        onConfirm = { h, m ->
                                timeHour = h
                                timeMinute = m
                        }
                )
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskTimePickerDialog(
        onDismiss: () -> Unit,
        onConfirm: (hour: Int, minute: Int) -> Unit
) {
        val state = rememberTimePickerState()
        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Pick time", fontWeight = FontWeight.Bold) },
                text = { TimePicker(state = state) },
                confirmButton = {
                        TextButton(
                                onClick = {
                                        onConfirm(state.hour, state.minute)
                                        onDismiss()
                                }
                        ) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
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
                                contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
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
private fun AddAccountSheet(onDismiss: () -> Unit, onSave: (String) -> Unit) {
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
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 22.dp)
                                        .padding(bottom = 28.dp)
                                        .animateContentSize()
                ) {
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                Box(
                                        modifier =
                                                Modifier.size(48.dp)
                                                        .background(
                                                                MaterialTheme.colorScheme.primary
                                                                        .copy(alpha = 0.12f),
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
                                        Text(
                                                "New account",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                        )
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
                                onValueChange = {
                                        name = it
                                        error = null
                                },
                                placeholder = { Text("Account name") },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor =
                                                        MaterialTheme.colorScheme.primary,
                                                unfocusedContainerColor =
                                                        MaterialTheme.colorScheme.surface,
                                                focusedContainerColor =
                                                        MaterialTheme.colorScheme.surface
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
                        ) { Text("Create account", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
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
        val entries by
                db.accountDao().observeEntries(account.id).collectAsState(initial = emptyList())
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
                                modifier =
                                        Modifier.size(44.dp)
                                                .background(
                                                        accent.copy(alpha = 0.12f),
                                                        CircleShape
                                                ),
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
                                Text(
                                        account.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                )
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

        val dateLabel =
                remember(entryDate) {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                .format(java.util.Date(entryDate))
                }

        ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
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
                                                modifier =
                                                        Modifier.size(48.dp)
                                                                .background(
                                                                        MaterialTheme.colorScheme
                                                                                .primary.copy(
                                                                                alpha = 0.12f
                                                                        ),
                                                                        CircleShape
                                                                ),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Text(
                                                        account.name.firstOrNull()?.uppercase()
                                                                ?: "?",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 20.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                )
                                        }
                                        Column {
                                                Text(
                                                        account.name,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                        "${entries.size} entries",
                                                        fontSize = 12.sp,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                }
                                IconButton(
                                        onClick = onDismiss,
                                        modifier =
                                                Modifier.size(36.dp)
                                                        .background(
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant.copy(
                                                                        alpha = 0.5f
                                                                ),
                                                                CircleShape
                                                        )
                                ) {
                                        Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Close",
                                                modifier = Modifier.size(18.dp)
                                        )
                                }
                        }

                        Spacer(Modifier.height(14.dp))

                        Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                border =
                                        BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.25f
                                                )
                                        ),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Column(Modifier.padding(18.dp)) {
                                        Text(
                                                "Total balance",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                                formatMoney(total, currency),
                                                fontSize = 30.sp,
                                                fontWeight = FontWeight.Bold
                                        )
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
                                                        border =
                                                                BorderStroke(
                                                                        1.dp,
                                                                        MaterialTheme.colorScheme
                                                                                .outlineVariant
                                                                ),
                                                        modifier = Modifier.fillMaxWidth()
                                                ) {
                                                        Row(
                                                                Modifier.padding(
                                                                        horizontal = 14.dp,
                                                                        vertical = 12.dp
                                                                ),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(10.dp)
                                                        ) {
                                                                Column(
                                                                        modifier =
                                                                                Modifier.weight(1f)
                                                                ) {
                                                                        Text(
                                                                                if (entry.note
                                                                                                .isNotBlank()
                                                                                )
                                                                                        entry.note
                                                                                else "Entry",
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Medium,
                                                                                fontSize = 14.sp
                                                                        )
                                                                        Text(
                                                                                SimpleDateFormat(
                                                                                                "dd MMM yyyy",
                                                                                                Locale.getDefault()
                                                                                        )
                                                                                        .format(
                                                                                                java.util
                                                                                                        .Date(
                                                                                                                entry.createdAt
                                                                                                        )
                                                                                        ),
                                                                                fontSize = 11.sp,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurfaceVariant
                                                                        )
                                                                }
                                                                Text(
                                                                        formatMoney(
                                                                                entry.amount,
                                                                                currency
                                                                        ),
                                                                        fontWeight =
                                                                                FontWeight.Bold,
                                                                        fontSize = 14.sp
                                                                )
                                                                IconButton(
                                                                        onClick = {
                                                                                deleteEntryTarget =
                                                                                        entry
                                                                        },
                                                                        modifier =
                                                                                Modifier.size(32.dp)
                                                                ) {
                                                                        Icon(
                                                                                Icons.Default
                                                                                        .DeleteOutline,
                                                                                contentDescription =
                                                                                        "Delete entry",
                                                                                tint =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurfaceVariant,
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                18.dp
                                                                                        )
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
                                Text(
                                        "Delete account",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                )
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
                        text = {
                                Text("Delete this entry of ${formatMoney(entry.amount, currency)}?")
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                scope.launch { dao.deleteEntry(entry.id) }
                                                deleteEntryTarget = null
                                        }
                                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = {
                                TextButton(onClick = { deleteEntryTarget = null }) {
                                        Text("Cancel")
                                }
                        }
                )
        }

        if (showDeleteAccount) {
                AlertDialog(
                        onDismissRequest = { showDeleteAccount = false },
                        title = { Text("Delete account?") },
                        text = { Text("Delete \"${account.name}\" and all its entries?") },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                scope.launch { dao.deleteAccount(account.id) }
                                                showDeleteAccount = false
                                                onDismiss()
                                        }
                                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = {
                                TextButton(onClick = { showDeleteAccount = false }) {
                                        Text("Cancel")
                                }
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
                        TextButton(
                                onClick = {
                                        state.selectedDateMillis?.let(onConfirm)
                                        onDismiss()
                                }
                        ) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        ) { DatePicker(state = state) }
}

// ---------------- existing components (unchanged) ----------------

@Composable
private fun SummaryMiniCard(
        title: String,
        value: String,
        valueColor: Color,
        modifier: Modifier = Modifier,
        selected: Boolean = false,
        onClick: () -> Unit = {},
) {
        val accent = MaterialTheme.colorScheme.primary
        Surface(
                onClick = onClick,
                modifier = modifier,
                shape = RoundedCornerShape(12.dp),
                color =
                        if (selected) accent.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border =
                        BorderStroke(
                                1.dp,
                                if (selected) accent else MaterialTheme.colorScheme.outlineVariant
                        ),
        ) {
                Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        horizontalAlignment = Alignment.Start,
                ) {
                        Text(
                                title,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                        )
                        Text(
                                value,
                                color = valueColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
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
                        color =
                                if (selected) accent
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp),
                )
                if (selected) {
                        Box(
                                modifier =
                                        Modifier.height(3.dp)
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
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                                if (isIncome) Icons.Default.Storefront
                                                else Icons.Default.Category,
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
                                text =
                                        (if (isIncome) "+ " else "- ") +
                                                formatMoney(transaction.amount, currency),
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
        month: Int,
        selectedDay: Int,
        daysWithTx: Set<Int>,
        onSelect: (Int) -> Unit,
) {
        val accent = MaterialTheme.colorScheme.primary
        val daysInMonth =
                remember(year, month) {
                        Calendar.getInstance()
                                .apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, 1)
                                }
                                .getActualMaximum(Calendar.DAY_OF_MONTH)
                }
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val listState = rememberLazyListState()

        LaunchedEffect(year, month, selectedDay) {
                val index = (selectedDay - 1).coerceIn(0, (daysInMonth - 1).coerceAtLeast(0))
                snapshotFlow { listState.layoutInfo.viewportSize.width }.first { it > 0 }
                val info = listState.layoutInfo
                val viewportW = info.viewportSize.width
                val itemW = info.visibleItemsInfo.firstOrNull()?.size ?: 0
                val offset = if (itemW > 0) -((viewportW - itemW) / 2).coerceAtLeast(0) else 0
                listState.animateScrollToItem(index, offset)
        }

        LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                                shape = RoundedCornerShape(14.dp),
                                color =
                                        if (selected) {
                                                MaterialTheme.colorScheme.surface
                                        } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.25f
                                                )
                                        },
                                border =
                                        BorderStroke(
                                                1.dp,
                                                if (selected) accent
                                                else MaterialTheme.colorScheme.outlineVariant,
                                        ),
                        ) {
                                Column(
                                        modifier =
                                                Modifier.widthIn(min = 52.dp)
                                                        .padding(
                                                                horizontal = 8.dp,
                                                                vertical = 5.dp
                                                        ),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(1.dp),
                                ) {
                                        Text(
                                                dayName,
                                                fontSize = 10.sp,
                                                color =
                                                        if (selected) accent
                                                        else
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                        )
                                        Text(
                                                day.toString(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (daysWithTx.contains(day)) {
                                                Box(
                                                        modifier =
                                                                Modifier.padding(top = 1.dp)
                                                                        .size(5.dp)
                                                                        .clip(CircleShape)
                                                                        .background(accent),
                                                )
                                        } else {
                                                Spacer(Modifier.height(5.dp))
                                        }
                                }
                        }
                }
        }
}
