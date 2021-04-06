package protect.budgetwatch

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

internal class TransactionCursorAdapter(context: Context?, cursor: Cursor?) : CursorAdapter(context, cursor, 0) {
    private val DATE_FORMATTER = SimpleDateFormat.getDateInstance()

    internal class ViewHolder {
        var nameField: TextView? = null
        var valueField: TextView? = null
        var dateField: TextView? = null
        var budgetField: TextView? = null
        var receiptIcon: ImageView? = null
        var note: TextView? = null
        var noteLayout: View? = null
    }

    // The newView method is used to inflate a new view and return it,
    // you don't bind any data to the view at this point.
    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val view = LayoutInflater.from(context).inflate(R.layout.transaction_layout, parent, false)
        val holder = ViewHolder()
        holder.nameField = view.findViewById<View>(R.id.name) as TextView
        holder.valueField = view.findViewById<View>(R.id.value) as TextView
        holder.dateField = view.findViewById<View>(R.id.date) as TextView
        holder.budgetField = view.findViewById<View>(R.id.budget) as TextView
        holder.receiptIcon = view.findViewById<View>(R.id.receiptIcon) as ImageView
        holder.note = view.findViewById<View>(R.id.note) as TextView
        holder.noteLayout = view.findViewById(R.id.noteLayout)
        view.tag = holder
        return view
    }

    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val holder = view.tag as ViewHolder

        // Extract properties from cursor
        val transaction = Transaction.toTransaction(cursor)

        // Populate fields with extracted properties
        holder.nameField!!.text = transaction.description
        holder.valueField!!.text = String.format(Locale.US, "%.2f", transaction.value)
        holder.budgetField!!.text = transaction.budget
        holder.dateField!!.text = DATE_FORMATTER.format(transaction.dateMs)
        if (transaction.receipt.isEmpty()) {
            holder.receiptIcon!!.visibility = View.GONE
        } else {
            holder.receiptIcon!!.visibility = View.VISIBLE
        }
        if (transaction.note.isEmpty()) {
            holder.noteLayout!!.visibility = View.GONE
            holder.note!!.text = ""
        } else {
            holder.noteLayout!!.visibility = View.VISIBLE
            holder.note!!.text = transaction.note
        }
    }
}