package protect.budgetwatch

/**
 * Exception thrown when something unexpected is
 * encountered with the format of data being
 * imported or exported.
 */
internal class FormatException : Exception {
    constructor(message: String?) : super(message) {}
    constructor(message: String?, rootCause: Exception?) : super(message, rootCause) {}
}