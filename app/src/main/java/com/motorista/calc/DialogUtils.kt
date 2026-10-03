package com.motorista.calc

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

object DialogUtils {
    fun aplicarCoresBotoes(dialog: AlertDialog) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#1FE7A0"))
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#FFFFFF"))
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL)?.setTextColor(Color.parseColor("#8B96AC"))
    }

    /** Aplica fundo escuro sólido com borda verde + cores de botão num
     * AlertDialog (DatePickerDialog também é um AlertDialog por baixo dos
     * panos). SEMPRE chame isso de dentro de um setOnShowListener, nunca
     * antes de dialog.show() — setar o fundo antes do show() não é
     * confiável em todos os aparelhos, porque o próprio show() reconstrói
     * parte da decoração da janela por dentro, descartando o que foi
     * definido antes. */
    fun aplicarTemaCompleto(dialog: AlertDialog) {
        aplicarCoresBotoes(dialog)
        try {
            val densidade = dialog.context.resources.displayMetrics.density
            val fundo = GradientDrawable().apply {
                setColor(Color.parseColor("#12161D"))
                setStroke((1 * densidade).toInt(), Color.parseColor("#2FB4A6"))
                cornerRadius = 16 * densidade
            }
            dialog.window?.setBackgroundDrawable(fundo)
        } catch (e: Exception) {
            // Se falhar por qualquer motivo, o diálogo ainda funciona —
            // só fica sem o fundo customizado.
        }
    }
}
