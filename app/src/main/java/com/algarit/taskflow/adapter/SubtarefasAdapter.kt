package com.algarit.taskflow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.algarit.taskflow.R
import com.algarit.taskflow.databinding.ItemSubtarefaBinding
import com.algarit.taskflow.model.Subtarefa

// A checklist da tarefa: tocar alterna o feito, tocar longo apaga.
class SubtarefasAdapter(
    private val aoAlternar: (Subtarefa) -> Unit,
    private val aoApagar: (Subtarefa) -> Unit
) : ListAdapter<Subtarefa, SubtarefasAdapter.VH>(DIFERENCA) {

    companion object {
        val DIFERENCA = object : DiffUtil.ItemCallback<Subtarefa>() {
            override fun areItemsTheSame(antigo: Subtarefa, novo: Subtarefa) = antigo.id == novo.id
            override fun areContentsTheSame(antigo: Subtarefa, novo: Subtarefa) = antigo == novo
        }
    }

    fun atualizar(lista: List<Subtarefa>) = submitList(lista)

    // Contagens para a barra de progresso do detalhe.
    fun concluidas() = currentList.count { it.concluida }
    fun total() = currentList.size

    class VH(val binding: ItemSubtarefaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemSubtarefaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val subtarefa = getItem(pos)
        holder.binding.itemFeito.isChecked = subtarefa.concluida
        holder.binding.itemFeito.isEnabled = true
        holder.binding.itemDescricao.text = subtarefa.descricao
        if (subtarefa.concluida) {
            holder.binding.itemDescricao.paintFlags = holder.binding.itemDescricao.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            holder.binding.itemDescricao.setTextColor(ContextCompat.getColor(holder.binding.root.context, R.color.texto_secundario))
        } else {
            holder.binding.itemDescricao.paintFlags = holder.binding.itemDescricao.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.binding.itemDescricao.setTextColor(ContextCompat.getColor(holder.binding.root.context, R.color.texto))
        }
        holder.binding.root.setOnClickListener { aoAlternar(subtarefa) }
        holder.binding.root.setOnLongClickListener { aoApagar(subtarefa); true }
    }
}
