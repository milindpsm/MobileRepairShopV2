package com.example.mobilerepairshopv2

import android.Manifest
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
import com.example.mobilerepairshopv2.data.model.Order
import com.example.mobilerepairshopv2.databinding.ActivityAddOrderBinding
import com.example.mobilerepairshopv2.ui.viewmodel.RepairViewModel
import com.example.mobilerepairshopv2.ui.viewmodel.RepairViewModelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddOrderBinding
    private var latestTmpUri: Uri? = null
    private var latestImageSavedPath: String? = null

    private val viewModel: RepairViewModel by viewModels {
        RepairViewModelFactory((application as RepairShopApplication).repository)
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let { uri ->
                Glide.with(this).load(uri).into(binding.imageViewPhone)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Add New Order"

        binding.buttonCapturePhoto.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        binding.buttonSaveOrder.setOnClickListener {
            saveOrder()
        }
    }

    private fun saveOrder() {
        val customerName = binding.editTextCustomerName.text.toString().trim()
        val customerContact = binding.editTextCustomerContact.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val totalCostText = binding.editTextTotalCost.text.toString().trim()
        val advanceTakenText = binding.editTextAdvanceTaken.text.toString().trim()

        if (customerContact.isEmpty() || totalCostText.isEmpty()) {
            Toast.makeText(this, "Customer Contact and Total Cost are required.", Toast.LENGTH_SHORT).show()
            return
        }

        val totalCost = totalCostText.toDoubleOrNull() ?: 0.0
        val advanceTaken = advanceTakenText.toDoubleOrNull() ?: 0.0

        val newOrder = Order(
            customerName = customerName.ifEmpty { null },
            customerContact = customerContact,
            imagePath = latestImageSavedPath,
            description = description.ifEmpty { null },
            totalCost = totalCost,
            advanceTaken = advanceTaken,
            status = getString(R.string.status_in),
            dateAdded = System.currentTimeMillis(),
            dateCompleted = null
        )

        viewModel.insertOrder(newOrder)
        Toast.makeText(this, "Order saved successfully!", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> launchCamera()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
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

        val tmpFile = File.createTempFile(imageFileName, ".jpg", storageDir).apply {
            latestImageSavedPath = absolutePath
        }

        return FileProvider.getUriForFile(this, "${packageName}.provider", tmpFile)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
