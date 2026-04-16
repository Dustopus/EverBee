package com.apislens.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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

private data class VerticalHeatmapCell(
    val dateKey: String,
    val count: Int,
    val dayOfWeek: Int,
    val weekIndex: Int,
    val month: Int
)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerticalHeatmap(
    dayCounts: Map<String, Int>,
    year: Int,
    onYearChange: (Int) -> Unit,
    dayRecords: Map<String, List<ChargeRecord>> = emptyMap(),
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
        val result = mutableListOf<VerticalHeatmapCell>()
        for (i in 0 until totalDays) {
            val dayCal = Calendar.getInstance()
            dayCal.set(year, Calendar.JANUARY, 1)
            dayCal.add(Calendar.DAY_OF_YEAR, i)
            val dateKey = sdfDay.format(dayCal.time)
            val dow = (dayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val weekIdx = (firstDayOfWeek + i) / 7
            val month = dayCal.get(Calendar.MONTH)
            result.add(VerticalHeatmapCell(
                dateKey = dateKey,
                count = dayCounts[dateKey] ?: 0,
                dayOfWeek = dow,
                weekIndex = weekIdx,
                month = month
            ))
        }
        result
    }

    val cellMap = remember(cells) {
        val map = mutableMapOf<Pair<Int, Int>, VerticalHeatmapCell>()
        for (cell in cells) {
            map[Pair(cell.dayOfWeek, cell.weekIndex)] = cell
        }
        map
    }

    val maxCount = cells.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val monthLabels = listOf("1 月", "2 月", "3 月", "4 月", "5 月", "6 月", "7 月", "8 月", "9 月", "10 月", "11 月", "12 月")
    val weekLabels = listOf("1", "2", "3", "4", "5", "6", "7")

    val monthStartWeeks = remember(cells) {
        val map = mutableMapOf<Int, Int>()
        for (cell in cells) {
            if (!map.containsKey(cell.month)) {
                map[cell.month] = cell.weekIndex
            }
        }
        map
    }

    val density = LocalDensity.current
    val monthLabelWidthPx = with(density) { 28.dp.toPx() }
    val gapPx = with(density) { 2.dp.toPx() }
    val cellSizePx = with(density) { 10.dp.toPx() }
    val weekLabelHeightPx = with(density) { 16.dp.toPx() }
    val gridStartYOffset = with(density) { 4.dp.toPx() }
    
    val availableHeightPx = totalWeeks * (cellSizePx + gapPx) - gapPx
    val availableWidthPx = 7 * (cellSizePx + gapPx) - gapPx
    
    val totalWidthPx = monthLabelWidthPx + with(density) { 8.dp.toPx() } + availableWidthPx
    val totalHeightPx = weekLabelHeightPx + with(density) { 8.dp.toPx() } + availableHeightPx
    
    val totalWidthDp = with(density) { totalWidthPx.toDp() }
    val totalHeightDp = with(density) { totalHeightPx.toDp() }

    val colorEmpty = MaterialTheme.colorScheme.surfaceContainerHighest
    val colorLow = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val colorMid = MaterialTheme.colorScheme.primaryContainer
    val colorHigh = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier,
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

            Canvas(
                modifier = Modifier
                    .width(totalWidthDp)
                    .height(totalHeightDp)
                    .pointerInput(cellMap, cellSizePx, gapPx, monthLabelWidthPx, weekLabelHeightPx, totalWeeks) {
                        detectTapGestures { offset ->
                            val gridStartX = monthLabelWidthPx + with(density) { 8.dp.toPx() }
                            val gridStartY = weekLabelHeightPx + with(density) { 8.dp.toPx() }
                            val relX = offset.x - gridStartX
                            val relY = offset.y - gridStartY
                            if (relX >= 0 && relY >= 0) {
                                val dayOfWeekIdx = (relX / (cellSizePx + gapPx)).toInt()
                                val weekIdx = (relY / (cellSizePx + gapPx)).toInt()
                                if (dayOfWeekIdx in 0 until 7 && weekIdx in 0 until totalWeeks) {
                                    val cell = cellMap[Pair(dayOfWeekIdx, weekIdx)]
                                    if (cell != null) {
                                        selectedDate = cell.dateKey
                                    }
                                }
                            }
                        }
                    }
            ) {
                val textPaint = android.graphics.Paint().apply {
                    textSize = with(density) { 9.sp.toPx() }
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                val weekTextPaint = android.graphics.Paint().apply {
                    textSize = with(density) { 9.sp.toPx() }
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                val gridStartX = monthLabelWidthPx + with(density) { 8.dp.toPx() }
                val gridStartY = weekLabelHeightPx + with(density) { 8.dp.toPx() }

                textPaint.color = labelColor.toArgb()
                for ((month, weekIdx) in monthStartWeeks) {
                    val y = gridStartY + weekIdx * (cellSizePx + gapPx) + cellSizePx / 2
                    drawContext.canvas.nativeCanvas.drawText(
                        monthLabels[month],
                        monthLabelWidthPx - with(density) { 4.dp.toPx() },
                        y + with(density) { 3.sp.toPx() },
                        textPaint
                    )
                }

                weekTextPaint.color = labelColor.toArgb()
                for (i in weekLabels.indices) {
                    val x = gridStartX + i * (cellSizePx + gapPx) + cellSizePx / 2
                    drawContext.canvas.nativeCanvas.drawText(
                        weekLabels[i],
                        x,
                        with(density) { 11.sp.toPx() },
                        weekTextPaint
                    )
                }

                val cornerRadius = CornerRadius(with(density) { 1.dp.toPx() }.coerceAtMost(cellSizePx * 0.2f))
                for (cell in cells) {
                    val intensity = (cell.count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
                    val color = when {
                        cell.count == 0 -> colorEmpty
                        intensity < 0.33f -> colorLow
                        intensity < 0.66f -> colorMid
                        else -> colorHigh
                    }
                    val x = gridStartX + cell.dayOfWeek * (cellSizePx + gapPx)
                    val y = gridStartY + cell.weekIndex * (cellSizePx + gapPx)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(cellSizePx, cellSizePx),
                        cornerRadius = cornerRadius
                    )
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
                        Text("• 纵轴（Y 轴）：第 1 周至第 53 周（按年份）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• 横轴（X 轴）：星期 1 至 7（周一至周日）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• 每个方块代表一年中的某一天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• 左侧月份标签标识每月起始位置", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("• 点击箭头：切换年份", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
