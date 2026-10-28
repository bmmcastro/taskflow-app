package com.algarit.taskflow.model

import com.google.gson.annotations.SerializedName

// Um utilizador registado (para login, membros e escolher responsáveis).
data class Utilizador(
    val id: Int,
    val nome: String,
    val email: String
)

// Um projeto de equipa.
data class Projeto(
    val id: Int,
    val nome: String,
    val descricao: String,
    @SerializedName("criador_id") val criadorId: Int
)

// Uma tarefa do quadro. O estado é exatamente o texto que a API usa.
// Todos os campos têm valores por omissão: assim o Gson usa o construtor
// sem argumentos e um tarefa sem "status" no JSON fica "Por fazer".
data class Tarefa(
    val id: Int = 0,
    val titulo: String = "",
    val descricao: String = "",
    val criador: String = "",
    val responsavel: String = "",
    var status: String = POR_FAZER,
    @SerializedName("data_conclusao") val dataConclusao: String? = null,
    val link: String = "",
    @SerializedName("projeto_id") val projetoId: Int = 0
) {
    companion object {
        const val POR_FAZER = "Por fazer"
        const val EM_CURSO = "Em curso"
        const val CONCLUIDA = "Concluída"
    }
}

// Um passo da checklist de uma tarefa.
data class Subtarefa(
    val id: Int,
    @SerializedName("tarefa_id") val tarefaId: Int,
    val descricao: String,
    val concluida: Boolean
)

// Relatório do projeto (GET /tarefas/relatorio).
data class RelatorioProjeto(
    val total: Int,
    val concluidas: Int,
    val emCurso: Int,
    val porFazer: Int,
    val atrasadas: Int,
    val percentagem: Int,
    val porPessoa: Map<String, Int>
)

// ----- corpos dos pedidos -----

data class LoginPedido(val email: String, val password: String)

data class LoginResposta(val mensagem: String?, val utilizador: Utilizador?)

data class RegistoPedido(val nome: String, val email: String, val password: String)

data class PerfilPedido(val nome: String, val email: String, val password: String?)

data class ProjetoPedido(
    val nome: String,
    val descricao: String,
    @SerializedName("utilizador_id") val utilizadorId: Int
)

data class MembroPedido(@SerializedName("utilizador_id") val utilizadorId: Int)

data class ConvitePedido(
    @SerializedName("email_convidado") val emailConvidado: String,
    @SerializedName("criador_id") val criadorId: Int,
    @SerializedName("enviar_email") val enviarEmail: Boolean
)

data class ConviteInfo(
    @SerializedName("email_convidado") val emailConvidado: String,
    val status: String
)

data class ConviteResposta(val token: String?, val mensagem: String?)

// O convite visto pelo convidado (GET /convites/{token}).
data class ConviteDetalhe(
    val projeto: String,
    val criador: String,
    val email: String,
    val status: String
)

data class TarefaPedido(
    val titulo: String,
    val descricao: String,
    val criador: String,
    val responsavel: String,
    val status: String,
    @SerializedName("data_conclusao") val dataConclusao: String,
    val link: String,
    @SerializedName("projeto_id") val projetoId: Int?
)

data class SubtarefaPedido(
    @SerializedName("tarefa_id") val tarefaId: Int,
    val descricao: String
)

data class SubtarefaEstado(val concluida: Boolean)
