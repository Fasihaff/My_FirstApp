package protect.budgetwatch

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import protect.budgetwatch.DBHelper
import protect.budgetwatch.ReceiptViewActivity
import protect.budgetwatch.TransactionViewActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class TransactionViewActivity : AppCompatActivity() {
    private var capturedUncommittedReceipt: String? = null
    private var _db: DBHelper? = null
    private var _nameEdit: EditText? = null
    private var _nameView: TextView? = null
    private var _accountEdit: EditText? = null
    private var _accountView: TextView? = null
    private var _valueEdit: EditText? = null
    private var _valueView: TextView? = null
    private var _noteEdit: EditText? = null
    private var _noteView: TextView? = null
    private var _budgetView: TextView? = null
    private var _dateView: TextView? = null
    private var _captureButton: Button? = null
    private var _viewButton: Button? = null
    private var _updateButton: Button? = null
    private var _receiptLayout: View? = null
    private var _endingDivider: View? = null
    private var _receiptLocationField: TextView? = null
    private var _noReceiptButtonLayout: View? = null
    private var _hasReceiptButtonLayout: View? = null
    private var _dateEdit: EditText? = null
    private var _budgetSpinner: Spinner? = null
    private var _transactionId = 0
    private var _type = 0
    private var _updateTransaction = false
    private var _viewTransaction = false
    private fun extractIntentFields(intent: Intent) {
        val b = intent.extras
        val action = intent.action
        if (b != null) {
            _transactionId = b.getInt("id")
            _type = b.getInt("type")
            _updateTransaction = b.getBoolean("update", false)
            _viewTransaction = b.getBoolean("view", false)
        } else if (action != null) {
            _updateTransaction = false
            _viewTransaction = false
            if (action == ACTION_NEW_EXPENSE) {
                _type = DBHelper.TransactionDbIds.EXPENSE
            } else if (action == ACTION_NEW_REVENUE) {
                _type = DBHelper.TransactionDbIds.REVENUE
            } else {
                Log.d(TAG, "Unsupported action '$action', bailing")
                finish()
            }
        } else {
            Log.d(TAG, "Launched TransactionViewActivity without arguments, bailing")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.transaction_view_activity)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        _db = DBHelper(this)
        _nameEdit = findViewById<View>(R.id.nameEdit) as EditText
        _nameView = findViewById<View>(R.id.nameView) as TextView
        _accountEdit = findViewById<View>(R.id.accountEdit) as EditText
        _accountView = findViewById<View>(R.id.accountView) as TextView
        _valueEdit = findViewById<View>(R.id.valueEdit) as EditText
        _valueView = findViewById<View>(R.id.valueView) as TextView
        _noteEdit = findViewById<View>(R.id.noteEdit) as EditText
        _noteView = findViewById<View>(R.id.noteView) as TextView
        _budgetView = findViewById<View>(R.id.budgetView) as TextView
        _dateView = findViewById<View>(R.id.dateView) as TextView
        _captureButton = findViewById<View>(R.id.captureButton) as Button
        _viewButton = findViewById<View>(R.id.viewButton) as Button
        _updateButton = findViewById<View>(R.id.updateButton) as Button
        _receiptLayout = findViewById(R.id.receiptLayout)
        _endingDivider = findViewById(R.id.endingDivider)
        _receiptLocationField = findViewById<View>(R.id.receiptLocation) as TextView
        _noReceiptButtonLayout = findViewById(R.id.noReceiptButtonLayout)
        _hasReceiptButtonLayout = findViewById(R.id.hasReceiptButtonLayout)
        _dateEdit = findViewById<View>(R.id.dateEdit) as EditText
        _budgetSpinner = findViewById<View>(R.id.budgetSpinner) as Spinner
        extractIntentFields(intent)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.i(TAG, "Received new intent")
        extractIntentFields(intent)
    }

    @SuppressLint("DefaultLocale")
    public override fun onResume() {
        super.onResume()
        if (_type == DBHelper.TransactionDbIds.EXPENSE) {
            if (_updateTransaction) {
                setTitle(R.string.editExpenseTransactionTitle)
            } else if (_viewTransaction) {
                setTitle(R.string.viewExpenseTransactionTitle)
            } else {
                setTitle(R.string.addExpenseTransactionTitle)
            }
        } else if (_type == DBHelper.TransactionDbIds.REVENUE) {
            if (_updateTransaction) {
                setTitle(R.string.editRevenueTransactionTitle)
            } else if (_viewTransaction) {
                setTitle(R.string.viewRevenueTransactionTitle)
            } else {
                setTitle(R.string.addRevenueTransactionTitle)
            }
        }
        val date: Calendar = GregorianCalendar()
        val dateFormatter = SimpleDateFormat.getDateInstance()
        _dateEdit!!.setText(dateFormatter.format(date.time))
        val dateSetListener = OnDateSetListener { view, year, month, day ->
            date[year, month] = day
            _dateEdit!!.setText(dateFormatter.format(date.time))
        }
        _dateEdit!!.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val year = date[Calendar.YEAR]
                val month = date[Calendar.MONTH]
                val day = date[Calendar.DATE]
                val datePicker = DatePickerDialog(this@TransactionViewActivity,
                        dateSetListener, year, month, day)
                datePicker.show()
            }
        }
        val actualBudgetNames = _db!!.budgetNames
        val budgetNames = LinkedList(actualBudgetNames)

        // A user is not allowed to create a blank budget. However, here
        // an empty budget is injected, so that a user can create a
        // transaction that has no budget.
        budgetNames.addFirst("")

        // Add budget items to spinner if it has not been initialized yet
        if (_budgetSpinner!!.count == 0) {
            val budgets = ArrayAdapter(this, R.layout.spinner_textview, budgetNames)
            _budgetSpinner!!.adapter = budgets
        }
        if (_updateTransaction || _viewTransaction) {
            val transaction = _db!!.getTransaction(_transactionId)
            (if (_updateTransaction) _nameEdit else _nameView)!!.text = transaction!!.description
            (if (_updateTransaction) _accountEdit else _accountView)!!.text = transaction.account
            val budgetIndex = budgetNames.indexOf(transaction.budget)
            if (budgetIndex >= 0) {
                _budgetSpinner!!.setSelection(budgetIndex)
            }
            _budgetView!!.text = if (_viewTransaction) transaction.budget else ""
            (if (_updateTransaction) _valueEdit else _valueView)!!.text = String.format(Locale.US, "%.2f", transaction.value)
            (if (_updateTransaction) _noteEdit else _noteView)!!.text = transaction.note
            (if (_updateTransaction) _dateEdit else _dateView)!!.text = dateFormatter.format(Date(transaction.dateMs))
            _receiptLocationField!!.text = transaction.receipt
            if (_viewTransaction) {
                _budgetSpinner!!.visibility = View.GONE
                _nameEdit!!.visibility = View.GONE
                _accountEdit!!.visibility = View.GONE
                _valueEdit!!.visibility = View.GONE
                _noteEdit!!.visibility = View.GONE
                _dateEdit!!.visibility = View.GONE

                // The no receipt layout need never be displayed
                // when only viewing a transaction, as one should
                // not be able to capture a receipt
                _noReceiptButtonLayout!!.visibility = View.GONE

                // If viewing a transaction, only display the receipt
                // field if a receipt is captured
                if (transaction.receipt.isEmpty() == false) {
                    _receiptLayout!!.visibility = View.VISIBLE
                    _endingDivider!!.visibility = View.VISIBLE
                    _hasReceiptButtonLayout!!.visibility = View.VISIBLE
                } else {
                    _receiptLayout!!.visibility = View.GONE
                    _endingDivider!!.visibility = View.GONE
                }
            } else {
                _budgetView!!.visibility = View.GONE
                _nameView!!.visibility = View.GONE
                _accountView!!.visibility = View.GONE
                _valueView!!.visibility = View.GONE
                _noteView!!.visibility = View.GONE
                _dateView!!.visibility = View.GONE

                // If editing a transaction, always list the receipt field
                _receiptLayout!!.visibility = View.VISIBLE
                _endingDivider!!.visibility = View.VISIBLE
                if (transaction.receipt.isEmpty() && capturedUncommittedReceipt == null) {
                    _noReceiptButtonLayout!!.visibility = View.VISIBLE
                    _hasReceiptButtonLayout!!.visibility = View.GONE
                } else {
                    _noReceiptButtonLayout!!.visibility = View.GONE
                    _hasReceiptButtonLayout!!.visibility = View.VISIBLE
                    _updateButton!!.visibility = View.VISIBLE
                }
            }
        } else {
            _budgetView!!.visibility = View.GONE
            _nameView!!.visibility = View.GONE
            _accountView!!.visibility = View.GONE
            _valueView!!.visibility = View.GONE
            _noteView!!.visibility = View.GONE
            _dateView!!.visibility = View.GONE

            // If adding a transaction, always list the receipt field
            _receiptLayout!!.visibility = View.VISIBLE
            _endingDivider!!.visibility = View.VISIBLE
            if (capturedUncommittedReceipt == null) {
                _noReceiptButtonLayout!!.visibility = View.VISIBLE
                _hasReceiptButtonLayout!!.visibility = View.GONE
            } else {
                _noReceiptButtonLayout!!.visibility = View.GONE
                _hasReceiptButtonLayout!!.visibility = View.VISIBLE
                _updateButton!!.visibility = View.VISIBLE
            }
        }
        val captureCallback = View.OnClickListener {
            if (ContextCompat.checkSelfPermission(this@TransactionViewActivity,
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                captureReceipt()
            }
            else
            {
                ActivityCompat.requestPermissions(this@TransactionViewActivity, arrayOf(Manifest.permission.CAMERA),
                        PERMISSIONS_REQUEST_CAMERA)
            }
        }
        _captureButton!!.setOnClickListener(captureCallback)
        _updateButton!!.setOnClickListener(captureCallback)
        _viewButton!!.setOnClickListener { v ->
            val i = Intent(v.context, ReceiptViewActivity::class.java)
            val b = Bundle()
            val receiptField = findViewById<View>(R.id.receiptLocation) as TextView
            var receipt: String? = receiptField.text.toString()
            if (capturedUncommittedReceipt != null) {
                receipt = capturedUncommittedReceipt
            }
            b.putString("receipt", receipt)
            i.putExtras(b)
            startActivity(i)
        }
    }

    private fun doSave() {
        val name = _nameEdit!!.text.toString()
        // name field is optional, so it is OK if it is empty
        val budget = _budgetSpinner!!.selectedItem as String
        if (budget == null) {
            Snackbar.make(_budgetSpinner!!, R.string.budgetMissing, Snackbar.LENGTH_LONG).show()
            return
        }
        val account = _accountEdit!!.text.toString()
        // The account field is optional, so it is OK if it is empty
        val valueStr = _valueEdit!!.text.toString()
        if (valueStr.isEmpty()) {
            Snackbar.make(_valueEdit!!, R.string.valueMissing, Snackbar.LENGTH_LONG).show()
            return
        }
        val value: Double
        value = try {
            valueStr.toDouble()
        } catch (e: NumberFormatException) {
            Snackbar.make(_valueEdit!!, R.string.valueInvalid, Snackbar.LENGTH_LONG).show()
            return
        }
        val note = _noteEdit!!.text.toString()
        // The note field is optional, so it is OK if it is empty
        val dateStr = _dateEdit!!.text.toString()
        val dateFormatter = SimpleDateFormat.getDateInstance()
        val dateMs: Long
        dateMs = try {
            dateFormatter.parse(dateStr).time
        } catch (e: ParseException) {
            Snackbar.make(_dateEdit!!, R.string.dateInvalid, Snackbar.LENGTH_LONG).show()
            return
        }
        var receipt: String? = _receiptLocationField!!.text.toString()
        if (capturedUncommittedReceipt != null) {
            // Delete the old receipt, it is no longer needed
            val oldReceipt = File(receipt)
            if (oldReceipt.delete() == false) {
                Log.e(TAG, "Unable to delete old receipt file: $capturedUncommittedReceipt")
            }

            // Remember the new receipt to save
            receipt = capturedUncommittedReceipt
            capturedUncommittedReceipt = null
        }
        if (_updateTransaction) {
            _db!!.updateTransaction(_transactionId, _type, name, account,
                    budget, value, note, dateMs, receipt)
        } else {
            _db!!.insertTransaction(_type, name, account, budget,
                    value, note, dateMs, receipt)
        }
        finish()
    }

    private fun captureReceipt() {
        if (capturedUncommittedReceipt != null) {
            Log.i(TAG, "Deleting unsaved image: $capturedUncommittedReceipt")
            val unneededReceipt = File(capturedUncommittedReceipt)
            if (unneededReceipt.delete() == false) {
                Log.e(TAG, "Unable to delete unnecessary file: $capturedUncommittedReceipt")
            }
            capturedUncommittedReceipt = null
        }
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val packageManager = packageManager
        if (packageManager == null) {
            Log.e(TAG, "Failed to get package manager, cannot take picture")
            Toast.makeText(applicationContext, R.string.pictureCaptureError,
                    Toast.LENGTH_LONG).show()
            return
        }
        if (takePictureIntent.resolveActivity(packageManager) == null) {
            Log.e(TAG, "Could not find an activity to take a picture")
            Toast.makeText(applicationContext, R.string.pictureCaptureError, Toast.LENGTH_LONG).show()
            return
        }
        val imageLocation = newImageLocation
        val imageUri: Uri

        // Starting in Android N (24+) sharing a file Uri is discouraged or prevented.
        // For those platforms a FileProvider is used to provide a content Uri. Older
        // platforms still use the file Uri, in part to also allow easier testing
        // using Robolectric.
        imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID!!, imageLocation!!)
        } else {
            Uri.fromFile(imageLocation)
        }
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        capturedUncommittedReceipt = imageLocation?.absolutePath
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onDestroy()
    {
        if (capturedUncommittedReceipt != null) {
            // The receipt was captured but never used
            Log.i(TAG, "Deleting unsaved image: $capturedUncommittedReceipt")
            val unneededReceipt = File(capturedUncommittedReceipt)
            if (unneededReceipt.delete() == false) {
                Log.e(TAG, "Unable to delete unnecessary file: $capturedUncommittedReceipt")
            }
            capturedUncommittedReceipt = null
        }
        _db!!.close()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (_viewTransaction) {
            menuInflater.inflate(R.menu.view_menu, menu)
        } else if (_updateTransaction) {
            menuInflater.inflate(R.menu.edit_menu, menu)
        } else {
            menuInflater.inflate(R.menu.add_menu, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_save) {
            doSave()
            return true
        }
        if (id == android.R.id.home) {
            finish()
            return true
        }
        if (id == R.id.action_edit) {
            finish()
            val i = Intent(applicationContext, TransactionViewActivity::class.java)
            val bundle = Bundle()
            bundle.putInt("id", _transactionId)
            bundle.putInt("type", _type)
            bundle.putBoolean("update", true)
            i.putExtras(bundle)
            startActivity(i)
            return true
        }
        if (id == R.id.action_delete) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.deleteTransactionTitle)
            builder.setMessage(R.string.deleteTransactionConfirmation)
            builder.setPositiveButton(R.string.confirm) { dialog, which ->
                Log.e(TAG, "Deleting transaction: $_transactionId")
                _db!!.deleteTransaction(_transactionId)
                finish()
                dialog.dismiss()
            }
            builder.setNegativeButton(R.string.cancel) { dialog, which -> dialog.dismiss() }
            val dialog = builder.create()
            dialog.show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private val newImageLocation: File?
        private get() {
            val imageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (imageDir == null) {
                Log.e(TAG, "Failed to locate directory for pictures")
                Toast.makeText(this, R.string.pictureCaptureError, Toast.LENGTH_LONG).show()
                return null
            }
            if (imageDir.exists() == false) {
                if (imageDir.mkdirs() == false) {
                    Log.e(TAG, "Failed to create receipts image directory")
                    Toast.makeText(this, R.string.pictureCaptureError, Toast.LENGTH_LONG).show()
                    return null
                }
            }
            val imageFilename = UUID.randomUUID()
            return File(imageDir, "$imageFilename.jpg")
        }

    private fun reencodeImageWithQuality(original: String?, quality: Int): Boolean {
        val destFile = File(original)
        val tmpLocation = newImageLocation
        try {
            if (tmpLocation == null) {
                throw IOException("Could not create location for tmp file")
            }
            val created = tmpLocation.createNewFile()
            if (created == false) {
                throw IOException("Could not create tmp file")
            }
            val bitmap = BitmapFactory.decodeFile(original)
            val fOut = FileOutputStream(tmpLocation)
            val fileWritten = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fOut)
            fOut.flush()
            fOut.close()
            if (fileWritten == false) {
                throw IOException("Could not down compress file")
            }
            val renamed = tmpLocation.renameTo(destFile)
            if (renamed == false) {
                throw IOException("Could not move converted file")
            }
            Log.i(TAG, "Image file $original saved at quality $quality")
            return true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to encode image", e)
            for (item in arrayOf(tmpLocation, destFile)) {
                if (item != null) {
                    val result = item.delete()
                    if (result == false) {
                        Log.w(TAG, "Failed to delete image file: " + item.absolutePath)
                    }
                }
            }
        }
        return false
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "Received image from camera")
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val jpegQualityLevelStr = prefs.getString("jpegQuality", "")
            var jpegQualityLevel = 40 // default value
            try {
                jpegQualityLevel = jpegQualityLevelStr!!.toInt()
            } catch (e: NumberFormatException) {
                // If the setting has no value or is otherwise invalid, fall back
                // on a default value
            }
            val JPEG_QUALITY_LEVEL = jpegQualityLevel
            if (resultCode != AppCompatActivity.RESULT_OK || JPEG_QUALITY_LEVEL == 100) {
                if (resultCode != AppCompatActivity.RESULT_OK) {
                    Log.e(TAG, "Failed to create receipt image: $resultCode")
                    // No image was actually created, simply forget the patch
                    capturedUncommittedReceipt = null
                } else {
                    Log.i(TAG, "Image file saved: $capturedUncommittedReceipt")
                }
                onResume()
            } else {
                Log.i(TAG, "Re-encoding image in background")
                val imageConverter: AsyncTask<Void?, Void?, Boolean?> = object : AsyncTask<Void?, Void?, Boolean?>() {

                    var dialog: ProgressDialog? = null
                    override fun onPreExecute() {
                        dialog = ProgressDialog(this@TransactionViewActivity)
                        dialog!!.setMessage(this@TransactionViewActivity.resources.getString(R.string.encodingReceipt))
                        dialog!!.setCancelable(false)
                        dialog!!.setCanceledOnTouchOutside(false)
                        dialog!!.show()
                    }

                    protected override fun doInBackground(vararg p0: Void?): Boolean? {
                        return reencodeImageWithQuality(capturedUncommittedReceipt, JPEG_QUALITY_LEVEL)
                    }

                    override fun onPostExecute(result: Boolean?) {
                        if (result != null && result) {
                            Log.i(TAG, "Image file re-encoded: $capturedUncommittedReceipt")
                        } else {
                            Log.e(TAG, "Failed to re-encode image")
                            // No image was actually created, simply forget the patch
                            capturedUncommittedReceipt = null
                        }
                        dialog!!.hide()
                        onResume()
                    }
                }
                imageConverter.execute()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.size > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted.
                captureReceipt()
            } else {
                // Camera permission rejected, inform user that
                // no receipt can be taken.
                Toast.makeText(applicationContext, R.string.noCameraPermissionError,
                        Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val TAG = "BudgetWatch"
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val PERMISSIONS_REQUEST_CAMERA = 2
        const val ACTION_NEW_EXPENSE = "ActionAddExpense"
        const val ACTION_NEW_REVENUE = "ActionAddRevenue"
    }
}