package protect.budgetwatch

import android.content.Context
import android.os.Environment
import android.util.Log
import protect.budgetwatch.MultiFormatImporter.importData
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Class for importing a database and receipts from a Zip file.
 */
class ZipDatabaseImporter : DatabaseImporter {
    @Throws(IOException::class, FormatException::class, InterruptedException::class)
    override fun importData(context: Context?, db: DBHelper?, input: InputStream?, updater: ImportExportProgressUpdater?) {
        val receiptDir = context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val buffer = ByteArray(1024)
        val zipStream = ZipInputStream(input)
        var entry: ZipEntry
        while (zipStream.nextEntry.also { entry = it } != null) {
            // The database entry always occurs last in the file record, after the
            // images has been put into place. This is necessary, because the
            // CSV import will only complete the import process if the
            // images already exist.
            if (entry.name == "database.csv") {
                val result = importData(context, db, zipStream, DataFormat.CSV, updater)
                if (result == false) {
                    Log.e(TAG, "Failed to import database.csv")
                }
            } else {
                if (receiptDir != null) {
                    val receipt = File(receiptDir, entry.name)
                    val out = FileOutputStream(receipt)
                    try {
                        var count: Int
                        while (zipStream.read(buffer, 0, buffer.size).also { count = it } > 0) {
                            out.write(buffer, 0, count)
                        }
                    } finally {
                        out.close()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "BudgetWatch"
    }
}