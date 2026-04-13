package com.apislens.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apislens.data.local.entity.Device
import com.apislens.ui.viewmodel.AddEditDeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDeviceScreen(
    editDeviceId: Long? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddEditDeviceViewModel = hiltViewModel()
) {
    LaunchedEffect(editDeviceId) {
        editDeviceId?.let { viewModel.loadDevice(it) }
    }

    val name by viewModel.name.collectAsState()
    val model by viewModel.model.collectAsState()
    val category by viewModel.category.collectAsState()
    val purchaseDate by viewModel.purchaseDate.collectAsState()
    val purchasePrice by viewModel.purchasePrice.collectAsState()
    val note by viewModel.note.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val showDatePicker by viewModel.showDatePicker.collectAsState()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            java.time.LocalDate.parse(purchaseDate.ifEmpty {
                java.time.LocalDate.now().toString()
            }).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) { null },
        yearRange = 2010..java.time.Year.now().value
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
                        viewModel.purchaseDate.value = date.toString()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.deviceId == null) "添加设备" else "编辑设备") },
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
                    if (viewModel.validate()) {
                        viewModel.save()
                        onSaved()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = "保存")
                }
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
                value = name,
                onValueChange = { viewModel.name.value = it },
                label = { Text("设备名称 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = name.isBlank(),
                supportingText = if (name.isBlank()) {{ Text("设备名称不能为空") }} else null
            )

            OutlinedTextField(
                value = model,
                onValueChange = { viewModel.model.value = it },
                label = { Text("设备型号") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Column {
                Text(
                    "设备分类",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Device.CATEGORIES) { cat ->
                        val isSelected = category == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.category.value = if (isSelected) "" else cat },
                            label = {
                                Text(
                                    cat,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                {
                                    Icon(
                                        categoryIcon(cat),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }
                if (category.isEmpty()) {
                    Text(
                        "请选择一个分类",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            OutlinedTextField(
                value = purchaseDate,
                onValueChange = { viewModel.purchaseDate.value = it },
                label = { Text("购买日期 (yyyy-MM-dd)") },
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
                value = purchasePrice,
                onValueChange = { viewModel.purchasePrice.value = it },
                label = { Text("购买价格 (元) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = purchasePrice.isBlank() || purchasePrice.toDoubleOrNull() == null,
                supportingText = if (purchasePrice.isBlank()) {{ Text("价格不能为空") }} else null
            )

            OutlinedTextField(
                value = note,
                onValueChange = { viewModel.note.value = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
        }
    }
}
