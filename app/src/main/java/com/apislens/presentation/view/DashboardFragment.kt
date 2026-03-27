package com.apislens.presentation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apislens.databinding.FragmentDashboardBinding
import com.apislens.presentation.adapter.DeviceCostAdapter
import com.apislens.presentation.viewmodel.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var adapter: DeviceCostAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DeviceCostAdapter { deviceWithCost ->
            val action = DashboardFragmentDirections
                .actionDashboardToDeviceDetail(deviceWithCost.device.id)
            findNavController().navigate(action)
        }
        binding.rvDevicesCost.adapter = adapter
        binding.rvDevicesCost.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val fmt = NumberFormat.getNumberInstance(Locale.CHINA).apply {
                        maximumFractionDigits = 2; minimumFractionDigits = 2
                    }

                    binding.tvTotalInvestment.text = "¥${fmt.format(state.totalInvestmentCents / 100.0)}"
                    binding.tvTotalDailyCost.text = "¥${fmt.format(state.totalDailyCost)}"
                    binding.tvDeviceCount.text = "${state.totalDevices}"

                    adapter.submitList(state.devices)
                    binding.layoutEmpty.isVisible = state.devices.isEmpty()
                    binding.rvDevicesCost.isVisible = state.devices.isNotEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
