package com.algarit.taskflow.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.algarit.taskflow.R
import com.algarit.taskflow.adapter.SubtarefasAdapter
import com.algarit.taskflow.databinding.ActivityDetalheTarefaBinding
import com.algarit.taskflow.databinding.DialogTarefaBinding
import com.algarit.taskflow.model.EstadoTarefa
import com.algarit.taskflow.model.Subtarefa
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
        binding.botaoApagar.setOnClickListener { confirmarApagar() }
        binding.botaoEditar.setOnClickListener { dialogEditar() }

        adaptador = SubtarefasAdapter(aoAlternar = { viewModel.alternarSubtarefa(it, args.tarefaId) },
                                     aoApagar = { viewModel.apagarSubtarefa(it, args.tarefaId) })
        binding.listaSubtarefas.layoutManager = LinearLayoutManager(requireContext())
        binding.listaSubtarefas.adapter = adaptador

        binding.botaoAdicionarSubtarefa.setOnClickListener {
            val texto = binding.campoNovaSubtarefa.text.toString().trim()
            // Validação com setError: uma subtarefa precisa de descrição.
            if (texto.isBlank()) { binding.campoNovaSubtarefa.error = getString(R.string.erro_subtarefa); return@setOnClickListener }
            binding.campoNovaSubtarefa.error = null
            viewModel.criarSubtarefa(args.tarefaId, texto)
            binding.campoNovaSubtarefa.setText("")
        }

        viewModel.tarefa.observe(viewLifecycleOwner) { estado ->
            // ProgressBar visível enquanto os dados são obtidos.
            binding.loading.isVisible = estado is UiState.Loading
            if (estado is UiState.Sucesso) { tarefa = estado.dados; preencher(estado.dados) }
            else if (estado is UiState.Erro) { aviso(estado.mensagem); findNavController().navigateUp() }
        }
        viewModel.subtarefas.observe(viewLifecycleOwner) { estado ->
            if (estado is UiState.Sucesso) {
                adaptador.atualizar(estado.dados)
                refrescarBarra(estado.dados)
            } else if (estado is UiState.Erro) {
                aviso(estado.mensagem)
            }
        }
        viewModel.membros.observe(viewLifecycleOwner) { } // fica pronto para o diálogo de edição
        viewModel.eliminada.observe(viewLifecycleOwner) { findNavController().navigateUp() }
        viewModel.erro.observe(viewLifecycleOwner) { aviso(it) }

        viewModel.carregarTudo(args.projetoId, args.tarefaId)
    }

    private fun preencher(t: Tarefa) {
        binding.detalheTitulo.text = t.titulo
        binding.detalheDescricao.isVisible = t.descricao.isNotBlank()
        binding.detalheDescricao.text = t.descricao
        binding.detalheAvatar.text = t.responsavel.trim().take(1).uppercase()
        binding.detalheResponsavel.text = t.responsavel
        binding.detalhePrazo.text = getString(R.string.prazo_label, dataCurta(t.dataConclusao))
        // A cor da etiqueta acompanha o estado: azul em curso, verde concluída, neutro por fazer.
        val estado = EstadoTarefa.de(t.status)
        binding.detalheEstado.text = estado.rotulo
        binding.detalheEstado.setTextColor(
            ContextCompat.getColor(requireContext(), when (estado) {
                EstadoTarefa.EM_CURSO -> R.color.azul
                EstadoTarefa.CONCLUIDA -> R.color.verde
                else -> R.color.texto_secundario
            })
        )
        // O link é anulável: só se mostra quando a tarefa tem um.
        val link = t.link
        binding.detalheLink.isVisible = !link.isNullOrBlank()
        binding.detalheLink.text = link.orEmpty()
        binding.detalheLink.setOnClickListener {
            if (link.isNullOrBlank()) return@setOnClickListener
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
            catch (_: Exception) { aviso(getString(R.string.erro_link)) }
        }
    }

    private fun refrescarBarra(subtarefas: List<Subtarefa>) {
        // Usamos a resposta da API, porque o DiffUtil atualiza a lista em segundo plano.
        val total = subtarefas.size
        val feitas = subtarefas.count { it.concluida }
        val percentagem = if (total == 0) 0 else (feitas * 100) / total
        binding.barraProgresso.progress = percentagem
        binding.textoPercentagem.text = getString(R.string.subtarefas_concluidas, feitas, total, percentagem)
    }

    // Editar a tarefa: o mesmo formulário da criação, já preenchido.
    private fun dialogEditar() {
        val t = tarefa ?: return
        // ViewBinding também no diálogo: sem inflate/findViewById.
        val dialogo = DialogTarefaBinding.inflate(layoutInflater)
        val campoTitulo = dialogo.campoTitulo
        val campoResponsavel = dialogo.campoResponsavel

        val nomesMembros = viewModel.membros.value ?: emptyList()
        campoResponsavel.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, nomesMembros))
        campoResponsavel.setOnClickListener { campoResponsavel.showDropDown() }
        // Spinner com as colunas do quadro: começa na coluna em que a tarefa está.
        dialogo.spinnerEstado.mostrarEstados(EstadoTarefa.de(t.status))

        campoTitulo.setText(t.titulo)
        dialogo.campoDescricao.setText(t.descricao)
        campoResponsavel.setText(t.responsavel)
        dialogo.campoPrazo.setText(dataCurta(t.dataConclusao))
        dialogo.campoLink.setText(t.link)
        prazoEscolhido = t.dataConclusao ?: ""

        dialogo.campoPrazo.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, ano, mes, dia ->
                prazoEscolhido = String.format(java.util.Locale.US, "%04d-%02d-%02d", ano, mes + 1, dia)
                dialogo.campoPrazo.setText("$dia/${mes + 1}/$ano")
                dialogo.campoPrazo.error = null
            }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]).show()
        }

        val alerta = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.editar_tarefa)
            .setView(dialogo.root)
            .setPositiveButton(R.string.guardar, null)
            .setNegativeButton(R.string.cancelar, null)
            .show()
        // O listener é colocado depois do show para não fechar se faltarem campos.
        alerta.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val titulo = campoTitulo.text.toString().trim()
            val responsavel = campoResponsavel.text.toString().trim()
            // Validação com setError no próprio campo.
            var valido = true
            if (titulo.isBlank()) { campoTitulo.error = getString(R.string.erro_titulo); valido = false }
            if (responsavel.isBlank()) { campoResponsavel.error = getString(R.string.erro_responsavel); valido = false }
            if (prazoEscolhido.isBlank()) { dialogo.campoPrazo.error = getString(R.string.erro_prazo); valido = false }
            if (!valido) return@setOnClickListener
            viewModel.editar(
                args.tarefaId,
                TarefaPedido(
                    titulo = titulo,
                    descricao = dialogo.campoDescricao.text.toString().trim(),
                    criador = t.criador,
                    responsavel = responsavel,
                    status = dialogo.spinnerEstado.estadoEscolhido.rotulo,
                    dataConclusao = prazoEscolhido,
                    link = dialogo.campoLink.text.toString().trim(),
                    projetoId = null
                )
            )
            alerta.dismiss()
        }
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
