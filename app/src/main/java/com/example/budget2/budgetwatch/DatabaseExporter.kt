package protect.budgetwatch

import android.content.Context
import java.io.IOException
import java.io.OutputStream

/**
 * Interface for a class which can export the contents of the database
 * in a given format.
 */
internal interface DatabaseExporter {
    /**
     * Export the database to the output stream in a given format.
     * @throws IOException
     */
    @Throws(IOException::class, InterruptedException::class)
    fun exportData(context: Context?, db: DBHelper?, startTimeMs: Long?, endTimeMs: Long?,
                   output: OutputStream?, updater: ImportExportProgressUpdater?)
}