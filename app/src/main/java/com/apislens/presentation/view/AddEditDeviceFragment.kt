package com.apislens.presentation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.apislens.data.local.entity.Device
import com.apislens.databinding.FragmentAddEditDeviceBinding
import com.apislens.presentation.viewmodel.AddEditDeviceViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AddEditDeviceFragment : Fragment() {

    private var _binding: FragmentAddEditDeviceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddEditDeviceViewModel by viewModels()
    private val args: AddEditDeviceFragmentArgs by navArgs()

    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // 持久化读取权限
            requireContext().contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.updateIconUri(uri.toString())
            Glide.with(this).load(uri).centerCrop().into(binding.ivDevicePhoto)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddEditDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = if (viewModel.isEditing) "编辑设备" else "添加设备"
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.btnSave.text = if (viewModel.isEditing) "更新" else "保存"

        // 设置分类下拉
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, Device.CATEGORIES)
        binding.dropdownCategory.setAdapter(categoryAdapter)
        binding.dropdownCategory.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateCategory(Device.CATEGORIES[position])
        }

        // 设置照片选择
        binding.btnPickPhoto.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
        }

        binding.etDate.setOnClickListener { showDatePicker() }
        binding.etDate.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDatePicker() }

        binding.btnSave.setOnClickListener {
            viewModel.updateName(binding.etName.text.toString())
            viewModel.updateModel(binding.etModel.text.toString())
            viewModel.updateCategory(binding.dropdownCategory.text.toString())
            viewModel.updatePurchasePrice(binding.etPrice.text.toString())
            viewModel.updateNote(binding.etNote.text.toString())
            viewModel.save()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.formState.collect { state ->
                    // 只在初始加载时填充字段
                    if (binding.etName.text.isNullOrEmpty() && state.name.isNotEmpty()) {
                        binding.etName.setText(state.name)
                        binding.etModel.setText(state.model)
                        binding.dropdownCategory.setText(state.category, false)
                        binding.etPrice.setText(state.purchasePrice)
                        binding.etDate.setText(state.purchaseDate)
                        binding.etNote.setText(state.note)
                        // 加载已有照片
                        state.iconUri?.let { uri ->
                            Glide.with(this@AddEditDeviceFragment).load(uri).centerCrop().into(binding.ivDevicePhoto)
                        }
                    }

                    state.error?.let { error ->
                        com.google.android.material.snackbar.Snackbar
                            .make(binding.root, error, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                            .show()
                        viewModel.clearError()
                    }

                    if (state.isSaved) {
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择购买日期")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            val dateStr = sdf.format(Date(millis))
            binding.etDate.setText(dateStr)
            viewModel.updatePurchaseDate(dateStr)
        }
        picker.show(parentFragmentManager, "date_picker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
