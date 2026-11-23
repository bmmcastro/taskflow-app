package com.algarit.taskflow.ui

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.algarit.taskflow.R
import com.algarit.taskflow.adapter.MembrosAdapter
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.databinding.ActivityMembrosBinding
import com.algarit.taskflow.databinding.DialogEmailBinding
import com.algarit.taskflow.model.Utilizador
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// Quem está no projeto: membros, convites pendentes, juntar e convidar.
class MembrosFragment : Fragment() {

    private var _binding: ActivityMembrosBinding? = null
    private val binding get() = _binding!!
    private val args: MembrosFragmentArgs by navArgs()
    private val viewModel: MembrosViewModel by viewModels()

    private lateinit var adaptador: MembrosAdapter
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

        adaptador = MembrosAdapter(aoSegurar = { confirmarRemover(it) })
        adaptador.criadorId = args.criadorId
        binding.listaMembros.layoutManager = LinearLayoutManager(requireContext())
        binding.listaMembros.adapter = adaptador

        binding.botaoAdicionar.setOnClickListener {
            agendarDialogo = true
            viewModel.carregarCandidatos(args.projetoId)
        }
        binding.botaoConvidar.setOnClickListener { dialogConvidar() }

        viewModel.membros.observe(viewLifecycleOwner) { lista ->
            binding.loading.isVisible = false
            adaptador.atualizar(lista)
        }
        viewModel.convitesTexto.observe(viewLifecycleOwner) { binding.textoConvites.text = it }
        viewModel.erro.observe(viewLifecycleOwner) {
            binding.loading.isVisible = false
            aviso(it)
        }
        viewModel.conviteCriado.observe(viewLifecycleOwner) { token ->
            if (token != null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.convites_pendentes)
                    .setMessage(getString(R.string.link_convite, token))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
        viewModel.candidatos.observe(viewLifecycleOwner) { candidatos ->
            // Só abrimos o diálogo quando o pedido foi mesmo pedido (e não ao voltar ao ecrã).
            if (!agendarDialogo || !isAdded) return@observe
            agendarDialogo = false
            if (candidatos.isEmpty()) { aviso(getString(R.string.sem_candidatos)); return@observe }
            val opcoes = candidatos.map { getString(R.string.candidato_opcao, it.nome, it.email) }.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.adicionar_membro)
                .setItems(opcoes) { _, qual ->
                    viewModel.juntar(args.projetoId, candidatos[qual].id)
                }
                .setNegativeButton(R.string.cancelar, null)
                .show()
        }

        binding.loading.isVisible = true
        viewModel.carregar(args.projetoId)
    }

    // Convidar por email: a API devolve o token do convite para partilharmos o link.
    private fun dialogConvidar() {
        if (args.criadorId != Sessao.utilizadorId) { aviso(getString(R.string.so_criador_convida)); return }
        // ViewBinding também nos diálogos: sem criar vistas nem findViewById no código.
        val dialogo = DialogEmailBinding.inflate(layoutInflater)
        dialogo.campoEmail.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        val alerta = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.convidar_por_email)
            .setView(dialogo.root)
            .setPositiveButton(R.string.guardar, null)
            .setNegativeButton(R.string.cancelar, null)
            .show()
        alerta.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val email = dialogo.campoEmail.text.toString().trim()
            // Validação do formato do email com setError no próprio campo.
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                dialogo.campoEmail.error = getString(R.string.erro_email)
                return@setOnClickListener
            }
            viewModel.convidar(args.projetoId, email, Sessao.utilizadorId)
            alerta.dismiss()
        }
    }

    private fun confirmarRemover(membro: Utilizador) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.remover_do_projeto)
            .setMessage(getString(R.string.remover_membro_pergunta, membro.nome))
            .setPositiveButton(R.string.apagar) { _, _ -> viewModel.remover(args.projetoId, membro.id) }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun aviso(mensagem: String) = requireContext().aviso(mensagem)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
