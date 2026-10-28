package com.algarit.taskflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.algarit.taskflow.R
import androidx.recyclerview.widget.LinearLayoutManager
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.databinding.ActivityMembrosBinding
import com.algarit.taskflow.model.Utilizador
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject

// Quem está no projeto: membros, convites pendentes, juntar e convidar.
class MembrosFragment : Fragment() {

    private var _binding: ActivityMembrosBinding? = null
    private val binding get() = _binding!!
    private val args: MembrosFragmentArgs by navArgs()

    private val membros = mutableListOf<Utilizador>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityMembrosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Api.iniciar(requireContext())
        Sessao.carregar(requireContext())

        binding.botaoVoltar.setOnClickListener { findNavController().navigateUp() }
        binding.botaoAdicionar.setOnClickListener { dialogAdicionar() }
        binding.botaoConvidar.setOnClickListener { dialogConvidar() }

        binding.listaMembros.layoutManager = LinearLayoutManager(requireContext())
        binding.listaMembros.adapter = MembrosAdapter()

        carregar()
    }

    private fun carregar() {
        Api.get("/projetos/${args.projetoId}/membros") { r ->
            if (!isAdded) return@get
            membros.clear()
            r.lista?.let { arr -> for (i in 0 until arr.length()) membros.add(Utilizador.deJson(arr.getJSONObject(i))) }
            binding.listaMembros.adapter?.notifyDataSetChanged()
        }
        Api.get("/projetos/${args.projetoId}/convites") { r ->
            if (!isAdded) return@get
            val pendentes = mutableListOf<String>()
            r.lista?.let { arr ->
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    pendentes.add("${c.optString("email_convidado")} (${c.optString("status")})")
                }
            }
            binding.textoConvites.text = if (pendentes.isEmpty()) "Sem convites pendentes."
            else pendentes.joinToString("\n")
        }
    }

    // Juntar alguém que já tem conta: escolhemos da lista de utilizadores.
    private fun dialogAdicionar() {
        Api.get("/utilizadores") { r ->
            if (!isAdded) return@get
            val todos = mutableListOf<Utilizador>()
            r.lista?.let { arr -> for (i in 0 until arr.length()) todos.add(Utilizador.deJson(arr.getJSONObject(i))) }
            val candidatos = todos.filter { u -> membros.none { it.id == u.id } }
            if (candidatos.isEmpty()) { aviso("Não há mais utilizadores registados"); return@get }
            val nomes = candidatos.map { "${it.nome} (${it.email})" }.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.adicionar_membro)
                .setItems(nomes) { _, qual ->
                    Api.post("/projetos/${args.projetoId}/membros", JSONObject().put("utilizador_id", candidatos[qual].id)) { resp ->
                        if (!isAdded) return@post
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
        if (args.criadorId != Sessao.utilizadorId) { aviso("Só o criador pode convidar"); return }
        val campo = EditText(requireContext())
        campo.hint = getString(R.string.email)
        campo.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        campo.setPadding(48, 24, 48, 24)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.convidar_por_email)
            .setView(campo)
            .setPositiveButton(R.string.guardar) { _, _ ->
                val email = campo.text.toString().trim().lowercase()
                Api.post("/projetos/${args.projetoId}/convidar", JSONObject()
                    .put("email_convidado", email)
                    .put("criador_id", Sessao.utilizadorId)
                    .put("enviar_email", false)) { r ->
                    if (!isAdded) return@post
                    val token = r.objeto?.optString("token")
                    if (r.sucesso && token != null) {
                        MaterialAlertDialogBuilder(requireContext())
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

    private fun aviso(mensagem: String) = requireContext().aviso(mensagem)

    // Lista simples dos membros, com o toque longo a remover.
    inner class MembrosAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<MembrosAdapter.VH>() {
        inner class VH(v: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {
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
            h.tag.visibility = if (m.id == args.criadorId) View.VISIBLE else View.GONE
            if (m.id == args.criadorId) h.tag.text = getString(R.string.criador)
            h.itemView.setOnLongClickListener { confirmarRemover(m); true }
        }
    }

    private fun confirmarRemover(m: Utilizador) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.remover_do_projeto)
            .setMessage("Remover ${m.nome} do projeto?")
            .setPositiveButton(R.string.apagar) { _, _ ->
                Api.delete("/projetos/${args.projetoId}/membros/${m.id}") { r ->
                    if (!isAdded) return@delete
                    aviso(r.texto.ifBlank { if (r.sucesso) "Membro removido" else getString(R.string.erro_rede) })
                    carregar()
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
