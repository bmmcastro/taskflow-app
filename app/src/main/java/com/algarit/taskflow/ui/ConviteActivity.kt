package com.algarit.taskflow.ui

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
import kotlinx.coroutines.CancellationException
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
        Sessao.carregar(this)
        setContentView(R.layout.activity_convite)
        aplicarInsets(findViewById(android.R.id.content))

        findViewById<ImageButton>(R.id.botao_voltar).setOnClickListener { finish() }
        campoConvite = findViewById(R.id.campo_convite)
        textoConvite = findViewById(R.id.texto_convite)
        botaoAceitar = findViewById(R.id.botao_aceitar)
        findViewById<Button>(R.id.botao_ver).setOnClickListener { verConvite() }

        botaoAceitar.setOnClickListener { aceitarConvite() }
        val conviteGuardado = savedInstanceState?.getString("convite")
            ?: intent.getStringExtra("token")
        if (!conviteGuardado.isNullOrBlank()) {
            campoConvite.setText(conviteGuardado)
            verConvite()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("convite", campoConvite.text.toString())
        super.onSaveInstanceState(outState)
    }

    // cola o link todo ou só o token: os dois caminhos chegam ao mesmo sítio
    private fun verConvite() {
        val colado = campoConvite.text.toString().trim()
        if (colado.isBlank()) { aviso("Cola o link do convite primeiro"); return }
        token = ""
        val tokenPedido = colado.trimEnd('/').substringAfterLast('/')

        botaoAceitar.visibility = View.GONE
        campoConvite.isEnabled = false
        findViewById<Button>(R.id.botao_ver).isEnabled = false
        textoConvite.visibility = View.VISIBLE
        textoConvite.text = getString(R.string.a_carregar)
        lifecycleScope.launch {
            try {
                val c = Api.servico.convitePorToken(tokenPedido)
                token = tokenPedido
                textoConvite.text = "Projeto: ${c.projeto}\n" +
                    "Convidado por: ${c.criador}\n" +
                    "Para o email: ${c.email}\n" +
                    "Estado: ${c.status}"
                botaoAceitar.visibility = View.VISIBLE
            } catch (t: CancellationException) {
                throw t
            } catch (t: Exception) {
                textoConvite.text = erroAmigavel(t)
            } finally {
                campoConvite.isEnabled = true
                findViewById<Button>(R.id.botao_ver).isEnabled = true
            }
        }
    }

    private fun aceitarConvite() {
        // Se o texto mudou, é preciso consultar esse convite antes de o aceitar.
        val tokenAtual = campoConvite.text.toString().trim().trimEnd('/').substringAfterLast('/')
        if (token.isBlank() || tokenAtual != token) {
            botaoAceitar.visibility = View.GONE
            aviso("Carrega em Ver convite antes de aceitar")
            return
        }
        // Sem sessão iniciada o id seria -1: exigimos login antes de aceitar.
        if (!Sessao.estaLogado()) {
            aviso("Inicia sessão primeiro para aceitares o convite")
            startActivity(Intent(this, MainActivity::class.java)
                .putExtra("convite_pendente", token))
            finish()
            return
        }
        botaoAceitar.isEnabled = false
        campoConvite.isEnabled = false
        findViewById<Button>(R.id.botao_ver).isEnabled = false
        lifecycleScope.launch {
            try {
                Api.servico.aceitarConvite(token, MembroPedido(Sessao.utilizadorId)).close()
                aviso("Agora és membro do projeto!")
                // A sessão está guardada: a MainActivity abre direto nos projetos.
                startActivity(Intent(this@ConviteActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                finish()
            } catch (t: CancellationException) {
                throw t
            } catch (t: Exception) {
                aviso(erroAmigavel(t))
            } finally {
                botaoAceitar.isEnabled = true
                campoConvite.isEnabled = true
                findViewById<Button>(R.id.botao_ver).isEnabled = true
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
