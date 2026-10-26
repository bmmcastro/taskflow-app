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

// Criar conta nova (o mesmo registo do site).
class RegistoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aplicarInsets(findViewById(android.R.id.content))
        Api.iniciar(this)
        setContentView(R.layout.activity_registo)

        val campoNome = findViewById<EditText>(R.id.campo_nome)
        val campoEmail = findViewById<EditText>(R.id.campo_email)
        val campoPassword = findViewById<EditText>(R.id.campo_password)
        val botaoCriar = findViewById<Button>(R.id.botao_criar)

        findViewById<TextView>(R.id.botao_entrar).setOnClickListener { finish() }

        botaoCriar.setOnClickListener {
            val nome = campoNome.text.toString().trim()
            val email = campoEmail.text.toString().trim()
            val password = campoPassword.text.toString()
            if (nome.isBlank() || email.isBlank() || password.isBlank()) {
                aviso("Preenche todos os campos"); return@setOnClickListener
            }
            botaoCriar.isEnabled = false
            Api.post("/utilizadores/registo", JSONObject()
                .put("nome", nome).put("email", email).put("password", password)) { r ->
                botaoCriar.isEnabled = true
                val novo = r.objeto
                if (r.sucesso && novo != null) {
                    Sessao.guardar(this, novo.optInt("id"), novo.optString("nome"), novo.optString("email"))
                    startActivity(Intent(this, ProjetosActivity::class.java))
                    finish()
                } else {
                    aviso(if (r.codigo == 400) "Faltam campos obrigatórios" else "Não foi possível criar a conta")
                }
            }
        }
    }
}
