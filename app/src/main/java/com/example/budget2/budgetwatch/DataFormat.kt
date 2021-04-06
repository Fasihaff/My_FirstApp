package protect.budgetwatch

enum class DataFormat(private val mimetype: String) {
    CSV("text/csv"), JSON("application/json"), ZIP("application/zip");

    /**
     * @return the file extension name for this data format.
     */
    fun extension(): String {
        return name.toLowerCase()
    }

    /**
     * @return the mime type for this data format.
     */
    fun mimetype(): String {
        return mimetype
    }

}