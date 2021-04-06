package protect.budgetwatch

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import android.util.JsonReader
import android.util.JsonToken
import com.google.common.base.Charsets
import protect.budgetwatch.DBHelper
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Class for importing a database from JSON formatted data.
 */
class JsonDatabaseImporter : DatabaseImporter {
    @Throws(IOException::class, FormatException::class, InterruptedException::class)
    override fun importData(context: Context?, db: DBHelper?, input: InputStream?, updater: ImportExportProgressUpdater?) {
        val reader = InputStreamReader(input, Charsets.UTF_8)
        val parser = JsonReader(reader)
        val database = db!!.writableDatabase
        database.beginTransaction()
        try {
            parser.beginArray()
            while (parser.hasNext()) {
                parser.beginObject()
                var id: Int? = null
                var name: String? = null
                var type: String? = null
                var description: String? = null
                var account: String? = null
                var budget: String? = null
                var value: Double? = null
                var note: String? = null
                var dateMs: Long? = null
                var receiptFilename: String? = null
                while (parser.hasNext()) {
                    val itemName = parser.nextName()
                    when (itemName) {
                        DBHelper.TransactionDbIds.NAME -> name = parser.nextString()
                        "ID" -> id = parser.nextInt()
                        DBHelper.TransactionDbIds.TYPE -> type = parser.nextString()
                        DBHelper.TransactionDbIds.DESCRIPTION -> description = parser.nextString()
                        DBHelper.TransactionDbIds.ACCOUNT -> account = parser.nextString()
                        DBHelper.TransactionDbIds.BUDGET -> budget = parser.nextString()
                        DBHelper.TransactionDbIds.VALUE -> value = parser.nextDouble()
                        DBHelper.TransactionDbIds.NOTE -> note = parser.nextString()
                        DBHelper.TransactionDbIds.DATE -> dateMs = parser.nextLong()
                        DBHelper.TransactionDbIds.RECEIPT -> receiptFilename = parser.nextString()
                        else -> throw FormatException("Issue parsing JSON data, unknown field: $itemName")
                    }
                }
                if (type == null) {
                    throw FormatException("Issue parsing JSON data, missing type")
                }
                when (type) {
                    "BUDGET" -> importBudget(database, db, name, value)
                    "EXPENSE", "REVENUE" -> importTransaction(context, database, db, id, type, description, account, budget, value, note, dateMs, receiptFilename)
                    else -> throw FormatException("Issue parsing JSON data, unexpected type: $type")
                }
                parser.endObject()
                updater!!.update()
                if (Thread.currentThread().isInterrupted) {
                    throw InterruptedException()
                }
            }
            parser.endArray()
            if (parser.peek() != JsonToken.END_DOCUMENT) {
                throw FormatException("Issue parsing JSON data, no more data expected but found some")
            }
            parser.close()

            // Do not close the parser, as it will close the input stream;
            // Closing the input stream is the responsibility of the caller.
            database.setTransactionSuccessful()
        } catch (e: IllegalArgumentException) {
            throw FormatException("Issue parsing JSON data", e)
        } finally {
            database.endTransaction()
            database.close()
        }
    }

    /**
     * Import a single transaction into the database using the given
     * session.
     */
    @Throws(FormatException::class)
    private fun importTransaction(context: Context?, database: SQLiteDatabase, helper: DBHelper?,
                                  id: Int?, typeStr: String, description: String?, account: String?,
                                  budget: String?, value: Double?, note: String?, dateMs: Long?,
                                  receiptFilename: String?) {
        var description = description
        var account = account
        var note = note
        var receiptFilename = receiptFilename
        val type: Int
        type = when (typeStr) {
            "EXPENSE" -> DBHelper.TransactionDbIds.EXPENSE
            "REVENUE" -> DBHelper.TransactionDbIds.REVENUE
            else -> throw FormatException("Unrecognized type: $typeStr")
        }

        // Ensure that the required data exists
        if (id == null || budget == null || value == null || dateMs == null) {
            throw FormatException("Missing required data in JSON record")
        }

        // All the other fields can be blank strings if they are missing.
        description = description ?: ""
        account = account ?: ""
        note = note ?: ""
        receiptFilename = receiptFilename ?: ""
        var receipt = ""
        if (receiptFilename.length > 0) {
            // There is a receipt here. If the file actually exists, go with it
            val dir = context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (dir != null) {
                val imageFile = File(dir, receiptFilename)
                if (imageFile.isFile) {
                    receipt = imageFile.absolutePath
                }
            }
        }
        helper!!.insertTransaction(database, id, type, description, account, budget, value, note, dateMs, receipt)
    }

    /**
     * Import a single budget into the database using the given
     * session.
     */
    @Throws(FormatException::class)
    private fun importBudget(database: SQLiteDatabase, helper: DBHelper?, name: String?, value: Double?) {
        // Check that both fields exist
        // Ensure that the required data exists
        if (name == null || value == null) {
            throw FormatException("Missing required data in JSON record")
        }
        helper!!.insertBudget(database, name, value.toInt())
    }
}