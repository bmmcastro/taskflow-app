package com.algarit.taskflow.ui

/* Estado do ecrã: a carregar, com dados ou com erro.
   Cada ViewModel expõe um estado deste tipo por ecrã. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Sucesso<T>(val dados: T) : UiState<T>
    data class Erro(val mensagem: String) : UiState<Nothing>
}
