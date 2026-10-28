package com.algarit.taskflow

import org.junit.Assert.assertEquals
import org.junit.Test
import com.algarit.taskflow.ui.dataCurta

// Testes à formatação de datas mostradas nos cartões.
class DataCurtaTest {

    @Test
    fun `data iso simples`() {
        assertEquals("02/11/2026", dataCurta("2026-11-02"))
    }

    @Test
    fun `data iso com hora`() {
        assertEquals("30/10/2026", dataCurta("2026-10-30T00:00:00.000Z"))
    }

    @Test
    fun `data vazia ou nula devolve travessão`() {
        assertEquals("—", dataCurta(null))
        assertEquals("—", dataCurta(""))
        assertEquals("—", dataCurta("   "))
    }

    @Test
    fun `data em formato desconhecido devolve-se a própria`() {
        assertEquals("amanhã", dataCurta("amanhã"))
    }
}
