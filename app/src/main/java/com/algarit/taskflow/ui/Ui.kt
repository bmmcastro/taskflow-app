package com.algarit.taskflow.ui

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// Pequenas ajudas usadas em todos os ecrãs, para manter o código curto.

// Respeita as barras do sistema (edge-to-edge) aplicando o espaçamento certo.
// Nos Fragments chamamos esta versão com a raiz do binding; nas Activities
// restantes (Convite) mantém-se a extensão sobre Activity.
fun aplicarInsetsEm(raiz: View) {
    ViewCompat.setOnApplyWindowInsetsListener(raiz) { v, insets ->
        val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(b.left, b.top, b.right, b.bottom)
        insets
    }
}

fun Activity.aplicarInsets(raiz: View) {
    aplicarInsetsEm(raiz)
}

fun Context.aviso(mensagem: String) = Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()

// "2026-11-02" ou "2026-11-02T00:00:00" -> "02/11/2026" (se vier vazia devolvemos "—").
fun dataCurta(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val dataLimpa = iso.split("T")[0].split(" ")[0]
    val partes = dataLimpa.split("-")
    return if (partes.size == 3) "${partes[2]}/${partes[1]}/${partes[0]}" else iso
}
