package one.tunkshif.dictlet

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import one.tunkshif.dictlet.dicts.SpanishDict
import one.tunkshif.dictlet.dicts.YoudaoCollins
import one.tunkshif.dictlet.model.RequestResult
import java.net.SocketTimeoutException

fun main() {
    val app = Javalin.create().apply {

        config.addStaticFiles("static/")

        exception(NullPointerException::class.java) { e, ctx ->
            ctx.json(
                RequestResult(
                    status = ctx.status(),
                    success = false,
                    message = e.message ?: "Null Pointer Exception",
                    result = null
                )
            )
        }

        exception(SocketTimeoutException::class.java) { e, ctx ->
            ctx.json(
                RequestResult(
                    status = ctx.status(),
                    success = false,
                    message = e.message ?: "Connection Timed Out",
                    result = null
                )
            )
        }

        error(404) { ctx ->
            ctx.json(
                RequestResult(
                    status = ctx.status(),
                    success = false,
                    message = "404 NOT FOUND",
                    result = null
                )
            )
        }

        error(500) { ctx ->
            ctx.json(
                RequestResult(
                    status = ctx.status(),
                    success = false,
                    message = "Internal Server Error",
                    result = null
                )
            )
        }
    }.start(getPort())

    app.routes {
        get("/spanishdict/query/:query") { ctx ->
            val query = ctx.pathParam("query")
            val isPosAbbr = ctx.queryParam<Boolean>("isPosAbbr", "true").get()
            val showGender = ctx.queryParam<Boolean>("showGender", "true").get()
            val result = SpanishDict.getWordResult(query, isPosAbbr, showGender)
            ctx.json(RequestResult(status = ctx.status(), result = result))
        }

        get("/spanishdict/conjugation/:query") { ctx ->
            val query = ctx.pathParam("query")
            val result = SpanishDict.getConjugationList(query)
            ctx.json(RequestResult(status = ctx.status(), result = result))
        }

        get("/youdao-collins/query/:query") { ctx ->
            val query = ctx.pathParam("query")
            val accentType = ctx.queryParam<Int>("accentType", "2")
                .check({ it in 1..2 }, "Available Options: 1 for PR and 2 for GA")
                .takeUnless { it.hasError() }?.get() ?: 2
            val isPosAbbr = ctx.queryParam<Boolean>("isPosAbbr", "true").get()
            val result = YoudaoCollins.getWordResult(query, accentType = accentType, isPosAbbr = isPosAbbr)
            ctx.json(RequestResult(status = ctx.status(), result = result))
        }
    }
}

fun getPort(): Int {
    val port = System.getenv("PORT")
    return port?.toInt() ?: 9000
}