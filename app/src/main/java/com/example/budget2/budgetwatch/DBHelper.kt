package protect.budgetwatch

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

/**
 * Helper class for managing data in the database
 */
class DBHelper(private val _context: Context) : SQLiteOpenHelper(_context, DATABASE_NAME, null, DATABASE_VERSION) {
    /**
     * All strings used with the budget table
     */
    internal object BudgetDbIds {
        const val TABLE = "budgets"
        const val NAME = "_id"
        const val MAX = "max"
    }

    /**
     * All strings used in the transaction table
     */
    internal object TransactionDbIds {
        const val TABLE = "transactions"
        const val NAME = "_id"
        const val TYPE = "type"
        const val DESCRIPTION = "description"
        const val ACCOUNT = "account"
        const val BUDGET = "budget"
        const val VALUE = "value"
        const val NOTE = "note"
        const val DATE = "date"
        const val RECEIPT = "receipt"
        const val EXPENSE = 1
        const val REVENUE = 2
    }

    /**
     * Send a notification that the transaction database has changed
     */
    private fun sendChangeNotification() {
        _context.sendBroadcast(Intent(TransactionDatabaseChangedReceiver.ACTION_DATABASE_CHANGED))
    }

    override fun onCreate(db: SQLiteDatabase) {
        // create table for budgets
        db.execSQL(
                "create table  " + BudgetDbIds.TABLE + "(" +
                        BudgetDbIds.NAME + " text primary key," +
                        BudgetDbIds.MAX + " INTEGER not null)")
        // create table for transactions
        db.execSQL("create table " + TransactionDbIds.TABLE + "(" +
                TransactionDbIds.NAME + " INTEGER primary key autoincrement," +
                TransactionDbIds.TYPE + " INTEGER not null," +
                TransactionDbIds.DESCRIPTION + " TEXT not null," +
                TransactionDbIds.ACCOUNT + " TEXT," +
                TransactionDbIds.BUDGET + " TEXT," +
                TransactionDbIds.VALUE + " REAL not null," +
                TransactionDbIds.NOTE + " TEXT," +
                TransactionDbIds.DATE + " INTEGER not null," +
                TransactionDbIds.RECEIPT + " TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Upgrade from version 1 to version 2
        if (oldVersion < 2 && newVersion >= 2) {
            db.execSQL("ALTER TABLE " + TransactionDbIds.TABLE
                    + " ADD COLUMN " + TransactionDbIds.RECEIPT + " TEXT")
        }
    }

    /**
     * Insert a budget into the database.
     *
     * @param name
     * name of the budget
     * @param max
     * the value of the budget, per month
     * @return true if the insertion was successful,
     * false otherwise
     */
    fun insertBudget(name: String?, max: Int): Boolean {
        val db = writableDatabase
        val result = insertBudget(db, name, max)
        db.close()
        return result
    }

    /**
     * Insert a budget into the database, using a provided
     * writable database instance. This is useful if
     * multiple insertions will occur in the same transaction.
     *
     * @param writableDb
     * writable database instance to use
     * @param name
     * name of the budget
     * @param max
     * the value of the budget, per month
     * @return true if the insertion was successful,
     * false otherwise
     */
    fun insertBudget(writableDb: SQLiteDatabase, name: String?, max: Int): Boolean {
        val contentValues = ContentValues()
        contentValues.put(BudgetDbIds.NAME, name)
        contentValues.put(BudgetDbIds.MAX, max)
        val newId = writableDb.insert(BudgetDbIds.TABLE, null, contentValues)
        return newId != -1L
    }

    /**
     * Update the budget value of a given budget in the database
     *
     * @param name
     * name of the budget to update
     * @param max
     * updated budget value to commit
     * @return true if the provided budget exists and the value
     * was successfully updated, false otherwise.
     */
    fun updateBudget(name: String, max: Int): Boolean {
        val contentValues = ContentValues()
        contentValues.put(BudgetDbIds.MAX, max)
        val db = writableDatabase
        val rowsUpdated = db.update(BudgetDbIds.TABLE, contentValues, BudgetDbIds.NAME + "=?", arrayOf(name))
        db.close()
        return rowsUpdated == 1
    }

    /**
     * Delete a given budget from the database
     *
     * @param name
     * name of the budget to delete
     * @return if the budget was successfully deleted,
     * false otherwise
     */
    fun deleteBudget(name: String): Boolean {
        val db = writableDatabase
        val rowsDeleted = db.delete(BudgetDbIds.TABLE,
                BudgetDbIds.NAME + " = ? ", arrayOf(name))
        db.close()
        return rowsDeleted == 1
    }

    /**
     * Get Budget object for the named budget in the database,
     * but only fill in the 'name' and 'max' fields;
     * the value of other fields is undefined.
     *
     * @param name
     * name of the budget to query
     * @return Budget object representing the named budget,
     * or null if it could not be queried
     */
    fun getBudgetStoredOnly(name: String): Budget? {
        val db = readableDatabase
        val data = db.rawQuery("select * from " + BudgetDbIds.TABLE +
                " where " + BudgetDbIds.NAME + "=?", arrayOf(name))
        var budget: Budget? = null
        if (data.count == 1) {
            data.moveToFirst()
            val goalName = data.getString(data.getColumnIndexOrThrow(BudgetDbIds.NAME))
            val goalMax = data.getInt(data.getColumnIndexOrThrow(BudgetDbIds.MAX))
            budget = Budget(goalName, goalMax, 0)
        }
        data.close()
        db.close()
        return budget
    }

    /**
     * Get Budget objects for each budget in the database,
     * with the 'current' field filled out from all transactions
     * between the provided dates.
     *
     * @param startDateMs
     * first date in milliseconds for transactions to compute
     * into the 'current' field.
     * @param endDateMs
     * last date in milliseconds for transactions to compute
     * into the 'current' field.
     * @return list of Budget objects, or an empty field if none
     * could be found.
     */
    fun getBudgets(startDateMs: Long, endDateMs: Long): List<Budget> {
        val db = readableDatabase
        val TOTAL_EXPENSE_COL = "total_expense"
        val TOTAL_REVENUE_COL = "total_revenue"
        val BUDGET_ID = BudgetDbIds.TABLE + "." + BudgetDbIds.NAME
        val BUDGET_MAX = BudgetDbIds.TABLE + "." + BudgetDbIds.MAX
        val TRANS_VALUE = TransactionDbIds.TABLE + "." + TransactionDbIds.VALUE
        val TRANS_TYPE = TransactionDbIds.TABLE + "." + TransactionDbIds.TYPE
        val TRANS_DATE = TransactionDbIds.TABLE + "." + TransactionDbIds.DATE
        val TRANS_BUDGET = TransactionDbIds.TABLE + "." + TransactionDbIds.BUDGET
        val data = db.rawQuery("select " + BUDGET_ID + ", " + BUDGET_MAX + ", " +
                "(select total(" + TRANS_VALUE + ") from " + TransactionDbIds.TABLE + " where " +
                BUDGET_ID + " = " + TRANS_BUDGET + " and " +
                TRANS_TYPE + " = ? and " +
                TRANS_DATE + " >= ? and " +
                TRANS_DATE + " <= ?) " +
                "as " + TOTAL_EXPENSE_COL + ", " +
                "(select total(" + TRANS_VALUE + ") from " + TransactionDbIds.TABLE + " where " +
                BUDGET_ID + " = " + TRANS_BUDGET + " and " +
                TRANS_TYPE + " = ? and " +
                TRANS_DATE + " >= ? and " +
                TRANS_DATE + " <= ?) " +
                "as " + TOTAL_REVENUE_COL + " " +
                "from " + BudgetDbIds.TABLE + " order by " + BUDGET_ID, arrayOf(
                Integer.toString(TransactionDbIds.EXPENSE),
                java.lang.Long.toString(startDateMs),
                java.lang.Long.toString(endDateMs),
                Integer.toString(TransactionDbIds.REVENUE),
                java.lang.Long.toString(startDateMs),
                java.lang.Long.toString(endDateMs)
        ))
        val budgets = LinkedList<Budget>()

        // Determine over how many months the budgets represent.
        // Adjust the budget max to match the number of months
        // represented.
        val date = Calendar.getInstance()
        date.timeInMillis = startDateMs
        val MONTHS_PER_YEAR = 12
        val startMonths = date[Calendar.YEAR] * MONTHS_PER_YEAR + date[Calendar.MONTH]
        date.timeInMillis = endDateMs
        val endMonths = date[Calendar.YEAR] * MONTHS_PER_YEAR + date[Calendar.MONTH]
        val totalMonthsInRange = endMonths - startMonths + 1
        if (data.moveToFirst()) {
            do {
                val name = data.getString(data.getColumnIndexOrThrow(BudgetDbIds.NAME))
                val max = data.getInt(data.getColumnIndexOrThrow(BudgetDbIds.MAX)) * totalMonthsInRange
                val expenses = data.getDouble(data.getColumnIndexOrThrow(TOTAL_EXPENSE_COL))
                val revenues = data.getDouble(data.getColumnIndexOrThrow(TOTAL_REVENUE_COL))
                val current = expenses - revenues
                val currentRounded = Math.ceil(current).toInt()
                budgets.add(Budget(name, max, currentRounded))
            } while (data.moveToNext())
        }
        data.close()
        db.close()
        return budgets
    }

    /**
     * Get Budget object representing transactions which
     * have no budget, e.g. the budget is blank. The 'current' field
     * will be filled out from all transactions between the provided
     * dates, and the 'max' field is left at 0.
     *
     * @param startDateMs
     * first date in milliseconds for transactions to compute
     * into the 'current' field.
     * @param endDateMs
     * last date in milliseconds for transactions to compute
     * into the 'current' field.
     * @return Budget object
     */
    fun getBlankBudget(startDateMs: Long, endDateMs: Long): Budget {
        val db = readableDatabase
        val TOTAL_EXPENSE_COL = "total_expense"
        val TOTAL_REVENUE_COL = "total_revenue"
        val TRANS_VALUE = TransactionDbIds.TABLE + "." + TransactionDbIds.VALUE
        val TRANS_TYPE = TransactionDbIds.TABLE + "." + TransactionDbIds.TYPE
        val TRANS_DATE = TransactionDbIds.TABLE + "." + TransactionDbIds.DATE
        val TRANS_BUDGET = TransactionDbIds.TABLE + "." + TransactionDbIds.BUDGET
        val data = db.rawQuery("select " +
                "(select total(" + TRANS_VALUE + ") from " + TransactionDbIds.TABLE + " where " +
                TRANS_BUDGET + " = '' and " +
                TRANS_TYPE + " = ? and " +
                TRANS_DATE + " >= ? and " +
                TRANS_DATE + " <= ?) " +
                "as " + TOTAL_EXPENSE_COL + ", " +
                "(select total(" + TRANS_VALUE + ") from " + TransactionDbIds.TABLE + " where " +
                TRANS_BUDGET + " = '' and " +
                TRANS_TYPE + " = ? and " +
                TRANS_DATE + " >= ? and " +
                TRANS_DATE + " <= ?) " +
                "as " + TOTAL_REVENUE_COL, arrayOf(
                Integer.toString(TransactionDbIds.EXPENSE),
                java.lang.Long.toString(startDateMs),
                java.lang.Long.toString(endDateMs),
                Integer.toString(TransactionDbIds.REVENUE),
                java.lang.Long.toString(startDateMs),
                java.lang.Long.toString(endDateMs)
        ))
        var total = 0
        if (data.moveToFirst()) {
            val expenses = data.getInt(data.getColumnIndexOrThrow(TOTAL_EXPENSE_COL))
            val revenues = data.getInt(data.getColumnIndexOrThrow(TOTAL_REVENUE_COL))
            total = expenses - revenues
        }
        data.close()
        db.close()
        return Budget("", 0, total)
    }

    /**
     * @return list of all budget names in the database
     */
    val budgetNames: List<String>
        get() {
            val db = readableDatabase
            val data = db.rawQuery("select " + BudgetDbIds.NAME + " from " + BudgetDbIds.TABLE +
                    " ORDER BY " + BudgetDbIds.NAME, null)
            val budgetNames = LinkedList<String>()
            if (data.moveToFirst()) {
                do {
                    val name = data.getString(data.getColumnIndexOrThrow(BudgetDbIds.NAME))
                    budgetNames.add(name)
                } while (data.moveToNext())
            }
            data.close()
            db.close()
            return budgetNames
        }

    /**
     * @return the number of budgets in the database
     */
    val budgetCount: Int
        get() {
            val db = readableDatabase
            val data = db.rawQuery("SELECT Count(*) FROM " + BudgetDbIds.TABLE, null)
            var numItems = 0
            if (data.count == 1) {
                data.moveToFirst()
                numItems = data.getInt(0)
            }
            data.close()
            db.close()
            return numItems
        }

    /**
     * Insert a transaction into the database.
     *
     * @return true if the insertion was successful,
     * false otherwise
     */
    fun insertTransaction(type: Int, description: String?, account: String?, budget: String?,
                          value: Double, note: String?, dateInMs: Long, receipt: String?): Boolean {
        val contentValues = ContentValues()
        contentValues.put(TransactionDbIds.TYPE, type)
        contentValues.put(TransactionDbIds.DESCRIPTION, description)
        contentValues.put(TransactionDbIds.ACCOUNT, account)
        contentValues.put(TransactionDbIds.BUDGET, budget)
        contentValues.put(TransactionDbIds.VALUE, value)
        contentValues.put(TransactionDbIds.NOTE, note)
        contentValues.put(TransactionDbIds.DATE, dateInMs)
        contentValues.put(TransactionDbIds.RECEIPT, receipt)
        val db = writableDatabase
        val newId = db.insert(TransactionDbIds.TABLE, null, contentValues)
        db.close()
        if (newId != -1L) {
            sendChangeNotification()
        }
        return newId != -1L
    }

    /**
     * Insert a transaction into the database, using a provided
     * writable database instance. This is useful if
     * multiple insertions will occur in the same transaction.
     *
     * @param writableDb
     * writable database instance to use
     * @return true if the insertion was successful,
     * false otherwise
     */
    fun insertTransaction(writableDb: SQLiteDatabase, id: Int, type: Int, description: String?, account: String?, budget: String?,
                          value: Double, note: String?, dateInMs: Long, receipt: String?): Boolean {
        val contentValues = ContentValues()
        contentValues.put(TransactionDbIds.NAME, id)
        contentValues.put(TransactionDbIds.TYPE, type)
        contentValues.put(TransactionDbIds.DESCRIPTION, description)
        contentValues.put(TransactionDbIds.ACCOUNT, account)
        contentValues.put(TransactionDbIds.BUDGET, budget)
        contentValues.put(TransactionDbIds.VALUE, value)
        contentValues.put(TransactionDbIds.NOTE, note)
        contentValues.put(TransactionDbIds.DATE, dateInMs)
        contentValues.put(TransactionDbIds.RECEIPT, receipt)
        val newId = writableDb.insert(TransactionDbIds.TABLE, null, contentValues)
        if (newId != -1L) {
            sendChangeNotification()
        }
        return newId != -1L
    }

    /**
     * Update the transaction in the database. The unique ID for
     * the transaction must be provided, all other fields represent
     * the values as will be updated in the database.
     *
     * @param id
     * unique id for the transaction
     * @return true if the provided transaction exists and the value
     * was successfully updated, false otherwise.
     */
    fun updateTransaction(id: Int, type: Int, description: String?,
                          account: String?, budget: String?, value: Double,
                          note: String?, dateInMs: Long, receipt: String?): Boolean {
        val contentValues = ContentValues()
        contentValues.put(TransactionDbIds.TYPE, type)
        contentValues.put(TransactionDbIds.DESCRIPTION, description)
        contentValues.put(TransactionDbIds.ACCOUNT, account)
        contentValues.put(TransactionDbIds.BUDGET, budget)
        contentValues.put(TransactionDbIds.VALUE, value)
        contentValues.put(TransactionDbIds.NOTE, note)
        contentValues.put(TransactionDbIds.DATE, dateInMs)
        contentValues.put(TransactionDbIds.RECEIPT, receipt)
        val db = writableDatabase
        val rowsUpdated = db.update(TransactionDbIds.TABLE, contentValues,
                TransactionDbIds.NAME + "=?", arrayOf(Integer.toString(id)))
        db.close()
        if (rowsUpdated == 1) {
            sendChangeNotification()
        }
        return rowsUpdated == 1
    }

    /**
     * Get Transaction object for the named transaction in the database,
     *
     * @param id
     * id of the transaction to query
     * @return Transaction object representing the named transaction,
     * or null if it could not be queried
     */
    fun getTransaction(id: Int): Transaction? {
        val db = readableDatabase
        val data = db.rawQuery("select * from " + TransactionDbIds.TABLE +
                " where " + TransactionDbIds.NAME + "=?", arrayOf(Integer.toString(id)))
        var transaction: Transaction? = null
        if (data.count == 1) {
            data.moveToFirst()
            transaction = Transaction.toTransaction(data)
        }
        data.close()
        db.close()
        return transaction
    }

    /**
     * Returns the number of transactions in the database
     * of the provided type.
     *
     * @param type
     * transaction type to query, either EXPENSE or
     * REVENUE
     * @return the number of transactions in the database
     * of the given type
     */
    fun getTransactionCount(type: Int): Int {
        val db = readableDatabase
        val data = db.rawQuery("SELECT Count(*) FROM " + TransactionDbIds.TABLE +
                " where " + TransactionDbIds.TYPE + "=?", arrayOf(Integer.toString(type)))
        var numItems = 0
        if (data.count == 1) {
            data.moveToFirst()
            numItems = data.getInt(0)
        }
        data.close()
        db.close()
        return numItems
    }

    /**
     * Delete a given transaction from the database
     *
     * @param id
     * id of the transaction to delete
     * @return if the transaction was successfully deleted,
     * false otherwise
     */
    fun deleteTransaction(id: Int): Boolean {
        val db = writableDatabase
        val rowsDeleted = db.delete(TransactionDbIds.TABLE,
                TransactionDbIds.NAME + " = ? ", arrayOf(Integer.toString(id)))
        db.close()
        if (rowsDeleted == 1) {
            sendChangeNotification()
        }
        return rowsDeleted == 1
    }

    /**
     * Returns a cursor pointing to all transaction which are
     * either expenses or revenues (depends on type) from the
     * provided budget (if not null) and meet the query (if not null).
     *
     * @param type
     * the type of transaction to query
     * @param budget
     * if not null, all returned expenses will be from this budget.
     * @param search
     * if not null, all returned expenses will have at least one field
     * which contains this query string
     */
    fun getTransactions(type: Int, budget: String?, search: String?, startDateMs: Long?, endDateMs: Long?): Cursor {
        val db = readableDatabase
        val args = LinkedList<String>()
        var query = "select * from " + TransactionDbIds.TABLE + " where " +
                TransactionDbIds.TYPE + "=" + type
        if (budget != null) {
            query += " AND " + TransactionDbIds.BUDGET + "=?"
            args.addLast(budget)
        }
        if (search != null) {
            query += " AND ( "
            val items = arrayOf(TransactionDbIds.DESCRIPTION, TransactionDbIds.ACCOUNT,
                    TransactionDbIds.VALUE, TransactionDbIds.NOTE)
            for (index in items.indices) {
                query += "( " + items[index] + " LIKE ? )"
                if (index < items.size - 1) {
                    query += " OR "
                }
                args.addLast("%$search%")
            }
            query += " )"
        }
        if (startDateMs != null && endDateMs != null) {
            query += " AND " + TransactionDbIds.DATE + " >= ? AND " +
                    TransactionDbIds.DATE + " <= ?"
            args.addLast(java.lang.Long.toString(startDateMs))
            args.addLast(java.lang.Long.toString(endDateMs))
        }
        query += " ORDER BY " + TransactionDbIds.DATE + " DESC"
        val argArray = args.toTypedArray()
        return db.rawQuery(query, argArray)
    }

    /**
     * @return Cursor pointing to all expense transactions
     * in the database
     */
    val expenses: Cursor
        get() = getTransactions(TransactionDbIds.EXPENSE, null, null, null, null)

    /**
     * @return Cursor pointing to all revenue transactions
     * in the database
     */
    val revenues: Cursor
        get() = getTransactions(TransactionDbIds.REVENUE, null, null, null, null)

    /**
     * Returns a Cursor pointing to all transactions in the database
     * with a receipt. If the provided endDate is not null, further
     * restricts returned transactions to have occurred on or before
     * the given date.
     *
     * @param endDate
     * date to limit transactions by; if not null will only
     * returns transactions on or before the given date.
     * @return Cursor pointing to transactions with receipts.
     */
    fun getTransactionsWithReceipts(endDate: Long?): Cursor {
        val argList: MutableList<String> = ArrayList()
        if (endDate != null) {
            argList.add(endDate.toString())
        }
        val args = argList.toTypedArray()
        val db = readableDatabase
        return db.rawQuery("select * from " + TransactionDbIds.TABLE + " where " +
                " LENGTH(" + TransactionDbIds.RECEIPT + ") > 0 " +
                if (endDate != null) " AND " + TransactionDbIds.DATE + "<=? " else "",
                args)
    }

    companion object {
        private const val DATABASE_NAME = "BudgetWatch.db"
        const val ORIGINAL_DATABASE_VERSION = 1
        const val DATABASE_VERSION = 2
    }

}