package com.algarit.taskflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.databinding.ActivityPerfilBinding
import org.json.JSONObject

// A conta do utilizador: mudar nome, email ou password, e terminar sessão.
class PerfilFragment : Fragment() {

    private var _binding: ActivityPerfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Api.iniciar(requireContext())
        Sessao.carregar(requireContext())

        binding.botaoVoltar.setOnClickListener { findNavController().navigateUp() }

        binding.perfilAvatar.text = Sessao.nome.trim().take(1).uppercase()
        binding.perfilNome.setText(Sessao.nome)
        binding.perfilEmail.setText(Sessao.email)

        binding.botaoGuardarPerfil.setOnClickListener {
            val nome = binding.perfilNome.text.toString().trim()
            val email = binding.perfilEmail.text.toString().trim()
            if (nome.isBlank() || email.isBlank()) { aviso("O nome e o email são obrigatórios"); return@setOnClickListener }
            val corpo = JSONObject().put("nome", nome).put("email", email)
            if (binding.perfilPassword.text.isNotBlank()) corpo.put("password", binding.perfilPassword.text.toString())

            Api.patch("/utilizadores/${Sessao.utilizadorId}", corpo) { r ->
                if (!isAdded) return@patch
                val obj = r.objeto
                if (r.sucesso && obj != null) {
                    Sessao.guardar(requireContext(), obj.optInt("id"), obj.optString("nome"), obj.optString("email"))
                    binding.perfilAvatar.text = nome.trim().take(1).uppercase()
                    binding.perfilPassword.setText("")
                    aviso("Conta atualizada")
                } else aviso(r.texto.ifBlank { getString(com.algarit.taskflow.R.string.erro_rede) })
            }
        }

        binding.botaoTerminarSessao.setOnClickListener {
            Sessao.sair(requireContext())
            // Terminar sessão limpa a pilha toda e volta ao login.
            findNavController().navigate(PerfilFragmentDirections.actionPerfilToLogin())
        }
    }

    private fun aviso(mensagem: String) = requireContext().aviso(mensagem)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
