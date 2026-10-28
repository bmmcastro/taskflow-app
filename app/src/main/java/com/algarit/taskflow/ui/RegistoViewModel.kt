package com.algarit.taskflow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.erroAmigavel
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.model.RegistoPedido
import kotlinx.coroutines.launch

class RegistoViewModel(app: Application) : AndroidViewModel(app) {

    private val _estado = MutableLiveData<UiState<Unit>>(UiState.Sucesso(Unit))
    val estado: LiveData<UiState<Unit>> = _estado

    fun registar(nome: String, email: String, password: String) {
        if (_estado.value is UiState.Loading) return
        _estado.value = UiState.Loading
        viewModelScope.launch {
            try {
                val novo = Api.servico.registo(RegistoPedido(nome, email, password))
                Sessao.guardar(getApplication(), novo.id, novo.nome, novo.email)
                _estado.value = UiState.Sucesso(Unit)
            } catch (t: Throwable) {
                _estado.value = UiState.Erro(erroAmigavel(t))
            }
        }
    }
}
