package com.algarit.taskflow.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.algarit.taskflow.R
import com.algarit.taskflow.adapter.SubtarefasAdapter
import com.algarit.taskflow.databinding.ActivityDetalheTarefaBinding
import com.algarit.taskflow.model.Tarefa
import com.algarit.taskflow.model.TarefaPedido
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

// O detalhe da tarefa: informação, editar, apagar e a checklist de subtarefas.
// Recebe tarefaId e projetoId por Safe Args.
class DetalheTarefaFragment : Fragment() {

    private var _binding: ActivityDetalheTarefaBinding? = null
    private val binding get() = _binding!!
    private val args: DetalheTarefaFragmentArgs by navArgs()
    private val viewModel: DetalheTarefaViewModel by viewModels()

    private var tarefa: Tarefa? = null
    private var prazoEscolhido = ""

    private lateinit var adaptador: SubtarefasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityDetalheTarefaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.botaoVoltar.setOnClickListener { findNavController().navigateUp() }
        binding.botaoApagar.setOnClickListener { confirmarApagar() }
        binding.botaoEditar.setOnClickListener { dialogEditar() }

        adaptador = SubtarefasAdapter(aoAlternar = { viewModel.alternarSubtarefa(it, args.tarefaId) },
                                     aoApagar = { viewModel.apagarSubtarefa(it, args.tarefaId) })
        binding.listaSubtarefas.layoutManager = LinearLayoutManager(requireContext())
        binding.listaSubtarefas.adapter = adaptador

        binding.botaoAdicionarSubtarefa.setOnClickListener {
            val texto = binding.campoNovaSubtarefa.text.toString().trim()
            if (texto.isNotBlank()) {
                viewModel.criarSubtarefa(args.tarefaId, texto)
                binding.campoNovaSubtarefa.setText("")
            }
        }

        viewModel.tarefa.observe(viewLifecycleOwner) { estado ->
            if (estado is UiState.Sucesso) { tarefa = estado.dados; preencher(estado.dados) }
            else if (estado is UiState.Erro) { aviso(estado.mensagem); findNavController().navigateUp() }
        }
        viewModel.subtarefas.observe(viewLifecycleOwner) { estado ->
            if (estado is UiState.Sucesso) {
                adaptador.atualizar(estado.dados)
                refrescarBarra()
            }
        }
        viewModel.membros.observe(viewLifecycleOwner) { } // fica pronto para o diálogo de edição
        viewModel.eliminada.observe(viewLifecycleOwner) { findNavController().navigateUp() }
        viewModel.erro.observe(viewLifecycleOwner) { aviso(it) }

        viewModel.carregarTudo(args.projetoId, args.tarefaId)
    }

    private fun preencher(t: Tarefa) {
        binding.detalheTitulo.text = t.titulo
        if (t.descricao.isBlank()) binding.detalheDescricao.visibility = View.GONE
        else { binding.detalheDescricao.visibility = View.VISIBLE; binding.detalheDescricao.text = t.descricao }
        binding.detalheAvatar.text = t.responsavel.trim().take(1).uppercase()
        binding.detalheResponsavel.text = t.responsavel
        binding.detalhePrazo.text = "Prazo: ${dataCurta(t.dataConclusao)}"
        binding.detalheEstado.text = t.status
        binding.detalheEstado.setTextColor(when (t.status) {
            Tarefa.EM_CURSO -> ContextCompat.getColor(requireContext(), R.color.azul)
            Tarefa.CONCLUIDA -> ContextCompat.getColor(requireContext(), R.color.verde)
            else -> ContextCompat.getColor(requireContext(), R.color.texto_secundario)
        })
        if (t.link.isBlank()) binding.detalheLink.visibility = View.GONE
        else {
            binding.detalheLink.visibility = View.VISIBLE
            binding.detalheLink.text = t.link
            binding.detalheLink.setOnClickListener {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(t.link))) }
                catch (_: Exception) { aviso("Link inválido") }
            }
        }
    }

    private fun refrescarBarra() {
        val total = adaptador.total()
        val feitas = adaptador.concluidas()
        val percentagem = if (total == 0) 0 else (feitas * 100) / total
        binding.barraProgresso.progress = percentagem
        binding.textoPercentagem.text = "$feitas de $total concluídas ($percentagem%)"
    }

    // Editar a tarefa: o mesmo formulário da criação, já preenchido.
    private fun dialogEditar() {
        val t = tarefa ?: return
        val nomesMembros = viewModel.membros.value ?: emptyList()
        val vista = layoutInflater.inflate(R.layout.dialog_tarefa, null)
        val campoTitulo = vista.findViewById<EditText>(R.id.campo_titulo)
        val campoDescricao = vista.findViewById<EditText>(R.id.campo_descricao)
        val campoResponsavel = vista.findViewById<AutoCompleteTextView>(R.id.campo_responsavel)
        val campoPrazo = vista.findViewById<EditText>(R.id.campo_prazo)
        val campoLink = vista.findViewById<EditText>(R.id.campo_link)

        campoResponsavel.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, nomesMembros))
        campoTitulo.setText(t.titulo)
        campoDescricao.setText(t.descricao)
        campoResponsavel.setText(t.responsavel)
        campoPrazo.setText(dataCurta(t.dataConclusao))
        campoLink.setText(t.link)
        prazoEscolhido = t.dataConclusao ?: ""

        campoPrazo.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, ano, mes, dia ->
                val m = String.format(java.util.Locale.getDefault(), "%02d", mes + 1)
                val d = String.format(java.util.Locale.getDefault(), "%02d", dia)
                prazoEscolhido = "$ano-$m-$d"
                campoPrazo.setText("$d/$m/$ano")
            }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]).show()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.editar_tarefa)
            .setView(vista)
            .setPositiveButton(R.string.guardar) { _, _ ->
                val titulo = campoTitulo.text.toString().trim()
                val responsavel = campoResponsavel.text.toString().trim()
                if (titulo.isBlank() || responsavel.isBlank() || prazoEscolhido.isBlank()) {
                    aviso("Faltam campos obrigatórios"); return@setPositiveButton
                }
                viewModel.editar(
                    args.tarefaId,
                    TarefaPedido(
                        titulo = titulo,
                        descricao = campoDescricao.text.toString().trim(),
                        criador = t.criador,
                        responsavel = responsavel,
                        status = t.status,
                        dataConclusao = prazoEscolhido,
                        link = campoLink.text.toString().trim(),
                        projetoId = null
                    )
                )
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun confirmarApagar() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.apagar)
            .setMessage(R.string.apagar_tarefa_pergunta)
            .setPositiveButton(R.string.apagar) { _, _ -> viewModel.apagar(args.tarefaId) }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun aviso(mensagem: String) = requireContext().aviso(mensagem)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
