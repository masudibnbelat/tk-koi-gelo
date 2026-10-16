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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
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
fun EditTransactionDialog(
    transaction: TransactionEntity,
    currency: String = "BDT",
    availableCategories: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onUpdate: (TransactionEntity) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Type fixed — original transaction er tai thakbe
    val type = transaction.type
    val isExpenseType = type == "EXPENSE"
    val fallback = if (isExpenseType) Categories.expense else Categories.income
    val categories = availableCategories.ifEmpty { fallback }
    val sectionColor = if (isExpenseType) ExpenseRed else IncomeGreen

    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var category by remember {
        mutableStateOf(
            transaction.category.takeIf { it.isNotBlank() }
                ?: categories.firstOrNull().orEmpty(),
        )
    }
    val dropdownCats =
        remember(categories, category) {
            if (category.isNotBlank() && category !in categories) {
                listOf(category) + categories
            } else {
                categories
            }
        }
    var title by remember { mutableStateOf(transaction.title) }
    var note by remember { mutableStateOf(transaction.note) }
    var selectedDate by remember {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = transaction.createdAt })
    }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val dateLabel =
        remember(selectedDate) {
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(selectedDate.time)
        }

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
                Text("Edit Transaction", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(if (isExpenseType) "Expense" else "Income", color = sectionColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = sanitizeAmountInput(it)
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Amount (${currencySymbol(currency)})") },
                leadingIcon = { Text(currencySymbol(currency), color = sectionColor, fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = sectionColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
            )

            AnimatedVisibility(
                visible = amount.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            CategoryDropdownField(
                selected = category.ifBlank { dropdownCats.firstOrNull().orEmpty() },
                categories = dropdownCats,
                expanded = showCategoryMenu,
                onExpandedChange = { showCategoryMenu = it },
                onSelect = {
                    category = it
                    showCategoryMenu = false
                },
                accentColor = sectionColor,
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
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ExpenseRed),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val parsedAmount = amount.trim().toDoubleOrNull()
                        when {
                            amount.isBlank() -> {
                                error = "Enter an amount."
                            }

                            parsedAmount == null || !parsedAmount.isFinite() || parsedAmount <= 0.0 -> {
                                error = "Enter an amount greater than zero."
                            }

                            category.isBlank() -> {
                                error = "Select a category."
                            }

                            else -> {
                                onUpdate(
                                    transaction.copy(
                                        title = title.trim().ifBlank { category },
                                        amount = parsedAmount,
                                        type = type,
                                        category = category,
                                        note = note.trim(),
                                        createdAt = selectedDate.timeInMillis,
                                    ),
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Update", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete transaction?") },
            text = { Text("Delete “${transaction.title}”? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
