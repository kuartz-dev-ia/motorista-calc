package com.motorista.calc

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color

object DialogUtils {
    fun aplicarCoresBotoes(dialog: AlertDialog) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#1FE7A0"))
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#FFFFFF"))
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL)?.setTextColor(Color.parseColor("#8B96AC"))
    }
}
