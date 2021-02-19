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
            ctx.json(RequestResult(
                success = false,
                message = e.message ?: "Null Pointer Exception",
                result = null
            ))
        }
        exception(SocketTimeoutException::class.java) { e, ctx ->
            ctx.json(RequestResult(
                success = false,
                message = e.message ?: "Connection Timed Out",
                result = null
            ))
        }
        error(404) { ctx ->
            ctx.json(RequestResult(
                success = false,
                message = "404 NOT FOUND",
                result = null
            ))
        }
    }.start(getPort())

    app.routes {
        get("/spanishdict/:query") { ctx ->
            val query = ctx.pathParam("query")
            val isAbbr = ctx.queryParam<Boolean>("isAbbr", "true").get()
            val showGender = ctx.queryParam<Boolean>("showGender", "true").get()
            val result = SpanishDict.getWordResult(query, isAbbr, showGender)
            ctx.json(RequestResult(result = result))
        }
    }
}

fun getPort(): Int {
    val port = System.getenv("PORT")
    return port?.toInt() ?: 9000
}