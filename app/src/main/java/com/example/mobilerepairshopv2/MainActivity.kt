package com.example.mobilerepairshopv2

import android.app.Activity
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
import com.example.mobilerepairshopv2.data.model.Repair
import com.example.mobilerepairshopv2.databinding.ActivityMainBinding
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
    private val repairViewModel: RepairViewModel by viewModels {
        RepairViewModelFactory((application as RepairShopApplication).repository)
    }
    private lateinit var adapter: RepairAdapter
    private var currentRepairListObserver: LiveData<List<Repair>>? = null

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
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
                    Toast.makeText(this, "Failed to read backup file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupClickListeners()
        setupSearch()
        observeData()
        updateDashboardForPeriod("Last 7 Days")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_backup -> {
                createAndShareBackup()
                true
            }
            R.id.action_restore -> {
                openRestoreFile()
                true
            }
            R.id.action_refresh -> {
                val currentPeriod = binding.buttonDateFilter.text.toString()
                updateDashboardForPeriod(currentPeriod.takeIf { !it.contains(" to ") } ?: "Last 7 Days")
                Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createAndShareBackup() {
        lifecycleScope.launch {
            val repairs = repairViewModel.getRepairsForBackup()
            val gson = Gson()
            val jsonString = gson.toJson(repairs)

            try {
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val fileName = "repair_backup_${sdf.format(Date())}.json"
                val cachePath = File(cacheDir, "backups")
                cachePath.mkdirs()
                val file = File(cachePath, fileName)
                FileOutputStream(file).use {
                    it.write(jsonString.toByteArray())
                }

                val contentUri = FileProvider.getUriForFile(this@MainActivity, "${packageName}.provider", file)

                if (contentUri != null) {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setDataAndType(contentUri, contentResolver.getType(contentUri))
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Save Backup To..."))
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to create backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openRestoreFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        openFileLauncher.launch(intent)
    }

    private fun showRestoreConfirmationDialog(repairs: List<Repair>) {
        AlertDialog.Builder(this)
            .setTitle("Restore Backup")
            .setMessage("This will overwrite all current data. Are you sure you want to proceed?")
            .setPositiveButton("Restore") { _, _ ->
                repairViewModel.restoreBackup(repairs)
                Toast.makeText(this, "Restore successful!", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ... All other functions for RecyclerView, Search, Stats, etc. remain the same ...
    private fun setupRecyclerView() {
        adapter = RepairAdapter { repair ->
            val intent = Intent(this, RepairDetailActivity::class.java).apply {
                putExtra(RepairDetailActivity.REPAIR_ID, repair.id)
            }
            startActivity(intent)
        }
        binding.recyclerViewRepairs.adapter = adapter
        binding.recyclerViewRepairs.layoutManager = LinearLayoutManager(this)
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

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    observeRepairList(repairViewModel.allRepairs)
                } else {
                    observeRepairList(repairViewModel.searchRepairs(newText))
                }
                return true
            }
        })
    }

    private fun observeData() {
        observeRepairList(repairViewModel.allRepairs)
        repairViewModel.pendingCount.observe(this) { count ->
            binding.statPendingCount.text = count?.toString() ?: "0"
        }
    }

    private fun observeRepairList(repairsLiveData: LiveData<List<Repair>>) {
        currentRepairListObserver?.removeObservers(this)
        currentRepairListObserver = repairsLiveData
        currentRepairListObserver?.observe(this) { repairs ->
            repairs?.let { adapter.submitList(it) }
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
                val formattedStartDate = sdf.format(Date(startDate))
                val formattedEndDate = sdf.format(Date(endDate))
                binding.buttonDateFilter.text = "$formattedStartDate to $formattedEndDate"
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
        repairViewModel.getStatsForPeriod(startDate, endDate).observe(this) { stats ->
            binding.statInCount.text = stats?.inCount?.toString() ?: "0"
            binding.statOutCount.text = stats?.outCount?.toString() ?: "0"

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
    