package com.algarit.taskflow.data

import com.algarit.taskflow.api.Api
import com.algarit.taskflow.model.Projeto
import com.algarit.taskflow.model.Tarefa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

// Camada de dados entre os ViewModels e a API (o padrão do slide de withContext).
// Quem chama não precisa de saber em que thread corre: as funções são suspend
// e o trabalho de rede fica confinado ao dispatcher de IO.
object Repo {

    // Os dados que o quadro precisa: nomes dos membros + tarefas do projeto.
    data class Quadro(val nomesMembros: List<String>, val tarefas: List<Tarefa>)

    /**
     * Carrega membros e tarefas EM PARALELO (async/await) dentro de IO.
     * Duas chamadas sequenciais custavam duas idas ao servidor; em paralelo
     * custa uma só espera — o ecrã aparece mais depressa.
     */
    suspend fun quadro(projetoId: Int): Quadro =
        withContext(Dispatchers.IO) {
            coroutineScope {
                val membros = async { Api.servico.membros(projetoId) }
                val tarefas = async { Api.servico.tarefas(projetoId) }
                Quadro(
                    nomesMembros = membros.await().map { it.nome },
                    tarefas = tarefas.await()
                )
            }
        }

    // Exemplo de leitura simples, também em IO pelo mesmo motivo.
    suspend fun projetos(utilizadorId: Int): List<Projeto> =
        withContext(Dispatchers.IO) { Api.servico.projetos(utilizadorId) }
}
