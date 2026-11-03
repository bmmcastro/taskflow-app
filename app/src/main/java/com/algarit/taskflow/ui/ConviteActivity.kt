package com.algarit.taskflow.ui

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.algarit.taskflow.MainActivity
import com.algarit.taskflow.R
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.api.erroAmigavel
import com.algarit.taskflow.model.MembroPedido
import kotlinx.coroutines.launch

// Quem recebe um convite cola o link (ou só o token) e entra no projeto.
// Fica fora do grafo de navegação: é o ponto de entrada de quem foi convidado.
class ConviteActivity : AppCompatActivity() {

    private var token = ""
    private lateinit var campoConvite: EditText
    private lateinit var textoConvite: TextView
    private lateinit var botaoAceitar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aplicarInsets(findViewById(android.R.id.content))
        Sessao.carregar(this)
        setContentView(R.layout.activity_convite)

        findViewById<ImageButton>(R.id.botao_voltar).setOnClickListener { finish() }
        campoConvite = findViewById(R.id.campo_convite)
        textoConvite = findViewById(R.id.texto_convite)
        botaoAceitar = findViewById(R.id.botao_aceitar)
        findViewById<Button>(R.id.botao_ver).setOnClickListener { verConvite() }

        botaoAceitar.setOnClickListener { aceitarConvite() }
    }

    // cola o link todo ou só o token: os dois caminhos chegam ao mesmo sítio
    private fun verConvite() {
        val colado = campoConvite.text.toString().trim()
        if (colado.isBlank()) { aviso("Cola o link do convite primeiro"); return }
        token = if (colado.contains("/")) colado.substringAfterLast('/') else colado

        textoConvite.visibility = View.VISIBLE
        textoConvite.text = getString(R.string.a_carregar)
        lifecycleScope.launch {
            try {
                val c = Api.servico.convitePorToken(token)
                textoConvite.text = "Projeto: ${c.projeto}\n" +
                    "Convidado por: ${c.criador}\n" +
                    "Para o email: ${c.email}\n" +
                    "Estado: ${c.status}"
                botaoAceitar.visibility = View.VISIBLE
            } catch (t: Throwable) {
                textoConvite.text = "Convite não encontrado"
            }
        }
    }

    private fun aceitarConvite() {
        // Sem sessão iniciada o id seria -1: exigimos login antes de aceitar.
        if (!Sessao.estaLogado()) {
            aviso("Inicia sessão primeiro para aceitares o convite")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        lifecycleScope.launch {
            try {
                Api.servico.aceitarConvite(token, MembroPedido(Sessao.utilizadorId))
                aviso("Agora és membro do projeto!")
                // A sessão está guardada: a MainActivity abre direto nos projetos.
                startActivity(Intent(this@ConviteActivity, MainActivity::class.java)
                    .putExtra("abrir_projetos", true))
                finish()
            } catch (t: Throwable) {
                aviso(erroAmigavel(t))
            }
        }
    }

    private fun aviso(mensagem: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setMessage(mensagem)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
