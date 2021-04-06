package protect.budgetwatch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * This Broadcast Receiver helps one determine if the transactions
 * database has changed since the receiver was created or was
 * last reset.
 */
class TransactionDatabaseChangedReceiver : BroadcastReceiver() {
    private var _hasChanged = false
    override fun onReceive(context: Context, intent: Intent) {
        _hasChanged = true
    }

    fun hasChanged(): Boolean {
        return _hasChanged
    }

    fun reset() {
        _hasChanged = false
    }

    companion object {
        const val ACTION_DATABASE_CHANGED = "protect.budgetwatch.TRANSACTION_DATABASE_CHANGED"
    }
}