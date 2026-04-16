package com.apislens.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.apislens.data.local.entity.DeviceNotificationSetting

@Composable
fun DeviceNotificationSettingDialog(
    deviceName: String,
    currentSetting: DeviceNotificationSetting?,
    globalReminderEnabled: Boolean,
    globalFirstThresholdDays: Int,
    globalRepeatIntervalDays: Int,
    onDismiss: () -> Unit,
    onSave: (DeviceNotificationSetting) -> Unit,
    onResetToGlobal: () -> Unit
) {
    var useCustomSettings by remember { mutableStateOf(currentSetting != null) }
    var reminderEnabled by remember { mutableStateOf(currentSetting?.reminderEnabled ?: globalReminderEnabled) }
    var firstThresholdDays by remember { mutableStateOf(currentSetting?.firstThresholdDays ?: globalFirstThresholdDays) }
    var repeatIntervalDays by remember { mutableStateOf(currentSetting?.repeatIntervalDays ?: globalRepeatIntervalDays) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "设备通知设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                HorizontalDivider()

                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "使用自定义设置",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (!useCustomSettings) "当前使用全局默认设置" else "将覆盖全局默认设置",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useCustomSettings,
                        onCheckedChange = { useCustomSettings = it }
                    )
                }

                if (useCustomSettings) {
                    HorizontalDivider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("开启提醒", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "全局默认：${if (globalReminderEnabled) "开启" else "关闭"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    NumberPickerRow(
                        label = "首次提醒天数",
                        value = firstThresholdDays,
                        range = 3..30,
                        globalValue = globalFirstThresholdDays,
                        onValueChange = { firstThresholdDays = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NumberPickerRow(
                        label = "重复提醒间隔",
                        value = repeatIntervalDays,
                        range = 1..14,
                        globalValue = globalRepeatIntervalDays,
                        onValueChange = { repeatIntervalDays = it }
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    if (useCustomSettings && currentSetting != null) {
                        OutlinedButton(onClick = {
                            onResetToGlobal()
                            onDismiss()
                        }) {
                            Text("恢复默认")
                        }
                    }

                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }

                    Button(
                        onClick = {
                            if (useCustomSettings) {
                                onSave(
                                    DeviceNotificationSetting(
                                        deviceId = currentSetting?.deviceId ?: 0L,
                                        reminderEnabled = reminderEnabled,
                                        firstThresholdDays = firstThresholdDays,
                                        repeatIntervalDays = repeatIntervalDays
                                    )
                                )
                            }
                            onDismiss()
                        },
                        enabled = !useCustomSettings || (currentSetting != null || currentSetting?.deviceId != null)
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberPickerRow(
    label: String,
    value: Int,
    range: IntRange,
    globalValue: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "全局默认：$globalValue 天",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (value > range.first) onValueChange(value - 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "减少",
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                "$value 天",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            IconButton(
                onClick = { if (value < range.last) onValueChange(value + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "增加",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
