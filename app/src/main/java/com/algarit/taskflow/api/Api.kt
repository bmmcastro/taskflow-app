package com.algarit.taskflow.api

import com.algarit.taskflow.R
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

// Converte as exceções das coroutines em mensagens para o utilizador.
// O contexto vem da Application (com.algarit.taskflow.TaskFlowApp).
fun erroAmigavel(t: Throwable): String {
    val contexto = com.algarit.taskflow.TaskFlowApp.contexto
    return when (t) {
        is SocketTimeoutException -> contexto.getString(R.string.erro_rede)
        is IOException -> contexto.getString(R.string.erro_rede)
        is HttpException -> {
            val corpo = try { t.response()?.errorBody()?.string()?.trim() } catch (_: Exception) { null }
            if (!corpo.isNullOrEmpty() && corpo.length <= 150 && !corpo.startsWith("<")) corpo
            else contexto.getString(R.string.erro_rede)
        }
        else -> t.message ?: contexto.getString(R.string.erro_rede)
    }
}
