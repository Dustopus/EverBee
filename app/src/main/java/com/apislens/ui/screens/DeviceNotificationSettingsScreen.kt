package com.apislens.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apislens.ui.components.DeviceNotificationSettingDialog
import com.apislens.ui.components.DeviceNotificationSettingItem
import com.apislens.ui.viewmodel.DeviceNotificationState
import com.apislens.ui.viewmodel.DeviceNotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceNotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: DeviceNotificationViewModel = hiltViewModel()
) {
    val devicesWithSettings by viewModel.devicesWithSettings.collectAsState()
    val globalReminderEnabled by viewModel.globalReminderEnabled.collectAsState()
    val globalFirstThresholdDays by viewModel.globalFirstThresholdDays.collectAsState()
    val globalRepeatIntervalDays by viewModel.globalRepeatIntervalDays.collectAsState()
    var selectedDeviceForNotification by remember { mutableStateOf<DeviceNotificationState?>(null) }
    
    selectedDeviceForNotification?.let { deviceState ->
        DeviceNotificationSettingDialog(
            deviceName = deviceState.device.name,
            currentSetting = deviceState.setting,
            globalReminderEnabled = globalReminderEnabled,
            globalFirstThresholdDays = globalFirstThresholdDays,
            globalRepeatIntervalDays = globalRepeatIntervalDays,
            onDismiss = { selectedDeviceForNotification = null },
            onSave = { setting ->
                val updatedSetting = setting.copy(deviceId = deviceState.device.id)
                viewModel.saveDeviceSetting(deviceState.device.id, updatedSetting)
            },
            onResetToGlobal = {
                viewModel.resetDeviceToGlobal(deviceState.device.id)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设备通知设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        if (devicesWithSettings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "暂无设备",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "请先添加设备后再进行设置",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "说明",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "为每个设备单独配置充电提醒参数。未单独设置的设备将使用全局默认设置（在设置页面的「充电提醒」模块中配置）。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "点击下方设备项可进入详细设置界面，可自定义是否开启提醒、首次提醒天数、重复提醒间隔等参数。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "设备列表",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            devicesWithSettings.forEach { deviceState ->
                                DeviceNotificationSettingItem(
                                    state = deviceState,
                                    onClick = { selectedDeviceForNotification = deviceState }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
