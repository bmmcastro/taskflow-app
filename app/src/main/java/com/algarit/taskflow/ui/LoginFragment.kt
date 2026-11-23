package com.algarit.taskflow.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.algarit.taskflow.R
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.databinding.ActivityLoginBinding

// Primeiro ecrã: entrar com a conta (a mesma do site).
// É o destino inicial do grafo de navegação.
class LoginFragment : Fragment() {

    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Sessao.carregar(requireContext())
        if (Sessao.estaLogado()) { irParaProjetos(); return }

        binding.botaoEntrar.setOnClickListener { entrar() }
        binding.botaoDemo.setOnClickListener {
            binding.campoEmail.setText(DEMO_EMAIL)
            binding.campoPassword.setText(DEMO_PASSWORD)
            entrar()
        }
        binding.botaoRegistar.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginToRegisto())
        }
        binding.botaoConvite.setOnClickListener {
            startActivity(Intent(requireContext(), ConviteActivity::class.java))
        }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            // ProgressBar visível enquanto o login está a ser validado no servidor.
            binding.loading.isVisible = estado is UiState.Loading
            binding.botaoEntrar.isEnabled = estado !is UiState.Loading
            if (estado is UiState.Sucesso) irParaProjetos()
            else if (estado is UiState.Erro) aviso(estado.mensagem)
        }
    }

    private fun entrar() {
        val email = binding.campoEmail.text.toString().trim()
        val password = binding.campoPassword.text.toString()
        // Validação com setError no próprio campo: preenchido e email com formato válido.
        var valido = true
        if (email.isBlank()) { binding.campoEmail.error = getString(R.string.erro_campos); valido = false }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.campoEmail.error = getString(R.string.erro_email); valido = false }
        if (password.isBlank()) { binding.campoPassword.error = getString(R.string.erro_campos); valido = false }
        if (!valido) return
        viewModel.entrar(email, password)
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

    companion object {
        // Conta de demonstração (a mesma que está no README e no site).
        private const val DEMO_EMAIL = "demo@taskflow.pt"
        private const val DEMO_PASSWORD = "1234"
    }
}
