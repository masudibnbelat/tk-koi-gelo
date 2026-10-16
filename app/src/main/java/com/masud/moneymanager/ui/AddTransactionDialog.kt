package com.masud.moneymanager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masud.moneymanager.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

private val ExpenseRed = Color(0xFFFF5C5C)
private val IncomeGreen = Color(0xFF1ED760)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    currency: String = "BDT",
    expenseCategories: List<String> = Categories.expense,
    incomeCategories: List<String> = Categories.income,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val safeExpenseCats = expenseCategories.ifEmpty { listOf("General") }
    val safeIncomeCats = incomeCategories.ifEmpty { listOf("Other") }

    var expenseAmount by remember { mutableStateOf("") }
    var expenseCategory by remember(safeExpenseCats) { mutableStateOf(safeExpenseCats.first()) }
    var incomeAmount by remember { mutableStateOf("") }
    var incomeCategory by remember(safeIncomeCats) { mutableStateOf(safeIncomeCats.first()) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var showExpenseCategoryMenu by remember { mutableStateOf(false) }
    var showIncomeCategoryMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val dateLabel =
        remember(selectedDate) {
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(selectedDate.time)
        }
    val accent = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .animateContentSize(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Add Transaction", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(38.dp).background(accent, CircleShape),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            Text("Expense", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = expenseAmount,
                onValueChange = {
                    expenseAmount = sanitizeAmountInput(it)
                    if (expenseAmount.isNotBlank()) incomeAmount = ""
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Expense Amount (${currencySymbol(currency)})") },
                leadingIcon = { Text(currencySymbol(currency), color = ExpenseRed, fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ExpenseRed,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
            )

            AnimatedVisibility(
                visible = expenseAmount.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Expense Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            CategoryDropdownField(
                selected = expenseCategory,
                categories = safeExpenseCats,
                expanded = showExpenseCategoryMenu,
                onExpandedChange = { showExpenseCategoryMenu = it },
                onSelect = {
                    expenseCategory = it
                    showExpenseCategoryMenu = false
                },
                accentColor = ExpenseRed,
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            Text("Income", color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = incomeAmount,
                onValueChange = {
                    incomeAmount = sanitizeAmountInput(it)
                    if (incomeAmount.isNotBlank()) expenseAmount = ""
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Income Amount (${currencySymbol(currency)})") },
                leadingIcon = { Text(currencySymbol(currency), color = IncomeGreen, fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IncomeGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
            )

            AnimatedVisibility(
                visible = incomeAmount.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Income Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            CategoryDropdownField(
                selected = incomeCategory,
                categories = safeIncomeCats,
                expanded = showIncomeCategoryMenu,
                onExpandedChange = { showIncomeCategoryMenu = it },
                onSelect = {
                    incomeCategory = it
                    showIncomeCategoryMenu = false
                },
                accentColor = IncomeGreen,
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            Text("Details", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column {
                        Text("Transaction Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(dateLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Note (Optional)") },
                shape = RoundedCornerShape(16.dp),
            )

            AnimatedVisibility(visible = error != null) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    Text(error ?: "", color = ExpenseRed, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = {
                    val isExpense = expenseAmount.isNotBlank()
                    val rawAmount = if (isExpense) expenseAmount else incomeAmount
                    val parsedAmount = rawAmount.trim().toDoubleOrNull()

                    when {
                        rawAmount.isBlank() -> {
                            error = "Enter an amount."
                        }

                        parsedAmount == null || !parsedAmount.isFinite() || parsedAmount <= 0.0 -> {
                            error = "Enter an amount greater than zero."
                        }

                        else -> {
                            onSave(
                                TransactionEntity(
                                    title = title.trim().ifBlank { if (isExpense) expenseCategory else incomeCategory },
                                    amount = parsedAmount,
                                    type = if (isExpense) "EXPENSE" else "INCOME",
                                    category = if (isExpense) expenseCategory else incomeCategory,
                                    note = note.trim(),
                                    createdAt = selectedDate.timeInMillis,
                                ),
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
            ) {
                Text("Save Transaction", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }

    if (showDatePicker) {
        SimpleDatePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                selectedDate = it
                showDatePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdownField(
    selected: String,
    categories: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    accentColor: Color,
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        Surface(
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(26.dp).background(accentColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(modifier = Modifier.size(9.dp).background(accentColor, CircleShape))
                    }
                    Text(selected, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        }

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            categories.forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = { onSelect(item) })
            }
        }
    }
}
