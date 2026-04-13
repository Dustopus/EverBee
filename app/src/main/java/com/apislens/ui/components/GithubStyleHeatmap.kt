package com.apislens.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apislens.data.local.entity.ChargeRecord
import com.apislens.ui.screens.ScrollableInfoBottomSheet
import com.apislens.ui.screens.SectionHeader
import java.text.SimpleDateFormat
import java.util.*

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

data class GithubHeatmapCell(
    val dateKey: String,
    val count: Int,
    val dayOfWeek: Int,
    val weekIndex: Int,
    val month: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GithubStyleHeatmap(
    dayCounts: Map<String, Int>,
    year: Int,
    onYearChange: (Int) -> Unit,
    dayRecords: Map<String, List<ChargeRecord>> = emptyMap(),
    cellSize: Dp = 12.dp,
    cellGap: Dp = 2.dp,
    modifier: Modifier = Modifier
) {
    var showInfoSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<String?>(null) }

    val cal = Calendar.getInstance()
    cal.set(year, Calendar.JANUARY, 1)
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val isLeap = isLeapYear(year)
    val totalDays = if (isLeap) 366 else 365
    val totalWeeks = ((firstDayOfWeek + totalDays) + 6) / 7

    val sdfDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cells = remember(dayCounts, year) {
        val result = mutableListOf<GithubHeatmapCell>()
        for (i in 0 until totalDays) {
            val dayCal = Calendar.getInstance()
            dayCal.set(year, Calendar.JANUARY, 1)
            dayCal.add(Calendar.DAY_OF_YEAR, i)
            val dateKey = sdfDay.format(dayCal.time)
            val dow = (dayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val weekIdx = (firstDayOfWeek + i) / 7
            val month = dayCal.get(Calendar.MONTH)
            result.add(GithubHeatmapCell(
                dateKey = dateKey,
                count = dayCounts[dateKey] ?: 0,
                dayOfWeek = dow,
                weekIndex = weekIdx,
                month = month
            ))
        }
        result
    }

    val maxCount = cells.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val monthLabels = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
    val weekLabels = listOf("一", "", "三", "", "五", "", "日")

    val weekToMonth = remember(cells) {
        val map = mutableMapOf<Int, Int>()
        for (cell in cells) {
            if (!map.containsKey(cell.weekIndex)) {
                map[cell.weekIndex] = cell.month
            }
        }
        map
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onYearChange(year - 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "上一年", modifier = Modifier.size(18.dp))
                    }
                    Text(
                        "${year}年",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { onYearChange(year + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "下一年", modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = { showInfoSheet = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "说明", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.width(20.dp),
                    verticalArrangement = Arrangement.spacedBy(cellGap)
                ) {
                    Spacer(modifier = Modifier.height(14.dp))
                    weekLabels.forEachIndexed { idx, label ->
                        Box(modifier = Modifier.height(cellSize), contentAlignment = Alignment.CenterEnd) {
                            if (label.isNotEmpty()) {
                                Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(cellGap)
                    ) {
                        var lastMonth = -1
                        for (w in 0 until totalWeeks) {
                            val m = weekToMonth[w] ?: lastMonth
                            if (m != lastMonth) {
                                Box(modifier = Modifier.width(cellSize)) {
                                    Text(
                                        monthLabels.getOrElse(m) { "" },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                lastMonth = m
                            } else {
                                Box(modifier = Modifier.width(cellSize))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(year) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (dragAmount > 60f) onYearChange(year - 1)
                                    else if (dragAmount < -60f) onYearChange(year + 1)
                                }
                            }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
                            for (dow in 0 until 7) {
                                Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                                    for (w in 0 until totalWeeks) {
                                        val cell = cells.find { it.dayOfWeek == dow && it.weekIndex == w }
                                        val count = cell?.count ?: 0
                                        val intensity = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
                                        val color = when {
                                            cell == null -> Color.Transparent
                                            count == 0 -> MaterialTheme.colorScheme.surfaceContainerHighest
                                            intensity < 0.33f -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            intensity < 0.66f -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        Surface(
                                            modifier = Modifier.size(cellSize),
                                            shape = RoundedCornerShape(2.dp),
                                            color = color,
                                            onClick = {
                                                if (cell != null) {
                                                    selectedDate = cell.dateKey
                                                }
                                            }
                                        ) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("少", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    Surface(modifier = Modifier.size(8.dp), shape = RoundedCornerShape(1.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {}
                    Surface(modifier = Modifier.size(8.dp), shape = RoundedCornerShape(1.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) {}
                    Surface(modifier = Modifier.size(8.dp), shape = RoundedCornerShape(1.dp), color = MaterialTheme.colorScheme.primaryContainer) {}
                    Surface(modifier = Modifier.size(8.dp), shape = RoundedCornerShape(1.dp), color = MaterialTheme.colorScheme.primary) {}
                }
                Spacer(modifier = Modifier.width(2.dp))
                Text("多", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    selectedDate?.let { dateKey ->
        val records = dayRecords[dateKey].orEmpty()
        val count = dayCounts[dateKey] ?: 0
        AlertDialog(
            onDismissRequest = { selectedDate = null },
            title = { Text(dateKey) },
            text = {
                if (records.isEmpty()) {
                    Text("该日无充电记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("充电次数：$count 次", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        records.forEach { record ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.startTime)),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "电量：${record.startLevel}% → ${record.endLevel?.toString() ?: "?"}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (record.note.isNotEmpty()) {
                                        Text(record.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDate = null }) { Text("关闭") }
            }
        )
    }

    if (showInfoSheet) {
        ScrollableInfoBottomSheet(
            title = "充电热力图说明",
            onDismiss = { showInfoSheet = false }
        ) {
            item { SectionHeader("布局说明") }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("• 纵轴（Y轴）：星期一至星期日", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• 横轴（X轴）：按周排列，标注月份", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• 每个方块代表一天，颜色深浅代表充电次数", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("交互说明") }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("• 左右滑动或点击箭头：切换年份", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• 点击色块：查看该日详细充电记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("颜色梯度定义") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeatmapColorBadge("无充电", MaterialTheme.colorScheme.surfaceContainerHighest, Modifier.weight(1f))
                    HeatmapColorBadge("低频", MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), Modifier.weight(1f))
                    HeatmapColorBadge("中频", MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
                    HeatmapColorBadge("高频", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "颜色深浅代表当天充电次数的相对频率。频率阈值基于当前年份所有日充电次数分布动态计算。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("统计标准") }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("• 每条充电记录按开始时间归属到对应日期", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• 同一天多次充电分别计数", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• 跨天充电仅计入开始日期", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapColorBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(16.dp),
                shape = MaterialTheme.shapes.extraSmall,
                color = color
            ) {}
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
