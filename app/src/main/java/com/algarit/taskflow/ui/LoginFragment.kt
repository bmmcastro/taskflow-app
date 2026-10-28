package com.algarit.taskflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.databinding.ActivityLoginBinding
import org.json.JSONObject

// Primeiro ecrã: entrar com a conta (a mesma do site).
// É o destino inicial do grafo de navegação.
class LoginFragment : Fragment() {

    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Api.iniciar(requireContext())
        Sessao.carregar(requireContext())
        if (Sessao.estaLogado()) { irParaProjetos(); return }

        binding.botaoEntrar.setOnClickListener { entrar() }
        binding.botaoDemo.setOnClickListener {
            binding.campoEmail.setText("demo@taskflow.pt")
            binding.campoPassword.setText("1234")
            entrar()
        }
        binding.botaoRegistar.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginToRegisto())
        }
        binding.botaoConvite.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), ConviteActivity::class.java))
        }
    }

    private fun entrar() {
        val email = binding.campoEmail.text.toString().trim()
        val password = binding.campoPassword.text.toString()
        if (email.isBlank() || password.isBlank()) { aviso("Preenche o email e a password"); return }

        binding.botaoEntrar.isEnabled = false
        Api.post("/utilizadores/login", JSONObject().put("email", email).put("password", password)) { r ->
            if (!isAdded) return@post
            binding.botaoEntrar.isEnabled = true
            val utilizador = r.objeto?.optJSONObject("utilizador")
            if (r.sucesso && utilizador != null) {
                Sessao.guardar(requireContext(), utilizador.optInt("id"), utilizador.optString("nome"), utilizador.optString("email"))
                irParaProjetos()
            } else {
                aviso(if (r.codigo == 401) "Email ou password errados" else "Não foi possível entrar")
            }
        }
    }

    // Login aceite: projetos passa a ser a raiz (popUpTo limpa o login da pilha).
    private fun irParaProjetos() {
        findNavController().navigate(LoginFragmentDirections.actionLoginToProjetos())
    }

    private fun aviso(mensagem: String) = requireContext().aviso(mensagem)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
