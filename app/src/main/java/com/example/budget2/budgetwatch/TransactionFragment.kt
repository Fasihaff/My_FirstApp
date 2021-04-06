package protect.budgetwatch

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import protect.budgetwatch.DBHelper
import protect.budgetwatch.Transaction.Companion.toTransaction
import protect.budgetwatch.TransactionViewActivity

class TransactionFragment : Fragment() {
    private var _transactionType = 0
    private var _db: DBHelper? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val arguments = arguments
        check(!(arguments == null || arguments.getInt("type", -1) == -1)) { "Required argument 'type' is missing" }
        _transactionType = arguments.getInt("type")
        _db = context?.let { DBHelper(it) }

        // If a budget has been passed then only transactions from that budget
        // will be displayed. Otherwise, all transactions wil be displayed.
        val b = requireActivity().intent.extras
        val budgetToDisplay = b?.getString("budget", null)

        // If a search has been passed in that will further filter what is displayed
        val searchToUse = arguments.getString("search", null)
        val layout = inflater.inflate(R.layout.list_layout, container, false)
        val listView = layout.findViewById<View>(R.id.list) as ListView
        val helpText = layout.findViewById<View>(R.id.helpText) as TextView
        val cursor = _db!!.getTransactions(_transactionType, budgetToDisplay, searchToUse, null, null)
        if (cursor.count > 0) {
            listView.visibility = View.VISIBLE
            helpText.visibility = View.GONE
        } else {
            listView.visibility = View.GONE
            helpText.visibility = View.VISIBLE
            val message: String
            if (searchToUse == null) {
                message = if (budgetToDisplay == null) {
                    val stringId = if (_transactionType ==
                        DBHelper.TransactionDbIds.EXPENSE) R.string.noExpenses
                    else R.string.noRevenues
                    resources.getString(stringId)
                } else {
                    val stringId = if (_transactionType ==
                        DBHelper.TransactionDbIds.EXPENSE) R.string.noExpensesForBudget
                    else R.string.noRevenuesForBudget
                    val base = resources.getString(stringId)
                    String.format(base, budgetToDisplay)
                }
            } else {
                val stringId = if (_transactionType == DBHelper.TransactionDbIds.EXPENSE)
                    R.string.searchEmptyExpenses else R.string.searchEmptyRevenues
                message = resources.getString(stringId)
                resources.getString(stringId)
            }
            helpText.text = message
        }
        val adapter = TransactionCursorAdapter(context, cursor)
        listView.adapter = adapter
        registerForContextMenu(listView)
        listView.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val selected = parent.getItemAtPosition(position) as Cursor
            if (selected == null) {
                Log.w(TAG, "Clicked transaction at position $position is null")
                return@OnItemClickListener
            }
            val transaction = toTransaction(selected)
            val i = Intent(view.context, TransactionViewActivity::class.java)
            val b = Bundle()
            b.putInt("id", transaction.id)
            b.putInt("type", _transactionType)
            b.putBoolean("view", true)
            i.putExtras(b)
            startActivity(i)
        }
        return layout
    }

    override fun onDestroyView() {
        _db!!.close()
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "BudgetWatch"
    }
}