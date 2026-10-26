package com.algarit.taskflow.api

import android.content.Context

// Guarda quem está com a app aberta (o mesmo papel que o localStorage faz no site).
object Sessao {
    private const val PREFS = "taskflow_sessao"

    var utilizadorId: Int = -1
    var nome: String = ""
    var email: String = ""

    fun carregar(contexto: Context) {
        val p = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        utilizadorId = p.getInt("id", -1)
        nome = p.getString("nome", "") ?: ""
        email = p.getString("email", "") ?: ""
    }

    fun guardar(contexto: Context, id: Int, nome: String, email: String) {
        utilizadorId = id; this.nome = nome; this.email = email
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("id", id).putString("nome", nome).putString("email", email).apply()
    }

    fun estaLogado(): Boolean = utilizadorId > 0

    fun sair(contexto: Context) {
        utilizadorId = -1; nome = ""; email = ""
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
