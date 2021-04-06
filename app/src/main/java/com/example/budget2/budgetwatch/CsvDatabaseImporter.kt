package protect.budgetwatch

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import com.google.common.base.Charsets
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Class for importing a database from CSV (Comma Separate Values)
 * formatted data.
 *
 * The database's transactions and budgets tables are both expected to
 * appear in the CSV data, with the transactions first. A header is expected
 * for each table showing the names of the columns. A newline separates
 * the transactions and budgets databases.
 */
class CsvDatabaseImporter : DatabaseImporter {
    @Throws(IOException::class, FormatException::class, InterruptedException::class)
    override fun importData(context: Context?, db: DBHelper?, input: InputStream?, updater: ImportExportProgressUpdater?) {
        val reader = InputStreamReader(input, Charsets.UTF_8)
        val parser = CSVParser(reader, CSVFormat.RFC4180.withHeader())
        val database = db!!.writableDatabase
        database.beginTransaction()
        try {
            for (record in parser) {
                val type = record[DBHelper.TransactionDbIds.TYPE]
                if (type == "BUDGET") {
                    importBudget(database, db, record)
                } else {
                    importTransaction(context!!, database, db, record)
                }
                updater!!.update()
                if (Thread.currentThread().isInterrupted) {
                    throw InterruptedException()
                }
            }

            // Do not close the parser, as it will close the input stream;
            // Closing the input stream is the responsibility of the caller.
            database.setTransactionSuccessful()
        } catch (e: IllegalArgumentException) {
            throw FormatException("Issue parsing CSV data", e)
        } catch (e: IllegalStateException) {
            throw FormatException("Issue parsing CSV data", e)
        } finally {
            database.endTransaction()
            database.close()
        }
    }

    /**
     * Extract a string from the items array. The index into the array
     * is determined by looking up the index in the fields map using the
     * "key" as the key. If no such key exists, defaultValue is returned
     * if it is not null. Otherwise, a FormatException is thrown.
     */
    @Throws(FormatException::class)
    private fun extractString(key: String, record: CSVRecord, defaultValue: String?): String? {
        var toReturn = defaultValue
        if (record.isMapped(key)) {
            toReturn = record[key]
        } else {
            if (defaultValue == null) {
                throw FormatException("Field not used but expected: $key")
            }
        }
        return toReturn
    }

    /**
     * Extract an integer from the items array. The index into the array
     * is determined by looking up the index in the fields map using the
     * "key" as the key. If no such key exists, or the data is not a valid
     * int, a FormatException is thrown.
     */
    @Throws(FormatException::class)
    private fun extractInt(key: String, record: CSVRecord): Int {
        if (record.isMapped(key) == false) {
            throw FormatException("Field not used but expected: $key")
        }
        return try {
            record[key].toInt()
        } catch (e: NumberFormatException) {
            throw FormatException("Failed to parse field: $key", e)
        }
    }

    /**
     * Extract an double from the items array. The index into the array
     * is determined by looking up the index in the fields map using the
     * "key" as the key. If no such key exists, or the data is not a valid
     * double, a FormatException is thrown.
     */
    @Throws(FormatException::class)
    private fun extractDouble(key: String, record: CSVRecord): Double {
        if (record.isMapped(key) == false) {
            throw FormatException("Field not used but expected: $key")
        }
        return try {
            record[key].toDouble()
        } catch (e: NumberFormatException) {
            throw FormatException("Failed to parse field: $key", e)
        }
    }

    /**
     * Extract an long from the items array. The index into the array
     * is determined by looking up the index in the fields map using the
     * "key" as the key. If no such key exists, or the data is not a valid
     * long, a FormatException is thrown.
     */
    @Throws(FormatException::class)
    private fun extractLong(key: String, record: CSVRecord): Long {
        if (record.isMapped(key) == false) {
            throw FormatException("Field not used but expected: $key")
        }
        return try {
            record[key].toLong()
        } catch (e: NumberFormatException) {
            throw FormatException("Failed to parse field: $key", e)
        }
    }

    /**
     * Import a single transaction into the database using the given
     * session.
     */
    @Throws(FormatException::class)
    private fun importTransaction(context: Context, database: SQLiteDatabase, helper: DBHelper, record: CSVRecord) {
        val id = extractInt(DBHelper.TransactionDbIds.NAME, record)
        val type: Int
        val typeStr = extractString(DBHelper.TransactionDbIds.TYPE, record, "")
        type = when (typeStr) {
            "EXPENSE" -> DBHelper.TransactionDbIds.EXPENSE
            "REVENUE" -> DBHelper.TransactionDbIds.REVENUE
            else -> throw FormatException("Unrecognized type: $typeStr")
        }
        val description = extractString(DBHelper.TransactionDbIds.DESCRIPTION, record, "")
        val account = extractString(DBHelper.TransactionDbIds.ACCOUNT, record, "")
        val budget = extractString(DBHelper.TransactionDbIds.BUDGET, record, "")
        val value = extractDouble(DBHelper.TransactionDbIds.VALUE, record)
        val note = extractString(DBHelper.TransactionDbIds.NOTE, record, "")
        val dateMs = extractLong(DBHelper.TransactionDbIds.DATE, record)
        var receipt = ""
        val potentialReceipt = extractString(DBHelper.TransactionDbIds.RECEIPT, record, "")
        if (potentialReceipt!!.length > 0) {
            // There is a receipt here. If the file actually exists, go with it
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (dir != null) {
                val imageFile = File(dir, potentialReceipt)
                if (imageFile.isFile) {
                    receipt = imageFile.absolutePath
                }
            }
        }
        helper.insertTransaction(database, id, type, description, account, budget, value, note, dateMs, receipt)
    }

    /**
     * Import a single budget into the database using the given
     * session.
     */
    @Throws(FormatException::class)
    private fun importBudget(database: SQLiteDatabase, helper: DBHelper, record: CSVRecord) {
        val name = extractString(DBHelper.BudgetDbIds.NAME, record, null)

        // The transaction field for value is used to indicate the budget value
        val budget = extractInt(DBHelper.TransactionDbIds.VALUE, record)
        helper.insertBudget(database, name, budget)
    }
}