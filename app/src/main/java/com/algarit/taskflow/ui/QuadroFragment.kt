package com.algarit.taskflow.ui

import android.app.DatePickerDialog
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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.algarit.taskflow.R
import com.algarit.taskflow.adapter.TarefasAdapter
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.databinding.ActivityQuadroBinding
import com.algarit.taskflow.model.Tarefa
import com.algarit.taskflow.model.TarefaPedido
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import java.util.Calendar

// O quadro do projeto: 3 colunas em tabs, filtro por pessoa e cartões de tarefa.
// Recebe projetoId, projetoNome e criadorId por Safe Args.
class QuadroFragment : Fragment() {

    private var _binding: ActivityQuadroBinding? = null
    private val binding get() = _binding!!
    private val args: QuadroFragmentArgs by navArgs()
    private val viewModel: QuadroViewModel by viewModels()

    private val tarefas = mutableListOf<Tarefa>()
    private var estadoAtual = Tarefa.POR_FAZER
    private var filtroPessoa = ""
    private var prazoEscolhido = ""

    private lateinit var adaptador: TarefasAdapter
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var textoVazio: TextView
    private lateinit var linhaFiltros: LinearLayout
    private lateinit var tabs: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityQuadroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Sessao.carregar(requireContext())
        binding.tituloProjeto.text = args.projetoNome

        binding.botaoVoltar.setOnClickListener { findNavController().navigateUp() }
        binding.botaoNovaTarefa.setOnClickListener { dialogTarefa(null) }
        binding.botaoMembros.setOnClickListener {
            findNavController().navigate(
                QuadroFragmentDirections.actionQuadroToMembros(args.projetoId, args.criadorId)
            )
        }
        binding.botaoRelatorio.setOnClickListener {
            findNavController().navigate(
                QuadroFragmentDirections.actionQuadroToRelatorio(args.projetoId)
            )
        }

        adaptador = TarefasAdapter(aoAbrir = { abrirDetalhe(it) }, aoSegurar = { moverTarefa(it) })
        binding.listaTarefas.layoutManager = LinearLayoutManager(requireContext())
        binding.listaTarefas.adapter = adaptador

        swipe = binding.swipeQuadro
        swipe.setOnRefreshListener { viewModel.carregarTudo(args.projetoId) }
        textoVazio = binding.textoVazio
        linhaFiltros = binding.linhaFiltros

        tabs = binding.tabsEstado
        listOf(getString(R.string.por_fazer), getString(R.string.em_curso), getString(R.string.concluida))
            .forEach { tabs.addTab(tabs.newTab().setText(it)) }
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                estadoAtual = when (tab.position) { 0 -> Tarefa.POR_FAZER; 1 -> Tarefa.EM_CURSO; else -> Tarefa.CONCLUIDA }
                refrescar()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        viewModel.membros.observe(viewLifecycleOwner) { construirChips(it) }
        viewModel.tarefas.observe(viewLifecycleOwner) { estado ->
            swipe.isRefreshing = estado is UiState.Loading
            when (estado) {
                is UiState.Sucesso -> { tarefas.clear(); tarefas.addAll(estado.dados); refrescar() }
                is UiState.Erro -> { textoVazio.isVisible = true; aviso(estado.mensagem) }
                else -> Unit
            }
        }
        viewModel.atualizar.observe(viewLifecycleOwner) { viewModel.carregarTudo(args.projetoId) }
        viewModel.erro.observe(viewLifecycleOwner) { aviso(it) }

        viewModel.carregarTudo(args.projetoId)
    }

    private fun construirChips(nomesMembros: List<String>) {
        linhaFiltros.removeAllViews()
        val opcoes = listOf(getString(R.string.todas_as_pessoas)) + nomesMembros
        opcoes.forEach { nome ->
            val chip = layoutInflater.inflate(R.layout.item_chip, linhaFiltros, false) as TextView
            chip.text = nome
            val ativo = nome == filtroPessoa || (filtroPessoa.isBlank() && nome == getString(R.string.todas_as_pessoas))
            chip.background = ContextCompat.getDrawable(requireContext(), if (ativo) R.drawable.bg_chip_ativo else R.drawable.bg_chip)
            chip.setTextColor(ContextCompat.getColor(requireContext(), if (ativo) android.R.color.white else R.color.texto))
            chip.setOnClickListener {
                filtroPessoa = if (nome == getString(R.string.todas_as_pessoas)) "" else nome
                construirChips(nomesMembros); refrescar()
            }
            linhaFiltros.addView(chip)
        }
    }

    // Mostra só as tarefas da coluna e pessoa escolhidas, ordenadas pelo prazo.
    private fun refrescar() {
        val visiveis = tarefas
            .filter { it.status == estadoAtual && (filtroPessoa.isBlank() || it.responsavel == filtroPessoa) }
            .sortedBy { it.dataConclusao ?: "9999-12-31" }
        adaptador.atualizar(visiveis)
        textoVazio.isVisible = visiveis.isEmpty()

        tabs.getTabAt(0)?.text = "${getString(R.string.por_fazer)} (${tarefas.count { it.status == Tarefa.POR_FAZER }})"
        tabs.getTabAt(1)?.text = "${getString(R.string.em_curso)} (${tarefas.count { it.status == Tarefa.EM_CURSO }})"
        tabs.getTabAt(2)?.text = "${getString(R.string.concluida)} (${tarefas.count { it.status == Tarefa.CONCLUIDA }})"
    }

    // Criar tarefa nova (tarefa == null) ou editar uma existente.
    private fun dialogTarefa(tarefa: Tarefa?) {
        val vista = layoutInflater.inflate(R.layout.dialog_tarefa, null)
        val campoTitulo = vista.findViewById<EditText>(R.id.campo_titulo)
        val campoDescricao = vista.findViewById<EditText>(R.id.campo_descricao)
        val campoResponsavel = vista.findViewById<AutoCompleteTextView>(R.id.campo_responsavel)
        val campoPrazo = vista.findViewById<EditText>(R.id.campo_prazo)
        val campoLink = vista.findViewById<EditText>(R.id.campo_link)

        val nomesMembros = viewModel.membros.value ?: emptyList()
        campoResponsavel.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, nomesMembros))
        prazoEscolhido = tarefa?.dataConclusao ?: ""
        if (tarefa != null) {
            campoTitulo.setText(tarefa.titulo)
            campoDescricao.setText(tarefa.descricao)
            campoResponsavel.setText(tarefa.responsavel)
            campoPrazo.setText(dataCurta(tarefa.dataConclusao))
            campoLink.setText(tarefa.link)
        }

        campoPrazo.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, ano, mes, dia ->
                val m = if (mes + 1 < 10) "0${mes + 1}" else "${mes + 1}"
                val d = if (dia < 10) "0$dia" else "$dia"
                prazoEscolhido = "$ano-$m-$d"
                campoPrazo.setText("$d/$m/$ano")
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (tarefa == null) R.string.nova_tarefa else R.string.editar_tarefa)
            .setView(vista)
            .setPositiveButton(R.string.guardar) { _, _ ->
                val titulo = campoTitulo.text.toString().trim()
                val responsavel = campoResponsavel.text.toString().trim()
                if (titulo.isBlank() || responsavel.isBlank() || prazoEscolhido.isBlank()) {
                    aviso("Faltam campos obrigatórios"); return@setPositiveButton
                }
                viewModel.guardar(
                    tarefaId = tarefa?.id,
                    pedido = TarefaPedido(
                        titulo = titulo,
                        descricao = campoDescricao.text.toString().trim(),
                        criador = tarefa?.criador ?: Sessao.nome,
                        responsavel = responsavel,
                        status = tarefa?.status ?: Tarefa.POR_FAZER,
                        dataConclusao = prazoEscolhido,
                        link = campoLink.text.toString().trim(),
                        projetoId = null
                    ),
                    projetoId = args.projetoId
                )
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    // Tocar longo num cartão: escolher para que coluna mover.
    private fun moverTarefa(tarefa: Tarefa) {
        val destinos = listOf(Tarefa.POR_FAZER, Tarefa.EM_CURSO, Tarefa.CONCLUIDA).filter { it != tarefa.status }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.mover_para)
            .setItems(destinos.toTypedArray()) { _, qual ->
                viewModel.mover(tarefa, destinos[qual])
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun abrirDetalhe(tarefa: Tarefa) {
        findNavController().navigate(
            QuadroFragmentDirections.actionQuadroToDetalhe(tarefaId = tarefa.id, projetoId = args.projetoId)
        )
    }

    private fun aviso(mensagem: String) = requireContext().aviso(mensagem)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
