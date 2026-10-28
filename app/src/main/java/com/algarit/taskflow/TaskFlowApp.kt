package com.algarit.taskflow

import android.app.Application
import android.content.Context

// Dá acesso ao contexto das mensagens de erro fora das Activities.
class TaskFlowApp : Application() {

    companion object {
        lateinit var contexto: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        contexto = applicationContext
    }
}
