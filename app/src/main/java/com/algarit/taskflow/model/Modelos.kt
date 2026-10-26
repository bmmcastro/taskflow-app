package com.algarit.taskflow.model

import org.json.JSONObject

// Um utilizador registado (para login, membros e escolher responsáveis).
data class Utilizador(val id: Int, val nome: String, val email: String) {
    companion object {
        fun deJson(o: JSONObject) = Utilizador(o.optInt("id"), o.optString("nome"), o.optString("email"))
    }
}

// Um projeto de equipa.
data class Projeto(val id: Int, val nome: String, val descricao: String, val criadorId: Int) {
    companion object {
        fun deJson(o: JSONObject) = Projeto(
            o.optInt("id"), o.optString("nome"), o.optString("descricao"), o.optInt("criador_id")
        )
    }
}

// Uma tarefa do quadro. O estado é exatamente o texto que a API usa.
data class Tarefa(
    val id: Int, val titulo: String, val descricao: String, val criador: String,
    val responsavel: String, var status: String, val dataConclusao: String?, val link: String, val projetoId: Int
) {
    companion object {
        const val POR_FAZER = "Por fazer"
        const val EM_CURSO = "Em curso"
        const val CONCLUIDA = "Concluída"
        fun deJson(o: JSONObject) = Tarefa(
            o.optInt("id"), o.optString("titulo"), o.optString("descricao"), o.optString("criador"),
            o.optString("responsavel"), o.optString("status", POR_FAZER),
            o.optString("data_conclusao").ifBlank { null },
            o.optString("link"), o.optInt("projeto_id")
        )
    }
}

// Um passo da checklist de uma tarefa.
data class Subtarefa(val id: Int, val tarefaId: Int, val descricao: String, val concluida: Boolean) {
    companion object {
        fun deJson(o: JSONObject) = Subtarefa(
            o.optInt("id"), o.optInt("tarefa_id"), o.optString("descricao"), o.optBoolean("concluida")
        )
    }
}
