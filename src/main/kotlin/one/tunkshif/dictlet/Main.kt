package one.tunkshif.dictlet

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import one.tunkshif.dictlet.dicts.SpanishDict
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

    }
}

fun getPort(): Int {
    val port = System.getenv("PORT")
    return port?.toInt() ?: 9000
}