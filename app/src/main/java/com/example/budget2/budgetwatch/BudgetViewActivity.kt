package protect.budgetwatch

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle

import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.snackbar.Snackbar

class BudgetViewActivity : AppCompatActivity()
{
    private val TAG = "BudgetWatch"
    private var _db: DBHelper? = null
    private var _budgetNameEdit: EditText? = null
    private var _budgetNameView: TextView? = null
    private var _valueEdit: EditText? = null
    private var _valueView: TextView? = null
    private var _budgetName: String? = null
    private var _updateBudget = false
    private var _viewBudget = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.budget_view_activity)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        _db = DBHelper(this)
        _budgetNameEdit = findViewById<View>(R.id.budgetNameEdit) as EditText
        _budgetNameView = findViewById<View>(R.id.budgetNameView) as TextView
        _valueEdit = findViewById<View>(R.id.valueEdit) as EditText
        _valueView = findViewById<View>(R.id.valueView) as TextView
        val b = intent.extras
        _budgetName = b?.getString("id")
        _updateBudget = b != null && b.getBoolean("update", false)
        _viewBudget = b != null && b.getBoolean("view", false)
    }


    public override fun onResume() {
        super.onResume()
        if (_updateBudget || _viewBudget) {
            (if (_updateBudget) _budgetNameEdit else _budgetNameView)!!.text = _budgetName
            val existingBudget = _db!!.getBudgetStoredOnly(_budgetName!!)
            (if (_updateBudget) _valueEdit else _valueView)!!.text = String.format("%d", existingBudget!!.max)
            if (_updateBudget) {
                setTitle(R.string.editBudgetTitle)
                _budgetNameView!!.visibility = View.GONE
                _valueView!!.visibility = View.GONE
            } else {
                _budgetNameEdit!!.visibility = View.GONE
                _valueEdit!!.visibility = View.GONE
                setTitle(R.string.viewBudgetTitle)
            }
        } else {
            setTitle(R.string.addBudgetTitle)
            _budgetNameView!!.visibility = View.GONE
            _valueView!!.visibility = View.GONE
        }
    }

    private fun doSave() {
        val budgetName = _budgetNameEdit!!.text.toString()
        val valueStr = _valueEdit!!.text.toString()
        val value: Int
        value = try {
            valueStr.toInt()
        } catch (e: NumberFormatException) {
            Int.MIN_VALUE
        }
        if (value < 0) {
            Snackbar.make(_valueEdit!!, R.string.budgetValueMissing, Snackbar.LENGTH_LONG).show()
            return
        }
        if (budgetName.length == 0) {
            Snackbar.make(_valueEdit!!, R.string.budgetTypeMissing, Snackbar.LENGTH_LONG).show()
            return
        }
        if (_updateBudget == false) {
            _db!!.insertBudget(budgetName, value)
        } else {
            _db!!.updateBudget(budgetName, value)
        }
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val b = intent.extras
        val viewBudget = b != null && b.getBoolean("view", false)
        val editBudget = b != null && b.getBoolean("update", false)
        if (viewBudget) {
            menuInflater.inflate(R.menu.view_menu, menu)
        } else if (editBudget) {
            menuInflater.inflate(R.menu.edit_menu, menu)
        } else {
            menuInflater.inflate(R.menu.add_menu, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val b = intent.extras
        val budgetName = b?.getString("id")
        if (id == R.id.action_edit) {
            finish()
            val i = Intent(applicationContext, BudgetViewActivity::class.java)
            val bundle = Bundle()
            bundle.putString("id", budgetName)
            bundle.putBoolean("update", true)
            i.putExtras(bundle)
            startActivity(i)
            return true
        }
        if (id == R.id.action_delete) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.deleteBudgetTitle)
            builder.setMessage(R.string.deleteBudgetConfirmation)
            builder.setPositiveButton(R.string.confirm) { dialog, which ->
                Log.e(TAG, "Deleting budget: $budgetName")
                _db!!.deleteBudget(budgetName!!)
                finish()
                dialog.dismiss()
            }
            builder.setNegativeButton(R.string.cancel) { dialog, which -> dialog.dismiss() }
            val dialog = builder.create()
            dialog.show()
            return true
        }
        if (id == R.id.action_save) {
            doSave()
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