package com.algarit.taskflow.api

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

// Cliente único para falar com a API do TaskFlow (a mesma do site taskflow.algarit.pt).
object Api {
    const val BASE = "https://taskflow.algarit.pt"

    private lateinit var fila: RequestQueue

    fun iniciar(contexto: Context) {
        if (!::fila.isInitialized) fila = Volley.newRequestQueue(contexto.applicationContext)
    }

    // O que a API devolve: código HTTP, texto e (se for JSON) o objeto ou lista.
    class Resposta(val sucesso: Boolean, val codigo: Int, val texto: String, val json: Any?) {
        val objeto: JSONObject? get() = json as? JSONObject
        val lista: JSONArray? get() = json as? JSONArray
    }

    fun get(caminho: String, params: Map<String, String> = emptyMap(), cb: (Resposta) -> Unit) {
        enviar(Request.Method.GET, url(caminho, params), null, cb)
    }

    fun post(caminho: String, corpo: JSONObject, cb: (Resposta) -> Unit) {
        enviar(Request.Method.POST, url(caminho), corpo, cb)
    }

    fun patch(caminho: String, corpo: JSONObject, cb: (Resposta) -> Unit) {
        enviar(Request.Method.PATCH, url(caminho), corpo, cb)
    }

    fun delete(caminho: String, cb: (Resposta) -> Unit) {
        enviar(Request.Method.DELETE, url(caminho), null, cb)
    }

    private fun url(caminho: String, params: Map<String, String> = emptyMap()): String {
        val sb = StringBuilder(BASE).append(caminho)
        if (params.isNotEmpty()) {
            sb.append('?')
            params.entries.forEachIndexed { i, (chave, valor) ->
                if (i > 0) sb.append('&')
                sb.append(URLEncoder.encode(chave, "UTF-8")).append('=')
                    .append(URLEncoder.encode(valor, "UTF-8"))
            }
        }
        return sb.toString()
    }

    private fun enviar(metodo: Int, url: String, corpo: JSONObject?, cb: (Resposta) -> Unit) {
        val pedido = object : StringRequest(metodo, url,
            { texto -> cb(Resposta(true, 200, texto, interpretar(texto))) },
            { erro ->
                val codigo = erro.networkResponse?.statusCode ?: 0
                val texto = erro.networkResponse?.data?.toString(Charsets.UTF_8) ?: (erro.message ?: "Erro de rede")
                cb(Resposta(false, codigo, texto, interpretar(texto)))
            }) {
            override fun getBody(): ByteArray = corpo?.toString()?.toByteArray(Charsets.UTF_8) ?: super.getBody()
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
        }
        fila.add(pedido)
    }

    // A API às vezes devolve JSON (objetos ou listas) e às vezes só texto simples.
    private fun interpretar(texto: String): Any = try {
        JSONObject(texto)
    } catch (_: Exception) {
        try { JSONArray(texto) } catch (_: Exception) { texto }
    }
}
