package com.masud.moneymanager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masud.moneymanager.data.AppDatabase
import com.masud.moneymanager.data.TransactionEntity
    import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionsScreen(
    db: AppDatabase,
    currency: String = "BDT",
    onAddTransaction: () -> Unit = {}
) {
    val dao = db.transactionDao()
    val transactions by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var search by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var deleteTarget by remember { mutableStateOf<TransactionEntity?>(null) }

    val filtered = transactions.filter { transaction ->
        val matchesSearch =
            transaction.title.contains(search, ignoreCase = true) ||
            transaction.category.contains(search, ignoreCase = true) ||
            transaction.note.contains(search, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "Income" -> transaction.type == "INCOME"
            "Expense" -> transaction.type == "EXPENSE"
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp, bottom = 12.dp)
            )

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search transactions") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(16.dp)
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                listOf("All", "Income", "Expense").forEachIndexed { index, filter ->
                    SegmentedButton(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                    ) {
                        Text(filter)
                    }
                }
            }

            if (filtered.isEmpty()) {
                EmptyState(
                    title = "No transactions found",
                    message = if (search.isBlank()) {
                        "Add an income or expense to get started."
                    } else {
                        "Try another search or filter."
                    }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            currency = currency,
                            onDelete = { deleteTarget = transaction }
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { transaction ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete transaction?") },
            text = { Text("Delete “${transaction.title}”? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { db.transactionDao().deleteById(transaction.id) }
                        deleteTarget = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TransactionItem(
    transaction: TransactionEntity,
    currency: String,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == "INCOME"
    val tint = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    val date = remember(transaction.createdAt) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transaction.createdAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = tint
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = transaction.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = "${transaction.category} • $date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = (if (isIncome) "+" else "−") + formatMoney(transaction.amount, currency),
                    color = tint,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete transaction", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}