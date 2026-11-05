package co.llanosoft.mercado;

/**
 * Oferta semanal de un productor (RF-02).
 * Guarda la cantidad disponible que se descuenta con cada reserva.
 */
public class Oferta {
    private final long id;
    private final String producto;
    private final String unidad;
    private final String productor;
    private final String vereda;
    private final int precio;
    private final String categoria;
    private int cantidadDisponible;

    public Oferta(long id, String producto, String unidad, int cantidadDisponible) {
        this(id, producto, unidad, cantidadDisponible, "", "", 0);
    }

    public Oferta(long id, String producto, String unidad, int cantidadDisponible,
                  String productor, String vereda, int precio) {
        this(id, producto, unidad, cantidadDisponible, productor, vereda, precio, "Otros");
    }

    public Oferta(long id, String producto, String unidad, int cantidadDisponible,
                  String productor, String vereda, int precio, String categoria) {
        if (cantidadDisponible < 0) {
            throw new IllegalArgumentException("La cantidad ofertada no puede ser negativa");
        }
        this.id = id;
        this.producto = producto;
        this.unidad = unidad;
        this.cantidadDisponible = cantidadDisponible;
        this.productor = productor;
        this.vereda = vereda;
        this.precio = precio;
        this.categoria = categoria;
    }

    public long getId() { return id; }
    public String getProducto() { return producto; }
    public String getUnidad() { return unidad; }
    public String getProductor() { return productor; }
    public String getVereda() { return vereda; }
    public int getPrecio() { return precio; }
    public String getCategoria() { return categoria; }
    public synchronized int getCantidadDisponible() { return cantidadDisponible; }

    /** Descuenta inventario; solo lo invoca ServicioReservas dentro de un bloque sincronizado. */
    synchronized void descontar(int cantidad) {
        this.cantidadDisponible -= cantidad;
    }
}
