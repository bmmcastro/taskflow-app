package com.algarit.taskflow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.erroAmigavel
import com.algarit.taskflow.model.ConvitePedido
import com.algarit.taskflow.model.MembroPedido
import com.algarit.taskflow.model.Utilizador
import kotlinx.coroutines.launch

class MembrosViewModel(app: Application) : AndroidViewModel(app) {

    private val _membros = MutableLiveData<List<Utilizador>>(emptyList())
    val membros: LiveData<List<Utilizador>> = _membros

    private val _convitesTexto = MutableLiveData("A carregar…")
    val convitesTexto: LiveData<String> = _convitesTexto

    // Utilizadores registados que ainda não são membros (para o diálogo de juntar).
    private val _candidatos = MutableLiveData<List<Utilizador>>()
    val candidatos: LiveData<List<Utilizador>> = _candidatos

    // Token do convite criado (para partilhar o link) ou mensagem de erro.
    private val _conviteCriado = MutableLiveData<String?>()
    val conviteCriado: LiveData<String?> = _conviteCriado

    private val _erro = MutableLiveData<String>()
    val erro: LiveData<String> = _erro

    fun carregar(projetoId: Int) {
        viewModelScope.launch {
            try {
                _membros.value = Api.servico.membros(projetoId)
            } catch (t: Throwable) {
                _erro.value = erroAmigavel(t)
            }
            try {
                val convites = Api.servico.convites(projetoId)
                _convitesTexto.value = if (convites.isEmpty()) "Sem convites pendentes."
                else convites.joinToString("\n") { "${it.emailConvidado} (${it.status})" }
            } catch (t: Throwable) {
                _convitesTexto.value = "Sem convites pendentes."
            }
        }
    }

    fun carregarCandidatos(projetoId: Int) {
        viewModelScope.launch {
            try {
                val todos = Api.servico.utilizadores()
                _candidatos.value = todos.filter { u -> _membros.value?.none { it.id == u.id } != false }
            } catch (t: Throwable) {
                _erro.value = erroAmigavel(t)
            }
        }
    }

    fun juntar(projetoId: Int, utilizadorId: Int) {
        viewModelScope.launch {
            try {
                Api.servico.juntarMembro(projetoId, MembroPedido(utilizadorId))
                carregar(projetoId)
            } catch (t: Throwable) {
                _erro.value = erroAmigavel(t)
            }
        }
    }

    fun remover(projetoId: Int, utilizadorId: Int) {
        viewModelScope.launch {
            try {
                Api.servico.removerMembro(projetoId, utilizadorId)
                carregar(projetoId)
            } catch (t: Throwable) {
                _erro.value = erroAmigavel(t)
            }
        }
    }

    fun convidar(projetoId: Int, email: String, criadorId: Int) {
        viewModelScope.launch {
            try {
                val resposta = Api.servico.convidar(
                    projetoId,
                    ConvitePedido(email, criadorId, enviarEmail = false)
                )
                _conviteCriado.value = resposta.token
                carregar(projetoId)
            } catch (t: Throwable) {
                _erro.value = erroAmigavel(t)
            }
        }
    }
}
