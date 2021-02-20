package one.tunkshif.dictlet.dicts

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import one.tunkshif.dictlet.model.Definition
import one.tunkshif.dictlet.model.Example
import one.tunkshif.dictlet.model.WordResult
import org.jsoup.Jsoup
import java.lang.NullPointerException
import java.util.*

object SpanishDict {
    private const val name: String = "SpanishDict"
    private const val apiUrl: String = "https://www.spanishdict.com/translate/"
    private const val audioApiUrl: String = "https://api.frdic.com/api/v2/speech/speakweb?langid=es&txt=QYN"

    private const val NO_TRANSLATION_TEXT = "no direct translation"

    /**
     * @return raw json data string extracted from the HTML script tag
     */
    private fun getJsonDataFromHtml(query: String): String {
        val doc = Jsoup.connect(apiUrl + query).get()
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
        val mapper = ObjectMapper()
        val neodict = mapper.readTree(getJsonDataFromHtml(query))
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

}