package com.apislens.presentation.view

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.apislens.databinding.FragmentSettingsBinding
import com.apislens.data.utils.ExportImportManager
import com.apislens.data.utils.ExportResult
import com.apislens.data.utils.ImportResult
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var exportImportManager: ExportImportManager

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                when (val result = exportImportManager.exportTo(uri)) {
                    is ExportResult.Success -> {
                        Snackbar.make(
                            binding.root,
                            "导出成功！${result.deviceCount} 台设备，${result.chargeRecordCount} 条充电，${result.usageRecordCount} 条使用记录",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    is ExportResult.Error -> {
                        Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                when (val result = exportImportManager.importFrom(uri)) {
                    is ImportResult.Success -> {
                        Snackbar.make(
                            binding.root,
                            "导入成功！${result.deviceCount} 台设备，${result.chargeRecordCount} 条充电，${result.usageRecordCount} 条使用记录",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    is ImportResult.Error -> {
                        Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnExport.setOnClickListener {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())
            exportLauncher.launch("apislens_backup_$timestamp.json")
        }

        binding.btnImport.setOnClickListener {
            importLauncher.launch("application/json")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
