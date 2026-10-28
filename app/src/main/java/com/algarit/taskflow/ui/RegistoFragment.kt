package com.algarit.taskflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.databinding.ActivityRegistoBinding
import org.json.JSONObject

// Criar conta nova (o mesmo registo do site).
class RegistoFragment : Fragment() {

    private var _binding: ActivityRegistoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityRegistoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Api.iniciar(requireContext())

        binding.botaoEntrar.setOnClickListener { findNavController().navigateUp() }

        binding.botaoCriar.setOnClickListener {
            val nome = binding.campoNome.text.toString().trim()
            val email = binding.campoEmail.text.toString().trim()
            val password = binding.campoPassword.text.toString()
            if (nome.isBlank() || email.isBlank() || password.isBlank()) {
                aviso("Preenche todos os campos"); return@setOnClickListener
            }
            binding.botaoCriar.isEnabled = false
            Api.post("/utilizadores/registo", JSONObject()
                .put("nome", nome).put("email", email).put("password", password)) { r ->
                if (!isAdded) return@post
                binding.botaoCriar.isEnabled = true
                val novo = r.objeto
                if (r.sucesso && novo != null) {
                    Sessao.guardar(requireContext(), novo.optInt("id"), novo.optString("nome"), novo.optString("email"))
                    findNavController().navigate(RegistoFragmentDirections.actionRegistoToProjetos())
                } else {
                    aviso(if (r.codigo == 400) "Faltam campos obrigatórios" else "Não foi possível criar a conta")
                }
            }
        }
    }

    private fun aviso(mensagem: String) = requireContext().aviso(mensagem)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
