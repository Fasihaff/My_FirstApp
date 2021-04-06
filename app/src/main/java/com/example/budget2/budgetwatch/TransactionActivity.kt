package protect.budgetwatch


import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.DatePicker
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.TabLayoutOnPageChangeListener
import protect.budgetwatch.CalendarUtil.getEndOfDayMs
import protect.budgetwatch.DBHelper

class TransactionActivity : AppCompatActivity() {
    private var _dbChanged: TransactionDatabaseChangedReceiver? = null
    private var _currentlySearching = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.transaction_activity)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        _dbChanged = TransactionDatabaseChangedReceiver()
        this.registerReceiver(_dbChanged, IntentFilter(TransactionDatabaseChangedReceiver.ACTION_DATABASE_CHANGED))
        val search = intent.getStringExtra(SearchManager.QUERY)
        resetView(search)
    }

    private fun resetView(search: String?) {
        val tabLayout = findViewById<View>(R.id.tabLayout) as TabLayout
        tabLayout.removeAllTabs()
        tabLayout.addTab(tabLayout.newTab().setText(R.string.expensesTitle))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.revenuesTitle))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL
        val viewPager = findViewById<View>(R.id.pager) as ViewPager
        val adapter: PagerAdapter = TransactionPagerAdapter(supportFragmentManager, search, tabLayout.tabCount)
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    public override fun onResume() {
        super.onResume()
        if (_dbChanged!!.hasChanged() || Intent.ACTION_SEARCH == intent.action) {
            var search: String? = null

            // Only use the search if the search view is open. When it is canceled
            // ignore the search.
            if (_currentlySearching) {
                search = intent.getStringExtra(SearchManager.QUERY)
            }
            resetView(search)
            _dbChanged!!.reset()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.transaction_menu, menu)

        // Associate searchable configuration with the SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setOnCloseListener {
            _currentlySearching = false

            // Re-populate the transactions
            onResume()

            // false: allow the default cleanup behavior on the search view on closing.
            false
        }
        searchView.setOnSearchClickListener { _currentlySearching = true }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_add) {
            val i = Intent(applicationContext, TransactionViewActivity::class.java)
            val b = Bundle()
            b.putInt("type", currentTabType)
            i.putExtras(b)
            startActivity(i)
            return true
        }
        if (id == R.id.action_purge_receipts) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.cleanupHelp)
            val view = layoutInflater.inflate(R.layout.cleanup_layout, null, false)
            builder.setView(view)
            builder.setNegativeButton(R.string.cancel) { dialog, which -> dialog.cancel() }
            builder.setPositiveButton(R.string.clean) { dialog, which ->
                val endDatePicker = view.findViewById<View>(R.id.endDate) as DatePicker
                val endOfBudgetMs = getEndOfDayMs(endDatePicker.year,
                        endDatePicker.month, endDatePicker.dayOfMonth)
              //  val task = DatabaseCleanupTask(this , endOfBudgetMs)
                //task.execute()
            }
            builder.show()
            return true
        }
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private val currentTabType: Int
       //private
    get() {
            val tabLayout = findViewById<View>(R.id.tabLayout) as TabLayout
            return if (tabLayout.selectedTabPosition == 0) {
                DBHelper.TransactionDbIds.EXPENSE
            } else {
                DBHelper.TransactionDbIds.REVENUE
            }
        }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            Log.d(TAG, "Received search: $query")
            setIntent(intent)
            // onResume() will be called right after this, so the search will be used
        }
    }

    public override fun onDestroy() {
        unregisterReceiver(_dbChanged)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BudgetWatch"
    }
}