package com.algarit.taskflow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.api.erroAmigavel
import com.algarit.taskflow.model.Tarefa
import com.algarit.taskflow.model.TarefaPedido
import kotlinx.coroutines.launch

class QuadroViewModel(app: Application) : AndroidViewModel(app) {

    private val _tarefas = MutableLiveData<UiState<List<Tarefa>>>(UiState.Loading)
    val tarefas: LiveData<UiState<List<Tarefa>>> = _tarefas

    private val _membros = MutableLiveData<List<String>>(emptyList())
    val membros: LiveData<List<String>> = _membros

    // Ação concluída (guardar/mover): quem observa recarrega a lista.
    private val _atualizar = MutableLiveData<Unit>()
    val atualizar: LiveData<Unit> = _atualizar

    private val _erro = MutableLiveData<String>()
    val erro: LiveData<String> = _erro

    fun carregarTudo(projetoId: Int) {
        _tarefas.value = UiState.Loading
        viewModelScope.launch {
            try {
                val membros = Api.servico.membros(projetoId)
                _membros.value = membros.map { it.nome }
            } catch (t: Throwable) {
                _membros.value = emptyList()
            }
            try {
                val lista = Api.servico.tarefas(projetoId)
                _tarefas.value = UiState.Sucesso(lista)
            } catch (t: Throwable) {
                _tarefas.value = UiState.Erro(erroAmigavel(t))
            }
        }
    }

    // Criar (tarefaId nulo) ou editar; o servidor exige os campos todos.
    fun guardar(tarefaId: Int?, pedido: TarefaPedido, projetoId: Int) {
        viewModelScope.launch {
            try {
                if (tarefaId == null) Api.servico.criarTarefa(pedido.copy(projetoId = projetoId))
                else Api.servico.editarTarefa(tarefaId, pedido)
                _atualizar.value = Unit
            } catch (t: Throwable) {
                _erro.value = erroAmigavel(t)
            }
        }
    }

    // Tocar longo no cartão: mover para outra coluna.
    fun mover(tarefa: Tarefa, novoStatus: String) {
        viewModelScope.launch {
            try {
                Api.servico.editarTarefa(
                    tarefa.id,
                    TarefaPedido(
                        titulo = tarefa.titulo,
                        descricao = tarefa.descricao,
                        criador = tarefa.criador,
                        responsavel = tarefa.responsavel,
                        status = novoStatus,
                        dataConclusao = tarefa.dataConclusao ?: "",
                        link = tarefa.link,
                        projetoId = null
                    )
                )
                _atualizar.value = Unit
            } catch (t: Throwable) {
                _erro.value = erroAmigavel(t)
            }
        }
    }
}
