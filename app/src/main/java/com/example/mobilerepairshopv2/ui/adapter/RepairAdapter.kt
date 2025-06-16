package com.example.mobilerepairshopv2.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilerepairshopv2.R
import com.example.mobilerepairshopv2.data.model.Repair
import com.example.mobilerepairshopv2.databinding.ItemRepairBinding
import java.util.concurrent.TimeUnit

class RepairAdapter(private val onItemClicked: (Repair) -> Unit) :
    ListAdapter<Repair, RepairAdapter.RepairViewHolder>(RepairDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepairViewHolder {
        val binding = ItemRepairBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RepairViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RepairViewHolder, position: Int) {
        val currentRepair = getItem(position)
        holder.itemView.setOnClickListener {
            onItemClicked(currentRepair)
        }
        holder.bind(currentRepair)
    }

    class RepairViewHolder(private val binding: ItemRepairBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(repair: Repair) {
            val context = binding.root.context
            binding.textViewOrderId.text = "ID: ${repair.id}"

            // --- THIS IS THE NEW LOGIC ---
            // If the customer name is not empty, show it.
            // Otherwise, show the customer contact number instead.
            binding.textViewCustomerName.text = if (!repair.customerName.isNullOrEmpty()) {
                repair.customerName
            } else {
                repair.customerContact
            }
            // --- END OF NEW LOGIC ---

            binding.chipStatus.text = repair.status

            if (repair.status == context.getString(R.string.status_out)) {
                binding.textViewDaysAgo.text = "Completed"
            } else {
                val currentTime = System.currentTimeMillis()
                val diff = currentTime - repair.dateAdded
                val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)

                binding.textViewDaysAgo.text = when (days) {
                    0L -> "Today"
                    1L -> "1 day ago"
                    else -> "$days days ago"
                }
            }

            when (repair.status) {
                context.getString(R.string.status_in) -> {
                    binding.chipStatus.setChipBackgroundColorResource(R.color.status_in_background)
                    binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.status_in_text))
                }
                context.getString(R.string.status_pending) -> {
                    binding.chipStatus.setChipBackgroundColorResource(R.color.status_pending_background)
                    binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending_text))
                }
                context.getString(R.string.status_out) -> {
                    binding.chipStatus.setChipBackgroundColorResource(R.color.status_out_background)
                    binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.status_out_text))
                }
            }
        }
    }

    companion object {
        private val RepairDiffCallback = object : DiffUtil.ItemCallback<Repair>() {
            override fun areItemsTheSame(oldItem: Repair, newItem: Repair): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Repair, newItem: Repair): Boolean {
                return oldItem == newItem
            }
        }
    }
}
