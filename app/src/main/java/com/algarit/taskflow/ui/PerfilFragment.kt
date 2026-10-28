package com.algarit.taskflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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
        binding.botaoVoltar.setOnClickListener { findNavController().navigateUp() }

        mostrarDados()

        binding.botaoGuardarPerfil.setOnClickListener {
            val nome = binding.perfilNome.text.toString().trim()
            val email = binding.perfilEmail.text.toString().trim()
            if (nome.isBlank() || email.isBlank()) { aviso("O nome e o email são obrigatórios"); return@setOnClickListener }
            viewModel.guardar(nome, email, binding.perfilPassword.text.toString())
        }

        viewModel.atualizado.observe(viewLifecycleOwner) { atualizado ->
            binding.perfilAvatar.text = atualizado.nome.trim().take(1).uppercase()
            binding.perfilPassword.setText("")
            mostrarDados()
            aviso("Conta atualizada")
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
