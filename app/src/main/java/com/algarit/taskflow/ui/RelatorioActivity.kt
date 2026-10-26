package com.algarit.taskflow.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.algarit.taskflow.R
import com.algarit.taskflow.api.Api

// As contas do projeto: total, concluídas, em curso, atrasadas e por pessoa.
class RelatorioActivity : AppCompatActivity() {

    private var projetoId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aplicarInsets(findViewById(android.R.id.content))
        Api.iniciar(this)
        projetoId = intent.getIntExtra("projeto_id", -1)
        if (projetoId <= 0) { finish(); return }

        setContentView(R.layout.activity_relatorio)
        findViewById<ImageButton>(R.id.botao_voltar).setOnClickListener { finish() }
        carregar()
    }

    private fun carregar() {
        Api.get("/tarefas/relatorio", mapOf("projeto_id" to projetoId.toString())) { r ->
            val dados = r.objeto ?: return@get
            findViewById<TextView>(R.id.numero_percentagem).text = "${dados.optInt("percentagem")}%"
            findViewById<TextView>(R.id.numero_total).text = dados.optInt("total").toString()
            findViewById<TextView>(R.id.numero_concluidas).text = dados.optInt("concluidas").toString()
            findViewById<TextView>(R.id.numero_em_curso).text = dados.optInt("emCurso").toString()
            findViewById<TextView>(R.id.numero_por_fazer).text = dados.optInt("porFazer").toString()
            findViewById<TextView>(R.id.numero_atrasadas).text = dados.optInt("atrasadas").toString()

            val linha = findViewById<LinearLayout>(R.id.linha_por_pessoa)
            linha.removeAllViews()
            val porPessoa = dados.optJSONObject("porPessoa")
            if (porPessoa == null || porPessoa.length() == 0) {
                val vazio = TextView(this)
                vazio.text = getString(R.string.a_carregar)
                vazio.setTextColor(getColor(R.color.texto_secundario))
                linha.addView(vazio)
                return@get
            }
            val chaves = porPessoa.keys()
            while (chaves.hasNext()) {
                val nome = chaves.next()
                val linhaPessoa = LinearLayout(this)
                linhaPessoa.orientation = LinearLayout.HORIZONTAL
                linhaPessoa.setPadding(0, 12, 0, 12)

                val esquerda = TextView(this)
                esquerda.text = nome
                esquerda.setTextColor(getColor(R.color.texto))
                esquerda.textSize = 15f
                esquerda.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                val direita = TextView(this)
                direita.text = porPessoa.optInt(nome).toString()
                direita.setTextColor(getColor(R.color.roxo))
                direita.textSize = 15f
                direita.setTypeface(null, android.graphics.Typeface.BOLD)
                direita.gravity = android.view.Gravity.END

                linhaPessoa.addView(esquerda)
                linhaPessoa.addView(direita)
                linha.addView(linhaPessoa)
            }
        }
    }
}
