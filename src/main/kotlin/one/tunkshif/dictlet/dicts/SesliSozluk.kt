package one.tunkshif.dictlet.dicts

import one.tunkshif.dictlet.extension.toKebabStyle
import one.tunkshif.dictlet.model.Definition
import one.tunkshif.dictlet.model.Example
import one.tunkshif.dictlet.model.WordResult
import org.jsoup.Jsoup
import java.lang.NullPointerException

object SesliSozluk {
    const val name = "SesliSözlük"
    private const val wordQueryApiUrl = "https://www.seslisozluk.net/en/what-is-the-meaning-of-%s/"

    fun getWordResult(query: String): WordResult {
        val doc = Jsoup.connect(wordQueryApiUrl.format(query.toKebabStyle())).get()
            .selectFirst("div.panel-body.sozluk") ?: throw NullPointerException("Cannot find the word $query in $name")

        val definitions = mutableListOf<Definition>()
        doc.select("dd.ordered-list").forEach {
            val sense = it.selectFirst("a.definition-link")
            val sentences = it.select("p")
            when {
                sense != null && sentences.isEmpty() -> Definition(sense = sense.text()) addToList definitions
                sense != null && sentences.isNotEmpty() -> {
                    val examples = mutableListOf<Example>()
                    sentences.forEach { sent ->
                        Example(
                            example = sent.select("q").last().text(),
                            exampleTranslation = sent.select("q").first().text()
                        ) addToList examples
                    }
                    Definition(
                        sense = sense.text(),
                        examples = examples
                    ) addToList definitions
                }
                sense == null -> Definition(sense = it.text()) addToList definitions
            }
        }

        return WordResult(
            word = doc.selectFirst("dt > h2 > dfn").text(),
            query = query,
            definitions = definitions
        )
    }
}