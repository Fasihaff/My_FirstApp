package protect.budgetwatch

import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.OutputStream

internal object MultiFormatExporter {
    private val TAG = "BudgetWatch"

    /**
     * Attempts to export data to the output stream in the
     * given format, if possible.
     *
     * The output stream is closed on success.
     *
     * @return true if the database was successfully exported,
     * false otherwise. If false, partial data may have been
     * written to the output stream, and it should be discarded.
     */
    @JvmStatic
    fun exportData(context: Context?, db: DBHelper?, startTimeMs: Long?, endTimeMs: Long?, output: OutputStream?, format: DataFormat, updater: ImportExportProgressUpdater?): Boolean {
        var exporter: DatabaseExporter? = null
        exporter = when (format) {
            DataFormat.CSV -> CsvDatabaseExporter()
            DataFormat.JSON -> JsonDatabaseExporter()
            DataFormat.ZIP -> ZipDatabaseExporter()
        }
        return if (exporter != null) {
            try {
                exporter.exportData(context, db, startTimeMs, endTimeMs, output, updater)
                return true
            } catch (e: IOException) {
                Log.e(TAG, "Failed to export data", e)
            } catch (e: InterruptedException) {
                Log.e(TAG, "Failed to export data", e)
            }
            false
        } else {
            Log.e(TAG, "Unsupported data format exported: " + format.name)
            false
        }
    }
}