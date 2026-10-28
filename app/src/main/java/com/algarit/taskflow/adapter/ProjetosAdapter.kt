package com.algarit.taskflow.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.databinding.ItemProjetoBinding
import com.algarit.taskflow.model.Projeto

// A lista de projetos: cada cartão abre o quadro, um toque longo dá o menu do projeto.
// ListAdapter + DiffUtil: ao submeter uma lista nova, só o que mudou é redesenhado.
class ProjetosAdapter(
    private val aoAbrir: (Projeto) -> Unit,
    private val aoSegurar: (Projeto) -> Unit
) : ListAdapter<Projeto, ProjetosAdapter.VH>(DIFERENCA) {

    companion object {
        // Compara a lista antiga com a nova: o id diz se é o mesmo projeto,
        // a data class diz se algum campo mudou.
        val DIFERENCA = object : DiffUtil.ItemCallback<Projeto>() {
            override fun areItemsTheSame(antigo: Projeto, novo: Projeto) = antigo.id == novo.id
            override fun areContentsTheSame(antigo: Projeto, novo: Projeto) = antigo == novo
        }
    }

    fun atualizar(lista: List<Projeto>) = submitList(lista)

    class VH(val binding: ItemProjetoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemProjetoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val projeto = getItem(pos)
        holder.binding.itemNome.text = projeto.nome
        if (projeto.descricao.isBlank()) holder.binding.itemDescricao.visibility = View.GONE
        else {
            holder.binding.itemDescricao.visibility = View.VISIBLE
            holder.binding.itemDescricao.text = projeto.descricao
        }
        holder.binding.itemTagCriador.visibility =
            if (projeto.criadorId == Sessao.utilizadorId) View.VISIBLE else View.GONE

        holder.binding.root.setOnClickListener { aoAbrir(projeto) }
        holder.binding.root.setOnLongClickListener { aoSegurar(projeto); true }
    }
}
