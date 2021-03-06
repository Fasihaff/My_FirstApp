package protect.budgetwatch

import android.content.Context
import java.io.IOException
import java.io.InputStream

/**
 * Interface for a class which can import the contents of a stream
 * into the database.
 */
 internal interface DatabaseImporter {
    /**
     * Import data from the input stream in a given format into
     * the database.
     * @throws IOException
     * @throws FormatException
     */
    @Throws(IOException::class, FormatException::class, InterruptedException::class)
    fun importData(context: Context?, db: DBHelper?, input: InputStream?, updater: ImportExportProgressUpdater?)
}