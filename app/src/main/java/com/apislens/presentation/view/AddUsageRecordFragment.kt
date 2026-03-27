package com.apislens.presentation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.apislens.databinding.FragmentAddUsageRecordBinding
import com.apislens.presentation.viewmodel.AddUsageRecordViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AddUsageRecordFragment : Fragment() {

    private var _binding: FragmentAddUsageRecordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddUsageRecordViewModel by viewModels()
    private val args: AddUsageRecordFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddUsageRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // 设置默认日期为今天
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
        binding.btnDate.text = today
        viewModel.updateDate(today)

        binding.btnDate.setOnClickListener { showDatePicker() }

        binding.btnSave.setOnClickListener {
            try {
                viewModel.updateHours(binding.etHours.text.toString().toIntOrNull() ?: 0)
                viewModel.updateMinutes(binding.etMinutes.text.toString().toIntOrNull() ?: 0)
            } catch (_: Exception) {}
            viewModel.updateNote(binding.etNote.text.toString())
            viewModel.save()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.formState.collect { state ->
                    state.error?.let { error ->
                        com.google.android.material.snackbar.Snackbar
                            .make(binding.root, error, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                            .show()
                        viewModel.clearError()
                    }
                    if (state.isSaved) findNavController().navigateUp()
                }
            }
        }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择日期")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            val dateStr = sdf.format(Date(millis))
            binding.btnDate.text = dateStr
            viewModel.updateDate(dateStr)
        }
        picker.show(parentFragmentManager, "date_picker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
