@file:Suppress("DEPRECATION")

package protect.budgetwatch

import android.app.Activity
import android.app.ProgressDialog

/**
 * An interface for communicating the progress
 * of exporting data to a file.
 */
open class ImportExportProgressUpdater(private val activity: Activity, private val dialog: ProgressDialog, private val baseMessage: String) {
    private var totalEntries: Int? = null
    private var entriesMoved = 0
    private var lastUpdateTimeMs: Long = 0
    open fun setTotal(totalEntries: Int) {
        this.totalEntries = totalEntries
    }

    /**
     * When a single record is exported or imported, either a budget or a
     * transaction, this should be invoked to update the UI.
     */
    open fun update() {
        entriesMoved += 1

        // So we do not spend all of our time updating the message,
        // only post an update periodically.
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTimeMs >= UPDATE_INTERVAL ||
                totalEntries != null && entriesMoved == totalEntries) {
            lastUpdateTimeMs = currentTime
            activity.runOnUiThread {
                val formatted: String
                formatted = if (totalEntries != null) {
                    String.format(baseMessage, entriesMoved, totalEntries)
                } else {
                    String.format(baseMessage, entriesMoved)
                }
                dialog.setMessage(formatted)
            }
        }
    }

    companion object {
        private const val UPDATE_INTERVAL: Long = 250
    }

}