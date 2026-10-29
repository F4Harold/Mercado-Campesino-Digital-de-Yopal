package co.llanosoft.mercado;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Regla de negocio para reservar productos del Mercado Campesino Digital.
 *
 * Implementa:
 *  - RF-04: el cliente reserva una cantidad de una oferta; el sistema verifica
 *           disponibilidad y descuenta el inventario de forma atómica.
 *  - RF-05: no se aceptan reservas después del viernes a las 6:00 p. m.
 *           de la semana del mercado (hora de Colombia).
 *  - RNF-06 (fiabilidad): dos clientes no pueden reservar la misma última unidad.
 */
public class ServicioReservas {

    /** Hora de cierre de reservas (RF-05). */
    public static final LocalTime HORA_CIERRE = LocalTime.of(18, 0);
    /** Máximo de unidades por reserva para evitar acaparamiento (RF-04). */
    public static final int MAXIMO_POR_RESERVA = 50;

    /**
     * Calcula el cierre de reservas para el mercado del sábado indicado (RF-05).
     * @param momento fecha-hora de cualquier momento de la semana del mercado
     * @return viernes anterior al sábado de mercado a las 18:00
     */
    public LocalDateTime cierreDeReservas(LocalDateTime momento) {
        LocalDateTime viernes = momento.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        // Si ya es sábado o domingo, el cierre de esta semana fue el viernes anterior.
        if (momento.getDayOfWeek() == DayOfWeek.SATURDAY || momento.getDayOfWeek() == DayOfWeek.SUNDAY) {
            viernes = momento.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
        }
        return viernes.toLocalDate().atTime(HORA_CIERRE);
    }

    /**
     * Reserva una cantidad de una oferta (RF-04, RF-05).
     * @param oferta     oferta publicada por el productor
     * @param cantidad   unidades solicitadas por el cliente
     * @param momento    fecha y hora de la solicitud (hora de Colombia)
     * @return cantidad que queda disponible tras la reserva
     * @throws ReservaRechazadaException si la reserva incumple alguna regla
     */
    public int reservar(Oferta oferta, int cantidad, LocalDateTime momento) {
        if (oferta == null) {
            throw new ReservaRechazadaException("La oferta no existe");
        }
        if (momento == null) {
            throw new ReservaRechazadaException("La fecha de la reserva es obligatoria");
        }
        if (cantidad <= 0) {
            throw new ReservaRechazadaException("La cantidad debe ser mayor que cero");
        }
        if (cantidad > MAXIMO_POR_RESERVA) {
            throw new ReservaRechazadaException("No se pueden reservar más de "
                    + MAXIMO_POR_RESERVA + " unidades por pedido");
        }
        // RF-05: el cierre es exacto; a las 18:00:00 en punto ya no se recibe.
        if (!momento.isBefore(cierreDeReservas(momento))) {
            throw new ReservaRechazadaException("Las reservas cerraron el viernes a las 6:00 p. m.");
        }
        // RF-04 + RNF-06: verificar y descontar en una sola operación atómica.
        synchronized (oferta) {
            int disponible = oferta.getCantidadDisponible();
            if (cantidad > disponible) {
                throw new ReservaRechazadaException("Solo quedan " + disponible + " "
                        + oferta.getUnidad() + " de " + oferta.getProducto());
            }
            oferta.descontar(cantidad);
            return oferta.getCantidadDisponible();
        }
    }
}
