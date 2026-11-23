package com.algarit.taskflow.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.algarit.taskflow.R
import com.algarit.taskflow.adapter.TarefasAdapter
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.databinding.ActivityQuadroBinding
import com.algarit.taskflow.databinding.DialogTarefaBinding
import com.algarit.taskflow.model.EstadoTarefa
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
    private var estadoAtual = EstadoTarefa.POR_FAZER
    private var filtroPessoa = ""
    private var prazoEscolhido = ""

    private lateinit var adaptador: TarefasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityQuadroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            estadoAtual = EstadoTarefa.entries.getOrElse(savedInstanceState.getInt("estado")) {
                EstadoTarefa.POR_FAZER
            }
            filtroPessoa = savedInstanceState.getString("filtro_pessoa") ?: ""
        }
        Sessao.carregar(requireContext())
        binding.tituloProjeto.text = args.projetoNome

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

        binding.swipeQuadro.setOnRefreshListener { viewModel.carregarTudo(args.projetoId) }

        // Filtro com Switch: ligado, esconde as tarefas concluídas.
        binding.switchEsconderConcluidas.setOnCheckedChangeListener { _, _ -> refrescar() }

        val tabs = binding.tabsEstado
        listOf(getString(R.string.por_fazer), getString(R.string.em_curso), getString(R.string.concluida))
            .forEach { tabs.addTab(tabs.newTab().setText(it)) }
        tabs.getTabAt(estadoAtual.ordinal)?.select()
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                estadoAtual = EstadoTarefa.entries[tab.position]
                refrescar()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        viewModel.membros.observe(viewLifecycleOwner) { construirChips(it) }
        viewModel.tarefas.observe(viewLifecycleOwner) { estado ->
            // ProgressBar visível enquanto os dados são obtidos (e o indicador de
            // atualização no topo, para quem puxou a lista para atualizar).
            binding.loading.isVisible = estado is UiState.Loading
            binding.swipeQuadro.isRefreshing = estado is UiState.Loading
            when (estado) {
                is UiState.Sucesso -> {
                    tarefas.clear(); tarefas.addAll(estado.dados)
                    // O texto de "sem tarefas" volta ao normal (podia estar a mostrar um erro).
                    binding.textoVazio.setText(R.string.sem_tarefas)
                    refrescar()
                }
                is UiState.Erro -> {
                    // Com tarefas já no ecrã, elas ficam como estão: o texto de "sem tarefas"
                    // só se mostra se ainda não houver nada, e nesse caso com o motivo do erro.
                    if (adaptador.itemCount == 0) {
                        binding.textoVazio.text = estado.mensagem
                        binding.textoVazio.isVisible = true
                    }
                    aviso(estado.mensagem)
                }
                else -> Unit
            }
        }
        viewModel.atualizar.observe(viewLifecycleOwner) { viewModel.carregarTudo(args.projetoId) }
        viewModel.erro.observe(viewLifecycleOwner) { aviso(it) }

        viewModel.carregarTudo(args.projetoId)
    }

    private fun construirChips(nomesMembros: List<String>) {
        val linhaFiltros = binding.linhaFiltros
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
    // Com o switch ligado, as concluídas desaparecem de todas as colunas.
    private fun refrescar() {
        val esconderConcluidas = binding.switchEsconderConcluidas.isChecked
        val visiveis = tarefas
            .filter { EstadoTarefa.de(it.status) == estadoAtual && (filtroPessoa.isBlank() || it.responsavel == filtroPessoa) }
            .filter { !esconderConcluidas || EstadoTarefa.de(it.status) != EstadoTarefa.CONCLUIDA }
            .sortedBy { it.dataConclusao ?: "9999-12-31" }
        adaptador.atualizar(visiveis)
        binding.textoVazio.isVisible = visiveis.isEmpty()

        binding.tabsEstado.getTabAt(0)?.text = "${getString(R.string.por_fazer)} (${tarefas.count { EstadoTarefa.de(it.status) == EstadoTarefa.POR_FAZER }})"
        binding.tabsEstado.getTabAt(1)?.text = "${getString(R.string.em_curso)} (${tarefas.count { EstadoTarefa.de(it.status) == EstadoTarefa.EM_CURSO }})"
        binding.tabsEstado.getTabAt(2)?.text = "${getString(R.string.concluida)} (${tarefas.count { EstadoTarefa.de(it.status) == EstadoTarefa.CONCLUIDA }})"
    }

    // Criar tarefa nova (tarefa == null) ou editar uma existente.
    private fun dialogTarefa(tarefa: Tarefa?) {
        // ViewBinding também nos diálogos: sem findViewById.
        val dialogo = DialogTarefaBinding.inflate(layoutInflater)
        val campoTitulo = dialogo.campoTitulo
        val campoResponsavel = dialogo.campoResponsavel

        val nomesMembros = viewModel.membros.value ?: emptyList()
        campoResponsavel.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, nomesMembros))
        campoResponsavel.setOnClickListener { campoResponsavel.showDropDown() }
        // Spinner com as colunas do quadro: ao editar começa na coluna atual.
        dialogo.spinnerEstado.mostrarEstados(EstadoTarefa.de(tarefa?.status))
        prazoEscolhido = tarefa?.dataConclusao ?: ""
        if (tarefa != null) {
            campoTitulo.setText(tarefa.titulo)
            dialogo.campoDescricao.setText(tarefa.descricao)
            campoResponsavel.setText(tarefa.responsavel)
            dialogo.campoPrazo.setText(dataCurta(tarefa.dataConclusao))
            dialogo.campoLink.setText(tarefa.link.orEmpty())
        }

        dialogo.campoPrazo.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, ano, mes, dia ->
                prazoEscolhido = String.format(java.util.Locale.US, "%04d-%02d-%02d", ano, mes + 1, dia)
                dialogo.campoPrazo.setText("$dia/${mes + 1}/$ano")
                dialogo.campoPrazo.error = null
            }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]).show()
        }

        val alerta = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (tarefa == null) R.string.nova_tarefa else R.string.editar_tarefa)
            .setView(dialogo.root)
            .setPositiveButton(R.string.guardar, null)
            .setNegativeButton(R.string.cancelar, null)
            .show()
        // Mantém o formulário aberto enquanto houver campos por preencher.
        alerta.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val titulo = campoTitulo.text.toString().trim()
            val responsavel = campoResponsavel.text.toString().trim()
            // Validação com setError no próprio campo.
            var valido = true
            if (titulo.isBlank()) { campoTitulo.error = getString(R.string.erro_titulo); valido = false }
            if (responsavel.isBlank()) { campoResponsavel.error = getString(R.string.erro_responsavel); valido = false }
            if (prazoEscolhido.isBlank()) { dialogo.campoPrazo.error = getString(R.string.erro_prazo); valido = false }
            if (!valido) return@setOnClickListener
            viewModel.guardar(
                tarefaId = tarefa?.id,
                pedido = TarefaPedido(
                    titulo = titulo,
                    descricao = dialogo.campoDescricao.text.toString().trim(),
                    criador = tarefa?.criador ?: Sessao.nome,
                    responsavel = responsavel,
                    status = dialogo.spinnerEstado.estadoEscolhido.rotulo,
                    dataConclusao = prazoEscolhido,
                    link = dialogo.campoLink.text.toString().trim(),
                    projetoId = null
                ),
                projetoId = args.projetoId
            )
            alerta.dismiss()
        }
    }

    // Tocar longo num cartão: escolher para que coluna mover.
    private fun moverTarefa(tarefa: Tarefa) {
        val destinos = EstadoTarefa.entries.filter { it.rotulo != tarefa.status }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.mover_para)
            .setItems(destinos.map { it.rotulo }.toTypedArray()) { _, qual ->
                viewModel.mover(tarefa, destinos[qual].rotulo)
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun abrirDetalhe(tarefa: Tarefa) {
        if (findNavController().currentDestination?.id != R.id.quadroFragment) return
        findNavController().navigate(
            QuadroFragmentDirections.actionQuadroToDetalhe(tarefaId = tarefa.id, projetoId = args.projetoId)
        )
    }

    private fun aviso(mensagem: String) = requireContext().aviso(mensagem)

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("estado", estadoAtual.ordinal)
        outState.putString("filtro_pessoa", filtroPessoa)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
