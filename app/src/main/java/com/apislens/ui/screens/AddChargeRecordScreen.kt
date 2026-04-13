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
    val chargeDate by viewModel.chargeDate.collectAsState()
    val chargeTime by viewModel.chargeTime.collectAsState()
    val showDatePicker by viewModel.showDatePicker.collectAsState()
    val showTimePicker by viewModel.showTimePicker.collectAsState()

    LaunchedEffect(Unit) {
        if (chargeDate.isEmpty()) {
            viewModel.chargeDate.value = LocalDate.now().toString()
        }
        if (chargeTime.isEmpty()) {
            viewModel.chargeTime.value = LocalTime.now().withSecond(0).withNano(0).toString()
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            LocalDate.parse(chargeDate.ifEmpty { LocalDate.now().toString() })
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        } catch (_: Exception) { null }
    )

    val timePickerState = rememberTimePickerState(
        initialHour = try {
            LocalTime.parse(chargeTime.ifEmpty { LocalTime.now().toString() }).hour
        } catch (_: Exception) { LocalTime.now().hour },
        initialMinute = try {
            LocalTime.parse(chargeTime.ifEmpty { LocalTime.now().toString() }).minute
        } catch (_: Exception) { LocalTime.now().minute },
        is24Hour = true
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.chargeDate.value = date.toString()
                    }
                    viewModel.showDatePicker.value = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDatePicker.value = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.showTimePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.chargeTime.value = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    viewModel.showTimePicker.value = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showTimePicker.value = false }) { Text("取消") }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("选择时间", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                TimePicker(state = timePickerState)
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
                onClick = { viewModel.save(deviceId); onSaved() },
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
            OutlinedTextField(
                value = chargeDate,
                onValueChange = { viewModel.chargeDate.value = it },
                label = { Text("充电日期") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.showDatePicker.value = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                    }
                }
            )

            OutlinedTextField(
                value = chargeTime,
                onValueChange = { viewModel.chargeTime.value = it },
                label = { Text("充电时间") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.showTimePicker.value = true }) {
                        Icon(Icons.Default.Schedule, contentDescription = "选择时间")
                    }
                }
            )

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
