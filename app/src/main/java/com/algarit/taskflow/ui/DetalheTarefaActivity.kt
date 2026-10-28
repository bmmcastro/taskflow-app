package com.algarit.taskflow.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.algarit.taskflow.R
import com.algarit.taskflow.adapter.SubtarefasAdapter
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.model.Subtarefa
import com.algarit.taskflow.model.Tarefa
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import java.util.Calendar

// O detalhe da tarefa: informação, editar, apagar e a checklist de subtarefas.
class DetalheTarefaActivity : AppCompatActivity() {

    private var tarefaId = -1
    private var projetoId = -1
    private var tarefa: Tarefa? = null
    private val nomesMembros = mutableListOf<String>()
    private var prazoEscolhido = ""

    private lateinit var adaptador: SubtarefasAdapter
    private lateinit var campoNova: EditText
    private lateinit var barra: ProgressBar
    private lateinit var textoPercentagem: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aplicarInsets(findViewById(android.R.id.content))
        Api.iniciar(this)
        tarefaId = intent.getIntExtra("tarefa_id", -1)
        projetoId = intent.getIntExtra("projeto_id", -1)
        if (tarefaId <= 0) { finish(); return }

        setContentView(R.layout.activity_detalhe_tarefa)

        findViewById<ImageButton>(R.id.botao_voltar).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.botao_apagar).setOnClickListener { confirmarApagar() }
        findViewById<TextView>(R.id.botao_editar).setOnClickListener { dialogEditar() }

        adaptador = SubtarefasAdapter(aoAlternar = { alternar(it) }, aoApagar = { apagarSubtarefa(it) })
        val lista = findViewById<RecyclerView>(R.id.lista_subtarefas)
        lista.layoutManager = LinearLayoutManager(this)
        lista.adapter = adaptador

        campoNova = findViewById(R.id.campo_nova_subtarefa)
        barra = findViewById(R.id.barra_progresso)
        textoPercentagem = findViewById(R.id.texto_percentagem)
        findViewById<ImageButton>(R.id.botao_adicionar_subtarefa).setOnClickListener { criarSubtarefa() }

        carregarTarefa()
    }

    // A API só lista por projeto, por isso buscamos todas e ficamos com a nossa.
    private fun carregarTarefa() {
        Api.get("/tarefas", mapOf("projeto_id" to projetoId.toString())) { r ->
            r.lista?.let { arr ->
                for (i in 0 until arr.length()) {
                    val t = Tarefa.deJson(arr.getJSONObject(i))
                    if (t.id == tarefaId) { tarefa = t; break }
                }
            }
            if (tarefa == null) { aviso("Tarefa não encontrada"); finish(); return@get }
            preencher()
            carregarMembros()
            carregarSubtarefas()
        }
    }

    private fun carregarMembros() {
        Api.get("/projetos/$projetoId/membros") { r ->
            nomesMembros.clear()
            r.lista?.let { arr -> for (i in 0 until arr.length()) nomesMembros.add(arr.getJSONObject(i).optString("nome")) }
        }
    }

    private fun preencher() {
        val t = tarefa ?: return
        findViewById<TextView>(R.id.detalhe_titulo).text = t.titulo
        val descricao = findViewById<TextView>(R.id.detalhe_descricao)
        if (t.descricao.isBlank()) descricao.visibility = View.GONE
        else { descricao.visibility = View.VISIBLE; descricao.text = t.descricao }
        findViewById<TextView>(R.id.detalhe_avatar).text = t.responsavel.trim().take(1).uppercase()
        findViewById<TextView>(R.id.detalhe_responsavel).text = t.responsavel
        findViewById<TextView>(R.id.detalhe_prazo).text = "Prazo: ${dataCurta(t.dataConclusao)}"
        val estado = findViewById<TextView>(R.id.detalhe_estado)
        estado.text = t.status
        estado.setTextColor(when (t.status) {
            Tarefa.EM_CURSO -> getColor(R.color.azul)
            Tarefa.CONCLUIDA -> getColor(R.color.verde)
            else -> getColor(R.color.texto_secundario)
        })
        val link = findViewById<TextView>(R.id.detalhe_link)
        if (t.link.isBlank()) link.visibility = View.GONE
        else {
            link.visibility = View.VISIBLE
            link.text = t.link
            link.setOnClickListener {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(t.link))) }
                catch (_: Exception) { aviso("Link inválido") }
            }
        }
    }

    private fun carregarSubtarefas() {
        Api.get("/subtarefas", mapOf("tarefa_id" to tarefaId.toString())) { r ->
            val lista = mutableListOf<Subtarefa>()
            r.lista?.let { arr -> for (i in 0 until arr.length()) lista.add(Subtarefa.deJson(arr.getJSONObject(i))) }
            adaptador.atualizar(lista)
            refrescarBarra()
        }
    }

    private fun refrescarBarra() {
        val total = adaptador.total()
        val feitas = adaptador.concluidas()
        val percentagem = if (total == 0) 0 else (feitas * 100) / total
        barra.progress = percentagem
        textoPercentagem.text = "$feitas de $total concluídas ($percentagem%)"
    }

    private fun criarSubtarefa() {
        val texto = campoNova.text.toString().trim()
        if (texto.isBlank()) { aviso("Escreve a subtarefa primeiro"); return }
        Api.post("/subtarefas", JSONObject()
            .put("tarefa_id", tarefaId)
            .put("descricao", texto)) { r ->
            if (r.sucesso) { campoNova.setText(""); carregarSubtarefas() }
            else aviso(r.texto.ifBlank { getString(R.string.erro_rede) })
        }
    }

    private fun alternar(subtarefa: Subtarefa) {
        Api.patch("/subtarefas/${subtarefa.id}", JSONObject().put("concluida", !subtarefa.concluida)) { r ->
            if (r.sucesso) carregarSubtarefas() else aviso(getString(R.string.erro_rede))
        }
    }

    private fun apagarSubtarefa(subtarefa: Subtarefa) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.apagar)
            .setMessage("Apagar \"${subtarefa.descricao}\"?")
            .setPositiveButton(R.string.apagar) { _, _ ->
                Api.delete("/subtarefas/${subtarefa.id}") { r ->
                    if (r.sucesso) carregarSubtarefas() else aviso(getString(R.string.erro_rede))
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    // Editar a tarefa: o mesmo formulário da criação, já preenchido.
    private fun dialogEditar() {
        val t = tarefa ?: return
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_tarefa, null)
        val campoTitulo = vista.findViewById<EditText>(R.id.campo_titulo)
        val campoDescricao = vista.findViewById<EditText>(R.id.campo_descricao)
        val campoResponsavel = vista.findViewById<AutoCompleteTextView>(R.id.campo_responsavel)
        val campoPrazo = vista.findViewById<EditText>(R.id.campo_prazo)
        val campoLink = vista.findViewById<EditText>(R.id.campo_link)

        campoResponsavel.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, nomesMembros))
        campoTitulo.setText(t.titulo)
        campoDescricao.setText(t.descricao)
        campoResponsavel.setText(t.responsavel)
        campoPrazo.setText(dataCurta(t.dataConclusao))
        campoLink.setText(t.link)
        prazoEscolhido = t.dataConclusao ?: ""

        campoPrazo.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, ano, mes, dia ->
                val m = String.format(java.util.Locale.getDefault(), "%02d", mes + 1)
                val d = String.format(java.util.Locale.getDefault(), "%02d", dia)
                prazoEscolhido = "$ano-$m-$d"
                campoPrazo.setText("$d/$m/$ano")
            }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]).show()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.editar_tarefa)
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
                    .put("responsavel", responsavel)
                    .put("status", t.status)
                    .put("data_conclusao", prazoEscolhido)
                    .put("link", campoLink.text.toString().trim())
                Api.patch("/tarefas/${t.id}", corpo) { r ->
                    if (r.sucesso) carregarTarefa() else aviso(r.texto.ifBlank { getString(R.string.erro_rede) })
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun confirmarApagar() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.apagar)
            .setMessage(R.string.apagar_tarefa_pergunta)
            .setPositiveButton(R.string.apagar) { _, _ ->
                Api.delete("/tarefas/$tarefaId") { r ->
                    if (r.sucesso) finish() else aviso(getString(R.string.erro_rede))
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }
}
