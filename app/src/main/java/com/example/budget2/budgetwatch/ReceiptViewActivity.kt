package protect.budgetwatch

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ShareActionProvider
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.view.MenuItemCompat
import java.io.File

class ReceiptViewActivity : AppCompatActivity() {
    private var receiptFilename: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.receipt_view_activity)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        val b = intent.extras
        receiptFilename = b!!.getString("receipt")
        val receiptView = findViewById<View>(R.id.imageView) as WebView
        receiptView.settings.builtInZoomControls = true
        receiptView.settings.allowFileAccess = true
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val data = "<html><body>" +
                "<img width=\"" + size.x + "\" " +
                "src=\"file://" + receiptFilename + "\"/>" +
                "</body></html>"
        receiptView.loadDataWithBaseURL("", data, "text/html", "utf-8", null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.share_menu, menu)

        // Locate MenuItem with ShareActionProvider
        val item = menu.findItem(R.id.action_share)

        // Fetch ShareActionProvider
        val shareActionProvider = MenuItemCompat.getActionProvider(item) as ShareActionProvider
        if (shareActionProvider == null) {
            Log.w(TAG, "Failed to find share action provider")
            return false
        }
        if (receiptFilename == null) {
            Log.w(TAG, "No receipt to share")
            return false
        }
        val shareIntent = Intent(Intent.ACTION_SEND)

        // Determine mimetype of image
        val opt = BitmapFactory.Options()
        opt.inJustDecodeBounds = true
        BitmapFactory.decodeFile(receiptFilename, opt)
        shareIntent.type = opt.outMimeType
        val outputUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, File(receiptFilename))
        shareIntent.putExtra(Intent.EXTRA_STREAM, outputUri)

        // set flag to give temporary permission to external app to use the FileProvider
        shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        shareActionProvider.setShareIntent(shareIntent)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG = "BudgetWatch"
    }
}