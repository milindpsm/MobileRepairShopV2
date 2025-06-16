package com.example.mobilerepairshopv2

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mobilerepairshopv2.data.model.Repair
import com.example.mobilerepairshopv2.databinding.ActivityMainBinding
import com.example.mobilerepairshopv2.ui.adapter.RepairAdapter
import com.example.mobilerepairshopv2.ui.viewmodel.RepairViewModel
import com.example.mobilerepairshopv2.ui.viewmodel.RepairViewModelFactory
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
    private var currentRepairListObserver: LiveData<List<Repair>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupClickListeners()
        observeData()
        updateDashboardForPeriod("Last 7 Days")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate both the main menu (for refresh/backup) and the search menu
        menuInflater.inflate(R.menu.main_menu, menu)
        menuInflater.inflate(R.menu.search_menu, menu)

        // Setup logic for the search icon in the toolbar
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

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

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // This function for refresh, backup, restore is now separate from search
        return when (item.itemId) {
            R.id.action_refresh -> {
                val currentPeriod = binding.buttonDateFilter.text.toString()
                updateDashboardForPeriod(currentPeriod.takeIf { !it.contains(" to ") } ?: "Last 7 Days")
                Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show()
                true
            }
            // Add backup/restore logic here if/when we re-implement it
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        repairAdapter = RepairAdapter { repair ->
            val intent = Intent(this, RepairDetailActivity::class.java).apply {
                putExtra(RepairDetailActivity.REPAIR_ID, repair.id)
            }
            startActivity(intent)
        }
        binding.recyclerViewRepairs.adapter = repairAdapter
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

        binding.buttonViewOrders.setOnClickListener {
            val intent = Intent(this, ViewOrdersActivity::class.java)
            startActivity(intent)
        }

        binding.buttonAddOrder.setOnClickListener {
            val intent = Intent(this, AddOrderActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeData() {
        observeRepairList(viewModel.allRepairs)
        viewModel.pendingCount.observe(this) { count ->
            binding.statPendingCount.text = count?.toString() ?: "0"
        }
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
        viewModel.getStatsForPeriod(startDate, endDate).observe(this) { stats ->
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
