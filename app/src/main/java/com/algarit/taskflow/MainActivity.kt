package com.algarit.taskflow

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.algarit.taskflow.databinding.ActivityMainBinding

// Activity unica (Aula 8): os ecra sao Fragments no NavHost.
// A toolbar esta ligada ao NavController: o titulo vem do grafo e a
// seta de voltar aparece nos ecra secundarios.
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // A toolbar acompanha o navigation; os insets empurram o ecrã todo
        // para baixo das barras do sistema.
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHost.navController
        binding.toolbar.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destino, _ ->
            // Os ecrãs iniciais já têm o seu título grande no próprio layout.
            binding.toolbar.isVisible = destino.id != R.id.loginFragment &&
                destino.id != R.id.registoFragment && destino.id != R.id.projetosFragment
            binding.toolbar.title = getString(R.string.voltar)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            vista.setPadding(barras.left, barras.top, barras.right, maxOf(barras.bottom, teclado.bottom))
            insets
        }

        // O LoginFragment trata da sessão e abre os projetos quando já estamos ligados.
    }
}
