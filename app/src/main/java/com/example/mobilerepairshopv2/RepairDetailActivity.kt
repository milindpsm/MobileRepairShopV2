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
import com.example.mobilerepairshopv2.data.model.Repair
import com.example.mobilerepairshopv2.databinding.ActivityRepairDetailBinding
import com.example.mobilerepairshopv2.ui.viewmodel.RepairViewModel
import com.example.mobilerepairshopv2.ui.viewmodel.RepairViewModelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RepairDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRepairDetailBinding
    private var currentRepair: Repair? = null
    private val repairViewModel: RepairViewModel by viewModels {
        RepairViewModelFactory((application as RepairShopApplication).repository)
    }
    private var isUpdating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRepairDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val repairId = intent.getLongExtra(REPAIR_ID, -1L)
        if (repairId == -1L) {
            Toast.makeText(this, "Error: Invalid Repair ID", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupStatusSpinner()
        setupTextChangedListeners()

        repairViewModel.getRepairById(repairId).observe(this) { repair ->
            repair?.let {
                if (currentRepair == null) {
                    currentRepair = it
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
            .setTitle("Delete Repair")
            .setMessage("Are you sure you want to delete this entry? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                currentRepair?.let {
                    repairViewModel.delete(it)
                    Toast.makeText(this, "Repair deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.baseline_delete_forever_24)
            .show()
    }

    private fun bindDataToViews(repair: Repair) {
        binding.detailTitle.text = getString(R.string.detail_title_format, repair.id)
        binding.detailCustomerName.text = getString(R.string.customer_name_format, repair.customerName)
        binding.detailCustomerContact.text = getString(R.string.contact_format, repair.customerContact)
        val notApplicable = getString(R.string.not_applicable)
        binding.detailAlternateContact.text = getString(R.string.alt_contact_format, repair.alternateContact ?: notApplicable)
        binding.detailImei.text = getString(R.string.imei_format, repair.imeiNumber ?: notApplicable)
        binding.detailDateAdded.text = getString(R.string.date_added_format, formatDate(repair.dateAdded))
        binding.detailDescription.setText(repair.description)

        if (repair.imagePath != null) {
            Glide.with(this)
                .load(File(repair.imagePath))
                .into(binding.detailImageViewPhone)
        }

        binding.detailTotalCost.setText(repair.totalCost.toString())
        binding.detailAdvanceTaken.setText(repair.advanceTaken.toString())
        validateAndCalculate()

        val statusArray = resources.getStringArray(R.array.status_array)
        val statusPosition = statusArray.indexOf(repair.status)
        if (statusPosition >= 0) {
            binding.spinnerStatus.setSelection(statusPosition)
        }
    }

    private fun setupTextChangedListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdating) {
                    validateAndCalculate()
                }
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
            this,
            R.array.status_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerStatus.adapter = adapter
        }
    }

    private fun saveChanges() {
        val selectedStatus = binding.spinnerStatus.selectedItem.toString()
        val totalCostText = binding.detailTotalCost.text.toString()
        val advanceTakenText = binding.detailAdvanceTaken.text.toString()
        val description = binding.detailDescription.text.toString().trim()

        if (totalCostText.isEmpty() || advanceTakenText.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Description, Cost and Advance fields cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        currentRepair?.let { repair ->
            repair.status = selectedStatus
            repair.totalCost = totalCostText.toDouble()
            repair.advanceTaken = advanceTakenText.toDouble()
            repair.description = description

            if (selectedStatus == getString(R.string.status_out) && repair.dateCompleted == null) {
                repair.dateCompleted = System.currentTimeMillis()
            }

            repairViewModel.update(repair)
            Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun sendWhatsAppMessage() {
        currentRepair?.let { repair ->
            val contactNumber = repair.customerContact
            if (contactNumber.isEmpty()) {
                Toast.makeText(this, "No primary contact number available.", Toast.LENGTH_LONG).show()
                return
            }

            if(repair.status != getString(R.string.status_out)) {
                Toast.makeText(this, "Can only send message for 'Out' status repairs.", Toast.LENGTH_LONG).show()
                return
            }

            val remainingDue = repair.totalCost - repair.advanceTaken
            val message = """
                Hi ${repair.customerName},
                Your mobile is repaired and ready to take home.
                Total Repairing Cost: ₹${"%.2f".format(repair.totalCost)}
                Remaining Cost: ₹${"%.2f".format(remainingDue)}
                Please collect your device from the shop. Thank you!
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
        const val REPAIR_ID = "repair_id"
    }
}
