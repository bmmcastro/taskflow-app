package com.algarit.taskflow.model

/* As três colunas do quadro. O servidor guarda o estado como texto
   (o rotulo), o enum dá ordem e tipo seguro ao código da app. */
enum class EstadoTarefa(val rotulo: String) {
    POR_FAZER("Por fazer"),
    EM_CURSO("Em curso"),
    CONCLUIDA("Concluída");

    companion object {
        /* O texto que vem da API -> enum (desconhecido cai em Por fazer). */
        fun de(status: String?): EstadoTarefa =
            entries.firstOrNull { it.rotulo == status } ?: POR_FAZER
    }
}
