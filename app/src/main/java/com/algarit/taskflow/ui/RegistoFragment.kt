package com.algarit.taskflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.algarit.taskflow.databinding.ActivityRegistoBinding

// Criar conta nova (o mesmo registo do site).
class RegistoFragment : Fragment() {

    private var _binding: ActivityRegistoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegistoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityRegistoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.botaoEntrar.setOnClickListener { findNavController().navigateUp() }

        binding.botaoCriar.setOnClickListener {
            val nome = binding.campoNome.text.toString().trim()
            val email = binding.campoEmail.text.toString().trim()
            val password = binding.campoPassword.text.toString()
            var valido = true
            if (nome.isBlank()) { binding.campoNome.error = getString(com.algarit.taskflow.R.string.erro_campos); valido = false }
            if (email.isBlank()) { binding.campoEmail.error = getString(com.algarit.taskflow.R.string.erro_campos); valido = false }
            if (password.isBlank()) { binding.campoPassword.error = getString(com.algarit.taskflow.R.string.erro_campos); valido = false }
            if (!valido) return@setOnClickListener
            viewModel.registar(nome, email, password)
        }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            binding.botaoCriar.isEnabled = estado !is UiState.Loading
            if (estado is UiState.Sucesso) {
                // A sessão já foi guardada pelo ViewModel: direto aos projetos.
                findNavController().navigate(RegistoFragmentDirections.actionRegistoToProjetos())
            } else if (estado is UiState.Erro) {
                aviso(estado.mensagem)
            }
        }
    }

    private fun aviso(mensagem: String) = requireContext().aviso(mensagem)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
