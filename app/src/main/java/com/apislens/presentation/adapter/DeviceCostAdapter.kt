package com.apislens.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.apislens.databinding.ItemDeviceCostBinding
import com.apislens.presentation.viewmodel.DeviceWithCost
import java.text.NumberFormat
import java.util.Locale

class DeviceCostAdapter(
    private val onItemClick: (DeviceWithCost) -> Unit
) : ListAdapter<DeviceWithCost, DeviceCostAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviceCostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemDeviceCostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DeviceWithCost) {
            val fmt = NumberFormat.getNumberInstance(Locale.CHINA).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }

            binding.tvDeviceName.text = item.device.name
            binding.tvDailyCost.text = fmt.format(item.dailyCost)
            binding.tvInvestment.text = "投入 ¥${fmt.format(item.device.purchasePrice)} · ${item.daysUsed} 天"

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DeviceWithCost>() {
        override fun areItemsTheSame(a: DeviceWithCost, b: DeviceWithCost) = a.device.id == b.device.id
        override fun areContentsTheSame(a: DeviceWithCost, b: DeviceWithCost) = a == b
    }
}
