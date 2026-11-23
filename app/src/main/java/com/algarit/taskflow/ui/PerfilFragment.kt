package com.algarit.taskflow.ui

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
import com.algarit.taskflow.databinding.ActivityPerfilBinding

// A conta do utilizador: mudar nome, email ou password, e terminar sessão.
class PerfilFragment : Fragment() {

    private var _binding: ActivityPerfilBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PerfilViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Sessao.carregar(requireContext())

        mostrarDados()

        binding.botaoGuardarPerfil.setOnClickListener {
            val nome = binding.perfilNome.text.toString().trim()
            val email = binding.perfilEmail.text.toString().trim()
            // Validação com setError no próprio campo.
            var valido = true
            if (nome.isBlank()) { binding.perfilNome.error = getString(R.string.erro_campos); valido = false }
            if (email.isBlank()) { binding.perfilEmail.error = getString(R.string.erro_campos); valido = false }
            else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.perfilEmail.error = getString(R.string.erro_email); valido = false }
            if (!valido) return@setOnClickListener
            viewModel.guardar(nome, email, binding.perfilPassword.text.toString())
        }

        viewModel.aGuardar.observe(viewLifecycleOwner) { aGuardar ->
            // ProgressBar visível enquanto a conta está a ser guardada.
            binding.loading.isVisible = aGuardar
            binding.botaoGuardarPerfil.isEnabled = !aGuardar
        }
        viewModel.atualizado.observe(viewLifecycleOwner) { atualizado ->
            binding.perfilAvatar.text = atualizado.nome.trim().take(1).uppercase()
            binding.perfilPassword.setText("")
            mostrarDados()
            aviso(getString(R.string.conta_atualizada))
        }
        viewModel.erro.observe(viewLifecycleOwner) { aviso(it) }

        binding.botaoTerminarSessao.setOnClickListener {
            Sessao.sair(requireContext())
            // Limpa a pilha toda e volta ao login.
            findNavController().navigate(PerfilFragmentDirections.actionPerfilToLogin())
        }
    }

    private fun mostrarDados() {
        binding.perfilAvatar.text = Sessao.nome.trim().take(1).uppercase()
        binding.perfilNome.setText(Sessao.nome)
        binding.perfilEmail.setText(Sessao.email)
    }

    private fun aviso(mensagem: String) = requireContext().aviso(mensagem)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
