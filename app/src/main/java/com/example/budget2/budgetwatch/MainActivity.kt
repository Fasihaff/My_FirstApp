package protect.budgetwatch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import protect.budgetwatch.ImportExportActivity
import protect.budgetwatch.intro.IntroActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAG = "BudgetWatch"
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val menuItems: MutableList<MainMenuItem> = LinkedList()
        menuItems.add(MainMenuItem(R.drawable.purse, R.string.budgetsTitle,
                R.string.budgetDescription))
        menuItems.add(MainMenuItem(R.drawable.transaction, R.string.transactionsTitle,
                R.string.transactionsDescription))
        val buttonList = findViewById<View>(R.id.list) as ListView
        val buttonListAdapter = MenuAdapter(this, menuItems)
        buttonList.adapter = buttonListAdapter
        buttonList.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val item = parent.getItemAtPosition(position) as MainMenuItem
            if (item == null) {
                Log.w(TAG, "Clicked menu item at position $position is null")
                return@OnItemClickListener
            }
            var goalClass: Class<*>? = null
            when (item.menuTextId) {
                R.string.budgetsTitle -> goalClass = BudgetActivity::class.java
                R.string.transactionsTitle -> goalClass = TransactionActivity::class.java
                else -> Log.w(TAG, "Unexpected menu text id: " + item.menuTextId)
            }
            if (goalClass != null) {
                val i = Intent(applicationContext, goalClass)
                startActivity(i)
            }
        }
        val prefs = getSharedPreferences("protect.budgetwatch", Context.MODE_PRIVATE)
        if (prefs.getBoolean("firstrun", true)) {
            startIntro()
            prefs.edit().putBoolean("firstrun", false).commit()
        }
    }

    internal class MainMenuItem(val iconId: Int, val menuTextId: Int, val menuDescId: Int)

    internal class MenuAdapter(context: Context?, items: List<MainMenuItem>?) : ArrayAdapter<MainMenuItem?>(
        context!!, 0, items!!
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // Get the data item for this position
            var convertView = convertView
            val item = getItem(position)

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.main_button,
                        parent, false)
            }
            val menuText = convertView!!.findViewById<View>(R.id.menu) as TextView
            val menuDescText = convertView!!.findViewById<View>(R.id.menudesc) as TextView
            val icon = convertView!!.findViewById<View>(R.id.image) as ImageView
            menuText.setText(item!!.menuTextId)
            menuDescText.setText(item.menuDescId)
            icon.setImageResource(item.iconId)
            return convertView
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_import_export) {
            val i = Intent(applicationContext, ImportExportActivity::class.java)
            startActivity(i)
            return true
        }
        if (id == R.id.action_settings) {
            val i = Intent(applicationContext, SettingsActivity::class.java)
            startActivity(i)
            return true
        }
        if (id == R.id.action_intro) {
            startIntro()
            return true
        }
        return super.onOptionsItemSelected(item)
    }



    private fun startIntro() {
        val intent = Intent(this, IntroActivity::class.java)
        startActivity(intent)
    }

}