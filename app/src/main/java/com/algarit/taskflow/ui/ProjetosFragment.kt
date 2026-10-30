package com.algarit.taskflow.ui

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
        binding.botaoPerfil.setOnClickListener {
            findNavController().navigate(ProjetosFragmentDirections.actionProjetosToPerfil())
        }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            binding.swipeProjetos.isRefreshing = estado is UiState.Loading
            when (estado) {
                is UiState.Sucesso -> {
                    adaptador.atualizar(estado.dados)
                    binding.textoVazio.isVisible = estado.dados.isEmpty()
                }
                is UiState.Erro -> {
                    binding.textoVazio.isVisible = true
                    // Snackbar com ação: relança o carregamento sem sair do ecrã.
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root, estado.mensagem, com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    )
                        .setAction(R.string.tentar_de_novo) { viewModel.carregar() }
                        .show()
                }
                else -> Unit
            }
        }

        carregar()
    }

    // Ao voltar de outros ecrãs, recarregamos para mostrar as novidades.
    override fun onResume() {
        super.onResume()
        if (Sessao.estaLogado() && _binding != null) carregar()
    }

    private fun carregar() = viewModel.carregar()

    private fun dialogNovoProjeto() {
        val dialogBinding = DialogNovoProjetoBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.novo_projeto)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.guardar) { _, _ ->
                val nome = dialogBinding.dialogNome.text.toString().trim()
                if (nome.isBlank()) { aviso("Dá um nome ao projeto"); return@setPositiveButton }
                viewModel.criar(nome, dialogBinding.dialogDescricao.text.toString().trim())
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun menuDoProjeto(projeto: Projeto) {
        if (projeto.criadorId != Sessao.utilizadorId) { aviso("Só o criador pode apagar o projeto"); return }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(projeto.nome)
            .setMessage(R.string.apagar_projeto_pergunta)
            .setPositiveButton(R.string.apagar) { _, _ -> viewModel.apagar(projeto.id) }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    // Safe Args: o id e o nome do projeto viajam com o tipo certo para o quadro.
    private fun abrirQuadro(projeto: Projeto) {
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
