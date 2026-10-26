package com.algarit.taskflow.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.algarit.taskflow.R
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import org.json.JSONObject

// Primeiro ecrã: entrar com a conta (a mesma do site).
class LoginActivity : AppCompatActivity() {

    private lateinit var campoEmail: EditText
    private lateinit var campoPassword: EditText
    private lateinit var botaoEntrar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aplicarInsets(findViewById(android.R.id.content))
        Api.iniciar(this)
        Sessao.carregar(this)
        if (Sessao.estaLogado()) { irParaProjetos(); return }

        setContentView(R.layout.activity_login)
        campoEmail = findViewById(R.id.campo_email)
        campoPassword = findViewById(R.id.campo_password)
        botaoEntrar = findViewById(R.id.botao_entrar)

        botaoEntrar.setOnClickListener { entrar() }
        findViewById<TextView>(R.id.botao_demo).setOnClickListener {
            campoEmail.setText("demo@taskflow.pt")
            campoPassword.setText("1234")
            entrar()
        }
        findViewById<TextView>(R.id.botao_registar).setOnClickListener {
            startActivity(Intent(this, RegistoActivity::class.java))
        }
        findViewById<TextView>(R.id.botao_convite).setOnClickListener {
            startActivity(Intent(this, ConviteActivity::class.java))
        }
    }

    private fun entrar() {
        val email = campoEmail.text.toString().trim()
        val password = campoPassword.text.toString()
        if (email.isBlank() || password.isBlank()) { aviso("Preenche o email e a password"); return }

        botaoEntrar.isEnabled = false
        Api.post("/utilizadores/login", JSONObject().put("email", email).put("password", password)) { r ->
            botaoEntrar.isEnabled = true
            val utilizador = r.objeto?.optJSONObject("utilizador")
            if (r.sucesso && utilizador != null) {
                Sessao.guardar(this, utilizador.optInt("id"), utilizador.optString("nome"), utilizador.optString("email"))
                irParaProjetos()
            } else {
                aviso(if (r.codigo == 401) "Email ou password errados" else "Não foi possível entrar")
            }
        }
    }

    private fun irParaProjetos() {
        startActivity(Intent(this, ProjetosActivity::class.java))
        finish()
    }
}
