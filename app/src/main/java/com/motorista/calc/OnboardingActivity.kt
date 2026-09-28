package com.motorista.calc

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {

    private data class Passo(val emoji: String, val titulo: String, val descricao: String)

    private val passos = listOf(
        Passo(
            "👋",
            "Bem-vindo ao Motorista Calc",
            "Controle, planeje, conquiste! O app calcula na hora se vale a pena aceitar cada corrida, acompanha sua meta e organiza as contas do seu veículo."
        ),
        Passo(
            "🔓",
            "Ative 2 permissões",
            "O app precisa de Acessibilidade (pra ler a tela do Uber/99) e Sobreposição (pra mostrar os cálculos por cima). Vá em Config → Diagnóstico e ative as duas."
        ),
        Passo(
            "🔑",
            "Peça a liberação do acesso",
            "Vá em Config → Licença, copie o ID do aparelho e envie pro administrador liberar seu uso. Sem isso, não dá pra iniciar jornada."
        ),
        Passo(
            "🎯",
            "Inicie sua Jornada",
            "Na tela inicial, preencha meta diária, carga horária e odômetro, e toque em Iniciar jornada. O app já te mostra quanto precisa faturar por dia."
        ),
        Passo(
            "🚗",
            "Card automático de corrida",
            "Quando surgir uma oferta no Uber/99, aparece um card colorido: verde (vale a pena), laranja (você decide), vermelho (não compensa) — com R$/km, R$/h e lucro líquido."
        ),
        Passo(
            "🫧",
            "Bolha flutuante",
            "Durante a jornada, um ícone fica flutuando sobre qualquer app — toque nele pra ver um resumo rápido sem precisar sair do Uber/99."
        ),
        Passo(
            "📊",
            "Explore o resto do app",
            "Histórico e Relatórios mostram seu desempenho. Config ajusta combustível e custos. Mais Opções tem Manutenções, Abastecimentos, Documentos, Contas, Saúde do Veículo e Radar de Eventos."
        )
    )

    private var passoAtual = 0
    private val pontos = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        montarPontos()

        findViewById<TextView>(R.id.btnPular).setOnClickListener { concluir() }
        findViewById<TextView>(R.id.btnProximo).setOnClickListener {
            if (passoAtual < passos.size - 1) {
                passoAtual++
                atualizarTela()
            } else {
                concluir()
            }
        }

        atualizarTela()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun montarPontos() {
        val container = findViewById<LinearLayout>(R.id.containerPontos)
        container.removeAllViews()
        pontos.clear()
        for (i in passos.indices) {
            val ponto = TextView(this).apply {
                text = "●"
                textSize = 10f
                setPadding(dp(4), 0, dp(4), 0)
                setTextColor(Color.parseColor("#232B36"))
            }
            pontos.add(ponto)
            container.addView(ponto)
        }
    }

    private fun atualizarTela() {
        val passo = passos[passoAtual]
        findViewById<TextView>(R.id.txtEmoji).text = passo.emoji
        findViewById<TextView>(R.id.txtTitulo).text = passo.titulo
        findViewById<TextView>(R.id.txtDescricao).text = passo.descricao

        for ((i, ponto) in pontos.withIndex()) {
            ponto.setTextColor(Color.parseColor(if (i == passoAtual) "#2FB4A6" else "#232B36"))
        }

        findViewById<TextView>(R.id.btnProximo).text = if (passoAtual == passos.size - 1) "Começar a usar" else "Próximo"
    }

    private fun concluir() {
        getSharedPreferences(RideAccessibilityService.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(RideAccessibilityService.PREF_TUTORIAL_VISTO, true)
            .apply()
        finish()
    }
}
