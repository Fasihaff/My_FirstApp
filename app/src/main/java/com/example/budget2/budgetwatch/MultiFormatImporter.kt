package protect.budgetwatch

import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.InputStream

internal object MultiFormatImporter {
    private const val TAG = "BudgetWatch"

    /**
     * Attempts to import data from the input stream of the
     * given format into the database.
     *
     * The input stream is not closed, and doing so is the
     * responsibility of the caller.
     *
     * @return true if the database was successfully imported,
     * false otherwise. If false, no data was written to
     * the database.
     */
    @JvmStatic
    fun importData(context: Context?, db: DBHelper?, input: InputStream?,
                   format: DataFormat, updater: ImportExportProgressUpdater?): Boolean {
        var importer: DatabaseImporter? = null
        importer = when (format) {
            DataFormat.CSV -> CsvDatabaseImporter()
            DataFormat.JSON -> JsonDatabaseImporter()
            DataFormat.ZIP -> ZipDatabaseImporter()
        }
        return if (importer != null) {
            try {
                importer.importData(context, db, input, updater)
                return true
            } catch (e: IOException) {
                Log.e(TAG, "Failed to input data", e)
            } catch (e: FormatException) {
                Log.e(TAG, "Failed to input data", e)
            } catch (e: InterruptedException) {
                Log.e(TAG, "Failed to input data", e)
            }
            false
        } else {
            Log.e(TAG, "Unsupported data format imported: " + format.name)
            false
        }
    }
}