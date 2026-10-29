package co.llanosoft.mercado;

/**
 * Oferta semanal de un productor (RF-02).
 * Guarda la cantidad disponible que se descuenta con cada reserva.
 */
public class Oferta {
    private final long id;
    private final String producto;
    private final String unidad;
    private int cantidadDisponible;

    public Oferta(long id, String producto, String unidad, int cantidadDisponible) {
        if (cantidadDisponible < 0) {
            throw new IllegalArgumentException("La cantidad ofertada no puede ser negativa");
        }
        this.id = id;
        this.producto = producto;
        this.unidad = unidad;
        this.cantidadDisponible = cantidadDisponible;
    }

    public long getId() { return id; }
    public String getProducto() { return producto; }
    public String getUnidad() { return unidad; }
    public synchronized int getCantidadDisponible() { return cantidadDisponible; }

    /** Descuenta inventario; solo lo invoca ServicioReservas dentro de un bloque sincronizado. */
    synchronized void descontar(int cantidad) {
        this.cantidadDisponible -= cantidad;
    }
}
