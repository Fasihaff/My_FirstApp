
package protect.budgetwatch

import android.app.Activity
import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import java.io.File

  internal class DatabaseCleanupTask : AsyncTask<Void?, Void?, Void?> {
    private val TAG = "BudgetWatch"
    private val activity: Activity
    private val receiptPurgeCutoff: Long?
    private var progress: ProgressDialog? = null

    constructor(activity: Activity) : super() {
        this.activity = activity
        receiptPurgeCutoff = null
    }

    constructor(activity: Activity, receiptPurgeCutoff: Long) : super() {
        this.activity = activity
        this.receiptPurgeCutoff = receiptPurgeCutoff
    }

    override fun onPreExecute() {
        this.progress = ProgressDialog(activity)
        progress!!.setTitle(R.string.cleaning)
        progress!!.setOnDismissListener { cancel(true) }
        progress!!.show()
    }

    private fun removeOldReceiptsFromTransactions(db: DBHelper) {
        val cursor = db.getTransactionsWithReceipts(receiptPurgeCutoff)
        while (cursor.moveToNext()) {
            val transaction = Transaction.toTransaction(cursor)
            val receipt = File(transaction.receipt)
            val result = receipt.delete()
            if (result == false) {
                Log.i(TAG, "Failed to delete old receipt from transaction: " + transaction.id)
            }
            db.updateTransaction(transaction.id, transaction.type, transaction.description,
                    transaction.account, transaction.budget, transaction.value, transaction.note,
                    transaction.dateMs,  /* no receipt */"")
        }
        cursor.close()
    }

    private fun correctTransactionsWithMissingReceipts(db: DBHelper) {
        val cursor = db.getTransactionsWithReceipts(null)
        while (cursor.moveToNext()) {
            val transaction = Transaction.toTransaction(cursor)
            if (transaction.receipt.isEmpty() == false) {
                val receipt = File(transaction.receipt)
                if (receipt.isFile == false) {
                    // This entry's receipt is missing. Cannot recover
                    // the receipt image, but can update database to remove
                    // the receipt from the transaction
                    db.updateTransaction(transaction.id, transaction.type, transaction.description,
                            transaction.account, transaction.budget, transaction.value, transaction.note,
                            transaction.dateMs,  /* no receipt */"")
                    Log.i(TAG, "Transaction " + transaction.id + " listed a receipt but it is missing, " +
                            "removing receipt")
                }
            }
        }
        cursor.close()
    }

    private fun deleteOrphanedReceipts(db: DBHelper) {
        val imageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (imageDir == null || !imageDir.exists()) {
            // There are no images to cleanup
            return
        }
        val cursor = db.getTransactionsWithReceipts(null)
        var files = imageDir.listFiles()
        if (files == null) {
            // If the directory could not be queried, fill in with
            // an empty list so nothing is processed.
            files = arrayOfNulls(0)
        }
        for (receipt in files) {
            // Search for this receipt attached to a transaction
            var found = false
            cursor.moveToPosition(-1)
            while (cursor.moveToNext()) {
                val transaction = Transaction.toTransaction(cursor)
                val transactionReceipt = File(transaction.receipt)
                if (transactionReceipt == receipt) {
                    // Found the receipt used in the database, ok to move on
                    found = true
                    break
                }
            }
            if (found == false) {
                Log.i(TAG, "Deleting orphaned receipt: " + receipt!!.absolutePath)
                val result = receipt.delete()
                if (result == false) {
                    Log.w(TAG, "Failed to delete orphaned receipt: " + receipt.absolutePath)
                }
            }
        }
        cursor.close()
    }
    override fun doInBackground(vararg params: Void?): Void? {
        val db = DBHelper(activity)
        if (receiptPurgeCutoff != null) {
            removeOldReceiptsFromTransactions(db)
        }
        correctTransactionsWithMissingReceipts(db)
        deleteOrphanedReceipts(db)
        db.close()
        return null
    }


    override fun onPostExecute(result: Void?) {
        progress!!.dismiss()
        Log.i(TAG, "Cleanup Complete")
    }

    override fun onCancelled() {
        progress!!.dismiss()
        Log.i(TAG, "Cleanup Cancelled")
    }


}
