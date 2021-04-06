package protect.budgetwatch

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

import androidx.appcompat.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.core.app.ActivityCompat
import com.google.common.collect.ImmutableMap
import protect.budgetwatch.CalendarUtil.getEndOfDayMs
import protect.budgetwatch.CalendarUtil.getStartOfDayMs
import protect.budgetwatch.ImportExportActivity
import protect.budgetwatch.ImportExportTask.TaskCompleteListener
import java.io.*

import java.text.DateFormat
import java.util.*

class ImportExportActivity : AppCompatActivity() {
    private var importExporter: ImportExportTask? = null
    private var _fileFormatMap: Map<String, DataFormat>? = null
    private var exportStartDateMs: Long? = null
    private var exportEndDateMs: Long? = null
    private val sdcardDir = Environment.getExternalStorageDirectory()
    private val exportFilename = "BudgetWatch"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.import_export_activity)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        _fileFormatMap = ImmutableMap.builder<String, DataFormat>()
                .put(resources.getString(R.string.csv), DataFormat.CSV)
                .put(resources.getString(R.string.json), DataFormat.JSON)
                .put(resources.getString(R.string.zip), DataFormat.ZIP)
                .build()
        for (id in intArrayOf(R.id.importFileFormatSpinner, R.id.exportFileFormatSpinner)) {
            val fileFormatSpinner = findViewById<View>(id) as Spinner
            val names: List<String> = ArrayList((_fileFormatMap as ImmutableMap<String, DataFormat>?)!!.keys)
            val dataAdapter = ArrayAdapter(this,
                    android.R.layout.simple_spinner_item, names)
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            fileFormatSpinner.adapter = dataAdapter
        }

        // If the application does not have permissions to external
        // storage, ask for it now
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this@ImportExportActivity,
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this@ImportExportActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
               ActivityCompat.requestPermissions(this@ImportExportActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSIONS_EXTERNAL_STORAGE)
            }
        }
        val dateRangeButton = findViewById<View>(R.id.dateRangeSelectButton) as Button
        dateRangeButton.setOnClickListener(View.OnClickListener {
            val builder = AlertDialog.Builder(this@ImportExportActivity)
            builder.setTitle(R.string.exportDateRangeHelp)
            val datePickerView = layoutInflater.inflate(R.layout.budget_date_picker_layout, null, false)
            builder.setView(datePickerView)
            builder.setNegativeButton(R.string.cancel) { dialog, which -> dialog.cancel() }
            builder.setPositiveButton(R.string.set, DialogInterface.OnClickListener { dialog, which ->
                val startDatePicker = datePickerView.findViewById<View>(R.id.startDate) as DatePicker
                val endDatePicker = datePickerView.findViewById<View>(R.id.endDate) as DatePicker
                val startDateMs = getStartOfDayMs(startDatePicker.year,
                        startDatePicker.month, startDatePicker.dayOfMonth)
                val endDateMs = getEndOfDayMs(endDatePicker.year,
                        endDatePicker.month, endDatePicker.dayOfMonth)
                if (startDateMs > endDateMs) {
                    Toast.makeText(this@ImportExportActivity, R.string.startDateAfterEndDate, Toast.LENGTH_LONG).show()
                    return@OnClickListener
                }
                exportStartDateMs = startDateMs
                exportEndDateMs = endDateMs
                val date = Calendar.getInstance()
                date.timeInMillis = exportStartDateMs!!
                val startDateString = DateFormat.getDateInstance(DateFormat.SHORT).format(date.time)
                date.timeInMillis = exportEndDateMs!!
                val endDateString = DateFormat.getDateInstance(DateFormat.SHORT).format(date.time)
                val dateRangeFormat = resources.getString(R.string.dateRangeFormat)
                val dateRangeString = String.format(dateRangeFormat, startDateString, endDateString)
                val dateRangeText = findViewById<View>(R.id.dateRangeText) as TextView
                dateRangeText.text = dateRangeString
            })
            builder.show()
        })
        val exportButton = findViewById<View>(R.id.exportButton) as Button
        exportButton.setOnClickListener { startExport(getSelectedFormat(R.id.exportFileFormatSpinner)) }


        // Check that there is an activity that can bring up a file chooser
        val intentPickAction = Intent(Intent.ACTION_PICK)
        val importFilesystem = findViewById<View>(R.id.importOptionFilesystemButton) as Button
        importFilesystem.setOnClickListener { chooseFileWithIntent(intentPickAction) }
        if (isCallable(applicationContext, intentPickAction) == false) {
            findViewById<View>(R.id.dividerImportFilesystem).visibility = View.GONE
            findViewById<View>(R.id.importOptionFilesystemTitle).visibility = View.GONE
            findViewById<View>(R.id.importOptionFilesystemExplanation).visibility = View.GONE
            importFilesystem.visibility = View.GONE
        }


        // Check that there is an application that can find content
        val intentGetContentAction = Intent(Intent.ACTION_GET_CONTENT)
        intentGetContentAction.addCategory(Intent.CATEGORY_OPENABLE)
        intentGetContentAction.type = "*/*"
        val importApplication = findViewById<View>(R.id.importOptionApplicationButton) as Button
        importApplication.setOnClickListener { chooseFileWithIntent(intentGetContentAction) }
        if (isCallable(applicationContext, intentGetContentAction) == false) {
            findViewById<View>(R.id.dividerImportApplication).visibility = View.GONE
            findViewById<View>(R.id.importOptionApplicationTitle).visibility = View.GONE
            findViewById<View>(R.id.importOptionApplicationExplanation).visibility = View.GONE
            importApplication.visibility = View.GONE
        }


        // This option, to import from the fixed location, should always be present
        val importButton = findViewById<View>(R.id.importOptionFixedButton) as Button
        importButton.setOnClickListener {
            val format = getSelectedFormat(R.id.importFileFormatSpinner)
            val importFile = File(sdcardDir, exportFilename + "." + format!!.extension())
            val uri = Uri.fromFile(importFile)
            try {
                val stream = FileInputStream(importFile)
                Log.d(TAG, "Starting import from fixed location: " + importFile.absolutePath)
                startImport(format, stream, uri)
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "Could not import file " + importFile.absolutePath, e)
                onImportComplete(false, uri)
            }
        }
    }

    private fun getSelectedFormat(id: Int): DataFormat? {
        val fileFormatSpinner = findViewById<View>(id) as Spinner
        val name = fileFormatSpinner.selectedItem as String
        return _fileFormatMap!![name]
    }

    private fun startImport(format: DataFormat?, target: InputStream, targetUri: Uri) {
        var format = format
        val listener = object : TaskCompleteListener {
            override fun onTaskComplete(success: Boolean) {
                onImportComplete(success, targetUri)
            }
        }
        val mimetype = getMimeType(targetUri)
        if (format == null && mimetype != null) {
            // Attempt to guess the data format based on the extension
            Log.d(TAG, "Attempting to determine file type for: $mimetype")
            for ((_, value) in _fileFormatMap!!) {
                if (mimetype.toLowerCase() == value.mimetype()) {
                    format = value
                    break
                }
            }
        }
        if (format != null) {
            Log.d(TAG, "Starting import of file")
            importExporter = ImportExportTask(this@ImportExportActivity,
                    format, target, listener)
            importExporter!!.execute()
        } else {
            // If format is still null, then we do not know what to import
            Log.w(TAG, "Could not import file because mimetype could not get determined")
            onImportComplete(false, targetUri)
            try {
                target.close()
            } catch (e: IOException) {
                Log.w(TAG, "Failed to close stream during import", e)
            }
        }
    }

    private fun startExport(format: DataFormat?) {
        val exportFile = File(sdcardDir, exportFilename + "." + format!!.extension())
        val listener = object : TaskCompleteListener {
            override fun onTaskComplete(success: Boolean) {
                onExportComplete(success, exportFile, format)
            }
        }
        importExporter = ImportExportTask(this@ImportExportActivity,
                format, exportFile, listener, exportStartDateMs, exportEndDateMs)
        importExporter!!.execute()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            var success = grantResults.size > 0
            for (grant in grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    success = false
                }
            }
            if (success == false) {
                // External storage permission rejected, inform user that
                // import/export is prevented
                Toast.makeText(applicationContext, R.string.noExternalStoragePermissionError,
                        Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        if (importExporter != null && importExporter!!.status != AsyncTask.Status.RUNNING) {
            importExporter!!.cancel(true)
        }
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun getMimeType(uri: Uri): String? {
        var mimeType: String? = null
        var fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        if (fileExtension != null) {
            fileExtension = fileExtension.toLowerCase()
            for (format in DataFormat.values()) {
                if (fileExtension == format.name.toLowerCase()) {
                    mimeType = format.mimetype()
                    break
                }
            }
        }
        if (mimeType == null && uri.scheme != null && uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val cr = contentResolver
            mimeType = cr.getType(uri)
            if (mimeType != null) {
                if (mimeType == "text/comma-separated-values") {
                    mimeType = "text/csv"
                }
            }
        }
        return mimeType
    }

    private fun onImportComplete(success: Boolean, path: Uri) {
        val builder = android.app.AlertDialog.Builder(this)
        if (success) {
            builder.setTitle(R.string.importSuccessfulTitle)
        } else {
            builder.setTitle(R.string.importFailedTitle)
        }
        val messageId = if (success) R.string.importedFrom else R.string.importFailed
        val template = resources.getString(messageId)

        // Get the filename of the file being imported
        val filename = path.toString()
        val message = String.format(template, filename)
        builder.setMessage(message)
        builder.setNeutralButton(R.string.ok) { dialog, which -> dialog.dismiss() }
        builder.create().show()
    }

    private fun onExportComplete(success: Boolean, path: File, format: DataFormat?) {
        val builder = android.app.AlertDialog.Builder(this)
        if (success) {
            builder.setTitle(R.string.exportSuccessfulTitle)
        } else {
            builder.setTitle(R.string.exportFailedTitle)
        }
        val messageId = if (success) R.string.exportedTo else R.string.exportFailed
        val template = resources.getString(messageId)
        val message = String.format(template, path.absolutePath)
        builder.setMessage(message)
        builder.setNeutralButton(R.string.ok) { dialog, which -> dialog.dismiss() }
        if (success) {
            val sendLabel = this@ImportExportActivity.resources.getText(R.string.sendLabel)
            builder.setPositiveButton(sendLabel) { dialog, which ->
                val outputUri = FileProvider.getUriForFile(this@ImportExportActivity, BuildConfig.APPLICATION_ID, path)
                val sendIntent = Intent(Intent.ACTION_SEND)
                sendIntent.putExtra(Intent.EXTRA_STREAM, outputUri)
                sendIntent.type = format!!.mimetype()

                // set flag to give temporary permission to external app to use the FileProvider
                sendIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                this@ImportExportActivity.startActivity(Intent.createChooser(sendIntent,
                        sendLabel))
                dialog.dismiss()
            }
        }
        builder.create().show()
    }

    /**
     * Determines if there is at least one activity that can perform the given intent
     */
    private fun isCallable(context: Context, intent: Intent): Boolean {
        val manager = context.packageManager ?: return false
        val list = manager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        for (info in list) {
            if (info.activityInfo.exported) {
                // There is one activity which is available to be called
                return true
            }
        }
        return false
    }

    private fun chooseFileWithIntent(intent: Intent) {
        try {
            startActivityForResult(intent, CHOOSE_EXPORT_FILE)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No activity found to handle intent", e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || requestCode != CHOOSE_EXPORT_FILE) {
            Log.w(TAG, "Failed onActivityResult(), result=$resultCode")
            return
        }
        val uri = data!!.data
        if (uri == null) {
            Log.e(TAG, "Activity returned a NULL URI")
            return
        }
        try {
            val reader: InputStream
            reader = (if (uri.scheme != null) {
                contentResolver.openInputStream(uri)
            } else {
                FileInputStream(File(uri.toString()))
            })!!
            Log.e(TAG, "Starting file import with: $uri")
            startImport(null, reader, uri)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Failed to import file: $uri", e)
            onImportComplete(false, uri)
        }
    }

    companion object {
        private const val TAG = "BudgetWatch"
        private const val PERMISSIONS_EXTERNAL_STORAGE = 1
        private const val CHOOSE_EXPORT_FILE = 2
    }
}