package protect.budgetwatch

import android.content.Context
import android.os.Environment
import protect.budgetwatch.MultiFormatExporter.exportData
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Class for exporting the database and its receipt images into a ZIP file
 */
class ZipDatabaseExporter : DatabaseExporter {
    @Throws(IOException::class, InterruptedException::class)
    override fun exportData(context: Context?, db: DBHelper?, startTimeMs: Long?, endTimeMs: Long?, outStream: OutputStream?, updater: ImportExportProgressUpdater?) {
        val out = ZipOutputStream(outStream)
        val receiptFolder = context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (receiptFolder != null) {
            val receipts = receiptFolder.listFiles()
            if (receipts != null) {
                val data = ByteArray(1024)
                for (receipt in receipts) {
                    val receiptEntry = ZipEntry(receipt.name)
                    out.putNextEntry(receiptEntry)
                    val image = FileInputStream(receipt)
                    try {
                        var count: Int
                        while (image.read(data, 0, data.size).also { count = it } != -1) {
                            out.write(data, 0, count)
                        }
                    } finally {
                        image.close()
                    }
                }
            }
        }

        // Write the database to the zip file as a CSV file
        val databaseEntry = ZipEntry("database.csv")
        out.putNextEntry(databaseEntry)
        exportData(context, db, startTimeMs, endTimeMs, out, DataFormat.CSV, updater)
    }
}