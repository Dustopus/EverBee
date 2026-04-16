package com.apislens.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apislens.ui.viewmodel.DeviceDetailViewModel
import com.apislens.ui.viewmodel.DeviceDetailState
import com.apislens.utils.BatteryHealthResult
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceId: Long,
    onBack: () -> Unit,
    onAddChargeRecord: () -> Unit,
    onEditDevice: (Long) -> Unit = {},
    viewModel: DeviceDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = DeviceDetailState())
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deviceId) { viewModel.loadDevice(deviceId) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除设备「${state.device?.name}」吗？此操作不可撤销，关联的充电和使用记录也将被删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.device?.let { viewModel.deleteDevice(it) }
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.device?.name ?: "设备详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    state.device?.let {
                        IconButton(onClick = { onEditDevice(it.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            state.device?.let { dev ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    DeviceInfoCard(
                        device = dev,
                        dailyCost = state.dailyCost,
                        daysUsed = state.daysUsed,
                        totalDepreciation = state.totalDepreciation
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onAddChargeRecord,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("添加充电记录")
                        }
                    }

                    if (state.batteryHealthData.isNotEmpty()) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "电池健康趋势",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                            BatteryHealthChart(
                                healthData = state.batteryHealthData,
                                healthScore = state.batteryHealthScore,
                                confidence = state.batteryConfidence,
                                referenceSegment = state.batteryReferenceSegment,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.chargeRecords.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.weight(2f)
                            ) {
                                com.apislens.ui.components.VerticalHeatmap(
                                    dayCounts = state.heatmapDayCounts,
                                    year = state.heatmapYear,
                                    onYearChange = { viewModel.setHeatmapYear(it) },
                                    dayRecords = state.heatmapDayRecords
                                )
                            }
                            
                            Box(
                                modifier = Modifier.weight(3f)
                            ) {
                                com.apislens.ui.components.ChargeRecordList(
                                    chargeRecords = state.chargeRecords,
                                    onDelete = { viewModel.deleteChargeRecord(it) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoCard(
    device: com.apislens.data.local.entity.Device,
    dailyCost: Double,
    daysUsed: Int,
    totalDepreciation: Double
) {
    var showDepreciationSheet by remember { mutableStateOf(false) }

    val lifecycleDays = device.lifecycleDays
    val lifecycleYears = device.lifecycleMonths / 12
    val lifecycleRemainMonths = device.lifecycleMonths % 12
    val lifecycleDesc = if (lifecycleRemainMonths > 0) "${lifecycleYears} 年 ${lifecycleRemainMonths} 月" else "${lifecycleYears} 年"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            categoryIcon(device.category.ifEmpty { "其他" }),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        device.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (device.category.isNotEmpty()) {
                        Text(
                            device.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem("型号", device.model.ifEmpty { "未知" })
                InfoItem("购入价格", "¥${String.format("%.2f", device.purchasePrice)}")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem("购买日期", device.purchaseDate)
                InfoItem("生命周期", "${device.lifecycleMonths} 个月")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InfoItem("累计折旧", "¥${String.format("%.2f", totalDepreciation)}")
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showDepreciationSheet = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "折旧说明",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                InfoItem("剩余价值", "¥${String.format("%.2f", device.purchasePrice - totalDepreciation)}")
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("每日折旧", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "¥${String.format("%.2f", dailyCost)}/天",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("已使用", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$daysUsed 天",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDepreciationSheet) {
        ScrollableInfoBottomSheet(
            title = "累计折旧计算说明",
            onDismiss = { showDepreciationSheet = false }
        ) {
            item { SectionHeader("计算公式") }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "累计折旧 = 每日折旧额 × 已使用天数\n每日折旧额 = 购入价格 ÷ 设备生命周期（${lifecycleDays}天）",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("参数说明") }
            item { ParameterItem("购入价格", "设备的购买价格，即设备初始价值", "> 0", "基础值") }
            item { ParameterItem("设备生命周期", "设备的经济使用寿命为 ${lifecycleDesc}（${lifecycleDays} 天），采用线性折旧法", "${lifecycleDays} 天（${lifecycleDesc}）", "除数") }
            item { ParameterItem("已使用天数", "从购买日期到今天的天数差值，最小值为 1", "≥ 1", "乘数") }
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("折旧方法说明") }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("线性折旧法（Straight-Line Depreciation）", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("假设设备价值在生命周期内均匀递减。每日折旧额固定不变，累计折旧随时间线性增长，最高不超过购入价格。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("计算示例") }
            item { ExampleCard("示例一：新购设备", "设备购入价格 ¥5,999，已使用 30 天", "每日折旧额 = ¥5,999 ÷ $lifecycleDays = ¥${String.format("%.2f", 5999.0 / lifecycleDays)}/天\n累计折旧 = ¥${String.format("%.2f", 5999.0 / lifecycleDays)} × 30 = ¥${String.format("%.2f", 5999.0 / lifecycleDays * 30)}", "¥${String.format("%.2f", 5999.0 / lifecycleDays * 30)}", MaterialTheme.colorScheme.primary) }
            item { Spacer(modifier = Modifier.height(8.dp)); ExampleCard("示例二：使用一年设备", "设备购入价格 ¥3,999，已使用 400 天", "每日折旧额 = ¥3,999 ÷ $lifecycleDays = ¥${String.format("%.2f", 3999.0 / lifecycleDays)}/天\n累计折旧 = ¥${String.format("%.2f", 3999.0 / lifecycleDays)} × 400 = ¥${String.format("%.2f", (3999.0 / lifecycleDays * 400).coerceAtMost(3999.0))}\n（未超过购入价格，取实际值）", "¥${String.format("%.2f", (3999.0 / lifecycleDays * 400).coerceAtMost(3999.0))}", MaterialTheme.colorScheme.primary) }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("注：累计折旧不超过购入价格。设备生命周期结束后，残值视为 0。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryHealthChart(
    healthData: List<Pair<String, Double>>,
    healthScore: Double? = null,
    confidence: String = "",
    referenceSegment: String? = null,
    modifier: Modifier = Modifier
) {
    var showInfoSheet by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (healthData.size >= 2 && healthScore != null) {
                val healthColor = when {
                    healthScore >= 80 -> com.apislens.ui.theme.SuccessGreen
                    healthScore >= 50 -> com.apislens.ui.theme.WarningAmber
                    else -> com.apislens.ui.theme.ErrorRed
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("当前健康度", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${String.format("%.1f", healthScore)}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = healthColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { showInfoSheet = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "计算说明", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (referenceSegment != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "参考片段：$referenceSegment",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                when (confidence) {
                    BatteryHealthResult.CONFIDENCE_LOW, BatteryHealthResult.CONFIDENCE_INSUFFICIENT -> {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = com.apislens.ui.theme.WarningAmber.copy(alpha = 0.1f)),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "近期充电较浅，健康度需深度充电后校准",
                                style = MaterialTheme.typography.labelSmall,
                                color = com.apislens.ui.theme.WarningAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    BatteryHealthResult.CONFIDENCE_INITIAL -> {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "首次计算，建议持续观察",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (healthScore / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = healthColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("基于 ${healthData.size} 次充电记录计算 · 置信度：${getConfidenceLabel(confidence)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("需要至少2次充电记录才能计算健康度", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showInfoSheet) {
        ScrollableInfoBottomSheet(
            title = "电池健康度计算说明 (V3.0)",
            onDismiss = { showInfoSheet = false }
        ) {
            item { SectionHeader("算法概述") }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("基于充电时长反推电池容量", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("通过深度充电片段的单位百分比充电耗时（分钟/%）反推电池当前最大容量，结合区间补偿和频次校准，指数平滑输出。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("核心步骤") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("1. 数据清洗：ΔSOC ≥ 5% 且 时长 ≥ 2 分钟", style = MaterialTheme.typography.bodySmall)
                        Text("2. 最佳片段：end_soc×0.5 + 跨度×0.4 + 时长分×0.1 得分最高", style = MaterialTheme.typography.bodySmall)
                        Text("3. 容量反推：rate = duration/ΔSOC → SoH = (BASE_RATE/rate) × 100", style = MaterialTheme.typography.bodySmall)
                        Text("4. 区间补偿：<20%:0.85 / >85%:1.35（非线性修正）", style = MaterialTheme.typography.bodySmall)
                        Text("5. 频次校准：日均>3次降置信度至 LOW", style = MaterialTheme.typography.bodySmall)
                        Text("6. 指数平滑：EMA 权重 20%（7天窗口）", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("关键参数") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("• BASE_RATE = 0.55 min/%（新电池基准充电速率）", style = MaterialTheme.typography.bodySmall)
                        Text("• 输入字段：start_soc、end_soc、duration（分钟）", style = MaterialTheme.typography.bodySmall)
                        Text("• 输出范围：50% - 105%（边界裁剪）", style = MaterialTheme.typography.bodySmall)
                        Text("• 置信度：HIGH / LOW / INITIAL / INSUFFICIENT", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("鲁棒性设计") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("• 浅充保护：找不到 ≥80% 片段时返回 LOW", style = MaterialTheme.typography.bodySmall)
                        Text("• 异常过滤：ΔSOC≥5 且时长≥2min 双重校验", style = MaterialTheme.typography.bodySmall)
                        Text("• 低电量补偿：SOC<20% 时因子 0.85（虚电效应）", style = MaterialTheme.typography.bodySmall)
                        Text("• 高电量补偿：SOC>85% 时因子 1.35（涓流阶段）", style = MaterialTheme.typography.bodySmall)
                        Text("• 频次检测：高频短充模式自动降低置信度", style = MaterialTheme.typography.bodySmall)
                        Text("• 平滑防抖：EMA 让曲线符合物理老化规律", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("算法版本演进") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("V1.0：充满比例×40% + 近期均值×60%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("→ 问题：浅充用户误判为「建议更换」", style = MaterialTheme.typography.bodySmall, color = com.apislens.ui.theme.ErrorRed)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("V2.0：深度片段 ΔSOC 反推 + 非线性补偿", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("→ 改进：解决浅充误判问题", style = MaterialTheme.typography.bodySmall, color = com.apislens.ui.theme.SuccessGreen)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("V3.0：充电时长反推容量 + 频次校准（当前）", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("→ 提升：引入时间维度，物理意义更明确", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private fun getConfidenceLabel(confidence: String): String {
    return when (confidence) {
        BatteryHealthResult.CONFIDENCE_HIGH -> "高"
        BatteryHealthResult.CONFIDENCE_LOW -> "低"
        BatteryHealthResult.CONFIDENCE_INITIAL -> "初始"
        BatteryHealthResult.CONFIDENCE_INSUFFICIENT -> "不足"
        else -> confidence
    }
}

@Composable
fun ChargeRecordTimelineItem(
    record: com.apislens.data.local.entity.ChargeRecord,
    isLast: Boolean,
    onDelete: ((com.apislens.data.local.entity.ChargeRecord) -> Unit)? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = {
                Column {
                    Text("确定要删除这条充电记录吗？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.startTime))}  ${record.startLevel}% → ${record.endLevel?.toString() ?: "?"}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete?.invoke(record)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(12.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {}
            if (!isLast) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .padding(horizontal = 1.dp)
                        .then(Modifier.background(MaterialTheme.colorScheme.outlineVariant))
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLast) 0.dp else 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.startTime)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text("电量: ${record.startLevel}% → ${record.endLevel?.toString() ?: "?"}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (record.note.isNotEmpty()) {
                        Text(record.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
                if (onDelete != null) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.background(color: Color): Modifier =
    this.then(drawBehind { drawRect(color) })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollableInfoBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 12.dp),
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ) {
                Box(Modifier.width(32.dp).height(4.dp))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "关闭") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                content = content
            )
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun ParameterItem(name: String, description: String, valueRange: String, weight: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(weight, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text("取值范围：$valueRange", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun HealthLevelBadge(range: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)), shape = MaterialTheme.shapes.small) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(range, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
fun ExampleCard(title: String, description: String, calculation: String, result: String, resultColor: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), shape = MaterialTheme.shapes.extraSmall) {
                Text(calculation, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, modifier = Modifier.padding(12.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("结果：", style = MaterialTheme.typography.bodyMedium)
                Text(result, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = resultColor)
            }
        }
    }
}
