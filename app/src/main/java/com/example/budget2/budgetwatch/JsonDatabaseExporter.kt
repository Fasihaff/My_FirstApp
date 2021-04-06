package protect.budgetwatch

import android.content.Context
import android.util.JsonWriter
import com.google.common.base.Charsets
import protect.budgetwatch.DBHelper
import java.io.*

/**
 * Class for exporting the database into JSON format.
 */
class JsonDatabaseExporter : DatabaseExporter {
    @Throws(IOException::class, InterruptedException::class)
    override fun exportData(context: Context?, db: DBHelper?, startTimeMs: Long?, endTimeMs: Long?, outStream: OutputStream?, updater: ImportExportProgressUpdater?) {
        val stream = OutputStreamWriter(outStream, Charsets.UTF_8)
        val output = BufferedWriter(stream)
        val writer = JsonWriter(output)
        var numEntries = 0
        val budgetNames = db!!.budgetNames
        numEntries += budgetNames.size
        val expenseTransactions = db.getTransactions(DBHelper.TransactionDbIds.EXPENSE, null, null, startTimeMs, endTimeMs)
        numEntries += expenseTransactions.count
        val revenueTransactions = db.getTransactions(DBHelper.TransactionDbIds.REVENUE, null, null, startTimeMs, endTimeMs)
        numEntries += revenueTransactions.count
        updater!!.setTotal(numEntries)
        try {
            writer.setIndent("   ")
            writer.beginArray()
            for (cursor in arrayOf(
                    db.getTransactions(DBHelper.TransactionDbIds.EXPENSE, null, null, startTimeMs, endTimeMs),
                    db.getTransactions(DBHelper.TransactionDbIds.REVENUE, null, null, startTimeMs, endTimeMs)
            )) {
                while (cursor.moveToNext()) {
                    val transaction = Transaction.toTransaction(cursor)
                    var receiptFilename = ""
                    if (transaction.receipt.length > 0) {
                        val receiptFile = File(transaction.receipt)
                        receiptFilename = receiptFile.name
                    }
                    writer.beginObject()
                    writer.name("ID").value(transaction.id.toLong())
                    writer.name(DBHelper.TransactionDbIds.TYPE).value(
                            if (transaction.type == DBHelper.TransactionDbIds.EXPENSE) "EXPENSE" else "REVENUE")
                    writer.name(DBHelper.TransactionDbIds.DESCRIPTION).value(transaction.description)
                    writer.name(DBHelper.TransactionDbIds.ACCOUNT).value(transaction.account)
                    writer.name(DBHelper.TransactionDbIds.BUDGET).value(transaction.budget)
                    writer.name(DBHelper.TransactionDbIds.VALUE).value(transaction.value)
                    writer.name(DBHelper.TransactionDbIds.NOTE).value(transaction.note)
                    writer.name(DBHelper.TransactionDbIds.DATE).value(transaction.dateMs)
                    writer.name(DBHelper.TransactionDbIds.RECEIPT).value(receiptFilename)
                    writer.endObject()
                    updater.update()
                    if (Thread.currentThread().isInterrupted) {
                        throw InterruptedException()
                    }
                }
                cursor.close()
            }
            for (budgetName in budgetNames) {
                val budget = db.getBudgetStoredOnly(budgetName)
                writer.beginObject()
                writer.name(DBHelper.TransactionDbIds.NAME).value(budget!!.name)
                writer.name(DBHelper.TransactionDbIds.TYPE).value("BUDGET")
                writer.name(DBHelper.TransactionDbIds.VALUE).value(budget.max.toDouble())
                writer.endObject()
                updater.update()
                if (Thread.currentThread().isInterrupted) {
                    throw InterruptedException()
                }
            }
            writer.endArray()
        } finally {
            writer.close()
        }
    }
}