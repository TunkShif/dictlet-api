package one.tunkshif.dictlet.model

data class RequestResult(
    val status: Int,
    val success: Boolean = true,
    val message: String = "success",
    val result: Any?
)