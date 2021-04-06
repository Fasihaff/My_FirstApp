package protect.budgetwatch

import java.util.*

internal object CalendarUtil {
    fun getStartOfMonthMs(year: Int, month: Int): Long {
        val date = Calendar.getInstance()
        date[Calendar.YEAR] = year
        date[Calendar.MONTH] = month
        date[Calendar.DAY_OF_MONTH] = date.getActualMinimum(Calendar.DAY_OF_MONTH)
        date[Calendar.HOUR_OF_DAY] = date.getActualMinimum(Calendar.HOUR_OF_DAY)
        date[Calendar.MINUTE] = date.getActualMinimum(Calendar.MINUTE)
        date[Calendar.SECOND] = date.getActualMinimum(Calendar.SECOND)
        date[Calendar.MILLISECOND] = date.getActualMinimum(Calendar.MILLISECOND)
        return date.timeInMillis
    }

    fun getEndOfMonthMs(year: Int, month: Int): Long {
        val date = Calendar.getInstance()
        date[Calendar.YEAR] = year
        date[Calendar.MONTH] = month
        date[Calendar.DAY_OF_MONTH] = date.getActualMaximum(Calendar.DAY_OF_MONTH)
        date[Calendar.HOUR_OF_DAY] = date.getActualMaximum(Calendar.HOUR_OF_DAY)
        date[Calendar.MINUTE] = date.getActualMaximum(Calendar.MINUTE)
        date[Calendar.SECOND] = date.getActualMaximum(Calendar.SECOND)
        date[Calendar.MILLISECOND] = date.getActualMaximum(Calendar.MILLISECOND)
        return date.timeInMillis
    }

    @JvmStatic
    fun getStartOfDayMs(year: Int, month: Int, day: Int): Long {
        val date = Calendar.getInstance()
        date[Calendar.YEAR] = year
        date[Calendar.MONTH] = month
        date[Calendar.DAY_OF_MONTH] = day
        date[Calendar.HOUR_OF_DAY] = date.getActualMinimum(Calendar.HOUR_OF_DAY)
        date[Calendar.MINUTE] = date.getActualMinimum(Calendar.MINUTE)
        date[Calendar.SECOND] = date.getActualMinimum(Calendar.SECOND)
        date[Calendar.MILLISECOND] = date.getActualMinimum(Calendar.MILLISECOND)
        return date.timeInMillis
    }

    @JvmStatic
    fun getEndOfDayMs(year: Int, month: Int, day: Int): Long {
        val date = Calendar.getInstance()
        date[Calendar.YEAR] = year
        date[Calendar.MONTH] = month
        date[Calendar.DAY_OF_MONTH] = day
        date[Calendar.HOUR_OF_DAY] = date.getActualMaximum(Calendar.HOUR_OF_DAY)
        date[Calendar.MINUTE] = date.getActualMaximum(Calendar.MINUTE)
        date[Calendar.SECOND] = date.getActualMaximum(Calendar.SECOND)
        date[Calendar.MILLISECOND] = date.getActualMaximum(Calendar.MILLISECOND)
        return date.timeInMillis
    }
}