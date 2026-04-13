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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceId: Long,
    onBack: () -> Unit,
    onAddChargeRecord: () -> Unit,
    onAddUsageRecord: () -> Unit,
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("添加充电")
                        }
                        FilledTonalButton(
                            onClick = onAddUsageRecord,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("添加使用")
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
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (state.chargeRecords.isNotEmpty()) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "充电热力图",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                            com.apislens.ui.components.GithubStyleHeatmap(
                                dayCounts = state.heatmapDayCounts,
                                year = state.heatmapYear,
                                onYearChange = { viewModel.setHeatmapYear(it) },
                                dayRecords = state.heatmapDayRecords
                            )
                        }
                    }

                    if (state.chargeRecords.isNotEmpty()) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "充电记录",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                            state.chargeRecords.take(10).forEachIndexed { index, record ->
                                ChargeRecordTimelineItem(
                                    record = record,
                                    isLast = index == state.chargeRecords.take(10).lastIndex,
                                    onDelete = { viewModel.deleteChargeRecord(it) }
                                )
                            }
                            if (state.chargeRecords.size > 10) {
                                Text(
                                    "还有 ${state.chargeRecords.size - 10} 条记录",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
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
                        "累计折旧 = 每日折旧额 × 已使用天数\n每日折旧额 = 购入价格 ÷ 设备生命周期（1095天）",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("参数说明") }
            item { ParameterItem("购入价格", "设备的购买价格，即设备初始价值", "> 0", "基础值") }
            item { ParameterItem("设备生命周期", "假设设备的经济使用寿命为 3 年（1095 天），采用线性折旧法", "固定 1095 天（3年）", "除数") }
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
            item { ExampleCard("示例一：新购设备", "设备购入价格 ¥5,999，已使用 30 天", "每日折旧额 = ¥5,999 ÷ 1095 = ¥5.48/天\n累计折旧 = ¥5.48 × 30 = ¥164.38", "¥164.38", MaterialTheme.colorScheme.primary) }
            item { Spacer(modifier = Modifier.height(8.dp)); ExampleCard("示例二：使用一年设备", "设备购入价格 ¥3,999，已使用 400 天", "每日折旧额 = ¥3,999 ÷ 1095 = ¥3.65/天\n累计折旧 = ¥3.65 × 400 = ¥1,460.82\n（未超过购入价格，取实际值）", "¥1,460.82", MaterialTheme.colorScheme.primary) }
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
    modifier: Modifier = Modifier
) {
    var showInfoSheet by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (healthData.size >= 2) {
                val latestHealth = healthData.last().second
                val healthColor = when {
                    latestHealth >= 80 -> com.apislens.ui.theme.SuccessGreen
                    latestHealth >= 50 -> com.apislens.ui.theme.WarningAmber
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
                            "${String.format("%.0f", latestHealth)}%",
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
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (latestHealth / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = healthColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("基于 ${healthData.size} 次充电记录计算", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("需要至少2次充电记录才能计算健康度", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showInfoSheet) {
        ScrollableInfoBottomSheet(
            title = "电池健康度计算说明",
            onDismiss = { showInfoSheet = false }
        ) {
            item { SectionHeader("计算公式") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Text("健康度 = 充满比例 × 40% + 近期最高电量均值 × 60%", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.padding(16.dp))
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("参数说明") }
            item { ParameterItem("充满比例", "充到 ≥95% 电量的次数占总充电次数的比例", "0% ~ 100%", "权重 40%") }
            item { ParameterItem("近期最高电量均值", "最近 5 次充电结束电量的算术平均值", "0% ~ 100%", "权重 60%") }
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("健康度分级标准") }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HealthLevelBadge("≥80%", "良好", com.apislens.ui.theme.SuccessGreen, Modifier.weight(1f))
                    HealthLevelBadge("50-79%", "一般", com.apislens.ui.theme.WarningAmber, Modifier.weight(1f))
                    HealthLevelBadge("<50%", "建议更换", com.apislens.ui.theme.ErrorRed, Modifier.weight(1f))
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)); SectionHeader("计算示例") }
            item { ExampleCard("示例一：健康电池", "设备共充电 10 次，其中 8 次充到 ≥95%，最近 5 次充电结束电量分别为 98%、100%、96%、99%、97%", "充满比例 = 8/10 = 80%\n近期最高电量均值 = (98+100+96+99+97)/5 = 98%\n健康度 = 80% × 40% + 98% × 60% = 32% + 58.8% = 90.8%", "90.8%", com.apislens.ui.theme.SuccessGreen) }
            item { Spacer(modifier = Modifier.height(8.dp)); ExampleCard("示例二：老化电池", "设备共充电 10 次，其中 2 次充到 ≥95%，最近 5 次充电结束电量分别为 72%、68%、75%、70%、65%", "充满比例 = 2/10 = 20%\n近期最高电量均值 = (72+68+75+70+65)/5 = 70%\n健康度 = 20% × 40% + 70% × 60% = 8% + 42% = 50%", "50.0%", com.apislens.ui.theme.WarningAmber) }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("注：至少需要 2 次充电记录才能计算健康度。充电记录越多，计算结果越准确。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
