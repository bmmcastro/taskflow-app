package com.algarit.taskflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.algarit.taskflow.R
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.model.Utilizador
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject

// Quem está no projeto: membros, convites pendentes, juntar e convidar.
class MembrosActivity : AppCompatActivity() {

    private var projetoId = -1
    private var criadorId = -1
    private val membros = mutableListOf<Utilizador>()

    private lateinit var recycler: RecyclerView
    private lateinit var textoConvites: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aplicarInsets(findViewById(android.R.id.content))
        Api.iniciar(this)
        Sessao.carregar(this)
        projetoId = intent.getIntExtra("projeto_id", -1)
        criadorId = intent.getIntExtra("criador_id", -1)
        if (projetoId <= 0) { finish(); return }

        setContentView(R.layout.activity_membros)
        findViewById<ImageButton>(R.id.botao_voltar).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.botao_adicionar).setOnClickListener { dialogAdicionar() }
        findViewById<ImageButton>(R.id.botao_convidar).setOnClickListener { dialogConvidar() }

        recycler = findViewById(R.id.lista_membros)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = MembrosAdapter()
        textoConvites = findViewById(R.id.texto_convites)

        carregar()
    }

    private fun carregar() {
        Api.get("/projetos/$projetoId/membros") { r ->
            membros.clear()
            r.lista?.let { arr -> for (i in 0 until arr.length()) membros.add(Utilizador.deJson(arr.getJSONObject(i))) }
            recycler.adapter?.notifyDataSetChanged()
        }
        Api.get("/projetos/$projetoId/convites") { r ->
            val pendentes = mutableListOf<String>()
            r.lista?.let { arr ->
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    pendentes.add("${c.optString("email_convidado")} (${c.optString("status")})")
                }
            }
            textoConvites.text = if (pendentes.isEmpty()) "Sem convites pendentes."
            else pendentes.joinToString("\n")
        }
    }

    // Juntar alguém que já tem conta: escolhemos da lista de utilizadores.
    private fun dialogAdicionar() {
        Api.get("/utilizadores") { r ->
            val todos = mutableListOf<Utilizador>()
            r.lista?.let { arr -> for (i in 0 until arr.length()) todos.add(Utilizador.deJson(arr.getJSONObject(i))) }
            val candidatos = todos.filter { u -> membros.none { it.id == u.id } }
            if (candidatos.isEmpty()) { aviso("Não há mais utilizadores registados"); return@get }
            val nomes = candidatos.map { "${it.nome} (${it.email})" }.toTypedArray()
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.adicionar_membro)
                .setItems(nomes) { _, qual ->
                    Api.post("/projetos/$projetoId/membros", JSONObject().put("utilizador_id", candidatos[qual].id)) { resp ->
                        aviso(resp.texto.ifBlank { if (resp.sucesso) "Membro adicionado" else getString(R.string.erro_rede) })
                        carregar()
                    }
                }
                .setNegativeButton(R.string.cancelar, null)
                .show()
        }
    }

    // Convidar por email: a API devolve o token do convite para partilharmos o link.
    private fun dialogConvidar() {
        if (criadorId != Sessao.utilizadorId) { aviso("Só o criador pode convidar"); return }
        val campo = EditText(this)
        campo.hint = getString(R.string.email)
        campo.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        campo.setPadding(48, 24, 48, 24)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.convidar_por_email)
            .setView(campo)
            .setPositiveButton(R.string.guardar) { _, _ ->
                val email = campo.text.toString().trim().lowercase()
                Api.post("/projetos/$projetoId/convidar", JSONObject()
                    .put("email_convidado", email)
                    .put("criador_id", Sessao.utilizadorId)
                    .put("enviar_email", false)) { r ->
                    val token = r.objeto?.optString("token")
                    if (r.sucesso && token != null) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.convites_pendentes)
                            .setMessage("Convite criado para $email.\n\nEnvia este link:\nhttps://taskflow.algarit.pt/#convite/$token")
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                        carregar()
                    } else aviso(r.texto.ifBlank { getString(R.string.erro_rede) })
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    // Lista simples dos membros, com o toque longo a remover.
    inner class MembrosAdapter : RecyclerView.Adapter<MembrosAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val avatar: TextView = v.findViewById(R.id.item_avatar)
            val nome: TextView = v.findViewById(R.id.item_nome)
            val email: TextView = v.findViewById(R.id.item_email)
            val tag: TextView = v.findViewById(R.id.item_tag_criador)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_membro, parent, false))

        override fun getItemCount() = membros.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val m = membros[pos]
            h.avatar.text = m.nome.trim().take(1).uppercase()
            h.nome.text = m.nome
            h.email.text = m.email
            h.tag.visibility = if (m.id == criadorId) View.VISIBLE else View.GONE
            if (m.id == criadorId) h.tag.text = getString(R.string.criador)
            h.itemView.setOnLongClickListener { confirmarRemover(m); true }
        }
    }

    private fun confirmarRemover(m: Utilizador) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.remover_do_projeto)
            .setMessage("Remover ${m.nome} do projeto?")
            .setPositiveButton(R.string.apagar) { _, _ ->
                Api.delete("/projetos/$projetoId/membros/${m.id}") { r ->
                    aviso(r.texto.ifBlank { if (r.sucesso) "Membro removido" else getString(R.string.erro_rede) })
                    carregar()
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }
}
