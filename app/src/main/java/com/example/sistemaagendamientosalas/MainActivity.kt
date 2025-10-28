package com.example.sistemaagendamientosalas

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.sistemaagendamientosalas.core.* // Importa todo tu paquete de lógica
import java.time.LocalDateTime

class MainActivity : AppCompatActivity() {

    // 1. Instanciación de Servicios (Inyección Manual de Dependencias)
    private lateinit var reservaService: ReservaService
    private lateinit var tvResultado: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Asegúrate de que el layout sea el que creaste (activity_main.xml)
        setContentView(R.layout.activity_main)

        // Inicializar la inyección de dependencias (DIP)
        val repository = MySQLReservaRepository()
        val notificador = EmailNotificacionService()
        // El servicio recibe las dependencias:
        reservaService = ReservaServiceImpl(repository, notificador)

        // 2. Conectar elementos de la UI
        val etProfesor = findViewById<EditText>(R.id.etProfesor)
        val etSala = findViewById<EditText>(R.id.etSala)
        val btnReservar = findViewById<Button>(R.id.btnReservar)
        tvResultado = findViewById<TextView>(R.id.tvResultado)

        // 3. Configurar la acción del botón
        btnReservar.setOnClickListener {
            // Llama a la función que ejecuta el servicio SOLID
            realizarReserva(etProfesor.text.toString(), etSala.text.toString())
        }
    }

    private fun realizarReserva(profesor: String, sala: String) {
        if (profesor.isBlank() || sala.isBlank()) {
            tvResultado.text = "Error: Debe ingresar nombre y sala."
            return
        }

        try {
            // Llama al servicio de reserva (SRP)
            reservaService.reservarSala(profesor, sala, LocalDateTime.now())
            tvResultado.text = "Éxito: Reserva creada para $profesor en $sala. (Ver consola para logs de notificación/guardado)"
        } catch (e: Exception) {
            // Manejo de errores
            tvResultado.text = "Fallo: No se pudo crear la reserva. Error: ${e.message}"
        }
    }
}