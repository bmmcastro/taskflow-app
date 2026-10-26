package com.algarit.taskflow.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.algarit.taskflow.R
import com.algarit.taskflow.adapter.TarefasAdapter
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.model.Tarefa
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import org.json.JSONObject
import java.util.Calendar

// O quadro do projeto: 3 colunas em tabs, filtro por pessoa e cartões de tarefa.
class QuadroActivity : AppCompatActivity() {

    private var projetoId = -1
    private var criadorId = -1
    private val tarefas = mutableListOf<Tarefa>()
    private val nomesMembros = mutableListOf<String>()
    private var estadoAtual = Tarefa.POR_FAZER
    private var filtroPessoa = ""
    private var prazoEscolhido = ""

    private lateinit var adaptador: TarefasAdapter
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var textoVazio: TextView
    private lateinit var linhaFiltros: LinearLayout
    private lateinit var tabs: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aplicarInsets(findViewById(android.R.id.content))
        Api.iniciar(this)
        Sessao.carregar(this)
        projetoId = intent.getIntExtra("projeto_id", -1)
        criadorId = intent.getIntExtra("criador_id", -1)
        if (projetoId <= 0) { finish(); return }

        setContentView(R.layout.activity_quadro)
        findViewById<TextView>(R.id.titulo_projeto).text = intent.getStringExtra("projeto_nome") ?: ""

        findViewById<ImageButton>(R.id.botao_voltar).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.botao_nova_tarefa).setOnClickListener { dialogTarefa(null) }
        findViewById<ImageButton>(R.id.botao_membros).setOnClickListener {
            val i = Intent(this, MembrosActivity::class.java)
            i.putExtra("projeto_id", projetoId); i.putExtra("criador_id", criadorId); startActivity(i)
        }
        findViewById<ImageButton>(R.id.botao_relatorio).setOnClickListener {
            val i = Intent(this, RelatorioActivity::class.java)
            i.putExtra("projeto_id", projetoId); startActivity(i)
        }

        adaptador = TarefasAdapter(aoAbrir = { abrirDetalhe(it) }, aoSegurar = { moverTarefa(it) })
        val lista = findViewById<RecyclerView>(R.id.lista_tarefas)
        lista.layoutManager = LinearLayoutManager(this)
        lista.adapter = adaptador

        swipe = findViewById(R.id.swipe_quadro)
        swipe.setOnRefreshListener { carregarTudo() }
        textoVazio = findViewById(R.id.texto_vazio)
        linhaFiltros = findViewById(R.id.linha_filtros)

        tabs = findViewById(R.id.tabs_estado)
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

        carregarTudo()
    }

    private fun carregarTudo() {
        swipe.isRefreshing = true
        Api.get("/projetos/$projetoId/membros") { r ->
            nomesMembros.clear()
            r.lista?.let { arr -> for (i in 0 until arr.length()) nomesMembros.add(arr.getJSONObject(i).optString("nome")) }
            construirChips()
        }
        Api.get("/tarefas", mapOf("projeto_id" to projetoId.toString())) { r ->
            swipe.isRefreshing = false
            tarefas.clear()
            r.lista?.let { arr -> for (i in 0 until arr.length()) tarefas.add(Tarefa.deJson(arr.getJSONObject(i))) }
            if (!r.sucesso) aviso(getString(R.string.erro_rede))
            refrescar()
        }
    }

    private fun construirChips() {
        linhaFiltros.removeAllViews()
        val opcoes = listOf(getString(R.string.todas_as_pessoas)) + nomesMembros
        opcoes.forEach { nome ->
            val chip = layoutInflater.inflate(R.layout.item_chip, linhaFiltros, false) as TextView
            chip.text = nome
            val ativo = nome == filtroPessoa || (filtroPessoa.isBlank() && nome == getString(R.string.todas_as_pessoas))
            chip.background = getDrawable(if (ativo) R.drawable.bg_chip_ativo else R.drawable.bg_chip)
            chip.setTextColor(getColor(if (ativo) android.R.color.white else R.color.texto))
            chip.setOnClickListener {
                filtroPessoa = if (nome == getString(R.string.todas_as_pessoas)) "" else nome
                construirChips(); refrescar()
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
        textoVazio.visibility = if (visiveis.isEmpty()) View.VISIBLE else View.GONE

        tabs.getTabAt(0)?.text = "${getString(R.string.por_fazer)} (${tarefas.count { it.status == Tarefa.POR_FAZER }})"
        tabs.getTabAt(1)?.text = "${getString(R.string.em_curso)} (${tarefas.count { it.status == Tarefa.EM_CURSO }})"
        tabs.getTabAt(2)?.text = "${getString(R.string.concluida)} (${tarefas.count { it.status == Tarefa.CONCLUIDA }})"
    }

    // Criar tarefa nova (tarefa == null) ou editar uma existente.
    private fun dialogTarefa(tarefa: Tarefa?) {
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_tarefa, null)
        val campoTitulo = vista.findViewById<EditText>(R.id.campo_titulo)
        val campoDescricao = vista.findViewById<EditText>(R.id.campo_descricao)
        val campoResponsavel = vista.findViewById<AutoCompleteTextView>(R.id.campo_responsavel)
        val campoPrazo = vista.findViewById<EditText>(R.id.campo_prazo)
        val campoLink = vista.findViewById<EditText>(R.id.campo_link)

        campoResponsavel.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, nomesMembros))
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
            DatePickerDialog(this, { _, ano, mes, dia ->
                val m = if (mes + 1 < 10) "0${mes + 1}" else "${mes + 1}"
                val d = if (dia < 10) "0$dia" else "$dia"
                prazoEscolhido = "$ano-$m-$d"
                campoPrazo.setText("$d/$m/$ano")
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (tarefa == null) R.string.nova_tarefa else R.string.editar_tarefa)
            .setView(vista)
            .setPositiveButton(R.string.guardar) { _, _ ->
                val titulo = campoTitulo.text.toString().trim()
                val responsavel = campoResponsavel.text.toString().trim()
                if (titulo.isBlank() || responsavel.isBlank() || prazoEscolhido.isBlank()) {
                    aviso("Faltam campos obrigatórios"); return@setPositiveButton
                }
                val corpo = JSONObject()
                    .put("titulo", titulo)
                    .put("descricao", campoDescricao.text.toString().trim())
                    .put("criador", tarefa?.criador ?: Sessao.nome)
                    .put("responsavel", responsavel)
                    .put("status", tarefa?.status ?: Tarefa.POR_FAZER)
                    .put("data_conclusao", prazoEscolhido)
                    .put("link", campoLink.text.toString().trim())
                if (tarefa == null) {
                    corpo.put("projeto_id", projetoId)
                    Api.post("/tarefas", corpo) { r -> depoisDeGuardar(r) }
                } else {
                    Api.patch("/tarefas/${tarefa.id}", corpo) { r -> depoisDeGuardar(r) }
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun depoisDeGuardar(r: Api.Resposta) {
        if (r.sucesso) carregarTudo() else aviso(r.texto.ifBlank { getString(R.string.erro_rede) })
    }

    // Tocar longo num cartão: escolher para que coluna mover.
    private fun moverTarefa(tarefa: Tarefa) {
        val destinos = listOf(Tarefa.POR_FAZER, Tarefa.EM_CURSO, Tarefa.CONCLUIDA).filter { it != tarefa.status }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.mover_para)
            .setItems(destinos.toTypedArray()) { _, qual ->
                val corpo = JSONObject()
                    .put("titulo", tarefa.titulo)
                    .put("descricao", tarefa.descricao)
                    .put("responsavel", tarefa.responsavel)
                    .put("status", destinos[qual])
                    .put("data_conclusao", tarefa.dataConclusao ?: "")
                    .put("link", tarefa.link)
                Api.patch("/tarefas/${tarefa.id}", corpo) { r -> depoisDeGuardar(r) }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun abrirDetalhe(tarefa: Tarefa) {
        val i = Intent(this, DetalheTarefaActivity::class.java)
        i.putExtra("tarefa_id", tarefa.id)
        startActivity(i)
    }
}
