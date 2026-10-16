package com.masud.moneymanager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

private enum class PickerStep { YEAR, MONTH, DAY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog(
    initialDate: Calendar,
    onDismiss: () -> Unit,
    onConfirm: (Calendar) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var year by remember { mutableStateOf(initialDate.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(initialDate.get(Calendar.MONTH)) }
    var day by remember { mutableStateOf(initialDate.get(Calendar.DAY_OF_MONTH)) }
    var step by remember { mutableStateOf(PickerStep.DAY) }

    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val accent = MaterialTheme.colorScheme.primary

    val daysInMonth = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    LaunchedEffect(year, month) {
        if (day > daysInMonth) day = daysInMonth
    }

    // Selected date + short day name (Fri, 16 Oct 2026)
    val selectedLabel = remember(year, month, day, daysInMonth) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day.coerceAtMost(daysInMonth))
        }
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(c.time)
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
                .padding(horizontal = 18.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Select Date", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        selectedLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = accent
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(34.dp)
                        .background(accent, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BreadcrumbChip(text = year.toString(), selected = step == PickerStep.YEAR) { step = PickerStep.YEAR }
                Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant)
                BreadcrumbChip(text = months[month], selected = step == PickerStep.MONTH) { step = PickerStep.MONTH }
                Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant)
                BreadcrumbChip(text = day.toString(), selected = step == PickerStep.DAY) { step = PickerStep.DAY }
            }

            Spacer(Modifier.height(14.dp))

            when (step) {
                PickerStep.YEAR -> {
                    val years = remember(year) { (year - 60..year + 40).toList() }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.height(420.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(years) { y ->
                            val selected = y == year
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        year = y
                                        step = PickerStep.MONTH
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 13.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        y.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                PickerStep.MONTH -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(360.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(months.size) { index ->
                            val selected = index == month
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        month = index
                                        step = PickerStep.DAY
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        months[index],
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                PickerStep.DAY -> {
                    val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    // Maser 1 tarikh kon bare pore (1 = Sunday)
                    val firstWeekDay = remember(year, month) {
                        Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }.get(Calendar.DAY_OF_WEEK)
                    }
                    val leadingBlanks = firstWeekDay - 1
                    val days = remember(daysInMonth) { (1..daysInMonth).toList() }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.height(410.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Barer nam header
                        items(7) { i ->
                            Box(
                                modifier = Modifier.aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    weekDays[i],
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // 1 tarikher ager faka ghor
                        items(leadingBlanks) {
                            Spacer(Modifier.aspectRatio(1f))
                        }
                        items(days) { d ->
                            val selected = d == day
                            Surface(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable { day = d },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        d.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    val now = Calendar.getInstance()
                    year = now.get(Calendar.YEAR)
                    month = now.get(Calendar.MONTH)
                    day = now.get(Calendar.DAY_OF_MONTH)
                }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Today", color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    val result = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day.coerceAtMost(daysInMonth))
                    }
                    onConfirm(result)
                }) {
                    Text("Select", color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
