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
import com.apislens.data.local.entity.Device
import com.apislens.databinding.FragmentDeviceListBinding
import com.apislens.presentation.adapter.DeviceAdapter
import com.apislens.presentation.viewmodel.DeviceListViewModel
import com.apislens.presentation.viewmodel.DeviceWithCost
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DeviceListFragment : Fragment() {

    private var _binding: FragmentDeviceListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DeviceListViewModel by viewModels()
    private lateinit var adapter: DeviceAdapter
    private var allDevices: List<DeviceWithCost> = emptyList()
    private var selectedCategory: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDeviceListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DeviceAdapter { deviceWithCost ->
            val action = DeviceListFragmentDirections
                .actionDeviceListToDeviceDetail(deviceWithCost.device.id)
            findNavController().navigate(action)
        }
        binding.rvDevices.adapter = adapter

        binding.fabAddDevice.setOnClickListener {
            val action = DeviceListFragmentDirections
                .actionDeviceListToAddDevice(0L)
            findNavController().navigate(action)
        }

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.devicesWithCost.collect { devices ->
                    allDevices = devices
                    updateCategoryChips(devices)
                    applyFilter()
                }
            }
        }
    }

    private fun updateCategoryChips(devices: List<DeviceWithCost>) {
        val categories = devices.map { it.device.category }.filter { it.isNotEmpty() }.distinct().sorted()
        if (categories.isEmpty()) {
            binding.chipGroupCategories.isVisible = false
            return
        }

        binding.chipGroupCategories.isVisible = true

        // Only rebuild if category count changed
        if (binding.chipGroupCategories.childCount != categories.size + 1) {
            binding.chipGroupCategories.removeAllViews()

            // "全部" chip
            val allChip = Chip(requireContext()).apply {
                text = "全部"
                isCheckable = true
                isChecked = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedCategory = null
                        applyFilter()
                    }
                }
            }
            binding.chipGroupCategories.addView(allChip)

            for (category in categories) {
                val chip = Chip(requireContext()).apply {
                    text = category
                    isCheckable = true
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            selectedCategory = category
                            applyFilter()
                        } else if (selectedCategory == category) {
                            selectedCategory = null
                            allChip.isChecked = true
                            applyFilter()
                        }
                    }
                }
                binding.chipGroupCategories.addView(chip)
            }
        }
    }

    private fun applyFilter() {
        val filtered = if (selectedCategory != null) {
            allDevices.filter { it.device.category == selectedCategory }
        } else {
            allDevices
        }
        adapter.submitList(filtered)
        binding.layoutEmpty.isVisible = filtered.isEmpty()
        binding.rvDevices.isVisible = filtered.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
