package com.algarit.taskflow.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.algarit.taskflow.R
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.model.Projeto

// A lista de projetos: cada cartão abre o quadro, um toque longo dá o menu do projeto.
class ProjetosAdapter(
    private val aoAbrir: (Projeto) -> Unit,
    private val aoSegurar: (Projeto) -> Unit
) : RecyclerView.Adapter<ProjetosAdapter.VH>() {

    private val itens = mutableListOf<Projeto>()

    fun atualizar(lista: List<Projeto>) {
        itens.clear()
        itens.addAll(lista)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val nome: TextView = v.findViewById(R.id.item_nome)
        val descricao: TextView = v.findViewById(R.id.item_descricao)
        val tagCriador: TextView = v.findViewById(R.id.item_tag_criador)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_projeto, parent, false))

    override fun getItemCount() = itens.size

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val projeto = itens[pos]
        holder.nome.text = projeto.nome
        if (projeto.descricao.isBlank()) holder.descricao.visibility = View.GONE
        else { holder.descricao.visibility = View.VISIBLE; holder.descricao.text = projeto.descricao }
        holder.tagCriador.visibility =
            if (projeto.criadorId == Sessao.utilizadorId) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { aoAbrir(projeto) }
        holder.itemView.setOnLongClickListener { aoSegurar(projeto); true }
    }
}
