package com.algarit.taskflow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.erroAmigavel
import com.algarit.taskflow.model.RelatorioProjeto
import kotlinx.coroutines.launch

class RelatorioViewModel(app: Application) : AndroidViewModel(app) {

    private val _estado = MutableLiveData<UiState<RelatorioProjeto>>(UiState.Loading)
    val estado: LiveData<UiState<RelatorioProjeto>> = _estado

    fun carregar(projetoId: Int) {
        _estado.value = UiState.Loading
        viewModelScope.launch {
            try {
                _estado.value = UiState.Sucesso(Api.servico.relatorio(projetoId))
            } catch (t: Throwable) {
                _estado.value = UiState.Erro(erroAmigavel(t))
            }
        }
    }
}
