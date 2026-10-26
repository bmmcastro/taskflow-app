package com.algarit.taskflow.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.algarit.taskflow.R
import com.algarit.taskflow.model.Subtarefa

// A checklist da tarefa: tocar alterna o feito, tocar longo apaga.
class SubtarefasAdapter(
    private val aoAlternar: (Subtarefa) -> Unit,
    private val aoApagar: (Subtarefa) -> Unit
) : RecyclerView.Adapter<SubtarefasAdapter.VH>() {

    private val itens = mutableListOf<Subtarefa>()

    fun atualizar(lista: List<Subtarefa>) {
        itens.clear()
        itens.addAll(lista)
        notifyDataSetChanged()
    }

    fun concluidas() = itens.count { it.concluida }
    fun total() = itens.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val feito: CheckBox = v.findViewById(R.id.item_feito)
        val descricao: TextView = v.findViewById(R.id.item_descricao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_subtarefa, parent, false))

    override fun getItemCount() = itens.size

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val subtarefa = itens[pos]
        holder.feito.isChecked = subtarefa.concluida
        holder.feito.isEnabled = true
        holder.descricao.text = subtarefa.descricao
        if (subtarefa.concluida) {
            holder.descricao.paintFlags = holder.descricao.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            holder.descricao.setTextColor(holder.descricao.context.getColor(R.color.texto_secundario))
        } else {
            holder.descricao.paintFlags = holder.descricao.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.descricao.setTextColor(holder.descricao.context.getColor(R.color.texto))
        }
        holder.itemView.setOnClickListener { aoAlternar(subtarefa) }
        holder.itemView.setOnLongClickListener { aoApagar(subtarefa); true }
    }
}
