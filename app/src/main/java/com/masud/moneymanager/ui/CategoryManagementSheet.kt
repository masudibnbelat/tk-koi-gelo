// FILE: app/src/main/java/com/masud/moneymanager/ui/CategoryManagementSheet.kt
package com.masud.moneymanager.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masud.moneymanager.data.AppDatabase
import com.masud.moneymanager.data.CategoryEntity
import kotlinx.coroutines.launch

private val ExpenseRed = Color(0xFFFF5C5C)
private val IncomeGreen = Color(0xFF1ED760)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementSheet(
    db: AppDatabase,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Expense, 1 = Income
    val type = if (selectedTab == 0) "EXPENSE" else "INCOME"
    val accent = if (selectedTab == 0) ExpenseRed else IncomeGreen

    val categories by db.categoryDao()
        .observeByType(type)
        .collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    var expandedId by remember { mutableStateOf<Long?>(null) }

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
                .padding(bottom = 24.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manage Categories", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Expense / Income tab
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Expense", "Income").forEachIndexed { index, label ->
                    val selected = selectedTab == index
                    val tabColor = if (index == 0) ExpenseRed else IncomeGreen
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { selectedTab = index; expandedId = null }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) tabColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Add button
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (selectedTab == 0) "Add Expense Category" else "Add Income Category",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            if (categories.isEmpty()) {
                EmptyState(
                    title = "No categories yet",
                    message = "Tap the button above to add your first one."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        val expanded = expandedId == cat.id
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                expandedId = if (expanded) null else cat.id
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .animateContentSize(animationSpec = tween(250))
                            ) {
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
                                            modifier = Modifier.size(30.dp).background(accent.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(modifier = Modifier.size(10.dp).background(accent, CircleShape))
                                        }
                                        Text(
                                            cat.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        if (expanded) "▲" else "▼",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }

                                // Animated edit/delete buttons
                                AnimatedVisibility(
                                    visible = expanded,
                                    enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                                    exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { editingTarget = cat; expandedId = null },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, accent)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Edit", color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { deleteTarget = cat; expandedId = null },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add dialog — duplicate name check soho
    if (showAddDialog) {
        CategoryEditDialog(
            title = if (selectedTab == 0) "Add Expense Category" else "Add Income Category",
            initialName = "",
            accent = accent,
            existingNames = categories.map { it.name },
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                scope.launch {
                    db.categoryDao().insert(CategoryEntity(name = name, type = type))
                }
                showAddDialog = false
            }
        )
    }

    // Edit dialog — nijer nam bad diye duplicate check
    editingTarget?.let { target ->
        CategoryEditDialog(
            title = "Edit Category",
            initialName = target.name,
            accent = accent,
            existingNames = categories.map { it.name }.filter { it != target.name },
            onDismiss = { editingTarget = null },
            onConfirm = { name ->
                scope.launch {
                    db.categoryDao().update(target.copy(name = name))
                }
                editingTarget = null
            }
        )
    }

    // Delete confirm
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete category?") },
            text = { Text("Delete \"${target.name}\"? Transactions using it will keep the old name.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { db.categoryDao().deleteById(target.id) }
                    deleteTarget = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CategoryEditDialog(
    title: String,
    initialName: String,
    accent: Color,
    existingNames: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    placeholder = { Text("Category name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
                )
                AnimatedVisibility(visible = error != null) {
                    Text(
                        error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = name.trim()
                when {
                    trimmed.isEmpty() -> error = "Enter a name."
                    existingNames.any { it.equals(trimmed, ignoreCase = true) } ->
                        error = "This category already exists."
                    else -> onConfirm(trimmed)
                }
            }) {
                Icon(Icons.Default.Check, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save", color = accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
