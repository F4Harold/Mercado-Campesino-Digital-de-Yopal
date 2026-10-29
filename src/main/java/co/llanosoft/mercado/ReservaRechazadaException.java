package co.llanosoft.mercado;

/** Se lanza cuando una reserva no cumple las reglas de RF-04 o RF-05. */
public class ReservaRechazadaException extends RuntimeException {
    public ReservaRechazadaException(String mensaje) { super(mensaje); }
}
