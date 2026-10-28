package com.algarit.taskflow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.erroAmigavel
import com.algarit.taskflow.model.Subtarefa
import com.algarit.taskflow.model.SubtarefaEstado
import com.algarit.taskflow.model.SubtarefaPedido
import com.algarit.taskflow.model.Tarefa
import com.algarit.taskflow.model.TarefaPedido
import kotlinx.coroutines.launch

class DetalheTarefaViewModel(app: Application) : AndroidViewModel(app) {

    private val _tarefa = MutableLiveData<UiState<Tarefa>>(UiState.Loading)
    val tarefa: LiveData<UiState<Tarefa>> = _tarefa

    private val _subtarefas = MutableLiveData<UiState<List<Subtarefa>>>(UiState.Loading)
    val subtarefas: LiveData<UiState<List<Subtarefa>>> = _subtarefas

    private val _membros = MutableLiveData<List<String>>(emptyList())
    val membros: LiveData<List<String>> = _membros

    private val _eliminada = MutableLiveData<Unit>()
    val eliminada: LiveData<Unit> = _eliminada

    private val _erro = MutableLiveData<String>()
    val erro: LiveData<String> = _erro

    // A API só lista por projeto, por isso buscamos todas e ficamos com a nossa.
    fun carregarTarefa(projetoId: Int, tarefaId: Int) {
        _tarefa.value = UiState.Loading
        viewModelScope.launch {
            try {
                val encontrada = Api.servico.tarefas(projetoId).firstOrNull { it.id == tarefaId }
                if (encontrada == null) {
                    _tarefa.value = UiState.Erro("Tarefa não encontrada")
                } else {
                    _tarefa.value = UiState.Sucesso(encontrada)
                }
            } catch (t: Throwable) {
                _tarefa.value = UiState.Erro(erroAmigavel(t))
            }
        }
    }

    fun carregarSubtarefas(tarefaId: Int) {
        viewModelScope.launch {
            try {
                _subtarefas.value = UiState.Sucesso(Api.servico.subtarefas(tarefaId))
            } catch (t: Throwable) {
                _subtarefas.value = UiState.Erro(erroAmigavel(t))
            }
        }
    }

    fun carregarMembros(projetoId: Int) {
        viewModelScope.launch {
            try {
                _membros.value = Api.servico.membros(projetoId).map { it.nome }
            } catch (t: Throwable) {
                _membros.value = emptyList()
            }
        }
    }

    fun criarSubtarefa(tarefaId: Int, descricao: String) {
        viewModelScope.launch {
            try {
                Api.servico.criarSubtarefa(SubtarefaPedido(tarefaId, descricao))
                carregarSubtarefas(tarefaId)
            } catch (t: Throwable) {
                _erro.value = erroAmigavel(t)
            }
        }
    }

    fun alternarSubtarefa(subtarefa: Subtarefa, tarefaId: Int) {
        viewModelScope.launch {
            try {
                Api.servico.alternarSubtarefa(subtarefa.id, SubtarefaEstado(!subtarefa.concluida))
                carregarSubtarefas(tarefaId)
            } catch (t: Throwable) {
                _erro.value = erroAmigavel(t)
            }
        }
    }

    fun apagarSubtarefa(subtarefa: Subtarefa, tarefaId: Int) {
        viewModelScope.launch {
            try {
                Api.servico.apagarSubtarefa(subtarefa.id)
                carregarSubtarefas(tarefaId)
            } catch (t: Throwable) {
                _erro.value = erroAmigavel(t)
            }
        }
    }

    fun editar(tarefaId: Int, pedido: TarefaPedido) {
        viewModelScope.launch {
            try {
                Api.servico.editarTarefa(tarefaId, pedido)
                carregarTarefa(argsProjetoId, tarefaId)
            } catch (t: Throwable) {
                _erro.value = erroAmigavel(t)
            }
        }
    }

    fun apagar(tarefaId: Int) {
        viewModelScope.launch {
            try {
                Api.servico.apagarTarefa(tarefaId)
                _eliminada.value = Unit
            } catch (t: Throwable) {
                _erro.value = erroAmigavel(t)
            }
        }
    }

    // O projetoId chega pelos Safe Args; guardamo-lo ao carregar para o editar.
    private var argsProjetoId = 0

    fun carregarTudo(projetoId: Int, tarefaId: Int) {
        argsProjetoId = projetoId
        carregarTarefa(projetoId, tarefaId)
        carregarMembros(projetoId)
        carregarSubtarefas(tarefaId)
    }
}
