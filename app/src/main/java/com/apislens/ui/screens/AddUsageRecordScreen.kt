package com.apislens.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apislens.ui.viewmodel.AddUsageRecordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUsageRecordScreen(
    deviceId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddUsageRecordViewModel = hiltViewModel()
) {
    val usageMinutes by viewModel.usageMinutes.collectAsState()
    val note by viewModel.note.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加使用记录") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = usageMinutes,
                onValueChange = {
                    if (it.isEmpty() || it.toIntOrNull()?.let { v -> v >= 0 } == true) {
                        viewModel.usageMinutes.value = it
                    }
                },
                label = { Text("使用时长 (分钟)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("请输入正整数") }
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
