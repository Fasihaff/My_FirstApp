package protect.budgetwatch

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import protect.budgetwatch.DBHelper
import java.util.*

@Suppress("DEPRECATION")
class ShortcutConfigure : AppCompatActivity() {
    internal class ShortcutOption {
        var name: String? = null
        var intent: Intent? = null
    }

    internal class ShortcutAdapter(context: Context?, items: List<ShortcutOption>?) : ArrayAdapter<ShortcutOption?>(
        context!!, 0, items!!
    ) {
        internal class ViewHolder {
            var name: TextView? = null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // Get the data item for this position
            var convertView = convertView
            val item = getItem(position)
            val holder: ViewHolder

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.shortcut_option_layout,
                        parent, false)
                holder = ViewHolder()
                holder.name = convertView.findViewById<View>(R.id.name) as TextView
                convertView.tag = holder
            } else {
                holder = convertView.tag as ViewHolder
            }
            holder.name!!.text = item!!.name
            return convertView!!
        }
    }

    // Prevent instances of the view activity from piling up; if one exists let this
    // one replace it.
    private val possibleShortcuts: List<ShortcutOption>
        private get() {
            val shortcuts = LinkedList<ShortcutOption>()
            for (transactionType in intArrayOf(DBHelper.TransactionDbIds.EXPENSE, DBHelper.TransactionDbIds.REVENUE)) {
                val shortcutIntent = Intent(this, TransactionViewActivity::class.java)
                shortcutIntent.action = Intent.ACTION_MAIN
                // Prevent instances of the view activity from piling up; if one exists let this
                // one replace it.
                shortcutIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                val bundle = Bundle()
                bundle.putInt("type", transactionType)
                shortcutIntent.putExtras(bundle)
                var title: String?
                title = if (transactionType == DBHelper.TransactionDbIds.EXPENSE) {
                    resources.getString(R.string.addExpenseTransactionShortcutTitle)
                } else {
                    resources.getString(R.string.addRevenueTransactionShortcutTitle)
                }
                val shortcutOption = ShortcutOption()
                shortcutOption.name = title
                shortcutOption.intent = shortcutIntent
                shortcuts.add(shortcutOption)
            }
            return shortcuts
        }

    public override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        // Set the result to CANCELED.  This will cause nothing to happen if the
        // aback button is pressed.
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.main_activity)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        toolbar.visibility = View.GONE
        val shortcutList = findViewById<View>(R.id.list) as ListView
        shortcutList.visibility = View.VISIBLE
        val adapter = ShortcutAdapter(this, possibleShortcuts)
        shortcutList.adapter = adapter
        shortcutList.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val shortcut = parent.getItemAtPosition(position) as ShortcutOption
            if (shortcut == null) {
                Log.w(TAG, "Clicked shortcut at position $position is null")
                return@OnItemClickListener
            }
            val icon: Parcelable = Intent.ShortcutIconResource.fromContext(this@ShortcutConfigure, R.mipmap.ic_launcher)
            val intent = Intent()
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut.intent)
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcut.name)
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    companion object {
        const val TAG = "BudgetWatch"
    }
}