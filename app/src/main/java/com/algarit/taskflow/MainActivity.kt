package com.algarit.taskflow

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.algarit.taskflow.databinding.ActivityMainBinding
import com.algarit.taskflow.ui.aplicarInsetsEm

// Activity única (Aula 8): os ecrãs são Fragments dentro do NavHost.
// A toolbar está ligada ao NavController: a seta de voltar aparece nos ecrãs
// secundários e o título "Voltar" deixa claro o que ela faz.
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // A toolbar acompanha a navigation; os insets empurram o ecrã todo
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
        aplicarInsetsEm(binding.root)
    }
}
