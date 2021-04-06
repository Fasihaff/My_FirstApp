@file:Suppress("DEPRECATION")

package protect.budgetwatch


import android.app.Activity
import android.app.ProgressDialog
import android.os.AsyncTask
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class ImportExportTask : AsyncTask<Void?, Void?, Boolean> {
    private val TAG = "BudgetWatch"
    private val activity: Activity
    private val doImport: Boolean
    private val format: DataFormat
    private val target: File?
    private val inputStream: InputStream?
    private val listener: TaskCompleteListener

    // Start and end times for exporting transactions
    private val startTimeMs: Long?
    private val endTimeMs: Long?
    private var progress: ProgressDialog? = null

    /**
     * Constructor which will setup a task for exporting to the given file
     */
    constructor(activity: Activity, format: DataFormat,
                target: File?, listener: TaskCompleteListener,
                startTimeMs: Long?, endTimeMs: Long?) : super() {
        this.activity = activity
        doImport = false
        this.format = format
        this.target = target
        inputStream = null
        this.listener = listener
        this.startTimeMs = startTimeMs
        this.endTimeMs = endTimeMs
    }

    /**
     * Constructor which will setup a task for importing from the given InputStream.
     */
    constructor(activity: Activity, format: DataFormat,
                input: InputStream?, listener: TaskCompleteListener) : super() {
        this.activity = activity
        doImport = true
        this.format = format
        target = null
        inputStream = input
        this.listener = listener
        startTimeMs = null
        endTimeMs = null
    }

    private fun performImport(inputStream: InputStream?, db: DBHelper): Boolean {
        val BASE_MESSAGE = activity.resources.getString(R.string.importProgressFormat)
        val updater = ImportExportProgressUpdater(activity, progress!!, BASE_MESSAGE)
        val result = MultiFormatImporter.importData(activity, db, inputStream, format, updater)
        Log.i(TAG, "Import result: $result")
        return result
    }

    private fun performExport(exportFile: File?, db: DBHelper, startTimeMs: Long?, endTimeMs: Long?): Boolean {
        var result = false
        val BASE_MESSAGE = activity.resources.getString(R.string.exportProgressFormat)
        val updater = ImportExportProgressUpdater(activity, progress!!, BASE_MESSAGE)
        try {
            val outStream =  FileOutputStream(exportFile)
            result = MultiFormatExporter.exportData(activity, db, startTimeMs, endTimeMs, outStream, format, updater)
            outStream.close()
        }
        catch (e: IOException)
        {
            Log.e(TAG, "Unable to export file", e)
        }
        Log.i(TAG, "Export of '" + exportFile!!.absolutePath + "' result: " + result)
        return result
    }


    override fun onPreExecute() {
        progress = ProgressDialog(activity)
        progress!!.setTitle(if (doImport) R.string.importing else R.string.exporting)
        progress!!.setOnDismissListener { cancel(true) }
        progress!!.show()
    }


    override fun doInBackground(vararg nothing: Void?): Boolean? {
        val result: Boolean
        val db = DBHelper(activity)
        result = if (doImport)
        {
            performImport(inputStream, db)
        }
        else
        {
            performExport(target, db, startTimeMs, endTimeMs)
        }
        db.close()
        return result
    }

    override fun onPostExecute(result: Boolean) {
        listener.onTaskComplete(result)
        progress!!.dismiss()
        Log.i(TAG, (if (doImport) "Import" else "Export") + " Complete")
    }

    override fun onCancelled() {
        progress!!.dismiss()
        Log.i(TAG, (if (doImport) "Import" else "Export") + " Cancelled")
    }

    interface TaskCompleteListener {
        fun onTaskComplete(success: Boolean)
    }

}