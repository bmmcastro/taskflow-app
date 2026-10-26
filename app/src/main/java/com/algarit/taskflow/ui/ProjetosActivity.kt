package com.algarit.taskflow.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.algarit.taskflow.R
import com.algarit.taskflow.adapter.ProjetosAdapter
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.model.Projeto
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject

// Ecrã principal depois do login: os projetos onde sou membro.
class ProjetosActivity : AppCompatActivity() {

    private lateinit var adaptador: ProjetosAdapter
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var textoVazio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aplicarInsets(findViewById(android.R.id.content))
        Api.iniciar(this)
        Sessao.carregar(this)
        if (!Sessao.estaLogado()) { irParaLogin(); return }

        setContentView(R.layout.activity_projetos)

        adaptador = ProjetosAdapter(aoAbrir = { abrirQuadro(it) }, aoSegurar = { menuDoProjeto(it) })
        val lista = findViewById<RecyclerView>(R.id.lista_projetos)
        lista.layoutManager = LinearLayoutManager(this)
        lista.adapter = adaptador

        swipe = findViewById(R.id.swipe_projetos)
        swipe.setOnRefreshListener { carregar() }
        textoVazio = findViewById(R.id.texto_vazio)

        findViewById<ImageButton>(R.id.botao_novo).setOnClickListener { dialogNovoProjeto() }
        findViewById<ImageButton>(R.id.botao_perfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregar()
    }

    // Ao voltar de outros ecrãs, recarregamos para mostrar as novidades.
    override fun onResume() {
        super.onResume()
        if (Sessao.estaLogado() && ::adaptador.isInitialized) carregar()
    }

    private fun carregar() {
        swipe.isRefreshing = true
        Api.get("/projetos", mapOf("utilizador_id" to Sessao.utilizadorId.toString())) { r ->
            swipe.isRefreshing = false
            val lista = mutableListOf<Projeto>()
            r.lista?.let { arr -> for (i in 0 until arr.length()) lista.add(Projeto.deJson(arr.getJSONObject(i))) }
            adaptador.atualizar(lista)
            textoVazio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            if (!r.sucesso) aviso(getString(R.string.erro_rede))
        }
    }

    private fun dialogNovoProjeto() {
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_novo_projeto, null)
        val campoNome = vista.findViewById<EditText>(R.id.dialog_nome)
        val campoDescricao = vista.findViewById<EditText>(R.id.dialog_descricao)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.novo_projeto)
            .setView(vista)
            .setPositiveButton(R.string.guardar) { _, _ ->
                val nome = campoNome.text.toString().trim()
                if (nome.isBlank()) { aviso("Dá um nome ao projeto"); return@setPositiveButton }
                Api.post("/projetos", JSONObject()
                    .put("nome", nome)
                    .put("descricao", campoDescricao.text.toString().trim())
                    .put("utilizador_id", Sessao.utilizadorId)) { r ->
                    if (r.sucesso) carregar() else aviso(r.texto.ifBlank { getString(R.string.erro_rede) })
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun menuDoProjeto(projeto: Projeto) {
        if (projeto.criadorId != Sessao.utilizadorId) { aviso("Só o criador pode apagar o projeto"); return }
        MaterialAlertDialogBuilder(this)
            .setTitle(projeto.nome)
            .setMessage(R.string.apagar_projeto_pergunta)
            .setPositiveButton(R.string.apagar) { _, _ ->
                Api.delete("/projetos/${projeto.id}?utilizador_id=${Sessao.utilizadorId}") { r ->
                    if (r.sucesso) carregar() else aviso(r.texto.ifBlank { getString(R.string.erro_rede) })
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun abrirQuadro(projeto: Projeto) {
        val intent = Intent(this, QuadroActivity::class.java)
        intent.putExtra("projeto_id", projeto.id)
        intent.putExtra("projeto_nome", projeto.nome)
        intent.putExtra("criador_id", projeto.criadorId)
        startActivity(intent)
    }

    private fun irParaLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
