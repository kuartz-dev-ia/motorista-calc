package com.motorista.calc

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EventosListaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_eventos_lista)
    }

    override fun onResume() {
        super.onResume()

        /*
         * A tela da lista será atualizada sempre que
         * o usuário retornar para esta Activity.
         *
         * A leitura dos eventos permanece centralizada
         * no EventoStorage.
         */
    }
}
