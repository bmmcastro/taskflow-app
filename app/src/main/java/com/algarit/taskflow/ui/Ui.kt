package com.algarit.taskflow.ui

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.algarit.taskflow.model.EstadoTarefa

// Pequenas ajudas usadas em todos os ecrãs, para manter o código curto.

// Respeita as barras do sistema (edge-to-edge) aplicando o espaçamento certo.
// Basta passar a raiz do binding do ecrã (Fragment ou Activity).
fun aplicarInsetsEm(raiz: View) {
    ViewCompat.setOnApplyWindowInsetsListener(raiz) { v, insets ->
        val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
        v.setPadding(b.left, b.top, b.right, maxOf(b.bottom, teclado.bottom))
        insets
    }
}

fun Activity.aplicarInsets(raiz: View) {
    aplicarInsetsEm(raiz)
}

fun Context.aviso(mensagem: String) = Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()

// Preenche um Spinner com as colunas do quadro e seleciona o estado indicado.
// É usado no formulário de criar/editar tarefa (quadro e detalhe).
fun Spinner.mostrarEstados(estado: EstadoTarefa) {
    val colunas = EstadoTarefa.entries.map { it.rotulo }
    adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, colunas).apply {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
    setSelection(estado.ordinal)
}

// Coluna escolhida num Spinner preenchido com mostrarEstados().
val Spinner.estadoEscolhido: EstadoTarefa
    get() = EstadoTarefa.entries.getOrElse(selectedItemPosition) { EstadoTarefa.POR_FAZER }

// "2026-11-02" ou "2026-11-02T00:00:00" -> "02/11/2026" (se vier vazia devolvemos "—").
fun dataCurta(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val dataLimpa = iso.split("T")[0].split(" ")[0]
    val partes = dataLimpa.split("-")
    return if (partes.size == 3) "${partes[2]}/${partes[1]}/${partes[0]}" else iso
}
