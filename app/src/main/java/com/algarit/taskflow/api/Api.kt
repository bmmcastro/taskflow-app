package com.algarit.taskflow.api

import com.algarit.taskflow.R
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

// Cliente único Retrofit: um serviço pronto a usar por todos os ViewModels.
object Api {
    const val BASE = "https://taskflow.algarit.pt/"

    val servico: ApiService by lazy {
        val cliente = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// Converte as exceções das coroutines em mensagens distintas para o utilizador.
// A ordem dos ramos conta: o MalformedJsonException também é um IOException,
// por isso é tratado antes do ramo da falta de ligação.
fun erroAmigavel(t: Throwable): String {
    val contexto = com.algarit.taskflow.TaskFlowApp.contexto
    return when (t) {
        // Timeout: passaram os 15 s de ligação ou os 30 s de leitura sem resposta.
        is SocketTimeoutException -> contexto.getString(R.string.erro_timeout)
        // Resposta inválida: o servidor respondeu, mas o corpo não é o JSON esperado
        // (mal formado ou com outra estrutura). Não mostramos o texto técnico do Gson.
        is JsonSyntaxException, is MalformedJsonException, is IllegalStateException ->
            contexto.getString(R.string.erro_resposta)
        // Sem ligação: DNS, rede desligada, ligação recusada.
        is IOException -> contexto.getString(R.string.erro_rede)
        is HttpException -> {
            // O servidor devolve a razão do erro (4xx/5xx) em texto curto; se não vier,
            // ou vier uma página HTML, mostramos a mensagem genérica.
            val corpo = try { t.response()?.errorBody()?.string()?.trim() } catch (_: Exception) { null }
            if (!corpo.isNullOrEmpty() && corpo.length <= 150 && !corpo.startsWith("<")) corpo
            else contexto.getString(R.string.erro_servidor)
        }
        // Qualquer outra falha: mensagem genérica, sem expor a exceção ao utilizador.
        else -> contexto.getString(R.string.erro_servidor)
    }
}
