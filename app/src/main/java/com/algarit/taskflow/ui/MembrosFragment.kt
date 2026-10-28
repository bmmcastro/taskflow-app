package com.algarit.taskflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.algarit.taskflow.R
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.databinding.ActivityMembrosBinding
import com.algarit.taskflow.model.Utilizador
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// Quem está no projeto: membros, convites pendentes, juntar e convidar.
class MembrosFragment : Fragment() {

    private var _binding: ActivityMembrosBinding? = null
    private val binding get() = _binding!!
    private val args: MembrosFragmentArgs by navArgs()
    private val viewModel: MembrosViewModel by viewModels()

    private val membros = mutableListOf<Utilizador>()
    private var agendarDialogo = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityMembrosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Sessao.carregar(requireContext())

        binding.botaoVoltar.setOnClickListener { findNavController().navigateUp() }
        binding.botaoAdicionar.setOnClickListener {
            agendarDialogo = true
            viewModel.carregarCandidatos(args.projetoId)
        }
        binding.botaoConvidar.setOnClickListener { dialogConvidar() }

        binding.listaMembros.layoutManager = LinearLayoutManager(requireContext())
        binding.listaMembros.adapter = MembrosAdapter()

        viewModel.membros.observe(viewLifecycleOwner) { lista ->
            membros.clear(); membros.addAll(lista)
            binding.listaMembros.adapter?.notifyDataSetChanged()
        }
        viewModel.convitesTexto.observe(viewLifecycleOwner) { binding.textoConvites.text = it }
        viewModel.erro.observe(viewLifecycleOwner) { aviso(it) }
        viewModel.conviteCriado.observe(viewLifecycleOwner) { token ->
            if (token != null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.convites_pendentes)
                    .setMessage("Envia este link:\nhttps://taskflow.algarit.pt/#convite/$token")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
        viewModel.candidatos.observe(viewLifecycleOwner) { candidatos ->
            if (!agendarDialogo || !isAdded) return@observe
            agendarDialogo = false
            if (candidatos.isEmpty()) { aviso("Não há mais utilizadores registados"); return@observe }
            val nomes = candidatos.map { "${it.nome} (${it.email})" }.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.adicionar_membro)
                .setItems(nomes) { _, qual ->
                    viewModel.juntar(args.projetoId, candidatos[qual].id)
                }
                .setNegativeButton(R.string.cancelar, null)
                .show()
        }

        viewModel.carregar(args.projetoId)
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
                viewModel.convidar(args.projetoId, campo.text.toString().trim().lowercase(), Sessao.utilizadorId)
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
            .setPositiveButton(R.string.apagar) { _, _ -> viewModel.remover(args.projetoId, m.id) }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
