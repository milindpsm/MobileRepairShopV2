package com.example.mobilerepairshopv2

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mobilerepairshopv2.data.model.Order
import com.example.mobilerepairshopv2.data.model.Repair
import com.example.mobilerepairshopv2.databinding.ActivityMainBinding
import com.example.mobilerepairshopv2.ui.adapter.OrderAdapter
import com.example.mobilerepairshopv2.ui.adapter.RepairAdapter
import com.example.mobilerepairshopv2.ui.viewmodel.RepairViewModel
import com.example.mobilerepairshopv2.ui.viewmodel.RepairViewModelFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: RepairViewModel by viewModels {
        RepairViewModelFactory((application as RepairShopApplication).repository)
    }
    private lateinit var repairAdapter: RepairAdapter
    private lateinit var orderAdapter: OrderAdapter
    private var currentRepairListObserver: LiveData<List<Repair>>? = null
    private var currentOrderListObserver: LiveData<List<Order>>? = null

    // This launcher is ONLY for restoring from a backup.
    private val openRestoreFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            readBackupFromFile(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerViews()
        setupClickListeners()
        observeData()
        updateDashboardForPeriod("Last 7 Days")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menuInflater.inflate(R.menu.search_menu, menu)

        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        setupSearch(searchView)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_backup -> {
                createAndShareBackup()
                true
            }
            R.id.action_restore -> {
                launchRestore()
                true
            }
            R.id.action_refresh -> {
                val currentPeriod = binding.buttonDateFilter.text.toString()
                updateDashboardForPeriod(currentPeriod.takeIf { !it.contains(" to ") } ?: "Last 7 Days")
                Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show()
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // --- NEW, ROBUST BACKUP FUNCTION USING SHARE INTENT ---
    private fun createAndShareBackup() {
        lifecycleScope.launch {
            // In a future version, you would combine both lists into a single backup object
            val repairs = viewModel.getRepairsForBackup()
            val gson = Gson()
            val jsonString = gson.toJson(repairs) // For now, we only back up repairs

            try {
                // 1. Save the file to a temporary, secure location inside the app's cache
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val fileName = "repair_shop_backup_${sdf.format(Date())}.json"
                val cachePath = File(cacheDir, "backups")
                cachePath.mkdirs() // Create the backups directory if it doesn't exist
                val file = File(cachePath, fileName)
                FileOutputStream(file).use {
                    it.write(jsonString.toByteArray())
                }

                // 2. Get a secure URI for that file using our FileProvider
                val contentUri = FileProvider.getUriForFile(this@MainActivity, "${packageName}.provider", file)

                if (contentUri != null) {
                    // 3. Create a "Share" intent
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setDataAndType(contentUri, contentResolver.getType(contentUri))
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                    }
                    // 4. Launch the Android Share dialog, allowing the user to choose where to save
                    startActivity(Intent.createChooser(shareIntent, "Save Backup To..."))
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to create backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun launchRestore() {
        openRestoreFileLauncher.launch(arrayOf("application/json"))
    }

    private fun readBackupFromFile(uri: Uri) {
        try {
            val jsonString = contentResolver.openInputStream(uri)?.use {
                BufferedReader(InputStreamReader(it)).readText()
            }
            if (jsonString != null) {
                val type = object : TypeToken<List<Repair>>() {}.type
                val repairs: List<Repair> = Gson().fromJson(jsonString, type)
                showRestoreConfirmationDialog(repairs)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to read or parse backup file.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showRestoreConfirmationDialog(repairs: List<Repair>) {
        AlertDialog.Builder(this)
            .setTitle("Restore Backup")
            .setMessage("This will overwrite all current data. Are you sure you want to proceed?")
            .setPositiveButton("Restore") { _, _ ->
                viewModel.restoreBackup(repairs)
                Toast.makeText(this, "Restore successful!", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupRecyclerViews() {
        repairAdapter = RepairAdapter { repair ->
            val intent = Intent(this, RepairDetailActivity::class.java).apply {
                putExtra(RepairDetailActivity.REPAIR_ID, repair.id)
            }
            startActivity(intent)
        }
        binding.recyclerViewRepairs.adapter = repairAdapter
        binding.recyclerViewRepairs.layoutManager = LinearLayoutManager(this)

        orderAdapter = OrderAdapter { order ->
            val intent = Intent(this, OrderDetailActivity::class.java).apply {
                putExtra(OrderDetailActivity.ORDER_ID, order.id)
            }
            startActivity(intent)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddRepair.setOnClickListener {
            val intent = Intent(this, AddRepairActivity::class.java)
            startActivity(intent)
        }

        binding.buttonDateFilter.setOnClickListener { view ->
            showDateFilterMenu(view)
        }
    }

    private fun setupSearch(searchView: SearchView?) {
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    observeRepairList(viewModel.allRepairs)
                } else {
                    observeRepairList(viewModel.searchRepairs(newText))
                }
                return true
            }
        })
    }

    private fun observeData() {
        observeRepairList(viewModel.allRepairs)
        // Note: pendingCount is now part of the main stats query
    }

    private fun observeRepairList(repairsLiveData: LiveData<List<Repair>>) {
        currentRepairListObserver?.removeObservers(this)
        currentRepairListObserver = repairsLiveData
        currentRepairListObserver?.observe(this) { repairs ->
            repairs?.let { repairAdapter.submitList(it) }
        }
    }

    private fun showDateFilterMenu(anchorView: android.view.View) {
        val popupMenu = PopupMenu(this, anchorView)
        popupMenu.menuInflater.inflate(R.menu.date_filter_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            val period = menuItem.title.toString()
            if (period == "Custom Range...") {
                showCustomDateRangePicker()
            } else {
                binding.buttonDateFilter.text = period
                updateDashboardForPeriod(period)
            }
            true
        }
        popupMenu.show()
    }

    private fun showCustomDateRangePicker() {
        val calendar = Calendar.getInstance()
        var startDate: Long = 0
        val startDatePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val startCalendar = Calendar.getInstance().apply { set(year, month, dayOfMonth, 0, 0, 0) }
            startDate = startCalendar.timeInMillis
            val endDatePicker = DatePickerDialog(this, { _, endYear, endMonth, endDayOfMonth ->
                val endCalendar = Calendar.getInstance().apply { set(endYear, endMonth, endDayOfMonth, 23, 59, 59) }
                val endDate = endCalendar.timeInMillis
                if (endDate < startDate) {
                    Toast.makeText(this, "End date cannot be before start date.", Toast.LENGTH_SHORT).show()
                    return@DatePickerDialog
                }
                val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                binding.buttonDateFilter.text = "${sdf.format(Date(startDate))} to ${sdf.format(Date(endDate))}"
                observeStats(startDate, endDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            endDatePicker.datePicker.minDate = startDate
            endDatePicker.setTitle("Select End Date")
            endDatePicker.show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        startDatePicker.setTitle("Select Start Date")
        startDatePicker.show()
    }

    private fun updateDashboardForPeriod(period: String) {
        val calendar = Calendar.getInstance()
        var endDate = calendar.timeInMillis
        when (period) {
            "Today" -> calendar.set(Calendar.HOUR_OF_DAY, 0)
            "Yesterday" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                val endOfYesterday = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
                }
                endDate = endOfYesterday.timeInMillis
            }
            "Last 7 Days" -> calendar.add(Calendar.DAY_OF_YEAR, -7)
            "Last 30 Days" -> calendar.add(Calendar.DAY_OF_YEAR, -30)
            "Last 5 Years" -> calendar.add(Calendar.YEAR, -5)
        }
        val startDate = calendar.timeInMillis
        observeStats(startDate, endDate)
    }

    private fun observeStats(startDate: Long, endDate: Long) {
        viewModel.getStatsForPeriod(startDate, endDate).observe(this) { stats ->
            binding.statInCount.text = stats?.inCount?.toString() ?: "0"
            binding.statOutCount.text = stats?.outCount?.toString() ?: "0"
            binding.statPendingCount.text = stats?.pendingCount?.toString() ?: "0"

            val estimatedRevenue = stats?.estimatedRevenue ?: 0.0
            binding.statEstimatedRevenue.text = "₹${"%.2f".format(estimatedRevenue)}"

            val advanceFromPending = stats?.advanceFromPending ?: 0.0
            val revenueFromOut = stats?.revenueFromOut ?: 0.0
            val actualRevenue = advanceFromPending + revenueFromOut
            binding.statActualRevenue.text = "₹${"%.2f".format(actualRevenue)}"

            val upcomingRevenue = stats?.upcomingRevenue ?: 0.0
            binding.statUpcomingRevenue.text = "₹${"%.2f".format(upcomingRevenue)}"
        }
    }
}
