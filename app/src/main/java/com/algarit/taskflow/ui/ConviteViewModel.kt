package com.algarit.taskflow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.api.erroAmigavel
import com.algarit.taskflow.model.ConviteDetalhe
import com.algarit.taskflow.model.MembroPedido
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Lógica do ecrã de convite: consultar o convite e aceitá-lo.
class ConviteViewModel(app: Application) : AndroidViewModel(app) {

    // Idle: ainda não foi consultado nenhum convite.
    private val _convite = MutableLiveData<UiState<ConviteDetalhe>>(UiState.Idle)
    val convite: LiveData<UiState<ConviteDetalhe>> = _convite

    // Verdadeiro enquanto o pedido de aceitar está a correr.
    private val _aAceitar = MutableLiveData(false)
    val aAceitar: LiveData<Boolean> = _aAceitar

    // Verdadeiro quando o convite foi aceite (seguimos para os projetos).
    private val _aceite = MutableLiveData(false)
    val aceite: LiveData<Boolean> = _aceite

    private val _erro = MutableLiveData<String>()
    val erro: LiveData<String> = _erro

    // Token do último convite consultado: só se aceita o que se viu.
    var tokenConsultado: String? = null
        private set

    // True se o campo ainda traz o mesmo convite que foi consultado.
    fun eOConviteConsultado(colado: String): Boolean {
        val token = tokenConsultado ?: return false
        return colado.trim().trimEnd('/').substringAfterLast('/') == token
    }

    // Cola o link todo ou só o token: os dois caminhos chegam ao mesmo sítio.
    fun ver(colado: String) {
        val token = colado.trim().trimEnd('/').substringAfterLast('/')
        tokenConsultado = null
        _convite.value = UiState.Loading
        viewModelScope.launch {
            try {
                val detalhe = Api.servico.convitePorToken(token)
                tokenConsultado = token
                _convite.value = UiState.Sucesso(detalhe)
            } catch (t: CancellationException) {
                throw t
            } catch (t: Exception) {
                _convite.value = UiState.Erro(erroAmigavel(t))
            }
        }
    }

    fun aceitar() {
        val token = tokenConsultado ?: return
        _aAceitar.value = true
        viewModelScope.launch {
            try {
                Api.servico.aceitarConvite(token, MembroPedido(Sessao.utilizadorId)).close()
                _aceite.value = true
            } catch (t: CancellationException) {
                throw t
            } catch (t: Exception) {
                _erro.value = erroAmigavel(t)
            } finally {
                _aAceitar.value = false
            }
        }
    }
}
