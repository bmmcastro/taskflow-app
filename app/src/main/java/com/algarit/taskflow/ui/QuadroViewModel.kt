package com.algarit.taskflow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.api.erroAmigavel
import com.algarit.taskflow.data.Repo
import com.algarit.taskflow.model.Tarefa
import com.algarit.taskflow.model.TarefaPedido
import kotlinx.coroutines.isActive
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

    // Job do carregamento atual: ao recarregar, cancelamos o anterior para não
    // termos duas coroutines a escrever no mesmo LiveData (cancelamento cooperativo).
    private var carregarJob: kotlinx.coroutines.Job? = null

    fun carregarTudo(projetoId: Int) {
        carregarJob?.cancel()
        _tarefas.value = UiState.Loading
        carregarJob = viewModelScope.launch {
            try {
                // Membros e tarefas em paralelo, na camada de dados (Repo).
                val dados = Repo.quadro(projetoId)
                _membros.value = dados.nomesMembros
                _tarefas.value = UiState.Sucesso(dados.tarefas)
            } catch (t: Throwable) {
                if (kotlinx.coroutines.currentCoroutineContext().isActive) {
                    _membros.value = emptyList()
                    _tarefas.value = UiState.Erro(erroAmigavel(t))
                }
            }        }
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
