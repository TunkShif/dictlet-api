package one.tunkshif.dictlet.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class Conjugation(
    val infinitive: String,
    val presentParticiple: String?,
    val pastParticiple: String,
    val gerund: String,
    val paradigms: MutableList<Tense>
)

data class Tense(
    val tense: String,
    val wordList: MutableList<Word>
) {
    infix fun addToList(list: MutableList<Tense>) = list.add(this)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Word(
    @JsonProperty("word") val word: String,
    @JsonProperty("pronoun") val pronoun: String?,
    @JsonProperty("isIrregular") val isIrregular: Boolean = false
)