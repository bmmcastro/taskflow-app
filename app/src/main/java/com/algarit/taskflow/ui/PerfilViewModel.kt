package com.algarit.taskflow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.algarit.taskflow.api.Api
import com.algarit.taskflow.api.Sessao
import com.algarit.taskflow.api.erroAmigavel
import com.algarit.taskflow.model.PerfilPedido
import com.algarit.taskflow.model.Utilizador
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class PerfilViewModel(app: Application) : AndroidViewModel(app) {

    private val _atualizado = MutableLiveData<Utilizador>()
    val atualizado: LiveData<Utilizador> = _atualizado

    // Verdadeiro enquanto o pedido de guardar está a correr (mostra a ProgressBar).
    private val _aGuardar = MutableLiveData(false)
    val aGuardar: LiveData<Boolean> = _aGuardar

    private val _erro = MutableLiveData<String>()
    val erro: LiveData<String> = _erro

    fun guardar(nome: String, email: String, password: String?) {
        if (_aGuardar.value == true) return
        _aGuardar.value = true
        viewModelScope.launch {
            try {
                val atualizado = Api.servico.atualizarPerfil(
                    Sessao.utilizadorId,
                    PerfilPedido(nome, email, password?.ifBlank { null })
                )
                Sessao.guardar(getApplication(), atualizado.id, atualizado.nome, atualizado.email)
                _atualizado.value = atualizado
            } catch (t: CancellationException) {
                throw t
            } catch (t: Exception) {
                _erro.value = erroAmigavel(t)
            } finally {
                _aGuardar.value = false
            }
        }
    }
}
