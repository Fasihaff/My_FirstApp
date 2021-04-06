package protect.budgetwatch

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import protect.budgetwatch.BudgetActivity
import java.text.DateFormat
import java.util.*



class BudgetActivity : AppCompatActivity() {
    private var _db: DBHelper? = null
    private val TAG = "BudgetWatch"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.budget_activity)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        _db = DBHelper(this)
    }


    override fun onResume() {
        super.onResume()
        val budgetList = findViewById<View>(R.id.list) as ListView
        val helpText = findViewById<View>(R.id.helpText) as TextView
        if (_db!!.budgetCount > 0) {
            budgetList.visibility = View.VISIBLE
            helpText.visibility = View.GONE
        } else {
            budgetList.visibility = View.GONE
            helpText.visibility = View.VISIBLE
            helpText.setText("no budgets")
        }
        val date = Calendar.getInstance()

        // Set to the last ms at the end of the month
        val dateMonthEndMs = CalendarUtil.getEndOfMonthMs(date[Calendar.YEAR],
                date[Calendar.MONTH])

        // Set to beginning of the month
        val dateMonthStartMs = CalendarUtil.getStartOfMonthMs(date[Calendar.YEAR],
                date[Calendar.MONTH])
        val b = intent.extras
        val budgetStartMs = b?.getLong("budgetStart", dateMonthStartMs) ?: dateMonthStartMs
        val budgetEndMs = b?.getLong("budgetEnd", dateMonthEndMs) ?: dateMonthEndMs
        date.timeInMillis = budgetStartMs
        val budgetStartString = DateFormat.getDateInstance(DateFormat.SHORT).format(date.time)
        date.timeInMillis = budgetEndMs
        val budgetEndString = DateFormat.getDateInstance(DateFormat.SHORT).format(date.time)
        val dateRangeFormat = resources.getString(R.string.dateRangeFormat)
        val dateRangeString = String.format(dateRangeFormat, budgetStartString, budgetEndString)
        val dateRangeField = findViewById<View>(R.id.dateRange) as TextView
        dateRangeField.text = dateRangeString
        val budgets = _db!!.getBudgets(budgetStartMs, budgetEndMs)
        val budgetListAdapter = BudgetAdapter(this, budgets)
        budgetList.adapter = budgetListAdapter
        registerForContextMenu(budgetList)
        budgetList.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val budget = parent.getItemAtPosition(position) as Budget
            if (budget == null) {
                Log.w(TAG, "Clicked budget at position $position is null")
                return@OnItemClickListener
            }
            val i = Intent(applicationContext, TransactionActivity::class.java)
            val bundle = Bundle()
            bundle.putString("budget", budget.name)
            i.putExtras(bundle)
            startActivity(i)
        }
        val blankBudget = _db!!.getBlankBudget(budgetStartMs, budgetEndMs)
        setupTotalEntry(budgets, blankBudget)
    }

    private fun setupTotalEntry(budgets: List<Budget>, blankBudget: Budget) {
        val budgetName = findViewById<View>(R.id.budgetName) as TextView
        val budgetValue = findViewById<View>(R.id.budgetValue) as TextView
        val budgetBar = findViewById<View>(R.id.budgetBar) as ProgressBar
        budgetName.setText(R.string.totalBudgetTitle)
        var max = 0
        var current = 0
        for (budget in budgets) {
            max += budget.max
            current += budget.current
        }
        current += blankBudget.current
        budgetBar.max = max
        budgetBar.progress = current
        val fraction = String.format(resources.getString(R.string.fraction), current, max)
        budgetValue.text = fraction
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (v.id == R.id.list) {
            val inflater = menuInflater
            inflater.inflate(R.menu.view_menu, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterContextMenuInfo
        val listView = findViewById<View>(R.id.list) as ListView
        if (info != null) {
            val budget = listView.getItemAtPosition(info.position) as Budget
            if (budget != null && item.itemId == R.id.action_edit) {
                val i = Intent(applicationContext, BudgetViewActivity::class.java)
                val bundle = Bundle()
                bundle.putString("id", budget.name)
                bundle.putBoolean("view", true)
                i.putExtras(bundle)
                startActivity(i)
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.budget_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_add) {
            val i = Intent(applicationContext, BudgetViewActivity::class.java)
            startActivity(i)
            return true
        }
        if (id == R.id.action_calendar) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.budgetDateRangeHelp)
            val view = layoutInflater.inflate(R.layout.budget_date_picker_layout, null, false)
            builder.setView(view)
            builder.setNegativeButton(R.string.cancel) { dialog, which -> dialog.cancel() }
            builder.setPositiveButton(R.string.set, DialogInterface.OnClickListener { dialog, which ->
                val startDatePicker = view.findViewById<View>(R.id.startDate) as DatePicker
                val endDatePicker = view.findViewById<View>(R.id.endDate) as DatePicker
                val startOfBudgetMs = CalendarUtil.getStartOfDayMs(startDatePicker.year,
                        startDatePicker.month, startDatePicker.dayOfMonth)
                val endOfBudgetMs = CalendarUtil.getEndOfDayMs(endDatePicker.year,
                        endDatePicker.month, endDatePicker.dayOfMonth)
                if (startOfBudgetMs > endOfBudgetMs) {
                    Toast.makeText(this@BudgetActivity, R.string.startDateAfterEndDate, Toast.LENGTH_LONG).show()
                    return@OnClickListener
                }
                val intent = Intent(this@BudgetActivity, BudgetActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
                val bundle = Bundle()
                bundle.putLong("budgetStart", startOfBudgetMs)
                bundle.putLong("budgetEnd", endOfBudgetMs)
                intent.putExtras(bundle)
                startActivity(intent)
                finish()
            })
            builder.show()
            return true
        }
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        _db!!.close()
        super.onDestroy()
    }
}