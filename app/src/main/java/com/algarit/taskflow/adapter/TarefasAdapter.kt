package com.algarit.taskflow.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.algarit.taskflow.R
import com.algarit.taskflow.ui.dataCurta
import com.algarit.taskflow.model.Tarefa

// Os cartões do quadro: tocar abre o detalhe, tocar longo permite mover de coluna.
class TarefasAdapter(
    private val aoAbrir: (Tarefa) -> Unit,
    private val aoSegurar: (Tarefa) -> Unit
) : RecyclerView.Adapter<TarefasAdapter.VH>() {

    private val itens = mutableListOf<Tarefa>()

    fun atualizar(lista: List<Tarefa>) {
        itens.clear()
        itens.addAll(lista)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: TextView = v.findViewById(R.id.item_avatar)
        val titulo: TextView = v.findViewById(R.id.item_titulo)
        val responsavel: TextView = v.findViewById(R.id.item_responsavel)
        val prazo: TextView = v.findViewById(R.id.item_prazo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_tarefa, parent, false))

    override fun getItemCount() = itens.size

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val tarefa = itens[pos]
        holder.avatar.text = tarefa.responsavel.trim().take(1).uppercase()
        holder.titulo.text = tarefa.titulo
        holder.responsavel.text = tarefa.responsavel
        val hoje = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        if (!tarefa.dataConclusao.isNullOrBlank() && tarefa.dataConclusao < hoje && tarefa.status != Tarefa.CONCLUIDA) {
            holder.prazo.setTextColor(holder.prazo.context.getColor(R.color.vermelho))
        } else {
            holder.prazo.setTextColor(holder.prazo.context.getColor(R.color.texto_secundario))
        }
        holder.prazo.text = dataCurta(tarefa.dataConclusao)

        holder.itemView.setOnClickListener { aoAbrir(tarefa) }
        holder.itemView.setOnLongClickListener { aoSegurar(tarefa); true }
    }
}
