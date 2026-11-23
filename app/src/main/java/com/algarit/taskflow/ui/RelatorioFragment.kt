package com.algarit.taskflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.algarit.taskflow.R
import com.algarit.taskflow.adapter.PessoasAdapter
import com.algarit.taskflow.adapter.TarefasPorPessoa
import com.algarit.taskflow.databinding.ActivityRelatorioBinding
import com.algarit.taskflow.model.RelatorioProjeto

// As contas do projeto: total, concluídas, em curso, atrasadas e por pessoa.
class RelatorioFragment : Fragment() {

    private var _binding: ActivityRelatorioBinding? = null
    private val binding get() = _binding!!
    private val args: RelatorioFragmentArgs by navArgs()
    private val viewModel: RelatorioViewModel by viewModels()

    private lateinit var adaptador: PessoasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityRelatorioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adaptador = PessoasAdapter()
        binding.listaPessoas.layoutManager = LinearLayoutManager(requireContext())
        binding.listaPessoas.adapter = adaptador

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            // ProgressBar visível enquanto os dados são obtidos.
            binding.loading.isVisible = estado is UiState.Loading
            if (estado is UiState.Sucesso) preencher(estado.dados)
            else if (estado is UiState.Erro) aviso(estado.mensagem)
        }
        viewModel.carregar(args.projetoId)
    }

    private fun preencher(dados: RelatorioProjeto) {
        binding.numeroPercentagem.text = getString(R.string.percentagem_valor, dados.percentagem)
        binding.progressoRelatorio.progress = dados.percentagem.coerceIn(0, 100)
        binding.numeroTotal.text = dados.total.toString()
        binding.numeroConcluidas.text = dados.concluidas.toString()
        binding.numeroEmCurso.text = dados.emCurso.toString()
        binding.numeroPorFazer.text = dados.porFazer.toString()
        binding.numeroAtrasadas.text = dados.atrasadas.toString()

        // Tarefas por pessoa: quem tem mais aparece primeiro.
        val porPessoa = dados.porPessoa.entries
            .sortedByDescending { it.value }
            .map { TarefasPorPessoa(it.key, it.value) }
        binding.textoSemAtribuicoes.isVisible = porPessoa.isEmpty()
        binding.listaPessoas.isVisible = porPessoa.isNotEmpty()
        adaptador.atualizar(porPessoa)
    }

    private fun aviso(mensagem: String) = requireContext().aviso(mensagem)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
