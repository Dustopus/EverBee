package com.apislens.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apislens.data.local.entity.Device
import com.apislens.ui.theme.SuccessGreen
import com.apislens.ui.theme.WarningAmber
import com.apislens.ui.viewmodel.DeviceListViewModel
import com.apislens.rust.RustCore
import com.apislens.utils.RelativeTimeFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    onDeviceClick: (Long) -> Unit,
    onAddDevice: () -> Unit,
    onBack: () -> Unit,
    viewModel: DeviceListViewModel = hiltViewModel()
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle(initialValue = emptyList())
    val allDevices by viewModel.allDevices.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedCategories by viewModel.selectedCategories.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsStateWithLifecycle(initialValue = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val lastChargeTimes by viewModel.lastChargeTimes.collectAsStateWithLifecycle(initialValue = emptyMap())
    var showSearch by remember { mutableStateOf(false) }

    val displayCategories = if (availableCategories.isNotEmpty()) availableCategories else Device.CATEGORIES

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("搜索设备...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    } else {
                        Text("设备列表")
                    }
                },
                navigationIcon = {
                    if (showSearch) {
                        IconButton(onClick = {
                            showSearch = false
                            viewModel.updateSearchQuery("")
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "关闭搜索")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (!showSearch) {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    }
                    if (selectedCategories.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearCategories() }) {
                            Icon(Icons.Default.FilterListOff, contentDescription = "清除过滤")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddDevice,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加设备")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CategoryFilterRow(
                categories = displayCategories,
                selectedCategories = selectedCategories,
                onCategoryToggle = { viewModel.toggleCategory(it) }
            )

            if (devices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Devices,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (allDevices.isEmpty()) "暂无设备，点击右下角添加" else "没有匹配的设备",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "${devices.size} 台设备",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(devices, key = { it.id }) { device ->
                        DeviceListItem(
                            device = device,
                            lastChargeTime = lastChargeTimes[device.id],
                            onClick = { onDeviceClick(device.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilterRow(
    categories: List<String>,
    selectedCategories: Set<String>,
    onCategoryToggle: (String) -> Unit
) {
    AnimatedVisibility(visible = categories.isNotEmpty()) {
        Column {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category in selectedCategories
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategoryToggle(category) },
                        label = {
                            Text(
                                category,
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
                                    categoryIcon(category),
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
            if (selectedCategories.isNotEmpty()) {
                Text(
                    "已选 ${selectedCategories.size} 个分类",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun categoryIcon(category: String) = when (category) {
    "手机" -> Icons.Default.PhoneAndroid
    "平板" -> Icons.Default.Tablet
    "笔记本" -> Icons.Default.Laptop
    "智能手表" -> Icons.Default.Watch
    "耳机" -> Icons.Default.Headphones
    "充电宝" -> Icons.Default.BatteryChargingFull
    "电池" -> Icons.Default.BatteryStd
    "游戏机" -> Icons.Default.SportsEsports
    "相机" -> Icons.Default.CameraAlt
    "音箱" -> Icons.Default.Speaker
    else -> Icons.Default.Category
}

@Composable
fun DeviceListItem(device: Device, lastChargeTime: Long? = null, onClick: () -> Unit) {
    val daysUsed = RustCore.daysSince(device.purchaseDate).toInt().coerceAtLeast(1)
    val dailyCost = RustCore.calculateDailyCost(device.purchasePriceCents, device.purchaseDate)
    val lastChargeText = lastChargeTime?.let { RelativeTimeFormatter.format(it) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            categoryIcon(device.category.ifEmpty { "其他" }),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (device.category.isNotEmpty()) {
                        Text(
                            device.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "¥${String.format("%.2f", dailyCost)}/天",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "已使用 $daysUsed 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (lastChargeText != null) {
                    Text(
                        "上次充电 $lastChargeText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
