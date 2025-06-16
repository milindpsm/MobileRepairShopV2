package com.example.mobilerepairshopv2

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mobilerepairshopv2.data.model.Order
import com.example.mobilerepairshopv2.databinding.ActivityViewOrdersBinding
import com.example.mobilerepairshopv2.ui.adapter.OrderAdapter
import com.example.mobilerepairshopv2.ui.viewmodel.RepairViewModel
import com.example.mobilerepairshopv2.ui.viewmodel.RepairViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ViewOrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewOrdersBinding
    private val viewModel: RepairViewModel by viewModels {
        RepairViewModelFactory((application as RepairShopApplication).repository)
    }
    private lateinit var adapter: OrderAdapter
    private var currentOrderListObserver: LiveData<List<Order>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        setupSearch()
        observeData()
        setupClickListeners()

        // Set initial filter to show all-time stats
        updateDashboardForPeriod("All Time")
    }

    private fun setupClickListeners() {
        binding.buttonDateFilterOrders.setOnClickListener { view ->
            showDateFilterMenu(view)
        }
    }

    private fun showDateFilterMenu(anchorView: android.view.View) {
        val popupMenu = PopupMenu(this, anchorView)
        // Use the same menu file as the main dashboard
        popupMenu.menuInflater.inflate(R.menu.date_filter_menu, popupMenu.menu)
        // Add "All Time" option specifically for this screen
        popupMenu.menu.add("All Time")

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val period = menuItem.title.toString()
            if (period == "Custom Range...") {
                showCustomDateRangePicker()
            } else {
                binding.buttonDateFilterOrders.text = period
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
                binding.buttonDateFilterOrders.text = "${sdf.format(Date(startDate))} to ${sdf.format(Date(endDate))}"
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
        var startDate: Long

        when (period) {
            "Today" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
                startDate = calendar.timeInMillis
            }
            "Yesterday" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
                startDate = calendar.timeInMillis
                val endOfYesterday = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
                }
                endDate = endOfYesterday.timeInMillis
            }
            "Last 7 Days" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                startDate = calendar.timeInMillis
            }
            "Last 30 Days" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                startDate = calendar.timeInMillis
            }
            "Last 1 Year" -> {
                calendar.add(Calendar.YEAR, -1)
                startDate = calendar.timeInMillis
            }
            "All Time" -> {
                // Use a very early start date to capture all records
                startDate = 0
            }
            else -> { // Handles custom range which is already set
                return
            }
        }
        observeStats(startDate, endDate)
    }

    private fun observeStats(startDate: Long, endDate: Long) {
        viewModel.getOrderStatsForPeriod(startDate, endDate).observe(this) { stats ->
            binding.statOrderInCount.text = stats?.inCount?.toString() ?: "0"
            binding.statOrderOutCount.text = stats?.outCount?.toString() ?: "0"
            binding.statOrderPendingCount.text = stats?.pendingCount?.toString() ?: "0"
        }
    }

    private fun setupRecyclerView() {
        adapter = OrderAdapter { order ->
            val intent = Intent(this, OrderDetailActivity::class.java).apply {
                putExtra(OrderDetailActivity.ORDER_ID, order.id)
            }
            startActivity(intent)
        }
        binding.recyclerViewOrders.adapter = adapter
        binding.recyclerViewOrders.layoutManager = LinearLayoutManager(this)
    }

    private fun setupSearch() {
        binding.searchViewOrders.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    observeOrderList(viewModel.allOrders)
                } else {
                    observeOrderList(viewModel.searchOrders(newText))
                }
                return true
            }
        })
    }

    private fun observeData() {
        observeOrderList(viewModel.allOrders)
    }

    private fun observeOrderList(ordersLiveData: LiveData<List<Order>>) {
        currentOrderListObserver?.removeObservers(this)
        currentOrderListObserver = ordersLiveData
        currentOrderListObserver?.observe(this) { orders ->
            orders?.let { adapter.submitList(it) }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
