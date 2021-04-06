@file:Suppress("DEPRECATION")

package protect.budgetwatch

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import protect.budgetwatch.DBHelper

 internal class TransactionPagerAdapter(fm: FragmentManager?, private val search: String?, private val numTabs: Int)
     : FragmentStatePagerAdapter(fm!!)
 {
    override fun getItem(position: Int): Fragment {
        val fragment: Fragment = TransactionFragment()
        val arguments = Bundle()
        val transactionType = if (position == 0) DBHelper.TransactionDbIds.EXPENSE else DBHelper.TransactionDbIds.REVENUE
        arguments.putInt("type", transactionType)
        if (search != null) {
            arguments.putString("search", search)
        }
        fragment.arguments = arguments
        return fragment
    }

    override fun getCount(): Int {
        return numTabs
    }

}