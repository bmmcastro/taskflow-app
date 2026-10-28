package com.algarit.taskflow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.api.erroAmigavel
import com.algarit.taskflow.model.Projeto
import com.algarit.taskflow.model.ProjetoPedido
import kotlinx.coroutines.launch

class ProjetosViewModel(app: Application) : AndroidViewModel(app) {

    private val _estado = MutableLiveData<UiState<List<Projeto>>>(UiState.Loading)
    val estado: LiveData<UiState<List<Projeto>>> = _estado

    fun carregar() {
        _estado.value = UiState.Loading
        viewModelScope.launch {
            try {
                val lista = Api.servico.projetos(Sessao.utilizadorId)
                _estado.value = UiState.Sucesso(lista)
            } catch (t: Throwable) {
                _estado.value = UiState.Erro(erroAmigavel(t))
            }
        }
    }

    fun criar(nome: String, descricao: String) {
        viewModelScope.launch {
            try {
                Api.servico.criarProjeto(ProjetoPedido(nome, descricao, Sessao.utilizadorId))
                carregar()
            } catch (t: Throwable) {
                _estado.value = UiState.Erro(erroAmigavel(t))
            }
        }
    }

    fun apagar(id: Int) {
        viewModelScope.launch {
            try {
                Api.servico.apagarProjeto(id, Sessao.utilizadorId)
                carregar()
            } catch (t: Throwable) {
                _estado.value = UiState.Erro(erroAmigavel(t))
            }
        }
    }
}
