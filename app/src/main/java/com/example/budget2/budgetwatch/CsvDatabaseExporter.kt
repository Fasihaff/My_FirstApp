package protect.budgetwatch

import android.content.Context
import com.google.common.base.Charsets
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.*
import java.text.DateFormat
import java.util.*
import java.util.Locale

/**
 * Class for exporting the database into CSV (Comma Separate Values)
 * format.
 */
class CsvDatabaseExporter : DatabaseExporter {
    @Throws(IOException::class, InterruptedException::class)
    override fun exportData(
        context: Context?, db: DBHelper?, startTimeMs: Long?,
        endTimeMs: Long?, outStream: OutputStream?, updater: ImportExportProgressUpdater?
    ) {

        val stream = OutputStreamWriter(outStream, Charsets.UTF_8)
        val output = BufferedWriter(stream)
        val printer = CSVPrinter(output, CSVFormat.RFC4180)
        var numEntries = 0
        val budgetNames = db!!.budgetNames
        numEntries += budgetNames.size
        val expenseTransactions = db.getTransactions(
            DBHelper.TransactionDbIds.EXPENSE,
            null,
            null,
            startTimeMs,
            endTimeMs
        )
        numEntries += expenseTransactions.count
        val revenueTransactions = db.getTransactions(
            DBHelper.TransactionDbIds.REVENUE,
            null,
            null,
            startTimeMs,
            endTimeMs
        )
        numEntries += revenueTransactions.count
        updater!!.setTotal(numEntries)
        try {
            // Print the header
            printer.printRecord(
                DBHelper.TransactionDbIds.NAME,
                DBHelper.TransactionDbIds.TYPE,
                DBHelper.TransactionDbIds.DESCRIPTION,
                DBHelper.TransactionDbIds.ACCOUNT,
                DBHelper.TransactionDbIds.BUDGET,
                DBHelper.TransactionDbIds.VALUE,
                DBHelper.TransactionDbIds.NOTE,
                DBHelper.TransactionDbIds.DATE,
                DATE_FORMATTED_FIELD,
                DBHelper.TransactionDbIds.RECEIPT
            )
            for (cursor in arrayOf(
                db.getTransactions(
                    DBHelper.TransactionDbIds.EXPENSE,
                    null,
                    null,
                    startTimeMs,
                    endTimeMs
                ),
                db.getTransactions(
                    DBHelper.TransactionDbIds.REVENUE,
                    null,
                    null,
                    startTimeMs,
                    endTimeMs
                )
            )) {
                while (cursor.moveToNext()) {
                    val transaction = Transaction.toTransaction(cursor)
                    var receiptFilename = ""
                    if (transaction.receipt.length > 0) {
                        val receiptFile = File(transaction.receipt)
                        receiptFilename = receiptFile.name
                    }
                    val currentLocale = Locale.getDefault()
                    val dateFormat = DateFormat.getDateTimeInstance(
                        DateFormat.DEFAULT, DateFormat.DEFAULT, currentLocale
                    )
                    val dateFormatted = dateFormat.format(Date(transaction.dateMs))
                    printer.printRecord(
                        transaction.id,
                        if (transaction.type == DBHelper.TransactionDbIds.EXPENSE) "EXPENSE" else "REVENUE",
                        transaction.description,
                        transaction.account,
                        transaction.budget,
                        transaction.value,
                        transaction.note,
                        transaction.dateMs,
                        dateFormatted,
                        receiptFilename
                    )
                    updater.update()
                    if (Thread.currentThread().isInterrupted) {
                        throw InterruptedException()
                    }
                }
                cursor.close()
            }
            for (budgetName in budgetNames) {
                val budget = db.getBudgetStoredOnly(budgetName)
                printer.printRecord(
                    budget!!.name,
                    "BUDGET",
                    "",  // blank description
                    "",  // blank account
                    "",  // blank budget (handled in id field)
                    budget.max,
                    "",  // blank note
                    "",  // blank date
                    "",  // blank formatted date
                    ""
                ) // blank receipt
                updater.update()
                if (Thread.currentThread().isInterrupted) {
                    throw InterruptedException()
                }
            }
        } finally {
            printer.close()
        }
    }
    companion object {
        private const val DATE_FORMATTED_FIELD = "date_formatted"
    }
}