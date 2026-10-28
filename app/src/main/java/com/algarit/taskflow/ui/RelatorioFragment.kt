package com.algarit.taskflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.algarit.taskflow.R
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.databinding.ActivityRelatorioBinding

// As contas do projeto: total, concluídas, em curso, atrasadas e por pessoa.
class RelatorioFragment : Fragment() {

    private var _binding: ActivityRelatorioBinding? = null
    private val binding get() = _binding!!
    private val args: RelatorioFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityRelatorioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Api.iniciar(requireContext())
        binding.botaoVoltar.setOnClickListener { findNavController().navigateUp() }
        carregar()
    }

    private fun carregar() {
        Api.get("/tarefas/relatorio", mapOf("projeto_id" to args.projetoId.toString())) { r ->
            if (!isAdded) return@get
            val dados = r.objeto ?: return@get
            binding.numeroPercentagem.text = "${dados.optInt("percentagem")}%"
            binding.numeroTotal.text = dados.optInt("total").toString()
            binding.numeroConcluidas.text = dados.optInt("concluidas").toString()
            binding.numeroEmCurso.text = dados.optInt("emCurso").toString()
            binding.numeroPorFazer.text = dados.optInt("porFazer").toString()
            binding.numeroAtrasadas.text = dados.optInt("atrasadas").toString()

            binding.linhaPorPessoa.removeAllViews()
            val porPessoa = dados.optJSONObject("porPessoa")
            if (porPessoa == null || porPessoa.length() == 0) {
                val vazio = TextView(requireContext())
                vazio.text = getString(R.string.a_carregar)
                vazio.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_secundario))
                binding.linhaPorPessoa.addView(vazio)
                return@get
            }
            val chaves = porPessoa.keys()
            while (chaves.hasNext()) {
                val nome = chaves.next()
                val linhaPessoa = LinearLayout(requireContext())
                linhaPessoa.orientation = LinearLayout.HORIZONTAL
                linhaPessoa.setPadding(0, 12, 0, 12)

                val esquerda = TextView(requireContext())
                esquerda.text = nome
                esquerda.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto))
                esquerda.textSize = 15f
                esquerda.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                val direita = TextView(requireContext())
                direita.text = porPessoa.optInt(nome).toString()
                direita.setTextColor(ContextCompat.getColor(requireContext(), R.color.roxo))
                direita.textSize = 15f
                direita.setTypeface(null, android.graphics.Typeface.BOLD)
                direita.gravity = android.view.Gravity.END

                linhaPessoa.addView(esquerda)
                linhaPessoa.addView(direita)
                binding.linhaPorPessoa.addView(linhaPessoa)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
