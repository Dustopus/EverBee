package com.apislens.presentation.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.apislens.R
import com.apislens.databinding.FragmentDeviceDetailBinding
import com.apislens.presentation.viewmodel.DeviceDetailUiState
import com.apislens.presentation.viewmodel.DeviceDetailViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class DeviceDetailFragment : Fragment() {

    private var _binding: FragmentDeviceDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DeviceDetailViewModel by viewModels()
    private val args: DeviceDetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDeviceDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        setupTabLayout()
        setupButtons()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> bindUi(state) }
            }
        }
    }

    private fun bindUi(state: DeviceDetailUiState) {
        val device = state.device ?: return
        val fmt = NumberFormat.getNumberInstance(Locale.CHINA).apply {
            maximumFractionDigits = 2; minimumFractionDigits = 2
        }

        binding.toolbar.title = device.name
        binding.tvDeviceName.text = device.name
        binding.tvModel.text = buildString {
            if (device.model.isNotEmpty()) append(device.model)
            if (device.category.isNotEmpty()) {
                if (isNotEmpty()) append(" · ")
                append(device.category)
            }
            if (isEmpty()) append("—")
        }
        binding.tvDailyCost.text = "¥${fmt.format(state.dailyCost)}"
        binding.tvTotalDepreciation.text = "¥${fmt.format(state.totalDepreciation)}"
        binding.tvDaysUsed.text = "${state.daysUsed}"

        updateBatteryChart(state)
        updateUsageChart(state)
        updateChargeChart(state)
    }

    private fun setupTabLayout() {
        // 添加 tab 标题
        binding.tabLayout.newTab().setText("电池健康").let { binding.tabLayout.addTab(it) }
        binding.tabLayout.newTab().setText("使用时长").let { binding.tabLayout.addTab(it) }
        binding.tabLayout.newTab().setText("充电习惯").let { binding.tabLayout.addTab(it) }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.chartBattery.visibility = View.GONE
                binding.chartUsage.visibility = View.GONE
                binding.chartCharge.visibility = View.GONE
                when (tab?.position) {
                    0 -> binding.chartBattery.visibility = View.VISIBLE
                    1 -> binding.chartUsage.visibility = View.VISIBLE
                    2 -> binding.chartCharge.visibility = View.VISIBLE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupButtons() {
        binding.btnAddCharge.setOnClickListener {
            val action = DeviceDetailFragmentDirections
                .actionDeviceDetailToAddCharge(args.deviceId)
            findNavController().navigate(action)
        }
        binding.btnAddUsage.setOnClickListener {
            val action = DeviceDetailFragmentDirections
                .actionDeviceDetailToAddUsage(args.deviceId)
            findNavController().navigate(action)
        }
        binding.btnEdit.setOnClickListener {
            val action = DeviceDetailFragmentDirections
                .actionDeviceDetailToEditDevice(args.deviceId)
            findNavController().navigate(action)
        }
        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("删除设备")
                .setMessage("确定删除此设备及其所有记录？此操作不可恢复。")
                .setPositiveButton("删除") { _, _ ->
                    viewModel.deleteDevice()
                    findNavController().navigateUp()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    // ── Charts ──

    private fun updateBatteryChart(state: DeviceDetailUiState) {
        val records = state.chargeRecords
        if (records.isEmpty()) {
            binding.chartBattery.clear()
            binding.chartBattery.setNoDataText("暂无充电记录")
            return
        }

        // 使用 CostCalculator 计算电池健康度
        val healthData = CostCalculator.calculateBatteryHealth(records)
        if (healthData.isEmpty()) {
            binding.chartBattery.clear()
            binding.chartBattery.setNoDataText("暂无充电记录")
            return
        }

        val entries = healthData.mapIndexed { index, (_, health) ->
            Entry(index.toFloat(), health.toFloat())
        }
        val labels = healthData.map { it.first }

        val colorPrimary = getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val colorError = getThemeColor(com.google.android.material.R.attr.colorError)
        val dataSet = LineDataSet(entries, "电池健康度 %").apply {
            this.color = colorPrimary
            setCircleColor(colorPrimary)
            lineWidth = 2.5f
            circleRadius = 3f
            setDrawCircleHole(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = colorPrimary
            fillAlpha = 30
        }

        // 添加 80% 健康度参考线
        val limitLine = com.github.mikephil.charting.components.LimitLine(80f, "健康阈值").apply {
            lineColor = colorError
            lineWidth = 1f
            enableDashedLine(10f, 10f, 0f)
            labelPosition = com.github.mikephil.charting.components.LimitLine.LimitLabelPosition.RIGHT_TOP
            textSize = 10f
        }

        binding.chartBattery.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = -45f
                labelCount = minOf(labels.size, 7)
            }
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 105f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1A000000")
                addLimitLine(limitLine)
            }
            axisRight.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            animateX(600)
            invalidate()
        }
    }

    private fun updateUsageChart(state: DeviceDetailUiState) {
        val records = state.usageRecords
        if (records.isEmpty()) {
            binding.chartUsage.clear()
            binding.chartUsage.setNoDataText("暂无使用记录")
            return
        }

        // 按周聚合
        val cal = Calendar.getInstance()
        val weeklyData = mutableMapOf<String, Float>()
        val sdfWeek = SimpleDateFormat("MM/dd", Locale.CHINA)
        val sdfRecord = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

        for (record in records) {
            try {
                cal.time = sdfRecord.parse(record.date) ?: continue
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val weekLabel = sdfWeek.format(cal.time)
                weeklyData[weekLabel] = (weeklyData[weekLabel] ?: 0f) + record.usageMinutes / 60f
            } catch (_: Exception) {}
        }

        val sortedKeys = weeklyData.keys.sorted()
        val entries = sortedKeys.mapIndexed { index, key ->
            BarEntry(index.toFloat(), weeklyData[key] ?: 0f)
        }

        val colorPrimary = getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val dataSet = BarDataSet(entries, "使用时长 (h)").apply {
            color = colorPrimary
            setDrawValues(true)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = String.format("%.1fh", value)
            }
        }

        binding.chartUsage.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(sortedKeys.map { "周$it" })
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = -45f
            }
            axisLeft.apply {
                axisMinimum = 0f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1A000000")
            }
            axisRight.isEnabled = false
            animateY(600)
            invalidate()
        }
    }

    private fun updateChargeChart(state: DeviceDetailUiState) {
        val records = state.chargeRecords
        if (records.isEmpty()) {
            binding.chartCharge.clear()
            binding.chartCharge.setNoDataText("暂无充电记录")
            return
        }

        // 按 4 小时区间统计充电次数
        val hourBuckets = FloatArray(6) // 0-3, 4-7, 8-11, 12-15, 16-19, 20-23
        val labels = listOf("0-3", "4-7", "8-11", "12-15", "16-19", "20-23")
        val cal = Calendar.getInstance()

        for (record in records) {
            cal.timeInMillis = record.startTime
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val bucket = hour / 4
            if (bucket in 0..5) hourBuckets[bucket]++
        }

        val entries = hourBuckets.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count)
        }

        val colorSecondary = getThemeColor(com.google.android.material.R.attr.colorSecondary)
        val dataSet = BarDataSet(entries, "充电次数").apply {
            color = colorSecondary
            setDrawValues(true)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = "${value.toInt()}次"
            }
        }

        binding.chartCharge.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                setDrawGridLines(false)
            }
            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1A000000")
            }
            axisRight.isEnabled = false
            animateY(600)
            invalidate()
        }
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
