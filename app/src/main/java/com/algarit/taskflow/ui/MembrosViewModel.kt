package com.algarit.taskflow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.algarit.taskflow.R
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.erroAmigavel
import com.algarit.taskflow.model.ConviteInfo
import com.algarit.taskflow.model.ConvitePedido
import com.algarit.taskflow.model.MembroPedido
import com.algarit.taskflow.model.Utilizador
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MembrosViewModel(app: Application) : AndroidViewModel(app) {

    private val _membros = MutableLiveData<List<Utilizador>>(emptyList())
    val membros: LiveData<List<Utilizador>> = _membros

    private val _convitesTexto = MutableLiveData(app.getString(R.string.a_carregar))
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
                // Membros e convites em paralelo (async/await): em vez de duas esperas
                // seguidas, uma só — o ecrã aparece mais depressa.
                coroutineScope {
                    val membros = async { Api.servico.membros(projetoId) }
                    val convites = async { Api.servico.convites(projetoId) }
                    _membros.value = membros.await()
                    _convitesTexto.value = textosDosConvites(convites.await())
                }
            } catch (t: CancellationException) {
                throw t
            } catch (t: Exception) {
                _erro.value = erroAmigavel(t)
            }
        }
    }

    fun carregarCandidatos(projetoId: Int) {
        viewModelScope.launch {
            try {
                // Ficam só os utilizadores registados que ainda não estão no projeto.
                val todos = Api.servico.utilizadores()
                _candidatos.value = todos.filter { u -> _membros.value.orEmpty().none { it.id == u.id } }
            } catch (t: CancellationException) {
                throw t
            } catch (t: Exception) {
                _erro.value = erroAmigavel(t)
            }
        }
    }

    fun juntar(projetoId: Int, utilizadorId: Int) {
        viewModelScope.launch {
            try {
                Api.servico.juntarMembro(projetoId, MembroPedido(utilizadorId)).close()
                carregar(projetoId)
            } catch (t: CancellationException) {
                throw t
            } catch (t: Exception) {
                _erro.value = erroAmigavel(t)
            }
        }
    }

    fun remover(projetoId: Int, utilizadorId: Int) {
        viewModelScope.launch {
            try {
                Api.servico.removerMembro(projetoId, utilizadorId).close()
                carregar(projetoId)
            } catch (t: CancellationException) {
                throw t
            } catch (t: Exception) {
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
            } catch (t: CancellationException) {
                throw t
            } catch (t: Exception) {
                _erro.value = erroAmigavel(t)
            }
        }
    }

    // Uma linha por convite pendente, ou o aviso de que não há nenhum.
    private fun textosDosConvites(convites: List<ConviteInfo>): String {
        val app = getApplication<Application>()
        if (convites.isEmpty()) return app.getString(R.string.sem_convites)
        return convites.joinToString("\n") {
            app.getString(R.string.convite_pendente, it.emailConvidado, it.status)
        }
    }
}
