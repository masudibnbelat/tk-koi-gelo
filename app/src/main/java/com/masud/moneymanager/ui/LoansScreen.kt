// FILE: app/src/main/java/com/masud/moneymanager/ui/LoansScreen.kt
package com.masud.moneymanager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masud.moneymanager.data.AppDatabase
import com.masud.moneymanager.data.LoanEntity
import com.masud.moneymanager.data.LoanRepaymentEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val GreenSoft = Color(0xFF1ED760)
private val RedSoft = Color(0xFFFF5C5C)

@Composable
fun LoansScreen(db: AppDatabase, currency: String = "BDT") {
        val dao = db.loanDao()
        val loans by dao.observeAllLoans().collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()
        var selectedTab by remember { mutableIntStateOf(0) }
        var showAddSheet by remember { mutableStateOf(false) }
        var selectedLoan by remember { mutableStateOf<LoanEntity?>(null) }
        var deleteTarget by remember { mutableStateOf<LoanEntity?>(null) }
        var repayTick by remember { mutableIntStateOf(0) }
        val symbol = currencySymbol(currency)
        val repayments by
                produceState(initialValue = emptyMap<Long, Double>(), loans, repayTick) {
                        value = loans.associate { it.id to dao.getTotalRepaid(it.id) }
                }
        fun remainingOf(loan: LoanEntity) =
                (loan.principalAmount - (repayments[loan.id] ?: 0.0)).coerceAtLeast(0.0)
        val activeLoans = loans.filter { remainingOf(it) > 0.001 }
        val completedLoans = loans.filter { remainingOf(it) <= 0.001 }
        val displayed = if (selectedTab == 0) activeLoans else completedLoans
        val toReceive = loans.filter { it.type == "LENT" }.sumOf { remainingOf(it) }
        val toPay = loans.filter { it.type == "BORROWED" }.sumOf { remainingOf(it) }
        Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                floatingActionButton = {
                        FloatingActionButton(
                                onClick = { showAddSheet = true },
                                shape = RoundedCornerShape(20.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                modifier = Modifier.size(58.dp)
                        ) {
                                Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add loan",
                                        modifier = Modifier.size(26.dp)
                                )
                        }
                }
        ) { padding ->
                Column(
                        modifier =
                                Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)
                ) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                                "Loan Manager",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                SummaryCard(
                                        title = "To receive",
                                        amount = toReceive,
                                        symbol = symbol,
                                        color = GreenSoft,
                                        modifier = Modifier.weight(1f)
                                )
                                SummaryCard(
                                        title = "To pay",
                                        amount = toPay,
                                        symbol = symbol,
                                        color = RedSoft,
                                        modifier = Modifier.weight(1f)
                                )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                                .copy(alpha = 0.35f)
                                                )
                                                .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                                listOf("Active" to GreenSoft, "Done" to RedSoft).forEachIndexed {
                                        i,
                                        (label, tint) ->
                                        val selected = selectedTab == i
                                        val count =
                                                if (i == 0) activeLoans.size
                                                else completedLoans.size
                                        val tabBg by
                                                animateColorAsState(
                                                        targetValue =
                                                                if (selected)
                                                                        tint.copy(alpha = 0.16f)
                                                                else Color.Transparent,
                                                        animationSpec =
                                                                spring(
                                                                        dampingRatio =
                                                                                Spring.DampingRatioMediumBouncy,
                                                                        stiffness =
                                                                                Spring.StiffnessLow
                                                                ),
                                                        label = "tabBg"
                                                )
                                        Box(
                                                modifier =
                                                        Modifier.weight(1f)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(tabBg)
                                                                .clickable { selectedTab = i }
                                                                .padding(vertical = 8.dp)
                                                                .animateContentSize(
                                                                        animationSpec =
                                                                                spring(
                                                                                        dampingRatio =
                                                                                                Spring.DampingRatioMediumBouncy,
                                                                                        stiffness =
                                                                                                Spring.StiffnessLow
                                                                                )
                                                                ),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Text(
                                                        "$label · $count",
                                                        fontWeight =
                                                                if (selected) FontWeight.Bold
                                                                else FontWeight.Medium,
                                                        fontSize = 13.sp,
                                                        color =
                                                                if (selected) tint
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                )
                                        }
                                }
                        }
                        Spacer(Modifier.height(12.dp))
                        AnimatedVisibility(
                                visible = displayed.isEmpty(),
                                enter = fadeIn(tween(300)),
                                exit = fadeOut(tween(200))
                        ) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                        modifier =
                                                                Modifier.size(64.dp)
                                                                        .background(
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceVariant
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.4f
                                                                                        ),
                                                                                CircleShape
                                                                        ),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Icon(
                                                                Icons.Default.Person,
                                                                contentDescription = null,
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant,
                                                                modifier = Modifier.size(28.dp)
                                                        )
                                                }
                                                Spacer(Modifier.height(12.dp))
                                                Text(
                                                        if (selectedTab == 0) "No active loans"
                                                        else "Nothing completed yet",
                                                        fontWeight = FontWeight.Medium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                        fontSize = 14.sp
                                                )
                                        }
                                }
                        }
                        LazyColumn(
                                contentPadding = PaddingValues(bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                                itemsIndexed(displayed, key = { _, it -> it.id }) { index, loan ->
                                        val stagger = (index * 30).coerceAtMost(240)
                                        AnimatedVisibility(
                                                visible = true,
                                                enter =
                                                        fadeIn(tween(300, delayMillis = stagger)) +
                                                                slideInVertically(
                                                                        tween(
                                                                                350,
                                                                                delayMillis =
                                                                                        stagger
                                                                        )
                                                                ) { -it / 2 },
                                                exit = fadeOut() + shrinkVertically()
                                        ) {
                                                LoanCard(
                                                        loan = loan,
                                                        repaid = repayments[loan.id] ?: 0.0,
                                                        symbol = symbol,
                                                        onClick = { selectedLoan = loan }
                                                )
                                        }
                                }
                        }
                }
        }
        if (showAddSheet) {
                AddLoanSheet(
                        onDismiss = { showAddSheet = false },
                        onSave = { person, type, amount, note ->
                                scope.launch {
                                        dao.insertLoan(
                                                LoanEntity(
                                                        personName = person,
                                                        type = type,
                                                        principalAmount = amount,
                                                        note = note
                                                )
                                        )
                                }
                                showAddSheet = false
                        }
                )
        }
        selectedLoan?.let { loan ->
                LoanDetailSheet(
                        loan = loan,
                        symbol = symbol,
                        db = db,
                        onDismiss = { selectedLoan = null },
                        onDeleteRequest = {
                                deleteTarget = loan
                                selectedLoan = null
                        },
                        onRepaymentAdded = { repayTick++ }
                )
        }
        deleteTarget?.let { loan ->
                AlertDialog(
                        onDismissRequest = { deleteTarget = null },
                        title = { Text("Delete loan?", fontWeight = FontWeight.Bold) },
                        text = { Text("Delete loan for ${loan.personName} with its history?") },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                scope.launch { dao.deleteLoan(loan.id) }
                                                deleteTarget = null
                                        }
                                ) {
                                        Text(
                                                "Delete",
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold
                                        )
                                }
                        },
                        dismissButton = {
                                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
                        }
                )
        }
}

@Composable
private fun SummaryCard(
        title: String,
        amount: Double,
        symbol: String,
        color: Color,
        modifier: Modifier = Modifier
) {
        Column(
                modifier =
                        modifier.clip(RoundedCornerShape(20.dp))
                                .background(color.copy(alpha = 0.10f))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .animateContentSize()
        ) {
                Text(title, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(
                        "$symbol ${String.format(Locale.US, "%,.0f", amount)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                )
        }
}

@Composable
private fun LoanCard(loan: LoanEntity, repaid: Double, symbol: String, onClick: () -> Unit) {
        val isLent = loan.type == "LENT"
        val accent = if (isLent) GreenSoft else RedSoft
        val remaining = (loan.principalAmount - repaid).coerceAtLeast(0.0)
        val progress =
                if (loan.principalAmount > 0)
                        (repaid / loan.principalAmount).toFloat().coerceIn(0f, 1f)
                else 0f
        val animatedProgress by
                animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(600),
                        label = "loanProgress"
                )
        Surface(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp).animateContentSize()) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Row(
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
                                                Text(
                                                        loan.personName.firstOrNull()?.uppercase()
                                                                ?: "?",
                                                        fontWeight = FontWeight.Bold,
                                                        color = accent,
                                                        fontSize = 16.sp
                                                )
                                        }
                                        Column {
                                                Text(
                                                        loan.personName,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 15.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                        if (isLent) "You lent" else "You borrowed",
                                                        fontSize = 12.sp,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                }
                                Text(
                                        "$symbol ${String.format(Locale.US, "%,.0f", remaining)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = accent
                                )
                        }
                        Spacer(Modifier.height(12.dp))
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                                .copy(alpha = 0.5f)
                                                )
                        ) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth(animatedProgress)
                                                        .fillMaxHeight()
                                                        .clip(CircleShape)
                                                        .background(accent)
                                )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text(
                                        "${(animatedProgress * 100).roundToInt()}% cleared",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accent
                                )
                                Text(
                                        "of $symbol ${String.format(Locale.US, "%,.0f", loan.principalAmount)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLoanSheet(onDismiss: () -> Unit, onSave: (String, String, Double, String) -> Unit) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var person by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("LENT") }
        var amount by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        val accent = if (type == "LENT") GreenSoft else RedSoft
        ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.background,
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
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text("New loan", fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
                        Spacer(Modifier.height(18.dp))
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                                .copy(alpha = 0.35f)
                                                )
                                                .padding(4.dp)
                        ) {
                                listOf("LENT" to "Lent", "BORROWED" to "Borrowed").forEach {
                                        (value, label) ->
                                        val selected = type == value
                                        val c = if (value == "LENT") GreenSoft else RedSoft
                                        Box(
                                                modifier =
                                                        Modifier.weight(1f)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(
                                                                        if (selected)
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surface
                                                                        else Color.Transparent
                                                                )
                                                                .clickable { type = value }
                                                                .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Text(
                                                        label,
                                                        fontWeight =
                                                                if (selected) FontWeight.Bold
                                                                else FontWeight.Medium,
                                                        fontSize = 14.sp,
                                                        color =
                                                                if (selected) c
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                )
                                        }
                                }
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                                value = person,
                                onValueChange = {
                                        person = it
                                        error = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Person's name") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                                value = amount,
                                onValueChange = {
                                        amount = sanitizeAmountInput(it)
                                        error = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Amount") },
                                singleLine = true,
                                keyboardOptions =
                                        KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = accent
                                        )
                        )
                        AnimatedVisibility(
                                visible = amount.isNotBlank(),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                        ) {
                                Column {
                                        Spacer(Modifier.height(10.dp))
                                        OutlinedTextField(
                                                value = note,
                                                onValueChange = { note = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                placeholder = { Text("Note (optional)") },
                                                shape = RoundedCornerShape(16.dp)
                                        )
                                }
                        }
                        AnimatedVisibility(visible = error != null) {
                                Text(
                                        error ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 8.dp)
                                )
                        }
                        Spacer(Modifier.height(20.dp))
                        Button(
                                onClick = {
                                        val value = amount.trim().toDoubleOrNull()
                                        when {
                                                person.isBlank() ->
                                                        error = "Enter the person's name."
                                                value == null ||
                                                        !value.isFinite() ||
                                                        value <= 0.0 ->
                                                        error = "Enter a valid amount."
                                                else ->
                                                        onSave(
                                                                person.trim(),
                                                                type,
                                                                value,
                                                                note.trim()
                                                        )
                                        }
                                },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                        )
                        ) { Text("Save loan", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanDetailSheet(
        loan: LoanEntity,
        symbol: String,
        db: AppDatabase,
        onDismiss: () -> Unit,
        onDeleteRequest: () -> Unit,
        onRepaymentAdded: () -> Unit = {}
) {
        val dao = db.loanDao()
        val repayments by dao.observeRepayments(loan.id).collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var showPayField by remember { mutableStateOf(false) }
        var payAmount by remember { mutableStateOf("") }
        var payNote by remember { mutableStateOf("") }
        var payDate by remember { mutableStateOf(System.currentTimeMillis()) }
        var showPayDatePicker by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val hasPayData = payAmount.isNotBlank() || payNote.isNotBlank()
        val totalRepaid = repayments.sumOf { it.amount }
        val remaining = (loan.principalAmount - totalRepaid).coerceAtLeast(0.0)
        val progress =
                if (loan.principalAmount > 0)
                        (totalRepaid / loan.principalAmount).toFloat().coerceIn(0f, 1f)
                else 0f
        val animatedProgress by
                animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(600),
                        label = "detailProgress"
                )
        val isLent = loan.type == "LENT"
        val accent = if (isLent) GreenSoft else RedSoft
        val payDateLabel =
                remember(payDate) {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(payDate))
                }
        val confirmPayment: () -> Unit = {
                val v = payAmount.trim().toDoubleOrNull()
                when {
                        v == null || !v.isFinite() || v <= 0.0 -> error = "Enter a valid amount."
                        v > remaining -> error = "Can't exceed remaining balance."
                        else -> {
                                scope.launch {
                                        dao.insertRepayment(
                                                LoanRepaymentEntity(
                                                        loanId = loan.id,
                                                        amount = v,
                                                        note = payNote.trim(),
                                                        createdAt = payDate
                                                )
                                        )
                                        payAmount = ""
                                        payNote = ""
                                        payDate = System.currentTimeMillis()
                                        error = null
                                        showPayField = false
                                        onRepaymentAdded()
                                }
                        }
                }
        }
        val topBtnColor by
                animateColorAsState(
                        targetValue =
                                if (hasPayData) accent
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        label = "topBtnColor"
                )
        ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.background,
                dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
                        Column(
                                modifier =
                                        Modifier.weight(1f, fill = false)
                                                .verticalScroll(rememberScrollState())
                                                .imePadding()
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        Box(
                                                modifier =
                                                        Modifier.size(52.dp)
                                                                .background(
                                                                        accent.copy(alpha = 0.12f),
                                                                        CircleShape
                                                                ),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Text(
                                                        loan.personName.firstOrNull()?.uppercase()
                                                                ?: "?",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 20.sp,
                                                        color = accent
                                                )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                        loan.personName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 18.sp
                                                )
                                                Text(
                                                        if (isLent) "You lent" else "You borrowed",
                                                        fontSize = 13.sp,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                        Button(
                                                onClick = {
                                                        if (hasPayData) confirmPayment()
                                                        else onDismiss()
                                                },
                                                modifier =
                                                        Modifier.height(40.dp).animateContentSize(),
                                                shape =
                                                        RoundedCornerShape(
                                                                topStart = 0.dp,
                                                                topEnd = 0.dp,
                                                                bottomStart = 14.dp,
                                                                bottomEnd = 14.dp
                                                        ),
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor = topBtnColor,
                                                                contentColor =
                                                                        if (hasPayData) Color.White
                                                                        else
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                        ),
                                                contentPadding =
                                                        PaddingValues(
                                                                horizontal = 18.dp,
                                                                vertical = 0.dp
                                                        )
                                        ) {
                                                Text(
                                                        if (hasPayData) "Confirm" else "Cancel",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                )
                                        }
                                }
                                Spacer(Modifier.height(18.dp))
                                Column(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .clip(RoundedCornerShape(20.dp))
                                                        .background(accent.copy(alpha = 0.08f))
                                                        .padding(18.dp)
                                ) {
                                        Text(
                                                "Remaining",
                                                fontSize = 12.sp,
                                                color = accent,
                                                fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                                "$symbol ${String.format(Locale.US, "%,.2f", remaining)}",
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .height(6.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceVariant
                                                                                .copy(alpha = 0.5f)
                                                                )
                                        ) {
                                                Box(
                                                        modifier =
                                                                Modifier.fillMaxWidth(
                                                                                animatedProgress
                                                                        )
                                                                        .fillMaxHeight()
                                                                        .clip(CircleShape)
                                                                        .background(accent)
                                                )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                                "${(animatedProgress * 100).roundToInt()}% · " +
                                                        "$symbol ${String.format(Locale.US, "%,.2f", totalRepaid)} of " +
                                                        "$symbol ${String.format(Locale.US, "%,.2f", loan.principalAmount)} repaid",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text("History", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(Modifier.height(8.dp))
                                if (repayments.isEmpty()) {
                                        Text(
                                                "No repayments yet.",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                repayments.forEach { r ->
                                                        AnimatedVisibility(
                                                                visible = true,
                                                                enter =
                                                                        fadeIn(tween(250)) +
                                                                                expandVertically(
                                                                                        tween(250)
                                                                                )
                                                        ) {
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .clip(
                                                                                                RoundedCornerShape(
                                                                                                        14.dp
                                                                                                )
                                                                                        )
                                                                                        .background(
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .surface
                                                                                        )
                                                                                        .padding(
                                                                                                14.dp
                                                                                        ),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .SpaceBetween,
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        Column(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        )
                                                                        ) {
                                                                                Text(
                                                                                        if (r.note.isNotBlank()
                                                                                        )
                                                                                                r.note
                                                                                        else
                                                                                                "Repayment",
                                                                                        fontSize =
                                                                                                14.sp,
                                                                                        fontWeight =
                                                                                                FontWeight
                                                                                                        .Medium,
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurface
                                                                                )
                                                                                Text(
                                                                                        SimpleDateFormat(
                                                                                                        "dd MMM yyyy",
                                                                                                        Locale.getDefault()
                                                                                                )
                                                                                                .format(
                                                                                                        Date(
                                                                                                                r.createdAt
                                                                                                        )
                                                                                                ),
                                                                                        fontSize =
                                                                                                11.sp,
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurfaceVariant
                                                                                )
                                                                        }
                                                                        Text(
                                                                                "$symbol ${String.format(Locale.US, "%,.2f", r.amount)}",
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold,
                                                                                color = GreenSoft,
                                                                                fontSize = 14.sp
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                                AnimatedVisibility(
                                        visible = showPayField && remaining > 0.001,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                ) {
                                        Column {
                                                Spacer(Modifier.height(12.dp))
                                                OutlinedTextField(
                                                        value = payAmount,
                                                        onValueChange = {
                                                                payAmount = sanitizeAmountInput(it)
                                                                error = null
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        placeholder = { Text("Repayment amount") },
                                                        singleLine = true,
                                                        keyboardOptions =
                                                                KeyboardOptions(
                                                                        keyboardType =
                                                                                KeyboardType.Decimal
                                                                ),
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors =
                                                                OutlinedTextFieldDefaults.colors(
                                                                        focusedBorderColor = accent
                                                                )
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                OutlinedTextField(
                                                        value = payNote,
                                                        onValueChange = { payNote = it },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        placeholder = { Text("Note (optional)") },
                                                        shape = RoundedCornerShape(16.dp)
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Surface(
                                                        onClick = { showPayDatePicker = true },
                                                        shape = RoundedCornerShape(16.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        modifier = Modifier.fillMaxWidth()
                                                ) {
                                                        Row(
                                                                Modifier.padding(
                                                                        horizontal = 14.dp,
                                                                        vertical = 14.dp
                                                                ),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(10.dp)
                                                        ) {
                                                                Icon(
                                                                        Icons.Default.CalendarMonth,
                                                                        contentDescription = null,
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                )
                                                                Text(payDateLabel, fontSize = 14.sp)
                                                        }
                                                }
                                                error?.let {
                                                        Text(
                                                                it,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .error,
                                                                fontSize = 13.sp,
                                                                modifier =
                                                                        Modifier.padding(top = 6.dp)
                                                        )
                                                }
                                        }
                                }
                                Spacer(Modifier.height(8.dp))
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                                Button(
                                        onClick = {
                                                if (showPayField) {
                                                        payAmount = ""
                                                        payNote = ""
                                                        payDate = System.currentTimeMillis()
                                                        error = null
                                                }
                                                showPayField = !showPayField
                                        },
                                        enabled = remaining > 0.001,
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(containerColor = accent)
                                ) {
                                        Text(
                                                if (showPayField) "Hide" else "Add payment",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                        )
                                }
                                OutlinedButton(
                                        onClick = onDeleteRequest,
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        shape = RoundedCornerShape(16.dp)
                                ) {
                                        Text(
                                                "Delete",
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold
                                        )
                                }
                        }
                        Spacer(Modifier.height(28.dp))
                }
        }
        if (showPayDatePicker) {
                LoanDatePicker(
                        initialMillis = payDate,
                        onDismiss = { showPayDatePicker = false },
                        onConfirm = { payDate = it }
                )
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanDatePicker(initialMillis: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
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
