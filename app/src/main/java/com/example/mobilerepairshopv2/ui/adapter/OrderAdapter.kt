package com.example.mobilerepairshopv2.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilerepairshopv2.R
import com.example.mobilerepairshopv2.data.model.Order
import com.example.mobilerepairshopv2.databinding.ItemOrderBinding
import java.util.concurrent.TimeUnit

class OrderAdapter(private val onItemClicked: (Order) -> Unit) :
    ListAdapter<Order, OrderAdapter.OrderViewHolder>(OrderDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val currentOrder = getItem(position)
        holder.itemView.setOnClickListener {
            onItemClicked(currentOrder)
        }
        holder.bind(currentOrder)
    }

    class OrderViewHolder(private val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: Order) {
            val context = binding.root.context
            binding.textViewOrderId.text = "ID: ${order.id}"

            // --- THIS IS THE NEW LOGIC ---
            // If the customer name is not empty, show it.
            // Otherwise, show the customer contact number instead.
            binding.textViewCustomerName.text = if (!order.customerName.isNullOrEmpty()) {
                order.customerName
            } else {
                order.customerContact
            }
            // --- END OF NEW LOGIC ---

            binding.chipStatus.text = order.status

            if (order.status == context.getString(R.string.status_out)) {
                binding.textViewDaysAgo.text = "Completed"
            } else {
                val currentTime = System.currentTimeMillis()
                val diff = currentTime - order.dateAdded
                val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)

                binding.textViewDaysAgo.text = when (days) {
                    0L -> "Today"
                    1L -> "1 day ago"
                    else -> "$days days ago"
                }
            }

            when (order.status) {
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
        private val OrderDiffCallback = object : DiffUtil.ItemCallback<Order>() {
            override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
                return oldItem == newItem
            }
        }
    }
}
