// FILE: app/src/main/java/com/masud/moneymanager/ui/LoansScreen.kt
package com.masud.moneymanager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.masud.moneymanager.data.AppDatabase
import com.masud.moneymanager.data.LoanEntity
import com.masud.moneymanager.data.LoanRepaymentEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val DarkGreen = Color(0xFF2E7D32)
private val DarkRed = Color(0xFFB71C1C)

@Composable
fun LoansScreen(db: AppDatabase, currency: String = "BDT") {
        val dao = db.loanDao()
        val loans by dao.observeAllLoans().collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()
        var selectedTab by remember { mutableIntStateOf(0) }
        var showAddSheet by remember { mutableStateOf(false) }
        var editingLoan by remember { mutableStateOf<LoanEntity?>(null) }
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
                        Spacer(Modifier.height(16.dp))
                        Text(
                                "Loans",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(14.dp))
                        LoanHeroCard(
                                toReceive = toReceive,
                                toPay = toPay,
                                symbol = symbol
                        )
                        Spacer(Modifier.height(14.dp))
                        Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Row(
                                        modifier = Modifier.padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                        listOf("Active" to DarkGreen, "Done" to DarkRed)
                                                .forEachIndexed { i, (label, tint) ->
                                                        val selected = selectedTab == i
                                                        val count =
                                                                if (i == 0) activeLoans.size
                                                                else completedLoans.size
                                                        val tabBg by
                                                                animateColorAsState(
                                                                        targetValue =
                                                                                if (selected) tint
                                                                                else Color.Transparent,
                                                                        animationSpec =
                                                                                tween(250),
                                                                        label = "tabBg"
                                                                )
                                                        val labelColor =
                                                                if (selected) Color.White
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        val badgeBg =
                                                                if (selected)
                                                                        Color.White.copy(
                                                                                alpha = 0.22f
                                                                        )
                                                                else tint.copy(alpha = 0.12f)
                                                        val badgeColor =
                                                                if (selected) Color.White
                                                                else tint
                                                        Box(
                                                                modifier =
                                                                        Modifier.weight(1f)
                                                                                .clip(
                                                                                        RoundedCornerShape(
                                                                                                10.dp
                                                                                        )
                                                                                )
                                                                                .background(tabBg)
                                                                                .clickable {
                                                                                        selectedTab =
                                                                                                i
                                                                                }
                                                                                .padding(
                                                                                        vertical =
                                                                                                9.dp
                                                                                ),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                Row(
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically,
                                                                        horizontalArrangement =
                                                                                Arrangement.spacedBy(
                                                                                        6.dp
                                                                                )
                                                                ) {
                                                                        Text(
                                                                                label,
                                                                                fontWeight =
                                                                                        if (selected)
                                                                                                FontWeight
                                                                                                        .Bold
                                                                                        else
                                                                                                FontWeight
                                                                                                        .Medium,
                                                                                fontSize = 13.sp,
                                                                                color = labelColor
                                                                        )
                                                                        Surface(
                                                                                shape =
                                                                                        RoundedCornerShape(
                                                                                                6.dp
                                                                                        ),
                                                                                color = badgeBg
                                                                        ) {
                                                                                Text(
                                                                                        count.toString(),
                                                                                        fontSize =
                                                                                                11.sp,
                                                                                        fontWeight =
                                                                                                FontWeight
                                                                                                        .Bold,
                                                                                        color =
                                                                                                badgeColor,
                                                                                        modifier =
                                                                                                Modifier.padding(
                                                                                                        horizontal =
                                                                                                                7.dp,
                                                                                                        vertical =
                                                                                                                2.dp
                                                                                                )
                                                                                )
                                                                        }
                                                                }
                                                        }
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
        if (showAddSheet || editingLoan != null) {
                AddLoanSheet(
                        editing = editingLoan,
                        alreadyRepaid = editingLoan?.let { repayments[it.id] } ?: 0.0,
                        onDismiss = {
                                showAddSheet = false
                                editingLoan = null
                        },
                        onSave = { person, type, amount, note ->
                                val current = editingLoan
                                scope.launch {
                                        if (current != null) {
                                                val updated =
                                                        current.copy(
                                                                personName = person,
                                                                type = type,
                                                                principalAmount = amount,
                                                                note = note
                                                        )
                                                dao.updateLoan(updated)
                                                selectedLoan = updated
                                        } else {
                                                dao.insertLoan(
                                                        LoanEntity(
                                                                personName = person,
                                                                type = type,
                                                                principalAmount = amount,
                                                                note = note
                                                        )
                                                )
                                        }
                                }
                                editingLoan = null
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
                        onEditRequest = { editingLoan = it },
                        onDeleteRequest = {
                                deleteTarget = loan
                                selectedLoan = null
                        },
                        onRepaymentAdded = { repayTick++ }
                )
        }
        deleteTarget?.let { loan ->
                DeleteLoanDialog(
                        personName = loan.personName,
                        onDismiss = { deleteTarget = null },
                        onConfirm = {
                                scope.launch { dao.deleteLoan(loan.id) }
                                deleteTarget = null
                        }
                )
        }
}

@Composable
private fun DeleteLoanDialog(
        personName: String,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
) {
        Dialog(onDismissRequest = onDismiss) {
                Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                ) {
                        Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                Box(
                                        modifier =
                                                Modifier.size(64.dp)
                                                        .background(
                                                                MaterialTheme.colorScheme.error.copy(
                                                                        alpha = 0.12f
                                                                ),
                                                                CircleShape
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(28.dp)
                                        )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                        "Delete loan?",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                        "Remove the loan for $personName and all of its repayment history? This can't be undone.",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(24.dp))
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        Button(
                                                onClick = onDismiss,
                                                modifier = Modifier.weight(1f).height(52.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceVariant.copy(
                                                                                alpha = 0.6f
                                                                        ),
                                                                contentColor =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                        ) {
                                                Text("Cancel", fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                                onClick = onConfirm,
                                                modifier = Modifier.weight(1f).height(52.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor = DarkRed,
                                                                contentColor = Color.White
                                                        )
                                        ) {
                                                Text("Delete", fontWeight = FontWeight.Bold)
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun LoanHeroCard(
        toReceive: Double,
        toPay: Double,
        symbol: String,
        modifier: Modifier = Modifier
) {
        Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
                Row(
                        modifier = Modifier.padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        LoanStat(
                                title = "To receive",
                                amount = toReceive,
                                symbol = symbol,
                                color = DarkGreen,
                                icon = Icons.Default.ArrowDownward,
                                modifier = Modifier.weight(1f)
                        )
                        Box(
                                modifier =
                                        Modifier.width(1.dp)
                                                .height(44.dp)
                                                .background(
                                                        MaterialTheme.colorScheme.outlineVariant
                                                )
                        )
                        LoanStat(
                                title = "To pay",
                                amount = toPay,
                                symbol = symbol,
                                color = DarkRed,
                                icon = Icons.Default.ArrowUpward,
                                modifier = Modifier.weight(1f)
                        )
                }
        }
}

@Composable
private fun LoanStat(
        title: String,
        amount: Double,
        symbol: String,
        color: Color,
        icon: ImageVector,
        modifier: Modifier = Modifier
) {
        Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                        Box(
                                modifier =
                                        Modifier.size(26.dp)
                                                .background(
                                                        color.copy(alpha = 0.12f),
                                                        CircleShape
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(14.dp)
                                )
                        }
                        Text(
                                title,
                                fontSize = 11.sp,
                                letterSpacing = 0.8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                        )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                        "$symbol ${String.format(Locale.US, "%,.0f", amount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                )
        }
}

@Composable
private fun LoanCard(loan: LoanEntity, repaid: Double, symbol: String, onClick: () -> Unit) {
        val isLent = loan.type == "LENT"
        val accent = if (isLent) DarkGreen else DarkRed
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
        val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        Surface(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 1.dp
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        // Progress ring avatar
                        Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(54.dp)
                        ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                        val stroke = 5.dp.toPx()
                                        drawArc(
                                                color = trackColor,
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                                color = accent,
                                                startAngle = -90f,
                                                sweepAngle = 360f * animatedProgress,
                                                useCenter = false,
                                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                                        )
                                }
                                Box(
                                        modifier =
                                                Modifier.size(42.dp)
                                                        .background(
                                                                accent.copy(alpha = 0.12f),
                                                                CircleShape
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Text(
                                                loan.personName.firstOrNull()?.uppercase() ?: "?",
                                                fontWeight = FontWeight.Bold,
                                                color = accent,
                                                fontSize = 16.sp
                                        )
                                }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        loan.personName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                        (if (isLent) "Lent" else "Borrowed") +
                                                " · ${(animatedProgress * 100).roundToInt()}% cleared",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                                Text(
                                        "$symbol ${String.format(Locale.US, "%,.0f", remaining)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = accent,
                                        maxLines = 1
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                        "of $symbol ${String.format(Locale.US, "%,.0f", loan.principalAmount)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                )
                        }
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLoanSheet(
        editing: LoanEntity? = null,
        alreadyRepaid: Double = 0.0,
        onDismiss: () -> Unit,
        onSave: (String, String, Double, String) -> Unit
) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val isEditing = editing != null
        var person by remember(editing) { mutableStateOf(editing?.personName.orEmpty()) }
        var type by remember(editing) { mutableStateOf(editing?.type ?: "LENT") }
        var amount by remember(editing) {
                mutableStateOf(
                        editing?.let { e ->
                                val a = e.principalAmount
                                if (a % 1.0 == 0.0) a.toLong().toString() else a.toString()
                        } ?: ""
                )
        }
        var note by remember(editing) { mutableStateOf(editing?.note.orEmpty()) }
        var error by remember { mutableStateOf<String?>(null) }
        val accent = if (type == "LENT") DarkGreen else DarkRed
        val saveBg by
                animateColorAsState(
                        targetValue = accent,
                        animationSpec = tween(300),
                        label = "saveBg"
                )
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
                                Text(
                                        if (isEditing) "Edit loan" else "New loan",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                        onClick = onDismiss,
                                        modifier =
                                                Modifier.size(36.dp)
                                                        .background(DarkRed, CircleShape)
                                ) {
                                        Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                        )
                                }
                        }
                        Spacer(Modifier.height(18.dp))
                        BoxWithConstraints(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                                .copy(alpha = 0.35f)
                                                )
                                                .padding(4.dp)
                        ) {
                                val pillW = maxWidth / 2
                                val pillX by
                                        animateDpAsState(
                                                targetValue = if (type == "LENT") 0.dp else pillW,
                                                animationSpec =
                                                        tween(
                                                                320,
                                                                easing = FastOutSlowInEasing
                                                        ),
                                                label = "typePillX"
                                        )
                                val pillBg by
                                        animateColorAsState(
                                                targetValue = accent.copy(alpha = 0.16f),
                                                animationSpec = tween(320),
                                                label = "typePillBg"
                                        )
                                Box(
                                        modifier =
                                                Modifier.width(pillW)
                                                        .height(46.dp)
                                                        .offset(x = pillX)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(pillBg)
                                )
                                Row(modifier = Modifier.fillMaxWidth().height(46.dp)) {
                                        listOf("LENT" to "Lent", "BORROWED" to "Borrowed").forEach {
                                                (value, label) ->
                                                val selected = type == value
                                                val c =
                                                        if (value == "LENT") DarkGreen
                                                        else DarkRed
                                                val textColor by
                                                        animateColorAsState(
                                                                targetValue =
                                                                        if (selected) c
                                                                        else
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant,
                                                                animationSpec = tween(250),
                                                                label = "typeText"
                                                        )
                                                Box(
                                                        modifier =
                                                                Modifier.weight(1f)
                                                                        .fillMaxHeight()
                                                                        .clip(
                                                                                RoundedCornerShape(
                                                                                        12.dp
                                                                                )
                                                                        )
                                                                        .clickable {
                                                                                type = value
                                                                        },
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Text(
                                                                label,
                                                                fontWeight =
                                                                        if (selected)
                                                                                FontWeight.Bold
                                                                        else FontWeight.Medium,
                                                                fontSize = 14.sp,
                                                                color = textColor
                                                        )
                                                }
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
                                leadingIcon = {
                                        Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                },
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
                                                isEditing && value < alreadyRepaid ->
                                                        error =
                                                                "Can't go below the repaid amount."
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
                                                containerColor = saveBg,
                                                contentColor = Color.White
                                        )
                        ) {
                                Text(
                                        if (isEditing) "Save changes" else "Save loan",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                )
                        }
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
        onEditRequest: (LoanEntity) -> Unit,
        onDeleteRequest: () -> Unit,
        onRepaymentAdded: () -> Unit = {}
) {
        val dao = db.loanDao()
        val repayments by dao.observeRepayments(loan.id).collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var showPayForm by remember { mutableStateOf(false) }
        var payAmount by remember { mutableStateOf("") }
        var payNote by remember { mutableStateOf("") }
        var payDate by remember { mutableStateOf(System.currentTimeMillis()) }
        var showPayDatePicker by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var showTopUpForm by remember { mutableStateOf(false) }
        var topUpAmount by remember { mutableStateOf("") }
        var topUpNote by remember { mutableStateOf("") }
        var topUpError by remember { mutableStateOf<String?>(null) }
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
        val accent = if (isLent) DarkGreen else DarkRed
        val payDateLabel =
                remember(payDate) {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(payDate))
                }

        fun resetPayForm() {
                payAmount = ""
                payNote = ""
                payDate = System.currentTimeMillis()
                error = null
                showPayForm = false
        }

        fun resetTopUpForm() {
                topUpAmount = ""
                topUpNote = ""
                topUpError = null
                showTopUpForm = false
        }

        ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.background,
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
                        // Header: avatar + name + type pill, close action
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
                                                loan.personName.firstOrNull()?.uppercase() ?: "?",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = accent
                                        )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                loan.personName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 19.sp,
                                                color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = accent.copy(alpha = 0.12f)
                                        ) {
                                                Text(
                                                        if (isLent) "LENT" else "BORROWED",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = accent,
                                                        modifier =
                                                                Modifier.padding(
                                                                        horizontal = 10.dp,
                                                                        vertical = 4.dp
                                                                )
                                                )
                                        }
                                }
                                IconButton(
                                        onClick = onDismiss,
                                        modifier =
                                                Modifier.size(40.dp)
                                                        .background(
                                                                DarkRed,
                                                                RoundedCornerShape(
                                                                        topStart = 0.dp,
                                                                        topEnd = 0.dp,
                                                                        bottomEnd = 0.dp,
                                                                        bottomStart = 14.dp
                                                                )
                                                        )
                                ) {
                                        Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                        )
                                }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Hero: remaining
                        Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = accent.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Column(Modifier.padding(18.dp)) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Text(
                                                        "Remaining",
                                                        fontSize = 12.sp,
                                                        color = accent,
                                                        fontWeight = FontWeight.Medium
                                                )
                                                Row(
                                                        verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                        if (remaining <= 0.001) {
                                                                Surface(
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        8.dp
                                                                                ),
                                                                        color =
                                                                                DarkGreen.copy(
                                                                                        alpha = 0.15f
                                                                                )
                                                                ) {
                                                                        Text(
                                                                                "Paid off",
                                                                                fontSize = 11.sp,
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold,
                                                                                color = DarkGreen,
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                horizontal =
                                                                                                        10.dp,
                                                                                                vertical =
                                                                                                        4.dp
                                                                                        )
                                                                        )
                                                                }
                                                                Spacer(Modifier.width(4.dp))
                                                        }
                                                        IconButton(
                                                                onClick = { onEditRequest(loan) },
                                                                modifier = Modifier.size(36.dp)
                                                        ) {
                                                                Icon(
                                                                        Icons.Default.Edit,
                                                                        contentDescription =
                                                                                "Edit loan",
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant,
                                                                        modifier =
                                                                                Modifier.size(18.dp)
                                                                )
                                                        }
                                                        IconButton(
                                                                onClick = onDeleteRequest,
                                                                modifier = Modifier.size(36.dp)
                                                        ) {
                                                                Icon(
                                                                        Icons.Default.Delete,
                                                                        contentDescription =
                                                                                "Delete loan",
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error,
                                                                        modifier =
                                                                                Modifier.size(18.dp)
                                                                )
                                                        }
                                                }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                                "$symbol ${String.format(Locale.US, "%,.2f", remaining)}",
                                                fontSize = 32.sp,
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
                                                                                .surfaceVariant.copy(
                                                                                alpha = 0.5f
                                                                        )
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
                                                "${(animatedProgress * 100).roundToInt()}% repaid · " +
                                                        "$symbol ${String.format(Locale.US, "%,.2f", totalRepaid)} of " +
                                                        "$symbol ${String.format(Locale.US, "%,.2f", loan.principalAmount)}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Actions — payment and top-up; no Hide toggle while a form is open
                        if (!showPayForm && !showTopUpForm) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                        if (remaining > 0.001) {
                                                Button(
                                                        onClick = { showPayForm = true },
                                                        modifier =
                                                                Modifier.weight(1f).height(52.dp),
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors =
                                                                ButtonDefaults.buttonColors(
                                                                        containerColor = accent
                                                                )
                                                ) {
                                                        Icon(
                                                                Icons.Default.Add,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                                "Payment",
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                        )
                                                }
                                        }
                                        Button(
                                                onClick = { showTopUpForm = true },
                                                modifier = Modifier.weight(1f).height(52.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor =
                                                                        accent.copy(alpha = 0.12f),
                                                                contentColor = accent
                                                        )
                                        ) {
                                                Icon(
                                                        if (isLent) Icons.Default.ArrowUpward
                                                        else Icons.Default.ArrowDownward,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                        if (isLent) "Lend" else "Borrow",
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }
                                }
                                Spacer(Modifier.height(16.dp))
                        }

                        // Pay form card — own Cancel/Save, fully replaces the old toggle flow
                        AnimatedVisibility(
                                visible = showPayForm && remaining > 0.001,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                        ) {
                                Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                                        tonalElevation = 1.dp,
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Column(Modifier.padding(16.dp)) {
                                                Text(
                                                        "New payment",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                )
                                                Spacer(Modifier.height(10.dp))
                                                OutlinedTextField(
                                                        value = payAmount,
                                                        onValueChange = {
                                                                payAmount = sanitizeAmountInput(it)
                                                                error = null
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        placeholder = { Text("Amount") },
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
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant.copy(
                                                                        alpha = 0.4f
                                                                ),
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
                                                Spacer(Modifier.height(12.dp))
                                                Row(
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(10.dp)
                                                ) {
                                                        TextButton(
                                                                onClick = { resetPayForm() },
                                                                modifier =
                                                                        Modifier.weight(1f)
                                                                                .height(50.dp),
                                                                shape = RoundedCornerShape(16.dp)
                                                        ) {
                                                                Text(
                                                                        "Cancel",
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = accent
                                                                )
                                                        }
                                                        Button(
                                                                onClick = {
                                                                        val v =
                                                                                payAmount.trim()
                                                                                        .toDoubleOrNull()
                                                                        when {
                                                                                v == null ||
                                                                                        !v.isFinite() ||
                                                                                        v <= 0.0 ->
                                                                                        error =
                                                                                                "Enter a valid amount."
                                                                                v > remaining ->
                                                                                        error =
                                                                                                "Can't exceed remaining balance."
                                                                                else -> {
                                                                                        scope.launch {
                                                                                                dao.insertRepayment(
                                                                                                        LoanRepaymentEntity(
                                                                                                                loanId =
                                                                                                                        loan.id,
                                                                                                                amount =
                                                                                                                        v,
                                                                                                                note =
                                                                                                                        payNote.trim(),
                                                                                                                createdAt =
                                                                                                                        payDate
                                                                                                        )
                                                                                                )
                                                                                                onRepaymentAdded()
                                                                                        }
                                                                                        resetPayForm()
                                                                                }
                                                                        }
                                                                },
                                                                modifier =
                                                                        Modifier.weight(1f)
                                                                                .height(50.dp),
                                                                shape = RoundedCornerShape(16.dp),
                                                                colors =
                                                                        ButtonDefaults.buttonColors(
                                                                                containerColor =
                                                                                        accent
                                                                        )
                                                        ) {
                                                                Text(
                                                                        "Save",
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color.White
                                                                )
                                                        }
                                                }
                                        }
                                }
                                Spacer(Modifier.height(16.dp))
                        }

                        // Top-up form — increase the loan (stored as a negative repayment,
                        // so remaining and history update automatically)
                        AnimatedVisibility(
                                visible = showTopUpForm,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                        ) {
                                Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                                        tonalElevation = 1.dp,
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Column(Modifier.padding(16.dp)) {
                                                Text(
                                                        if (isLent) "Lend" else "Borrow",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                )
                                                Spacer(Modifier.height(10.dp))
                                                OutlinedTextField(
                                                        value = topUpAmount,
                                                        onValueChange = {
                                                                topUpAmount =
                                                                        sanitizeAmountInput(it)
                                                                topUpError = null
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        placeholder = { Text("Amount") },
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
                                                        value = topUpNote,
                                                        onValueChange = { topUpNote = it },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        placeholder = { Text("Note (optional)") },
                                                        shape = RoundedCornerShape(16.dp)
                                                )
                                                topUpError?.let {
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
                                                Spacer(Modifier.height(12.dp))
                                                Row(
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(10.dp)
                                                ) {
                                                        TextButton(
                                                                onClick = { resetTopUpForm() },
                                                                modifier =
                                                                        Modifier.weight(1f)
                                                                                .height(50.dp),
                                                                shape = RoundedCornerShape(16.dp)
                                                        ) {
                                                                Text(
                                                                        "Cancel",
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = accent
                                                                )
                                                        }
                                                        Button(
                                                                onClick = {
                                                                        val v =
                                                                                topUpAmount.trim()
                                                                                        .toDoubleOrNull()
                                                                        if (v == null ||
                                                                                        !v.isFinite() ||
                                                                                        v <= 0.0
                                                                        ) {
                                                                                topUpError =
                                                                                        "Enter a valid amount."
                                                                        } else {
                                                                                scope.launch {
                                                                                        dao.insertRepayment(
                                                                                                LoanRepaymentEntity(
                                                                                                        loanId =
                                                                                                                loan.id,
                                                                                                        amount =
                                                                                                                -v,
                                                                                                        note =
                                                                                                                topUpNote.trim(),
                                                                                                        createdAt =
                                                                                                                System.currentTimeMillis()
                                                                                                )
                                                                                        )
                                                                                        onRepaymentAdded()
                                                                                }
                                                                                resetTopUpForm()
                                                                        }
                                                                },
                                                                modifier =
                                                                        Modifier.weight(1f)
                                                                                .height(50.dp),
                                                                shape = RoundedCornerShape(16.dp),
                                                                colors =
                                                                        ButtonDefaults.buttonColors(
                                                                                containerColor =
                                                                                        accent
                                                                        )
                                                        ) {
                                                                Text(
                                                                        "Save",
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color.White
                                                                )
                                                        }
                                                }
                                        }
                                }
                                Spacer(Modifier.height(16.dp))
                        }

                        // History — timeline style
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        "History",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                )
                                Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accent.copy(alpha = 0.12f)
                                ) {
                                        Text(
                                                repayments.size.toString(),
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
                        }
                        Spacer(Modifier.height(10.dp))
                        if (repayments.isEmpty()) {
                                Text(
                                        "No repayments yet.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                )
                        } else {
                                RepaymentTimeline(
                                        repayments = repayments,
                                        symbol = symbol,
                                        accent = accent
                                )
                        }

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

@Composable
private fun RepaymentTimeline(
        repayments: List<LoanRepaymentEntity>,
        symbol: String,
        accent: Color
) {
        Column(modifier = Modifier.fillMaxWidth()) {
                repayments.forEachIndexed { index, r ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(top = 4.dp)
                                ) {
                                        Box(
                                                modifier =
                                                        Modifier.size(12.dp)
                                                                .background(accent, CircleShape)
                                        )
                                        if (index != repayments.lastIndex) {
                                                Box(
                                                        modifier =
                                                                Modifier.width(2.dp)
                                                                        .height(46.dp)
                                                                        .background(
                                                                                accent.copy(
                                                                                        alpha = 0.25f
                                                                                )
                                                                        )
                                                )
                                        }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(
                                        modifier =
                                                Modifier.weight(1f)
                                                        .padding(
                                                                bottom =
                                                                        if (index !=
                                                                                        repayments
                                                                                                .lastIndex
                                                                        )
                                                                                10.dp
                                                                        else 0.dp
                                                        )
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                val isTopUp = r.amount < 0
                                                Text(
                                                        if (r.note.isNotBlank()) r.note
                                                        else if (isTopUp) "Top-up"
                                                        else "Repayment",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                        (if (isTopUp) "+ " else "") +
                                                                "$symbol ${String.format(Locale.US, "%,.2f", kotlin.math.abs(r.amount))}",
                                                        fontWeight = FontWeight.Bold,
                                                        color = accent,
                                                        fontSize = 14.sp
                                                )
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                                        .format(Date(r.createdAt)),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                        }
                }
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
