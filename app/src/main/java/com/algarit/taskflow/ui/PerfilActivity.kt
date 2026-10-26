package com.algarit.taskflow.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.algarit.taskflow.R
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import org.json.JSONObject

// A conta do utilizador: mudar nome, email ou password, e terminar sessão.
class PerfilActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aplicarInsets(findViewById(android.R.id.content))
        Api.iniciar(this)
        Sessao.carregar(this)
        setContentView(R.layout.activity_perfil)

        findViewById<ImageButton>(R.id.botao_voltar).setOnClickListener { finish() }

        val avatar = findViewById<TextView>(R.id.perfil_avatar)
        val campoNome = findViewById<EditText>(R.id.perfil_nome)
        val campoEmail = findViewById<EditText>(R.id.perfil_email)
        val campoPassword = findViewById<EditText>(R.id.perfil_password)

        avatar.text = Sessao.nome.trim().take(1).uppercase()
        campoNome.setText(Sessao.nome)
        campoEmail.setText(Sessao.email)

        findViewById<Button>(R.id.botao_guardar_perfil).setOnClickListener {
            val nome = campoNome.text.toString().trim()
            val email = campoEmail.text.toString().trim()
            if (nome.isBlank() || email.isBlank()) { aviso("O nome e o email são obrigatórios"); return@setOnClickListener }
            val corpo = JSONObject().put("nome", nome).put("email", email)
            if (campoPassword.text.isNotBlank()) corpo.put("password", campoPassword.text.toString())

            Api.patch("/utilizadores/${Sessao.utilizadorId}", corpo) { r ->
                val obj = r.objeto
                if (r.sucesso && obj != null) {
                    Sessao.guardar(this, obj.optInt("id"), obj.optString("nome"), obj.optString("email"))
                    avatar.text = nome.trim().take(1).uppercase()
                    campoPassword.setText("")
                    aviso("Conta atualizada")
                } else aviso(r.texto.ifBlank { getString(R.string.erro_rede) })
            }
        }

        findViewById<TextView>(R.id.botao_terminar_sessao).setOnClickListener {
            Sessao.sair(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }
}
