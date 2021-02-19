package one.tunkshif.dictlet.model

data class RequestResult(
    val success: Boolean = true,
    val message: String = "success",
    val result: WordResult?
)