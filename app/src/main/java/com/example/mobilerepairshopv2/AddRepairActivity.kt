package com.example.mobilerepairshopv2

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.mobilerepairshopv2.data.model.Repair
import com.example.mobilerepairshopv2.databinding.ActivityAddRepairBinding
import com.example.mobilerepairshopv2.ui.viewmodel.RepairViewModel
import com.example.mobilerepairshopv2.ui.viewmodel.RepairViewModelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddRepairActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRepairBinding
    private var latestTmpUri: Uri? = null
    private var latestImageSavedPath: String? = null
    private var selectedDateTimestamp = System.currentTimeMillis()

    private val repairViewModel: RepairViewModel by viewModels {
        RepairViewModelFactory((application as RepairShopApplication).repository)
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .into(binding.imageViewPhone)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to take photos.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRepairBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateDateText()
        binding.buttonSelectDate.setOnClickListener {
            showDatePicker()
        }

        binding.buttonCapturePhoto.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        binding.buttonSaveRepair.setOnClickListener {
            saveRepair()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
            selectedDateTimestamp = selectedCalendar.timeInMillis
            updateDateText()
        }, year, month, day).show()
    }

    private fun updateDateText() {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        binding.textSelectedDate.text = "Date: ${sdf.format(Date(selectedDateTimestamp))}"
    }

    private fun saveRepair() {
        val customerName = binding.editTextCustomerName.text.toString().trim()
        val customerContact = binding.editTextCustomerContact.text.toString().trim()
        val alternateContact = binding.editTextAlternateContact.text.toString().trim()
        val imei = binding.editTextImei.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val totalCostText = binding.editTextTotalCost.text.toString().trim()
        val advanceTakenText = binding.editTextAdvanceTaken.text.toString().trim()

        // --- MODIFIED: Validation now only checks for essential fields ---
        if (customerContact.isEmpty() || totalCostText.isEmpty()) {
            Toast.makeText(this, "Customer Contact and Total Cost are required.", Toast.LENGTH_SHORT).show()
            return
        }

        val totalCost = totalCostText.toDoubleOrNull() ?: 0.0
        val advanceTaken = advanceTakenText.toDoubleOrNull() ?: 0.0

        val newRepair = Repair(
            // --- MODIFIED: Handle potentially empty fields gracefully ---
            customerName = customerName.ifEmpty { null },
            customerContact = customerContact,
            alternateContact = alternateContact.ifEmpty { null },
            imeiNumber = imei.ifEmpty { null },
            description = description.ifEmpty { null },
            imagePath = latestImageSavedPath,
            totalCost = totalCost,
            advanceTaken = advanceTaken,
            status = getString(R.string.status_in),
            dateAdded = selectedDateTimestamp,
            dateCompleted = null
        )

        repairViewModel.insert(newRepair)
        Toast.makeText(this, "Repair saved successfully!", Toast.LENGTH_LONG).show()
        finish()
    }

    // --- (The camera functions below this line remain unchanged) ---
    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(this, "Camera access is needed to add a photo.", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        getTmpFileUri().let { uri ->
            latestTmpUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    private fun getTmpFileUri(): Uri {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        val tmpFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        ).apply {
            latestImageSavedPath = absolutePath
        }

        return FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            tmpFile
        )
    }
}
