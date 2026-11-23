package com.algarit.taskflow.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.algarit.taskflow.MainActivity
import com.algarit.taskflow.R
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.databinding.ActivityConviteBinding

// Quem recebe um convite cola o link (ou só o token) e entra no projeto.
// Fica fora do grafo de navegação: é o ponto de entrada de quem foi convidado.
class ConviteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConviteBinding
    private val viewModel: ConviteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Sessao.carregar(this)
        binding = ActivityConviteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarInsetsEm(binding.raiz)

        binding.botaoVoltar.setOnClickListener { finish() }
        binding.botaoVer.setOnClickListener { verConvite() }
        binding.botaoAceitar.setOnClickListener { aceitarConvite() }

        // O convite vem do link (intent) ou do estado guardado ao rodar o ecrã.
        val conviteGuardado = savedInstanceState?.getString("convite")
            ?: intent.getStringExtra("token")
        if (!conviteGuardado.isNullOrBlank()) {
            binding.campoConvite.setText(conviteGuardado)
            verConvite()
        }
        observar()
    }

    private fun observar() {
        viewModel.convite.observe(this) { estado ->
            val aCarregar = estado is UiState.Loading
            binding.loading.isVisible = aCarregar
            binding.campoConvite.isEnabled = !aCarregar
            binding.botaoVer.isEnabled = !aCarregar
            binding.textoConvite.isVisible = estado !is UiState.Idle
            binding.botaoAceitar.isVisible = estado is UiState.Sucesso
            binding.textoConvite.text = when (estado) {
                is UiState.Sucesso -> {
                    val c = estado.dados
                    getString(R.string.convite_detalhe, c.projeto, c.criador, c.email, c.status)
                }
                is UiState.Erro -> estado.mensagem
                is UiState.Loading -> getString(R.string.a_carregar)
                is UiState.Idle -> ""
            }
        }
        viewModel.aAceitar.observe(this) { aAceitar ->
            binding.botaoAceitar.isEnabled = !aAceitar
            binding.botaoVer.isEnabled = !aAceitar
        }
        viewModel.aceite.observe(this) { aceite ->
            if (!aceite) return@observe
            aviso(getString(R.string.convite_aceite))
            // A sessão está guardada: a MainActivity abre direto nos projetos.
            startActivity(Intent(this@ConviteActivity, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            finish()
        }
        viewModel.erro.observe(this) { aviso(it) }
    }

    private fun verConvite() {
        val colado = binding.campoConvite.text.toString().trim()
        // Validação com setError no próprio campo.
        if (colado.isBlank()) { binding.campoConvite.error = getString(R.string.erro_colar_convite); return }
        binding.campoConvite.error = null
        viewModel.ver(colado)
    }

    private fun aceitarConvite() {
        val colado = binding.campoConvite.text.toString().trim()
        if (!viewModel.eOConviteConsultado(colado)) {
            // O texto mudou depois de consultar: é preciso ver o convite antes de aceitar.
            binding.botaoAceitar.isVisible = false
            aviso(getString(R.string.erro_ver_convite))
            return
        }
        if (!Sessao.estaLogado()) {
            // Sem sessão iniciada o id seria -1: pedimos o login e retomamos o convite a seguir.
            aviso(getString(R.string.erro_sessao_convite))
            startActivity(Intent(this, MainActivity::class.java)
                .putExtra("convite_pendente", viewModel.tokenConsultado))
            finish()
            return
        }
        viewModel.aceitar()
    }

    // Os avisos usam a Toast comum (Context.aviso, em Ui.kt): sobrevive à mudança
    // de ecrã, ao contrário de um dialog, que fecharia junto com a Activity.

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("convite", binding.campoConvite.text.toString())
        super.onSaveInstanceState(outState)
    }
}
