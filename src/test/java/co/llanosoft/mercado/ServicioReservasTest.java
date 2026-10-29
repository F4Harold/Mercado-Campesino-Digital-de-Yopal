package co.llanosoft.mercado;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas unitarias de RF-04, RF-05 y RNF-06. */
class ServicioReservasTest {

    private ServicioReservas servicio;
    private Oferta queso;
    // Miércoles 4 de noviembre de 2026, 10:00 a. m.
    private final LocalDateTime miercoles = LocalDateTime.of(2026, 11, 4, 10, 0);

    @BeforeEach
    void preparar() {
        servicio = new ServicioReservas();
        queso = new Oferta(1, "Queso llanero", "libras", 10);
    }

    @Test @DisplayName("CP-01 Reserva válida descuenta inventario")
    void reservaValidaDescuentaInventario() {
        int restante = servicio.reservar(queso, 3, miercoles);
        assertEquals(7, restante);
    }

    @Test @DisplayName("CP-02 Reservar exactamente lo disponible deja inventario en cero (límite)")
    void reservarTodoLoDisponible() {
        assertEquals(0, servicio.reservar(queso, 10, miercoles));
    }

    @Test @DisplayName("CP-03 Cantidad mayor a la disponible es rechazada")
    void cantidadMayorAlInventario() {
        ReservaRechazadaException ex = assertThrows(ReservaRechazadaException.class,
                () -> servicio.reservar(queso, 11, miercoles));
        assertTrue(ex.getMessage().contains("Solo quedan 10"));
        assertEquals(10, queso.getCantidadDisponible());
    }

    @Test @DisplayName("CP-04 Cantidad cero o negativa es rechazada")
    void cantidadInvalida() {
        assertThrows(ReservaRechazadaException.class, () -> servicio.reservar(queso, 0, miercoles));
        assertThrows(ReservaRechazadaException.class, () -> servicio.reservar(queso, -2, miercoles));
    }

    @Test @DisplayName("CP-05 Viernes 5:59 p. m. se acepta (límite)")
    void viernesAntesDelCierre() {
        LocalDateTime viernes = LocalDateTime.of(2026, 11, 6, 17, 59, 59);
        assertEquals(9, servicio.reservar(queso, 1, viernes));
    }

    @Test @DisplayName("CP-06 Viernes 6:00 p. m. en punto se rechaza (límite)")
    void viernesEnElCierre() {
        LocalDateTime viernes = LocalDateTime.of(2026, 11, 6, 18, 0);
        assertThrows(ReservaRechazadaException.class, () -> servicio.reservar(queso, 1, viernes));
    }

    @Test @DisplayName("CP-07 Sábado se rechaza")
    void sabadoRechazado() {
        LocalDateTime sabado = LocalDateTime.of(2026, 11, 7, 7, 0);
        assertThrows(ReservaRechazadaException.class, () -> servicio.reservar(queso, 1, sabado));
    }

    @Test @DisplayName("CP-08 Dos clientes por la última libra: solo uno la obtiene")
    void concurrenciaUltimaUnidad() throws Exception {
        Oferta ultima = new Oferta(2, "Queso llanero", "libras", 1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger rechazos = new AtomicInteger();
        Callable<Void> cliente = () -> {
            salida.await();
            try { servicio.reservar(ultima, 1, miercoles); exitos.incrementAndGet(); }
            catch (ReservaRechazadaException e) { rechazos.incrementAndGet(); }
            return null;
        };
        Future<Void> a = pool.submit(cliente);
        Future<Void> b = pool.submit(cliente);
        salida.countDown();
        a.get(); b.get();
        pool.shutdown();
        assertEquals(1, exitos.get());
        assertEquals(1, rechazos.get());
        assertEquals(0, ultima.getCantidadDisponible());
    }

    @Test @DisplayName("CP-09 Más de 50 unidades por reserva se rechaza")
    void maximoPorReserva() {
        Oferta platano = new Oferta(3, "Plátano", "racimos", 100);
        assertThrows(ReservaRechazadaException.class, () -> servicio.reservar(platano, 51, miercoles));
    }
}
