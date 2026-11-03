package com.algarit.taskflow.ui

/* Estado do ecrã: parado, a carregar, com dados ou com erro.
   Cada ViewModel expõe um estado deste tipo por ecrã. O Idle é o estado
   inicial dos formulários (nada aconteceu ainda) — sem ele, começar em
   Sucesso fazia navegar assim que o ecrã observasse o valor. */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Sucesso<T>(val dados: T) : UiState<T>
    data class Erro(val mensagem: String) : UiState<Nothing>
}
