package protect.budgetwatch

import android.database.Cursor
import protect.budgetwatch.DBHelper

class Transaction private constructor(val id: Int, val type: Int, val description: String, val account: String,
                                      val budget: String, val value: Double, val note: String, val dateMs: Long,
                                      val receipt: String) {

    companion object {
        private fun toBlankIfNull(string: String?): String {
            return string ?: ""
        }

        @JvmStatic
        fun toTransaction(cursor: Cursor): Transaction {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.NAME))
            val type = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.TYPE))
            val description = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.DESCRIPTION))
            val account = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.ACCOUNT))
            val budget = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.BUDGET))
            val value = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.VALUE))
            val note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.NOTE))
            val dateMs = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.DATE))
            val receipt = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TransactionDbIds.RECEIPT))
            return Transaction(id, type, toBlankIfNull(description), toBlankIfNull(account),
                    toBlankIfNull(budget), value, toBlankIfNull(note), dateMs,
                    toBlankIfNull(receipt))
        }
    }

}