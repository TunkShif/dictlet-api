package one.tunkshif.dictlet.dicts

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import one.tunkshif.dictlet.model.*
import org.jsoup.Jsoup
import java.lang.NullPointerException
import java.util.*

object SpanishDict {
    const val name: String = "SpanishDict"
    private const val wordQueryApiUrl = "https://www.spanishdict.com/translate/"
    private const val conjugationApiUrl = "https://www.spanishdict.com/conjugate/"
    private const val audioApiUrl = "https://api.frdic.com/api/v2/speech/speakweb?langid=es&txt=QYN"

    private val mapper = ObjectMapper()

    private const val NO_TRANSLATION_TEXT = "no direct translation"

    private fun getJsonDataFromHtml(url: String): String {
        val doc = Jsoup.connect(url).get()
        return doc.select("script").filter {
            it.data().contains("window.SD_COMPONENT_DATA")
        }.takeIf { it.isNotEmpty() }
            ?.first()?.data()
            ?.split("window.SD_COMPONENT_DATA = ")
            ?.last()?.trim()?.trimEnd(';') ?: throw NullPointerException("Cannot find data tag in raw html")
    }

    private fun getAudioUrl(query: String): String =
        audioApiUrl + Base64.getEncoder().encodeToString(query.toByteArray())

    private fun getGender(genderCode: String, isPosAbbr: Boolean): String {
        // TODO: need a better way to deal with gender
        return if (isPosAbbr) {
            when (genderCode) {
                "F", "FX" -> "f"
                "M", "MX" -> "m"
                "CO" -> "m&f"
                "null" -> ""
                else -> ""
            }
        } else {
            when (genderCode) {
                "F", "FX" -> "feminine"
                "M", "MX" -> "masculine"
                "CO" -> "feminine or masculine"
                "null" -> ""
                else -> ""
            }
        }
    }

    fun getWordResult(query: String, isPosAbbr: Boolean = false, showGender: Boolean = true): WordResult {
        val neodict = mapper.readTree(getJsonDataFromHtml(wordQueryApiUrl + query))
            .get("sdDictionaryResultsProps")?.get("entry")?.get("neodict")
            ?: throw NullPointerException("Cannot find the word $query in $name")

        val definitions = mutableListOf<Definition>()
        val posAbbr = if (isPosAbbr) "abbrEn" else "nameEn"

        neodict.forEach { entryItem ->
            entryItem.get("posGroups").forEach { posItem ->
                posItem.get("senses").forEach { senseItem ->
                    val context = senseItem.get("contextEn").asText()
                    var pos = senseItem.get("partOfSpeech").get(posAbbr).asText()

                    senseItem.get("gender").takeIf { !it.isNull && showGender }?.asText()?.let {
                        pos = if (isPosAbbr) "$pos.${getGender(it, isPosAbbr)}" else "$pos ${getGender(it, isPosAbbr)}"
                    }

                    senseItem.get("translations").forEach { translationItem ->
                        val translation = translationItem.get("translation").asText()
                        val sense = if (context.isEmpty()) translation
                        else "$context, ${if (translation.isEmpty()) NO_TRANSLATION_TEXT else translation}"
                        Definition(
                            pos = pos,
                            sense = sense,
                            examples = mapper.readerFor(object : TypeReference<MutableList<Example>>() {})
                                .readValue(translationItem.get("examples"))
                        ) addToList definitions
                    }
                }
            }
        }

        return WordResult(
            word = neodict.get(0).get("subheadword").asText(),
            query = query,
            audioUrl = getAudioUrl(query),
            definitions = definitions
        )
    }

    fun getConjugationList(query: String): Conjugation {
        val conjugation = mapper.readTree(getJsonDataFromHtml(conjugationApiUrl + query))
            .get("verb") ?: throw NullPointerException("Cannot find conjugation for $query in $name")
        val paradigms = mutableListOf<Tense>()
        conjugation.get("paradigms").fields().forEach {
            Tense(
                tense = it.key,
                wordList = mapper.readerFor(object : TypeReference<MutableList<Word>>() {}).readValue(it.value)
            ) addToList paradigms
        }
        return Conjugation(
            infinitive = conjugation["infinitive"].asText(),
            presentParticiple = null,
            pastParticiple = conjugation.get("pastParticiple").get("word").asText(),
            gerund = conjugation.get("gerund").get("word").asText(),
            paradigms = paradigms
        )
    }

}