package com.apislens.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apislens.ui.components.BeeOverlay
import com.apislens.ui.theme.ThemeMode
import com.apislens.ui.viewmodel.ExportStatus
import com.apislens.ui.viewmodel.ImportStatus
import com.apislens.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel(), onNavigateToDeviceNotificationSettings: () -> Unit) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val firstThresholdDays by viewModel.firstThresholdDays.collectAsState()
    val repeatIntervalDays by viewModel.repeatIntervalDays.collectAsState()
    val supportDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { viewModel.exportData(it) } }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importData(uri)
        }
    }

    val currentExportFileName: () -> String = {
        val sdf = java.text.SimpleDateFormat("yyyyMMdd_HH'h'mm'm'ss's'", java.util.Locale.getDefault())
        "ApisLens_Export_${sdf.format(java.util.Date())}.json"
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "需要通知权限才能发送充电提醒", Toast.LENGTH_SHORT).show()
        }
    }

    var beeCount by remember { mutableIntStateOf(0) }
    var easterEggClicks by remember { mutableIntStateOf(0) }
    var lastEasterEggClickTime by remember { mutableLongStateOf(0L) }
    val easterEggInteractionSource = remember { MutableInteractionSource() }

    var showThresholdDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var aboutTextPosition by remember { mutableStateOf<Offset?>(null) }

    val exportStatus by viewModel.exportStatus.collectAsState()
    val importStatus by viewModel.importStatus.collectAsState()

    if (showThresholdDialog) {
        NumberPickerDialog(
            title = "首次提醒天数",
            current = firstThresholdDays,
            range = 3..30,
            onConfirm = {
                viewModel.setFirstThresholdDays(it)
                showThresholdDialog = false
            },
            onDismiss = { showThresholdDialog = false }
        )
    }

    if (showIntervalDialog) {
        NumberPickerDialog(
            title = "重复提醒间隔（天）",
            current = repeatIntervalDays,
            range = 1..14,
            onConfirm = {
                viewModel.setRepeatIntervalDays(it)
                showIntervalDialog = false
            },
            onDismiss = { showIntervalDialog = false }
        )
    }

    when (exportStatus) {
        is ExportStatus.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearExportStatus() },
                text = { Text("✅导出成功") },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearExportStatus() }) { Text("确定") }
                }
            )
        }
        is ExportStatus.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearExportStatus() },
                text = { Text("❌导出失败，请重试") },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearExportStatus() }) { Text("确定") }
                }
            )
        }
        ExportStatus.Idle -> {}
    }

    when (importStatus) {
        is ImportStatus.Success -> {
            val result = importStatus as ImportStatus.Success
            AlertDialog(
                onDismissRequest = { viewModel.clearImportStatus() },
                text = { Text("✅导入成功，导入了${result.deviceCount}条设备记录，导入了${result.chargeRecordCount}条充电记录，导入了${result.usageRecordCount}条使用记录") },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearImportStatus() }) { Text("确定") }
                }
            )
        }
        is ImportStatus.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearImportStatus() },
                text = { Text("❌导入失败，请重试") },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearImportStatus() }) { Text("确定") }
                }
            )
        }
        ImportStatus.Idle -> {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("外观", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("主题模式", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(Modifier.selectableGroup()) {
                            ThemeModeOption(
                                label = "跟随系统",
                                icon = Icons.Default.BrightnessAuto,
                                selected = themeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                            )
                            ThemeModeOption(
                                label = "亮色模式",
                                icon = Icons.Default.LightMode,
                                selected = themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                            )
                            ThemeModeOption(
                                label = "暗色模式",
                                icon = Icons.Default.DarkMode,
                                selected = themeMode == ThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                            )
                        }
                        if (supportDynamicColor) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Monet 动态取色", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("根据壁纸自动生成配色方案", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = dynamicColor,
                                    onCheckedChange = { viewModel.setDynamicColor(it) }
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("充电提醒", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("充电提醒", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("设备长时间未充电时发送通知", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    viewModel.setReminderEnabled(enabled)
                                }
                            )
                        }
                        if (reminderEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showThresholdDialog = true },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("首次提醒", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("超过此天数未充电时首次提醒", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$firstThresholdDays 天", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showIntervalDialog = true },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("重复提醒间隔", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("首次提醒后每隔此天数再次提醒", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$repeatIntervalDays 天", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToDeviceNotificationSettings() },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("设备通知设置", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("为每个设备单独配置充电提醒参数", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("数据管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { exportLauncher.launch(currentExportFileName()) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("导出数据")
                            }
                            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("导入数据")
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "关于",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .onGloballyPositioned { coords ->
                                        aboutTextPosition = Offset(
                                            coords.positionInRoot().x + coords.size.width / 2f,
                                            coords.positionInRoot().y + coords.size.height / 2f
                                        )
                                    }
                                    .clickable(indication = null, interactionSource = easterEggInteractionSource, role = null) {
                                    val now = System.currentTimeMillis()
                                    if (now - lastEasterEggClickTime > 3000L) {
                                        easterEggClicks = 1
                                    } else {
                                        easterEggClicks++
                                    }
                                    lastEasterEggClickTime = now
                                    if (easterEggClicks >= 7) {
                                        easterEggClicks = 0
                                        beeCount = (beeCount + 1).coerceAtMost(10)
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("版本", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("1.0.0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ApisLens — 设备折旧成本追踪工具", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            BeeOverlay(
                beeCount = beeCount,
                spawnX = aboutTextPosition?.x,
                spawnY = aboutTextPosition?.y
            )
        }
    }
}

@Composable
private fun NumberPickerDialog(
    title: String,
    current: Int,
    range: IntRange,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableIntStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (selected > range.first) selected-- }) {
                        Icon(Icons.Default.Remove, contentDescription = "减少")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "$selected",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = { if (selected < range.last) selected++ }) {
                        Icon(Icons.Default.Add, contentDescription = "增加")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "范围：${range.first} ~ ${range.last} 天",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ThemeModeOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
