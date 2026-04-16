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
    val lifecycleMonths by viewModel.lifecycleMonths.collectAsState()
    val note by viewModel.note.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val showDatePicker by viewModel.showDatePicker.collectAsState()

    var showLifecycleInfo by remember { mutableStateOf(false) }

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

    if (showLifecycleInfo) {
        AlertDialog(
            onDismissRequest = { showLifecycleInfo = false },
            title = { Text("设备生命周期说明") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "生命周期定义",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "设备生命周期是指设备从购入到退役的预期使用时间，以月为单位计算。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "参与运算逻辑",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "• 折旧计算：每日折旧额 = 购入价格 ÷ (生命周期月数 × 30)，累计折旧不超过购入价格",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "• 维护周期预警：当设备接近生命周期终点时，系统将提示设备更新建议",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "• 设备更新建议：超过生命周期的设备将被标记为建议更换",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "默认值为 36 个月（3 年），可根据设备类型和个人使用习惯调整。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLifecycleInfo = false }) { Text("知道了") }
            }
        )
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
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category.ifEmpty { "请选择分类" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        Device.CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            categoryIcon(cat),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (category == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            cat,
                                            fontWeight = if (category == cat) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (category == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.category.value = cat
                                    categoryExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
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
                value = lifecycleMonths,
                onValueChange = {
                    if (it.isEmpty() || it.toIntOrNull() != null) {
                        viewModel.lifecycleMonths.value = it
                    }
                },
                label = { Text("设备生命周期 (月) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = lifecycleMonths.toIntOrNull()?.let { it <= 0 } ?: true,
                trailingIcon = {
                    IconButton(onClick = { showLifecycleInfo = true }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "生命周期说明",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                supportingText = {
                    val months = lifecycleMonths.toIntOrNull() ?: 0
                    if (months > 0) {
                        Text("${months} 个月（约 ${months / 12} 年 ${months % 12} 月）")
                    } else if (lifecycleMonths.isNotEmpty()) {
                        Text("请输入正整数", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("默认 ${Device.DEFAULT_LIFECYCLE_MONTHS} 个月（3 年）")
                    }
                }
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
