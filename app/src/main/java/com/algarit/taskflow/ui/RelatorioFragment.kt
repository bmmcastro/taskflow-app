package com.algarit.taskflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.algarit.taskflow.R
import com.algarit.taskflow.databinding.ActivityRelatorioBinding
import com.algarit.taskflow.model.RelatorioProjeto

// As contas do projeto: total, concluídas, em curso, atrasadas e por pessoa.
class RelatorioFragment : Fragment() {

    private var _binding: ActivityRelatorioBinding? = null
    private val binding get() = _binding!!
    private val args: RelatorioFragmentArgs by navArgs()
    private val viewModel: RelatorioViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityRelatorioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.botaoVoltar.setOnClickListener { findNavController().navigateUp() }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            if (estado is UiState.Sucesso) preencher(estado.dados)
            else if (estado is UiState.Erro) aviso(estado.mensagem)
        }
        viewModel.carregar(args.projetoId)
    }

    private fun preencher(dados: com.algarit.taskflow.model.RelatorioProjeto) {
        binding.numeroPercentagem.text = "${dados.percentagem}%"
        binding.numeroTotal.text = dados.total.toString()
        binding.numeroConcluidas.text = dados.concluidas.toString()
        binding.numeroEmCurso.text = dados.emCurso.toString()
        binding.numeroPorFazer.text = dados.porFazer.toString()
        binding.numeroAtrasadas.text = dados.atrasadas.toString()

        binding.linhaPorPessoa.removeAllViews()
        if (dados.porPessoa.isEmpty()) {
            val vazio = TextView(requireContext())
            vazio.text = getString(R.string.a_carregar)
            vazio.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_secundario))
            binding.linhaPorPessoa.addView(vazio)
            return
        }
        dados.porPessoa.entries.sortedByDescending { it.value }.forEach { (nome, quantidade) ->
            val linhaPessoa = LinearLayout(requireContext())
            linhaPessoa.orientation = LinearLayout.HORIZONTAL
            linhaPessoa.setPadding(0, 12, 0, 12)

            val esquerda = TextView(requireContext())
            esquerda.text = nome
            esquerda.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto))
            esquerda.textSize = 15f
            esquerda.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val direita = TextView(requireContext())
            direita.text = quantidade.toString()
            direita.setTextColor(ContextCompat.getColor(requireContext(), R.color.roxo))
            direita.textSize = 15f
            direita.setTypeface(null, android.graphics.Typeface.BOLD)
            direita.gravity = android.view.Gravity.END

            linhaPessoa.addView(esquerda)
            linhaPessoa.addView(direita)
            binding.linhaPorPessoa.addView(linhaPessoa)
        }
    }

    private fun aviso(mensagem: String) = requireContext().aviso(mensagem)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
