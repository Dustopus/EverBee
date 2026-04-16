package com.apislens.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apislens.ui.viewmodel.AddChargeRecordViewModel
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChargeRecordScreen(
    deviceId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddChargeRecordViewModel = hiltViewModel()
) {
    val startLevel by viewModel.startLevel.collectAsState()
    val endLevel by viewModel.endLevel.collectAsState()
    val note by viewModel.note.collectAsState()
    
    val startDate by viewModel.startDate.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val endTime by viewModel.endTime.collectAsState()
    val isOvernightCharge by viewModel.isOvernightCharge.collectAsState()
    
    val showStartDatePicker by viewModel.showStartDatePicker.collectAsState()
    val showStartTimePicker by viewModel.showStartTimePicker.collectAsState()
    val showEndDatePicker by viewModel.showEndDatePicker.collectAsState()
    val showEndTimePicker by viewModel.showEndTimePicker.collectAsState()
    val timeValidationError by viewModel.timeValidationError.collectAsState()

    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            LocalDate.parse(startDate.ifEmpty { LocalDate.now().toString() })
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        } catch (_: Exception) { null }
    )

    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            LocalDate.parse(endDate.ifEmpty { LocalDate.now().toString() })
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        } catch (_: Exception) { null }
    )

    val startTimePickerState = rememberTimePickerState(
        initialHour = try {
            LocalTime.parse(startTime.ifEmpty { LocalTime.now().toString() }).hour
        } catch (_: Exception) { LocalTime.now().hour },
        initialMinute = try {
            LocalTime.parse(startTime.ifEmpty { LocalTime.now().toString() }).minute
        } catch (_: Exception) { LocalTime.now().minute },
        is24Hour = true
    )

    val endTimePickerState = rememberTimePickerState(
        initialHour = try {
            LocalTime.parse(endTime.ifEmpty { LocalTime.now().toString() }).hour
        } catch (_: Exception) { LocalTime.now().hour },
        initialMinute = try {
            LocalTime.parse(endTime.ifEmpty { LocalTime.now().toString() }).minute
        } catch (_: Exception) { LocalTime.now().minute },
        is24Hour = true
    )

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.showStartDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.onStartDateChanged(date.toString())
                    }
                    viewModel.showStartDatePicker.value = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showStartDatePicker.value = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showStartTimePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.showStartTimePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onStartTimeChanged(String.format("%02d:%02d", startTimePickerState.hour, startTimePickerState.minute))
                    viewModel.showStartTimePicker.value = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showStartTimePicker.value = false }) { Text("取消") }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("选择开始时间", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                TimePicker(state = startTimePickerState)
            }
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.showEndDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.onEndDateChanged(date.toString())
                    }
                    viewModel.showEndDatePicker.value = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showEndDatePicker.value = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    if (showEndTimePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.showEndTimePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEndTimeChanged(String.format("%02d:%02d", endTimePickerState.hour, endTimePickerState.minute))
                    viewModel.showEndTimePicker.value = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showEndTimePicker.value = false }) { Text("取消") }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("选择结束时间", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                TimePicker(state = endTimePickerState)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加充电记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (viewModel.validateTimeLogic()) {
                        viewModel.save(deviceId)
                        onSaved()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Save, contentDescription = "保存")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "⏰ 充电时间段",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("开始", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = startDate,
                                onValueChange = { viewModel.startDate.value = it },
                                label = { Text("日期") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.showStartDatePicker.value = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.DateRange, contentDescription = "选择开始日期", modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(" ", style = MaterialTheme.typography.labelSmall)
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { viewModel.onStartTimeChanged(it) },
                                label = { Text("时间") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.showStartTimePicker.value = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Schedule, contentDescription = "选择开始时间", modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Icon(
                        Icons.Default.South,
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("结束", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = endDate,
                                onValueChange = { viewModel.onEndDateChanged(it) },
                                label = { 
                                    Text(
                                        if (isOvernightCharge) "次日" else "日期"
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                readOnly = true,
                                colors = if (isOvernightCharge) {
                                    OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                } else {
                                    OutlinedTextFieldDefaults.colors()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.showEndDatePicker.value = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.DateRange, contentDescription = "选择结束日期", modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(" ", style = MaterialTheme.typography.labelSmall)
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { viewModel.onEndTimeChanged(it) },
                                label = { Text("时间") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.showEndTimePicker.value = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.AccessTime, contentDescription = "选择结束时间", modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }

                    if (isOvernightCharge) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Nightlight,
                                    contentDescription = "跨天",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "检测到跨夜充电",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            timeValidationError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "错误",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            OutlinedTextField(
                value = startLevel,
                onValueChange = {
                    if (it.isEmpty() || it.toIntOrNull()?.let { v -> v in 0..100 } == true) {
                        viewModel.startLevel.value = it
                    }
                },
                label = { Text("开始电量 (%)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text("范围: 0-100") }
            )
            
            OutlinedTextField(
                value = endLevel,
                onValueChange = {
                    if (it.isEmpty() || it.toIntOrNull()?.let { v -> v in 0..100 } == true) {
                        viewModel.endLevel.value = it
                    }
                },
                label = { Text("结束电量 (%)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text("范围: 0-100") }
            )
            
            OutlinedTextField(
                value = note,
                onValueChange = { viewModel.note.value = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }
}
