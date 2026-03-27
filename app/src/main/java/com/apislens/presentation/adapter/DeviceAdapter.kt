package com.apislens.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.apislens.databinding.ItemDeviceBinding
import com.apislens.presentation.viewmodel.DeviceWithCost
import java.text.NumberFormat
import java.util.Locale

class DeviceAdapter(
    private val onItemClick: (DeviceWithCost) -> Unit
) : ListAdapter<DeviceWithCost, DeviceAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DeviceWithCost) {
            val fmt = NumberFormat.getNumberInstance(Locale.CHINA).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }

            binding.tvDeviceName.text = item.device.name
            binding.tvDeviceModel.text = item.device.model.ifEmpty { "—" }
            binding.tvDailyCost.text = fmt.format(item.dailyCost)
            binding.tvDaysUsed.text = "已使用 ${item.daysUsed} 天"
            binding.tvPurchasePrice.text = "购入 ¥${fmt.format(item.device.purchasePrice)}"

            // 显示设备图标/照片
            if (item.device.iconUri != null) {
                Glide.with(binding.ivDeviceIcon.context)
                    .load(item.device.iconUri)
                    .centerCrop()
                    .into(binding.ivDeviceIcon)
            } else {
                binding.ivDeviceIcon.setImageResource(com.apislens.R.drawable.ic_devices)
            }

            // 显示分类标签
            if (item.device.category.isNotEmpty()) {
                binding.tvCategory.text = item.device.category
                binding.tvCategory.visibility = android.view.View.VISIBLE
            } else {
                binding.tvCategory.visibility = android.view.View.GONE
            }

            binding.cardDevice.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DeviceWithCost>() {
        override fun areItemsTheSame(a: DeviceWithCost, b: DeviceWithCost) = a.device.id == b.device.id
        override fun areContentsTheSame(a: DeviceWithCost, b: DeviceWithCost) = a == b
    }
}
