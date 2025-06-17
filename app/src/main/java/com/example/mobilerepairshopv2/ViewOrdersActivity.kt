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
        observeData()
        setupClickListeners()

        updateDashboardForPeriod("All Time")
    }

    // MODIFIED: This now inflates BOTH menus
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menuInflater.inflate(R.menu.search_menu, menu)

        // Hide backup/restore which don't apply to orders
        menu?.findItem(R.id.action_backup)?.isVisible = false
        menu?.findItem(R.id.action_restore)?.isVisible = false

        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        setupSearch(searchView)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                val currentPeriod = binding.buttonDateFilterOrders.text.toString()
                updateDashboardForPeriod(currentPeriod.takeIf { !it.contains(" to ") } ?: "All Time")
                Toast.makeText(this, "Orders dashboard refreshed", Toast.LENGTH_SHORT).show()
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupClickListeners() {
        binding.buttonDateFilterOrders.setOnClickListener { view ->
            showDateFilterMenu(view)
        }
        binding.fabAddOrder.setOnClickListener {
            val intent = Intent(this, AddOrderActivity::class.java)
            startActivity(intent)
        }
    }

    // NEW: The search logic is now attached to the toolbar menu item
    private fun setupSearch(searchView: SearchView?) {
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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

    // ... (All other functions remain the same) ...
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
            else -> { // "All Time"
                startDate = 0
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

    private fun showDateFilterMenu(anchorView: android.view.View) {
        val popupMenu = PopupMenu(this, anchorView)
        popupMenu.menuInflater.inflate(R.menu.date_filter_menu, popupMenu.menu)
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
        // This function remains the same
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
