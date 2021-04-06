package protect.budgetwatch

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TextView

internal class
BudgetAdapter(context: Context, items: List<Budget?>?) : ArrayAdapter<Budget?>(context, 0, items!!) {
    private val FRACTION_FORMAT: String

    internal class ViewHolder {
        var budgetName: TextView? = null
        var budgetBar: ProgressBar? = null
        var budgetValue: TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get the data item for this position
        var convertView = convertView
        val item = getItem(position)
        val holder: ViewHolder

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.budget_layout,
                    parent, false)
            holder = ViewHolder()
            holder.budgetName = convertView.findViewById<View>(R.id.budgetName) as TextView
            holder.budgetBar = convertView.findViewById<View>(R.id.budgetBar) as ProgressBar
            holder.budgetValue = convertView.findViewById<View>(R.id.budgetValue) as TextView
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }
        holder.budgetName!!.text = item!!.name
        holder.budgetBar!!.max = item.max
        holder.budgetBar!!.progress = item.current
        val fraction = String.format(FRACTION_FORMAT, item.current, item.max)
        holder.budgetValue!!.text = fraction
        return convertView!!
    }

    init {
        FRACTION_FORMAT = context.resources.getString(R.string.fraction)
    }
}