package com.example.sistemaagendamientosalas.core

import java.time.LocalDateTime
import kotlin.random.Random

// =====================================================================
// 1. ABSTRACCIONES Y CONTRATOS (ISP y DIP)
// =====================================================================

// I) INTERFACE SEGREGATION: Interfaces pequeñas y específicas.
interface ReservaService {
    fun reservarSala(nombreProfesor: String, sala: String, fecha: LocalDateTime)
    fun cancelarReserva(idReserva: Int)
}

interface NotificacionService {
    fun notificarEstudiantes(mensaje: String)
}

interface ReporteService {
    fun generarReporteMensual(): String
}

interface AprobacionService {
    fun aprobarReservaEspecial(idReserva: Int)
    fun rechazarReservaEspecial(idReserva: Int, motivo: String)
}

// D) DEPENDENCY INVERSION: Abstracción del repositorio de datos.
interface ReservaRepository {
    fun guardar(reserva: Reserva)
    fun eliminar(idReserva: Int)
    fun obtenerTodas(): List<Reserva>
}

// =====================================================================
// 2. ENTIDADES Y CLASES DE DATOS (LSP)
// =====================================================================

// L) LISKOV SUBSTITUTION: Clase base para la entidad Reserva.
open class Reserva(
    val profesor: String,
    val sala: String,
    val fecha: LocalDateTime
) {
    val id: Int = Random.nextInt(1000, 9999)

    override fun toString(): String = "[ID:$id] - $profesor - $sala @ $fecha"
}

// Subclase que respeta el contrato.
class ReservaEspecial(
    profesor: String,
    sala: String,
    fecha: LocalDateTime,
    val motivo: String
) : Reserva(profesor, sala, fecha) {

    override fun toString(): String = "[ID:$id] - $profesor - $sala (Especial: $motivo) @ $fecha"
}

// =====================================================================
// 3. IMPLEMENTACIONES CONCRETAS (SRP, OCP)
// =====================================================================

// S) SINGLE RESPONSIBILITY: Clase dedicada solo a gestionar la lógica de reservas.
class ReservaServiceImpl(
    // Inyección de dependencias (DIP)
    private val repository: ReservaRepository,
    private val notificador: NotificacionService
) : ReservaService {

    override fun reservarSala(nombreProfesor: String, sala: String, fecha: LocalDateTime) {
        // En una app real, aquí iría la lógica de verificación de disponibilidad
        val reserva = Reserva(nombreProfesor, sala, fecha)
        repository.guardar(reserva)
        notificador.notificarEstudiantes("Nueva reserva: $reserva")
    }

    override fun cancelarReserva(idReserva: Int) {
        repository.eliminar(idReserva)
        notificador.notificarEstudiantes("La reserva #$idReserva ha sido cancelada")
    }
}

// Implementación de notificaciones (OCP: Abierto a extensión).
class EmailNotificacionService : NotificacionService {
    override fun notificarEstudiantes(mensaje: String) {
        println("📧 Enviando email a estudiantes: '$mensaje'")
    }
}

// Implementación de reportes (SRP).
class ReporteServiceImpl(private val repository: ReservaRepository) : ReporteService {
    override fun generarReporteMensual(): String {
        val reservas = repository.obtenerTodas()
        return "📜 Reporte mensual: \n" + reservas.joinToString("\n")
    }
}

// Implementación de aprobaciones (SRP).
class AprobacionServiceImpl : AprobacionService {
    override fun aprobarReservaEspecial(idReserva: Int) {
        println("✔️ Reserva especial $idReserva aprobada.")
    }

    override fun rechazarReservaEspecial(idReserva: Int, motivo: String) {
        println("❌ Reserva especial $idReserva rechazada. Motivo: $motivo")
    }
}

// Implementación concreta del repositorio (DIP: Implementa la abstracción).
class MySQLReservaRepository : ReservaRepository {
    private val reservas = mutableListOf<Reserva>()

    override fun guardar(reserva: Reserva) {
        // ❌ VULNERABILIDAD #3: Valor Mágico (100)
        if (reserva.sala.length > 100) {
            println("ERROR: Sala excede límite de longitud.")
            return
        }

        // ❌ VULNERABILIDAD #1: Inyección SQL (A03: Injection)
        // Se construye una consulta SQL insegura concatenando directamente el input del profesor.
        val sqlInsegura = "INSERT INTO Reservas (profesor, sala, fecha) VALUES ('${reserva.profesor}', '${reserva.sala}', '${reserva.fecha}')"
        println("TODO: EJECUTAR SQL INSEGURA: $sqlInsegura")

        reservas.add(reserva)

        // ❌ VULNERABILIDAD #2: Exposición de Datos Sensibles (A04: Insecure Design/Logging)
        // Se imprime información sensible en el Logcat (Log de Android)
        println("LOG: Reserva guardada para el profesor ${reserva.profesor} con ID ${reserva.id}")
    }

    override fun eliminar(idReserva: Int) {
        reservas.removeIf { it.id == idReserva }
        println("🗑️ Eliminado en MySQL reserva #$idReserva")
    }

    override fun obtenerTodas(): List<Reserva> = reservas
}
