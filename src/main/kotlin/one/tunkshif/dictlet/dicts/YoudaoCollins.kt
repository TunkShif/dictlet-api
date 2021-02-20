package one.tunkshif.dictlet.dicts

import one.tunkshif.dictlet.model.Definition
import one.tunkshif.dictlet.model.Example
import one.tunkshif.dictlet.model.WordResult
import org.jsoup.Jsoup
import java.lang.NullPointerException

object YoudaoCollins {
    const val name = "Youdao Collins EN-CN"
    private const val wordQueryApiUrl = "http://m.youdao.com/singledict?q=%s&dict=collins&le=eng&more=false"
    private const val audioApiUrl = "https://dict.youdao.com/dictvoice?audio=%s&type=%d"

    private fun splitEnglishAndChinese(str: String): List<String> {
        val matcher = """([\w .-_,"']+)(\(?.+)""".toRegex()
        return matcher.find(str)!!.groupValues.takeLast(2)
    }

    fun getWordResult(query: String, accentType: Int = 2, isPosAbbr: Boolean = true): WordResult {
        val doc = Jsoup.connect(wordQueryApiUrl.format(query)).get()
        doc.selectFirst(".per-collins-entry") ?: throw NullPointerException("Cannot find the word $query in $name")

        val definitions = mutableListOf<Definition>()
        doc.select(".per-collins-entry > ul > li.mcols-layout > div.col2")
            .filter { it.selectFirst("span").attr("title").isNotEmpty() }
            .forEach { defItem ->
                val pos = defItem.selectFirst("span").let {
                    it.remove()
                    if (isPosAbbr) it.attr("title") else it.text()
                }
                val examples = mutableListOf<Example>()
                defItem.select("div.mcols-layout")
                    .takeIf { it.isNotEmpty() }
                    ?.forEach { sentItem ->
                        sentItem.select("div.col2").forEach {
                            val sentence = it.select("p")
                            Example(
                                example = sentence.first().text(),
                                exampleTranslation = sentence.last().text()
                            ) addToList examples
                        }
                        sentItem.remove()
                    }
                val sense = splitEnglishAndChinese(defItem.text().split("→").first().trim())
                Definition(
                    pos = pos,
                    sense = sense.first().trim(),
                    senseTranslation = sense.last(),
                    examples = examples
                ) addToList definitions
            }

        return WordResult(
            word = doc.selectFirst("h4 > span.title").text(),
            query = query,
            phonetics = doc.selectFirst("h4 > em.phonetic").text(),
            audioUrl = audioApiUrl.format(query, accentType),
            definitions = definitions
        )
    }
}