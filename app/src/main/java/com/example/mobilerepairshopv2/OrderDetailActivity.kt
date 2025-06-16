package com.example.mobilerepairshopv2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.mobilerepairshopv2.data.model.Order
import com.example.mobilerepairshopv2.databinding.ActivityOrderDetailBinding
import com.example.mobilerepairshopv2.ui.viewmodel.RepairViewModel
import com.example.mobilerepairshopv2.ui.viewmodel.RepairViewModelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailBinding
    private var currentOrder: Order? = null
    private val viewModel: RepairViewModel by viewModels {
        RepairViewModelFactory((application as RepairShopApplication).repository)
    }
    private var isUpdating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val orderId = intent.getLongExtra(ORDER_ID, -1L)
        if (orderId == -1L) {
            Toast.makeText(this, "Error: Invalid Order ID", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupStatusSpinner()
        setupTextChangedListeners()

        viewModel.getOrderById(orderId).observe(this) { order ->
            order?.let {
                if (currentOrder == null) {
                    currentOrder = it
                    bindDataToViews(it)
                }
            }
        }

        binding.buttonSaveChanges.setOnClickListener {
            saveChanges()
        }

        binding.buttonSendMessage.setOnClickListener {
            sendWhatsAppMessage()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                showDeleteConfirmationDialog()
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Order")
            .setMessage("Are you sure you want to delete this order? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                currentOrder?.let {
                    viewModel.deleteOrder(it)
                    Toast.makeText(this, "Order deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.ic_baseline_delete_24)
            .show()
    }

    private fun bindDataToViews(order: Order) {
        binding.detailTitle.text = "Order ID: ${order.id}"
        binding.detailCustomerName.text = "Customer: ${order.customerName ?: "N/A"}"
        binding.detailCustomerContact.text = "Contact: ${order.customerContact}"
        binding.detailDateAdded.text = "Date Added: ${formatDate(order.dateAdded)}"
        binding.detailDescription.text = "Description: ${order.description ?: "N/A"}"

        if (order.imagePath != null) {
            Glide.with(this).load(File(order.imagePath)).into(binding.detailImageViewPhone)
        }

        binding.detailTotalCost.setText(order.totalCost.toString())
        binding.detailAdvanceTaken.setText(order.advanceTaken.toString())
        validateAndCalculate()

        val statusArray = resources.getStringArray(R.array.status_array)
        val statusPosition = statusArray.indexOf(order.status)
        if (statusPosition >= 0) {
            binding.spinnerStatus.setSelection(statusPosition)
        }
    }

    private fun setupTextChangedListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdating) validateAndCalculate()
            }
        }
        binding.detailTotalCost.addTextChangedListener(textWatcher)
        binding.detailAdvanceTaken.addTextChangedListener(textWatcher)
    }

    private fun validateAndCalculate() {
        isUpdating = true
        val totalCost = binding.detailTotalCost.text.toString().toDoubleOrNull() ?: 0.0
        var advanceTaken = binding.detailAdvanceTaken.text.toString().toDoubleOrNull() ?: 0.0

        if (advanceTaken > totalCost) {
            advanceTaken = totalCost
            binding.layoutAdvanceTaken.error = getString(R.string.advance_error)
            binding.detailAdvanceTaken.setText(advanceTaken.toString())
            binding.detailAdvanceTaken.text?.let { binding.detailAdvanceTaken.setSelection(it.length) }
        } else {
            binding.layoutAdvanceTaken.error = null
        }

        val remainingDue = totalCost - advanceTaken
        binding.detailRemainingDue.text = getString(R.string.remaining_due_format, remainingDue)
        isUpdating = false
    }

    private fun setupStatusSpinner() {
        ArrayAdapter.createFromResource(
            this, R.array.status_array, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerStatus.adapter = adapter
        }
    }

    private fun saveChanges() {
        val selectedStatus = binding.spinnerStatus.selectedItem.toString()
        val totalCostText = binding.detailTotalCost.text.toString()
        val advanceTakenText = binding.detailAdvanceTaken.text.toString()

        if (totalCostText.isEmpty()) {
            Toast.makeText(this, "Total Cost cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        currentOrder?.let { order ->
            order.status = selectedStatus
            order.totalCost = totalCostText.toDouble()
            order.advanceTaken = advanceTakenText.toDoubleOrNull() ?: 0.0
            order.description = binding.detailDescription.text.toString().trim().ifEmpty { null }

            if (selectedStatus == getString(R.string.status_out) && order.dateCompleted == null) {
                order.dateCompleted = System.currentTimeMillis()
            }

            viewModel.updateOrder(order)
            Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun sendWhatsAppMessage() {
        currentOrder?.let { order ->
            val contactNumber = order.customerContact
            if (order.status != getString(R.string.status_out)) {
                Toast.makeText(this, "Can only send message for 'Out' status orders.", Toast.LENGTH_LONG).show()
                return
            }

            val remainingDue = order.totalCost - order.advanceTaken
            val message = """
                Hi ${order.customerName ?: "Valued Customer"},
                Your order is ready for pickup.
                Total Cost: ₹${"%.2f".format(order.totalCost)}
                Remaining Cost: ₹${"%.2f".format(remainingDue)}
                Thank you!
            """.trimIndent()

            val formattedNumber = "+91$contactNumber"
            val url = "https://api.whatsapp.com/send?phone=$formattedNumber&text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())

            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "WhatsApp is not installed on this device.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    companion object {
        const val ORDER_ID = "order_id"
    }
}
