package com.algarit.taskflow.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.algarit.taskflow.R
import com.algarit.taskflow.adapter.ProjetosAdapter
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.databinding.ActivityProjetosBinding
import com.algarit.taskflow.databinding.DialogNovoProjetoBinding
import com.algarit.taskflow.model.Projeto
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

// Ecrã principal depois do login: os projetos onde sou membro.
class ProjetosFragment : Fragment() {

    private var _binding: ActivityProjetosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProjetosViewModel by viewModels()
    private lateinit var adaptador: ProjetosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityProjetosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Sessao.carregar(requireContext())
        if (!Sessao.estaLogado()) { irParaLogin(); return }

        adaptador = ProjetosAdapter(aoAbrir = { abrirQuadro(it) }, aoSegurar = { menuDoProjeto(it) })
        binding.listaProjetos.layoutManager = LinearLayoutManager(requireContext())
        binding.listaProjetos.adapter = adaptador

        binding.swipeProjetos.setOnRefreshListener { viewModel.carregar() }
        binding.botaoNovo.setOnClickListener { dialogNovoProjeto() }
        binding.botaoConvite.setOnClickListener {
            startActivity(Intent(requireContext(), ConviteActivity::class.java))
        }
        binding.botaoPerfil.setOnClickListener {
            findNavController().navigate(ProjetosFragmentDirections.actionProjetosToPerfil())
        }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            // ProgressBar visível enquanto os dados são obtidos.
            binding.loading.isVisible = estado is UiState.Loading
            binding.swipeProjetos.isRefreshing = estado is UiState.Loading
            when (estado) {
                is UiState.Sucesso -> {
                    adaptador.atualizar(estado.dados)
                    binding.textoVazio.setText(R.string.sem_projetos)
                    binding.textoVazio.isVisible = estado.dados.isEmpty()
                }
                is UiState.Erro -> {
                    // Com projetos já no ecrã, eles ficam como estão. Só quando não há
                    // lista é que o motivo do erro aparece no lugar da lista.
                    if (adaptador.itemCount == 0) {
                        binding.textoVazio.text = estado.mensagem
                        binding.textoVazio.isVisible = true
                    }
                    // Snackbar com ação: relança o carregamento sem sair do ecrã.
                    Snackbar.make(binding.root, estado.mensagem, Snackbar.LENGTH_LONG)
                        .setAction(R.string.tentar_de_novo) { viewModel.carregar() }
                        .show()
                }
                else -> Unit
            }
        }

        // Depois do login (ou registo), retomamos o convite que ficou por aceitar.
        val convitePendente = requireActivity().intent.getStringExtra("convite_pendente")
        if (!convitePendente.isNullOrBlank()) {
            requireActivity().intent.removeExtra("convite_pendente")
            startActivity(Intent(requireContext(), ConviteActivity::class.java)
                .putExtra("token", convitePendente))
        }
    }

    // Ao voltar de outros ecrãs, recarregamos para mostrar as novidades.
    override fun onResume() {
        super.onResume()
        if (Sessao.estaLogado() && _binding != null) viewModel.carregar()
    }

    private fun dialogNovoProjeto() {
        val dialogo = DialogNovoProjetoBinding.inflate(layoutInflater)
        val alerta = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.novo_projeto)
            .setView(dialogo.root)
            .setPositiveButton(R.string.guardar, null)
            .setNegativeButton(R.string.cancelar, null)
            .show()
        alerta.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nome = dialogo.dialogNome.text.toString().trim()
            // Validação com setError no próprio campo.
            if (nome.isBlank()) {
                dialogo.dialogNome.error = getString(R.string.erro_nome_projeto)
                return@setOnClickListener
            }
            viewModel.criar(nome, dialogo.dialogDescricao.text.toString().trim())
            alerta.dismiss()
        }
    }

    private fun menuDoProjeto(projeto: Projeto) {
        if (projeto.criadorId != Sessao.utilizadorId) { aviso(getString(R.string.so_criador_apaga)); return }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(projeto.nome)
            .setMessage(R.string.apagar_projeto_pergunta)
            .setPositiveButton(R.string.apagar) { _, _ -> viewModel.apagar(projeto.id) }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    // Safe Args: o id e o nome do projeto viajam com o tipo certo para o quadro.
    private fun abrirQuadro(projeto: Projeto) {
        // Um segundo toque durante a transição já não pertence a este ecrã.
        if (findNavController().currentDestination?.id != R.id.projetosFragment) return
        findNavController().navigate(
            ProjetosFragmentDirections.actionProjetosToQuadro(
                projetoId = projeto.id,
                projetoNome = projeto.nome,
                criadorId = projeto.criadorId
            )
        )
    }

    private fun irParaLogin() {
        findNavController().navigate(ProjetosFragmentDirections.actionProjetosToLogin())
    }

    private fun aviso(mensagem: String) = requireContext().aviso(mensagem)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
