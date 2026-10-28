package com.algarit.taskflow.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.algarit.taskflow.MainActivity
import com.algarit.taskflow.R
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import org.json.JSONObject

// Quem recebe um convite cola o link (ou só o token) e entra no projeto.
class ConviteActivity : AppCompatActivity() {

    private var token = ""
    private lateinit var textoConvite: TextView
    private lateinit var botaoAceitar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aplicarInsets(findViewById(android.R.id.content))
        Api.iniciar(this)
        Sessao.carregar(this)
        setContentView(R.layout.activity_convite)

        findViewById<ImageButton>(R.id.botao_voltar).setOnClickListener { finish() }
        textoConvite = findViewById(R.id.texto_convite)
        botaoAceitar = findViewById(R.id.botao_aceitar)

        findViewById<Button>(R.id.botao_ver).setOnClickListener { verConvite() }
        botaoAceitar.setOnClickListener { aceitarConvite() }
    }

    // O utilizador cola o link todo; o token é o último pedaço depois de "convite/".
    private fun extrairToken(texto: String): String {
        val limpo = texto.trim()
        val marca = "convite/"
        val pos = limpo.lastIndexOf(marca)
        return if (pos >= 0) limpo.substring(pos + marca.length) else limpo
    }

    private fun verConvite() {
        if (!Sessao.estaLogado()) { aviso("Entra primeiro na tua conta, depois volta aqui"); return }
        token = extrairToken(findViewById<EditText>(R.id.campo_convite).text.toString())
        if (token.isBlank()) { aviso("Cola o link do convite"); return }

        Api.get("/convites/$token") { r ->
            val c = r.objeto
            if (r.sucesso && c != null) {
                textoConvite.visibility = android.view.View.VISIBLE
                botaoAceitar.visibility = android.view.View.VISIBLE
                textoConvite.text = "Projeto: ${c.optString("projeto")}\n" +
                    "Convidado por: ${c.optString("criador")}\n" +
                    "Para o email: ${c.optString("email")}\n" +
                    "Estado: ${c.optString("status")}"
            } else aviso(r.texto.ifBlank { "Convite não encontrado" })
        }
    }

    private fun aceitarConvite() {
        Api.post("/convites/$token/aceitar", JSONObject().put("utilizador_id", Sessao.utilizadorId)) { r ->
            if (r.sucesso) {
                aviso("Agora és membro do projeto!")
                // A sessão está guardada: a MainActivity abre direto nos projetos.
                startActivity(Intent(this, MainActivity::class.java)
                    .putExtra("abrir_projetos", true))
                finish()
            } else aviso(r.texto.ifBlank { getString(R.string.erro_rede) })
        }
    }
}
