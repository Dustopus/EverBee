package com.apislens.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.sqrt

data class PieChartData(
    val label: String,
    val purchaseValue: Double,
    val depreciationValue: Double
)

data class PieSlice(
    val label: String,
    val value: Double,
    val color: Color,
    val startAngle: Float,
    val sweepAngle: Float
)

@Composable
fun CostPieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier
) {
    var selectedSeries by remember { mutableIntStateOf(0) }
    var selectedSlice by remember { mutableIntStateOf(-1) }

    val seriesLabels = listOf("购入价格", "折旧成本")

    val monetColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer
    )
    val otherColor = MaterialTheme.colorScheme.outlineVariant

    val slices = remember(data, selectedSeries, monetColors) {
        val sorted = if (selectedSeries == 0) {
            data.sortedByDescending { it.purchaseValue }
        } else {
            data.sortedByDescending { it.depreciationValue }
        }

        val top5 = sorted.take(5)
        val rest = sorted.drop(5)

        val chartData = if (rest.isNotEmpty()) {
            val restPurchase = rest.sumOf { it.purchaseValue }
            val restDepreciation = rest.sumOf { it.depreciationValue }
            top5 + PieChartData(label = "其他", purchaseValue = restPurchase, depreciationValue = restDepreciation)
        } else {
            top5
        }

        val total = if (selectedSeries == 0) chartData.sumOf { it.purchaseValue } else chartData.sumOf { it.depreciationValue }
        if (total <= 0.0) return@remember emptyList()

        var currentAngle = -90f
        chartData.mapIndexed { index, item ->
            val value = if (selectedSeries == 0) item.purchaseValue else item.depreciationValue
            val sweep = (value / total * 360f).toFloat()
            val color = if (item.label == "其他") otherColor else monetColors[index % monetColors.size]
            val slice = PieSlice(
                label = item.label,
                value = value,
                color = color,
                startAngle = currentAngle,
                sweepAngle = sweep
            )
            currentAngle += sweep
            slice
        }
    }

    val totalValue = slices.sumOf { it.value }

    val surfaceContainerLow = MaterialTheme.colorScheme.surfaceContainerLow
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "成本分布",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "按设备查看价格与折旧占比",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(seriesLabels.indices.toList()) { index ->
                    FilterChip(
                        selected = selectedSeries == index,
                        onClick = {
                            selectedSeries = index
                            selectedSlice = -1
                        },
                        label = { Text(seriesLabels[index], style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryContainer,
                            selectedLabelColor = onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = outlineVariant,
                            selectedBorderColor = primary,
                            enabled = true,
                            selected = selectedSeries == index
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (slices.isNotEmpty()) {
                val density = LocalDensity.current
                val chartSizeDp = 180.dp
                val chartSizePx = with(density) { chartSizeDp.toPx() }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(chartSizeDp)
                            .pointerInput(slices) {
                                detectTapGestures { offset ->
                                    val center = Offset(chartSizePx / 2, chartSizePx / 2)
                                    val radius = chartSizePx / 2 - with(density) { 8.dp.toPx() }
                                    val dx = offset.x - center.x
                                    val dy = offset.y - center.y
                                    val dist = sqrt(dx * dx + dy * dy)
                                    if (dist <= radius) {
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (angle < -90f) angle += 360f
                                        angle += 90f
                                        if (angle >= 360f) angle -= 360f

                                        var found = -1
                                        for ((i, slice) in slices.withIndex()) {
                                            var start = slice.startAngle + 90f
                                            if (start >= 360f) start -= 360f
                                            val end = start + slice.sweepAngle
                                            if (angle >= start && angle < end) {
                                                found = i
                                                break
                                            }
                                        }
                                        selectedSlice = if (found == selectedSlice) -1 else found
                                    } else {
                                        selectedSlice = -1
                                    }
                                }
                            }
                    ) {
                        val center = Offset(chartSizePx / 2, chartSizePx / 2)
                        val radius = chartSizePx / 2 - with(density) { 8.dp.toPx() }
                        val innerRadius = radius * 0.55f

                        for ((i, slice) in slices.withIndex()) {
                            val isSelected = i == selectedSlice
                            val r = if (isSelected) radius + with(density) { 4.dp.toPx() } else radius

                            drawArc(
                                color = slice.color,
                                startAngle = slice.startAngle,
                                sweepAngle = slice.sweepAngle,
                                useCenter = true,
                                topLeft = Offset(center.x - r, center.y - r),
                                size = Size(r * 2, r * 2)
                            )

                            if (isSelected) {
                                drawArc(
                                    color = slice.color.copy(alpha = 0.3f),
                                    startAngle = slice.startAngle,
                                    sweepAngle = slice.sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(center.x - r - with(density) { 2.dp.toPx() }, center.y - r - with(density) { 2.dp.toPx() }),
                                    size = Size((r + with(density) { 2.dp.toPx() }) * 2, (r + with(density) { 2.dp.toPx() }) * 2),
                                    style = Stroke(width = with(density) { 3.dp.toPx() })
                                )
                            }
                        }

                        drawCircle(
                            color = surfaceContainerLow,
                            radius = innerRadius,
                            center = center
                        )

                        if (selectedSlice >= 0 && selectedSlice < slices.size) {
                            val sel = slices[selectedSlice]
                            val pct = if (totalValue > 0) (sel.value / totalValue * 100).toInt() else 0
                            val textPaint = android.graphics.Paint().apply {
                                textSize = with(density) { 14.sp.toPx() }
                                isAntiAlias = true
                                color = sel.color.hashCode()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            val labelPaint = android.graphics.Paint().apply {
                                textSize = with(density) { 10.sp.toPx() }
                                isAntiAlias = true
                                color = onSurfaceVariant.hashCode()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                "$pct%",
                                center.x,
                                center.y - with(density) { 4.dp.toPx() },
                                textPaint
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                sel.label,
                                center.x,
                                center.y + with(density) { 12.dp.toPx() },
                                labelPaint
                            )
                        } else if (slices.isNotEmpty()) {
                            val textPaint = android.graphics.Paint().apply {
                                textSize = with(density) { 11.sp.toPx() }
                                isAntiAlias = true
                                color = onSurfaceVariant.hashCode()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            val totalStr = "¥${String.format("%.0f", totalValue)}"
                            drawContext.canvas.nativeCanvas.drawText(
                                totalStr,
                                center.x,
                                center.y + with(density) { 4.dp.toPx() },
                                textPaint
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    slices.forEachIndexed { index, slice ->
                        val pct = if (totalValue > 0) (slice.value / totalValue * 100).toInt() else 0
                        val isSelected = index == selectedSlice
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(12.dp),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = slice.color
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                slice.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "¥${String.format("%.0f", slice.value)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "$pct%",
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurfaceVariant,
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无设备数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant
                    )
                }
            }
        }
    }
}
