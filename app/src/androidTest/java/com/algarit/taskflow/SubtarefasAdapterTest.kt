package com.algarit.taskflow

import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.algarit.taskflow.adapter.SubtarefasAdapter
import com.algarit.taskflow.model.Subtarefa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubtarefasAdapterTest {

    @Test
    fun tocarNaCheckboxPedeParaGuardar() {
        val instrumentacao = InstrumentationRegistry.getInstrumentation()
        instrumentacao.runOnMainSync {
            val contexto = ContextThemeWrapper(instrumentacao.targetContext, R.style.Theme_TaskFlow)
            val subtarefa = Subtarefa(1, 10, "Preparar apresentação", false)
            var escolhida: Subtarefa? = null
            val adaptador = SubtarefasAdapter(aoAlternar = { escolhida = it }, aoApagar = {})
            adaptador.atualizar(listOf(subtarefa))
            val linha = adaptador.onCreateViewHolder(FrameLayout(contexto), 0)
            adaptador.onBindViewHolder(linha, 0)

            linha.binding.itemFeito.performClick()

            assertEquals(subtarefa, escolhida)
            // A caixa aguarda a confirmação da API em vez de simular uma gravação.
            assertFalse(linha.binding.itemFeito.isChecked)
        }
    }

    @Test
    fun reutilizarLinhaMostraOEstadoCorreto() {
        val instrumentacao = InstrumentationRegistry.getInstrumentation()
        instrumentacao.runOnMainSync {
            val contexto = ContextThemeWrapper(instrumentacao.targetContext, R.style.Theme_TaskFlow)
            val porFazer = Subtarefa(1, 10, "Preparar apresentação", false)
            val concluida = Subtarefa(2, 10, "Rever texto", true)
            var escolhida: Subtarefa? = null
            val adaptador = SubtarefasAdapter(aoAlternar = { escolhida = it }, aoApagar = {})
            adaptador.atualizar(listOf(porFazer, concluida))
            val linha = adaptador.onCreateViewHolder(FrameLayout(contexto), 0)
            adaptador.onBindViewHolder(linha, 0)
            adaptador.onBindViewHolder(linha, 1)

            assertTrue(linha.binding.itemFeito.isChecked)
            linha.binding.root.performClick()
            assertEquals(concluida, escolhida)
        }
    }
}
