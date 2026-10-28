package com.algarit.taskflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.databinding.ActivityProjetosBinding
import com.algarit.taskflow.adapter.ProjetosAdapter
import com.algarit.taskflow.model.Projeto
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject

// Ecrã principal depois do login: os projetos onde sou membro.
class ProjetosFragment : Fragment() {

    private var _binding: ActivityProjetosBinding? = null
    private val binding get() = _binding!!
    private lateinit var adaptador: ProjetosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityProjetosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Api.iniciar(requireContext())
        Sessao.carregar(requireContext())
        if (!Sessao.estaLogado()) { irParaLogin(); return }

        adaptador = ProjetosAdapter(aoAbrir = { abrirQuadro(it) }, aoSegurar = { menuDoProjeto(it) })
        binding.listaProjetos.layoutManager = LinearLayoutManager(requireContext())
        binding.listaProjetos.adapter = adaptador

        binding.swipeProjetos.setOnRefreshListener { carregar() }
        binding.botaoNovo.setOnClickListener { dialogNovoProjeto() }
        binding.botaoPerfil.setOnClickListener {
            findNavController().navigate(ProjetosFragmentDirections.actionProjetosToPerfil())
        }

        carregar()
    }

    // Ao voltar de outros ecrãs, recarregamos para mostrar as novidades.
    override fun onResume() {
        super.onResume()
        if (Sessao.estaLogado() && _binding != null) carregar()
    }

    private fun carregar() {
        binding.swipeProjetos.isRefreshing = true
        Api.get("/projetos", mapOf("utilizador_id" to Sessao.utilizadorId.toString())) { r ->
            if (!isAdded) return@get
            binding.swipeProjetos.isRefreshing = false
            val lista = mutableListOf<Projeto>()
            r.lista?.let { arr -> for (i in 0 until arr.length()) lista.add(Projeto.deJson(arr.getJSONObject(i))) }
            adaptador.atualizar(lista)
            binding.textoVazio.isVisible = lista.isEmpty()
            if (!r.sucesso) aviso(getString(com.algarit.taskflow.R.string.erro_rede))
        }
    }

    private fun dialogNovoProjeto() {
        val vista = layoutInflater.inflate(com.algarit.taskflow.R.layout.dialog_novo_projeto, null)
        val campoNome = vista.findViewById<com.google.android.material.textfield.TextInputEditText>(com.algarit.taskflow.R.id.dialog_nome)
        val campoDescricao = vista.findViewById<com.google.android.material.textfield.TextInputEditText>(com.algarit.taskflow.R.id.dialog_descricao)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(com.algarit.taskflow.R.string.novo_projeto)
            .setView(vista)
            .setPositiveButton(com.algarit.taskflow.R.string.guardar) { _, _ ->
                val nome = campoNome.text.toString().trim()
                if (nome.isBlank()) { aviso("Dá um nome ao projeto"); return@setPositiveButton }
                Api.post("/projetos", JSONObject()
                    .put("nome", nome)
                    .put("descricao", campoDescricao.text.toString().trim())
                    .put("utilizador_id", Sessao.utilizadorId)) { r ->
                    if (!isAdded) return@post
                    if (r.sucesso) carregar() else aviso(r.texto.ifBlank { getString(com.algarit.taskflow.R.string.erro_rede) })
                }
            }
            .setNegativeButton(com.algarit.taskflow.R.string.cancelar, null)
            .show()
    }

    private fun menuDoProjeto(projeto: Projeto) {
        if (projeto.criadorId != Sessao.utilizadorId) { aviso("Só o criador pode apagar o projeto"); return }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(projeto.nome)
            .setMessage(com.algarit.taskflow.R.string.apagar_projeto_pergunta)
            .setPositiveButton(com.algarit.taskflow.R.string.apagar) { _, _ ->
                Api.delete("/projetos/${projeto.id}?utilizador_id=${Sessao.utilizadorId}") { r ->
                    if (!isAdded) return@delete
                    if (r.sucesso) carregar() else aviso(r.texto.ifBlank { getString(com.algarit.taskflow.R.string.erro_rede) })
                }
            }
            .setNegativeButton(com.algarit.taskflow.R.string.cancelar, null)
            .show()
    }

    // Safe Args: o id e o nome do projeto viajam com o tipo certo para o quadro.
    private fun abrirQuadro(projeto: Projeto) {
        findNavController().navigate(
            ProjetosFragmentDirections.actionProjetosToQuadro(
                projetoId = projeto.id,
                projetoNome = projeto.nome,
                criadorId = projeto.criadorId
            )
        )
    }

    private fun irParaLogin() {
        findNavController().navigate(ProjetosFragmentDirections.actionProjetosToLogin())
    }

    private fun aviso(mensagem: String) = requireContext().aviso(mensagem)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
