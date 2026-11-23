package com.algarit.taskflow

import com.algarit.taskflow.model.EstadoTarefa
import com.algarit.taskflow.model.Projeto
import com.algarit.taskflow.model.Subtarefa
import com.algarit.taskflow.model.Tarefa
import com.algarit.taskflow.model.Utilizador
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Testes ao mapeamento JSON → modelos (o mesmo caminho que o Retrofit usa).
class ModelosTest {

    private val gson = Gson()

    @Test
    fun `lê uma tarefa completa`() {
        val tarefa = gson.fromJson(
            "{\"id\":1,\"titulo\":\"Os Maias\",\"descricao\":\"\",\"criador\":\"bruno\"," +
            "\"responsavel\":\"bruno\",\"status\":\"Em curso\",\"data_conclusao\":\"2026-10-30\"," +
            "\"link\":\"\",\"projeto_id\":1}",
            Tarefa::class.java
        )
        assertEquals(1, tarefa.id)
        assertEquals("Os Maias", tarefa.titulo)
        assertEquals("Em curso", tarefa.status)
        assertEquals("2026-10-30", tarefa.dataConclusao)
        assertEquals(1, tarefa.projetoId)
    }

    @Test
    fun `tarefa sem data de conclusão fica sem prazo`() {
        val tarefa = gson.fromJson(
            "{\"id\":2,\"titulo\":\"X\",\"responsavel\":\"a\",\"status\":\"Por fazer\",\"projeto_id\":1}",
            Tarefa::class.java
        )
        assertNull(tarefa.dataConclusao)
    }

    @Test
    fun `tarefa sem link deixa o link a null`() {
        // A API devolve "link": null quando a tarefa não tem link.
        val tarefa = gson.fromJson(
            "{\"id\":4,\"titulo\":\"X\",\"responsavel\":\"a\",\"link\":null,\"projeto_id\":1}",
            Tarefa::class.java
        )
        assertNull(tarefa.link)
        assertTrue(tarefa.link.isNullOrBlank())
    }

    @Test
    fun `tarefa sem estado fica Por fazer`() {
        val tarefa = gson.fromJson(
            "{\"id\":3,\"titulo\":\"X\",\"responsavel\":\"a\",\"projeto_id\":1}",
            Tarefa::class.java
        )
        assertEquals(EstadoTarefa.POR_FAZER.rotulo, tarefa.status)
    }

    @Test
    fun `texto da API é convertido para o enum do estado`() {
        // Texto desconhecido ou em falta cai sempre em Por fazer.
        assertEquals(EstadoTarefa.POR_FAZER, EstadoTarefa.de(null))
        assertEquals(EstadoTarefa.POR_FAZER, EstadoTarefa.de("outra coisa"))
        assertEquals(EstadoTarefa.EM_CURSO, EstadoTarefa.de("Em curso"))
        assertEquals(EstadoTarefa.CONCLUIDA, EstadoTarefa.de("Concluída"))
    }

    @Test
    fun `lê utilizador, projeto e subtarefa`() {
        val u = gson.fromJson(
            "{\"id\":5,\"nome\":\"Demo\",\"email\":\"demo@taskflow.pt\"}",
            Utilizador::class.java
        )
        assertEquals("demo@taskflow.pt", u.email)

        val p = gson.fromJson(
            "{\"id\":1,\"nome\":\"Projeto Final\",\"descricao\":\"x\",\"criador_id\":1}",
            Projeto::class.java
        )
        assertEquals(1, p.criadorId)

        val s = gson.fromJson(
            "{\"id\":7,\"tarefa_id\":1,\"descricao\":\"Correr o script\",\"concluida\":true}",
            Subtarefa::class.java
        )
        assertEquals(true, s.concluida)
        assertEquals(1, s.tarefaId)
    }
}
