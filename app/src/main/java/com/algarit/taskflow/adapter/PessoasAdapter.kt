package com.algarit.taskflow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.algarit.taskflow.databinding.ItemPessoaBinding

// Uma pessoa e a contagem das suas tarefas (uma linha do relatório).
data class TarefasPorPessoa(val nome: String, val quantidade: Int)

// Lista "Tarefas por pessoa" do relatório: nome à esquerda, contagem à direita.
// ListAdapter + DiffUtil, como as restantes listas da app.
class PessoasAdapter : ListAdapter<TarefasPorPessoa, PessoasAdapter.VH>(DIFERENCA) {

    companion object {
        // O nome diz se é a mesma pessoa; a data class diz se a contagem mudou.
        val DIFERENCA = object : DiffUtil.ItemCallback<TarefasPorPessoa>() {
            override fun areItemsTheSame(antigo: TarefasPorPessoa, novo: TarefasPorPessoa) = antigo.nome == novo.nome
            override fun areContentsTheSame(antigo: TarefasPorPessoa, novo: TarefasPorPessoa) = antigo == novo
        }
    }

    fun atualizar(lista: List<TarefasPorPessoa>) = submitList(lista)

    class VH(val binding: ItemPessoaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemPessoaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val pessoa = getItem(pos)
        holder.binding.pessoaNome.text = pessoa.nome
        holder.binding.pessoaQuantidade.text = pessoa.quantidade.toString()
    }
}
