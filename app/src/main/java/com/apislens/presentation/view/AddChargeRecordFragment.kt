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
import com.apislens.databinding.FragmentAddChargeRecordBinding
import com.apislens.presentation.viewmodel.AddChargeRecordViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AddChargeRecordFragment : Fragment() {

    private var _binding: FragmentAddChargeRecordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddChargeRecordViewModel by viewModels()
    private val args: AddChargeRecordFragmentArgs by navArgs()

    private val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddChargeRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        updateTimeButtons()

        binding.sliderStartLevel.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.tvStartLevel.text = "${value.toInt()}%"
                viewModel.updateStartLevel(value.toInt())
            }
        }
        binding.sliderStartLevel.value = 20f

        binding.sliderEndLevel.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.tvEndLevel.text = "${value.toInt()}%"
                viewModel.updateEndLevel(value.toInt())
            }
        }
        binding.sliderEndLevel.value = 100f

        binding.btnStartTime.setOnClickListener { showDateTimePicker(isStart = true) }
        binding.btnEndTime.setOnClickListener { showDateTimePicker(isStart = false) }

        binding.btnSave.setOnClickListener {
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

    private fun updateTimeButtons() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.formState.collect { state ->
                    binding.btnStartTime.text = timeFormatter.format(Date(state.startTime))
                    binding.btnEndTime.text = timeFormatter.format(Date(state.endTime))
                }
            }
        }
    }

    private fun showDateTimePicker(isStart: Boolean) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isStart) "开始日期" else "结束日期")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { dateMillis ->
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(12)
                .setMinute(0)
                .setTitleText(if (isStart) "开始时间" else "结束时间")
                .build()

            timePicker.addOnPositiveButtonClickListener {
                val cal = Calendar.getInstance()
                cal.timeInMillis = dateMillis
                cal.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                cal.set(Calendar.MINUTE, timePicker.minute)
                val millis = cal.timeInMillis

                if (isStart) viewModel.updateStartTime(millis)
                else viewModel.updateEndTime(millis)
            }
            timePicker.show(parentFragmentManager, "time_picker")
        }
        datePicker.show(parentFragmentManager, "date_picker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
