package com.algarit.taskflow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.erroAmigavel
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.model.LoginPedido
import com.algarit.taskflow.model.RegistoPedido
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    // Idle: ainda nada aconteceu (não navega para lado nenhum).
    private val _estado = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val estado: LiveData<UiState<Unit>> = _estado

    fun entrar(email: String, password: String) {
        if (_estado.value is UiState.Loading) return
        _estado.value = UiState.Loading
        viewModelScope.launch {
            try {
                val resposta = Api.servico.login(LoginPedido(email, password))
                val utilizador = resposta.utilizador
                if (utilizador != null) {
                    Sessao.guardar(getApplication(), utilizador.id, utilizador.nome, utilizador.email)
                    _estado.value = UiState.Sucesso(Unit)
                } else {
                    // O servidor não aceitou as credenciais: usa a razão que ele devolve.
                    _estado.value = UiState.Erro(
                        resposta.mensagem ?: getApplication<Application>().getString(com.algarit.taskflow.R.string.erro_login)
                    )
                }
            } catch (t: CancellationException) {
                throw t
            } catch (t: Exception) {
                _estado.value = UiState.Erro(erroAmigavel(t))
            }
        }
    }
}
