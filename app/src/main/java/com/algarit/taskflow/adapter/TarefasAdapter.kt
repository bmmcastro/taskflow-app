package com.algarit.taskflow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.algarit.taskflow.R
import com.algarit.taskflow.databinding.ItemTarefaBinding
import com.algarit.taskflow.ui.dataCurta
import com.algarit.taskflow.model.Tarefa

// Os cartões do quadro: tocar abre o detalhe, tocar longo permite mover de coluna.
// ListAdapter + DiffUtil: só o que mudou é redesenhado, com animações.
class TarefasAdapter(
    private val aoAbrir: (Tarefa) -> Unit,
    private val aoSegurar: (Tarefa) -> Unit
) : ListAdapter<Tarefa, TarefasAdapter.VH>(DIFERENCA) {

    companion object {
        val DIFERENCA = object : DiffUtil.ItemCallback<Tarefa>() {
            override fun areItemsTheSame(antigo: Tarefa, novo: Tarefa) = antigo.id == novo.id
            override fun areContentsTheSame(antigo: Tarefa, novo: Tarefa) = antigo == novo
        }
    }

    fun atualizar(lista: List<Tarefa>) = submitList(lista)

    class VH(val binding: ItemTarefaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemTarefaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val tarefa = getItem(pos)
        holder.binding.itemAvatar.text = tarefa.responsavel.trim().take(1).uppercase()
        holder.binding.itemTitulo.text = tarefa.titulo
        holder.binding.itemResponsavel.text = tarefa.responsavel
        val hoje = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        if (!tarefa.dataConclusao.isNullOrBlank() && tarefa.dataConclusao < hoje && com.algarit.taskflow.model.EstadoTarefa.de(tarefa.status) != com.algarit.taskflow.model.EstadoTarefa.CONCLUIDA) {
            holder.binding.itemPrazo.setTextColor(ContextCompat.getColor(holder.binding.root.context, R.color.vermelho))
        } else {
            holder.binding.itemPrazo.setTextColor(ContextCompat.getColor(holder.binding.root.context, R.color.texto_secundario))
        }
        holder.binding.itemPrazo.text = dataCurta(tarefa.dataConclusao)

        holder.binding.root.setOnClickListener { aoAbrir(tarefa) }
        holder.binding.root.setOnLongClickListener { aoSegurar(tarefa); true }
    }
}
