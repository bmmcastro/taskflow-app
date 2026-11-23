package com.algarit.taskflow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.algarit.taskflow.R
import com.algarit.taskflow.databinding.ItemMembroBinding
import com.algarit.taskflow.model.Utilizador

// A lista de membros do projeto: tocar longo permite remover.
// ListAdapter + DiffUtil: só o que mudou é redesenhado.
class MembrosAdapter(
    private val aoSegurar: (Utilizador) -> Unit
) : ListAdapter<Utilizador, MembrosAdapter.VH>(DIFERENCA) {

    companion object {
        // O id diz se é a mesma pessoa; a data class diz se algum campo mudou.
        val DIFERENCA = object : DiffUtil.ItemCallback<Utilizador>() {
            override fun areItemsTheSame(antigo: Utilizador, novo: Utilizador) = antigo.id == novo.id
            override fun areContentsTheSame(antigo: Utilizador, novo: Utilizador) = antigo == novo
        }
    }

    fun atualizar(lista: List<Utilizador>) = submitList(lista)

    // Quem criou o projeto mostra a etiqueta "Criador".
    var criadorId: Int = -1

    class VH(val binding: ItemMembroBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemMembroBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val membro = getItem(pos)
        holder.binding.itemAvatar.text = membro.nome.trim().take(1).uppercase()
        holder.binding.itemNome.text = membro.nome
        holder.binding.itemEmail.text = membro.email
        val eCriador = membro.id == criadorId
        holder.binding.itemTagCriador.isVisible = eCriador
        if (eCriador) holder.binding.itemTagCriador.text = holder.binding.root.context.getString(R.string.criador)
        holder.binding.root.setOnLongClickListener { aoSegurar(membro); true }
    }
}
