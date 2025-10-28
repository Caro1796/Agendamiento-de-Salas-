package com.example.sistemaagendamientosalas.test

import com.example.sistemaagendamientosalas.core.EmailNotificacionService
import com.example.sistemaagendamientosalas.core.MySQLReservaRepository
import com.example.sistemaagendamientosalas.core.Reserva
import com.example.sistemaagendamientosalas.core.ReservaEspecial
import com.example.sistemaagendamientosalas.core.ReservaServiceImpl
import com.example.sistemaagendamientosalas.core.ReporteServiceImpl
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * Ejercicio: Pruebas unitarias para la lógica central del Sistema de Agendamiento.
 * Se prueba la clase ReservaServiceImpl, asegurando que cumple con su única responsabilidad (SRP).
 */
class ReservaServiceTest {

    // Dependencias a simular o instanciar (DIP en acción)
    private lateinit var repository: MySQLReservaRepository
    private lateinit var notificador: EmailNotificacionService
    private lateinit var reservaService: ReservaServiceImpl
    private lateinit var reporteService: ReporteServiceImpl // Añadido para prueba de reporte

    @Before
    fun setup() {
        // Inicialización antes de cada prueba (limpia el entorno)
        repository = MySQLReservaRepository()
        notificador = EmailNotificacionService()
        // Inyección de dependencias reales para la prueba
        reservaService = ReservaServiceImpl(repository, notificador)
        reporteService = ReporteServiceImpl(repository) // Inicializado el servicio de reporte
    }

    // =========================================================
    // ESCENARIO 1: RESERVA BÁSICA (CUMPLIMIENTO DE SRP)
    // =========================================================

    @Test
    fun testReservarSala_debeGuardarYNotificar() {
        // GIVEN: Un estado inicial limpio
        val reservasAntes = repository.obtenerTodas().size
        val nombre = "Felipe Nuñez"
        val sala = "Sala B201"
        val fecha = LocalDateTime.now().plusHours(2)

        // WHEN: Se intenta realizar una reserva
        reservaService.reservarSala(nombre, sala, fecha)

        // THEN: Se debe verificar que la reserva fue guardada correctamente
        val reservasDespues = repository.obtenerTodas().size

        // Assert: Verificar que el repositorio ahora tiene una reserva más
        assertTrue("El número de reservas debe incrementarse", reservasDespues == reservasAntes + 1)

        val nuevaReserva = repository.obtenerTodas().last()
        assertTrue("El nombre del profesor debe coincidir", nuevaReserva.profesor == nombre)
    }

    // =========================================================
    // ESCENARIO 2: CANCELACIÓN DE RESERVA
    // =========================================================

    @Test
    fun testCancelarReserva_debeEliminarDelRepositorio() {
        // GIVEN: Una reserva existente para cancelar
        reservaService.reservarSala("Profesor X", "Sala X10", LocalDateTime.now())
        val reservaGuardada = repository.obtenerTodas().last()
        val idReserva = reservaGuardada.id // Obtiene el ID generado

        val reservasAntes = repository.obtenerTodas().size

        // WHEN: Se llama a la función de cancelación
        reservaService.cancelarReserva(idReserva)

        // THEN: El repositorio debe tener una reserva menos y la reserva no debe existir
        val reservasDespues = repository.obtenerTodas().size

        assertTrue("El número de reservas debe decrementarse", reservasDespues == reservasAntes - 1)
        assertTrue("La reserva debe haber sido eliminada", repository.obtenerTodas().none { it.id == idReserva })
    }

    // =========================================================
    // ESCENARIO 3: CANCELACIÓN FALLIDA (ID NO EXISTENTE)
    // =========================================================

    @Test
    fun testCancelarReserva_idNoExistenteNoDebeFallar() {
        // GIVEN: El repositorio está vacío (o tiene datos, pero la prueba usa un ID inválido)
        val idInvalido = 99999
        reservaService.reservarSala("Profesor Y", "Sala Y20", LocalDateTime.now())
        val reservasAntes = repository.obtenerTodas().size

        // WHEN: Se intenta cancelar un ID que no existe
        reservaService.cancelarReserva(idInvalido)

        // THEN: El conteo de reservas NO debe cambiar (y la función no debe lanzar excepción)
        val reservasDespues = repository.obtenerTodas().size
        assertTrue("La cancelación de un ID inválido no debe cambiar el conteo", reservasDespues == reservasAntes)
    }

    // =========================================================
    // ESCENARIO 4: PRUEBA DE SUSTITUCIÓN DE LISKOV (LSP)
    // =========================================================

    @Test
    fun testLSP_ReservaEspecialPuedeSerGuardadaComoReserva() {
        // GIVEN: Una instancia de la subclase ReservaEspecial
        val reservaEspecial = ReservaEspecial("Profesor Z", "Auditorio", LocalDateTime.now(), "Conferencia")
        val reservasAntes = repository.obtenerTodas().size

        // WHEN: El repositorio (que espera una 'Reserva') guarda la subclase.
        // LSP se cumple si el repositorio puede usar la subclase sin problemas.
        repository.guardar(reservaEspecial)

        // THEN: El conteo debe incrementarse y el objeto guardado debe ser del tipo correcto.
        val reservasDespues = repository.obtenerTodas().size
        // Se asume que la última reserva guardada es la especial.
        val reservaGuardada = repository.obtenerTodas().last()

        assertTrue("El repositorio debe aceptar y guardar la ReservaEspecial", reservasDespues == reservasAntes + 1)
        assertTrue("El objeto guardado debe ser una ReservaEspecial", reservaGuardada is ReservaEspecial)
    }

    // =========================================================
    // ESCENARIO 5: INTEGRACIÓN MÍNIMA (REPORTE)
    // =========================================================

    @Test
    fun testReporteService_debeObtenerTodasLasReservas() {
        // GIVEN: Se han realizado dos reservas
        reservaService.reservarSala("Profesor A", "Sala A1", LocalDateTime.now())
        reservaService.reservarSala("Profesor B", "Sala B2", LocalDateTime.now())

        // WHEN: El servicio de reporte genera el reporte
        val reporte = reporteService.generarReporteMensual()

        // THEN: El reporte debe contener evidencia de ambas reservas guardadas.
        val lineasReporte = reporte.split("\n").filter { it.startsWith("[ID:") }

        assertTrue("El reporte debe contener al menos dos líneas de reserva", lineasReporte.size >= 2)
        assertTrue("El reporte debe ser legible", reporte.contains("Profesor A"))
    }
}