package one.tunkshif.dictlet.model

import com.fasterxml.jackson.annotation.JsonProperty

data class WordResult(
    val word: String,
    val query: String,
    val phonetics: String? = null,
    val audioUrl: String? = null,
    val definitions: MutableList<Definition>
)

data class Definition(
    val pos: String? = null,
    val sense: String,
    val senseTranslation: String? = null,
    val examples: MutableList<Example> = mutableListOf()
) {
    infix fun addToList(list: MutableList<Definition>) = list.add(this)
}

data class Example(
    @JsonProperty("textEs") val example: String,
    @JsonProperty("textEn") val exampleTranslation: String?
) {
    infix fun addToList(list: MutableList<Example>) = list.add(this)
}