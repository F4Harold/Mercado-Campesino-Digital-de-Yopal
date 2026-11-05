package co.llanosoft.mercado;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Ofertas de ejemplo en memoria (RF-02, RF-03). En producción serían PostgreSQL. */
public class RepositorioOfertas {
    private final List<Oferta> ofertas = List.of(
        new Oferta(1, "Queso llanero", "libras", 20, "Don Álvaro", "La Guafilla", 14000, "Lácteos"),
        new Oferta(2, "Yuca", "libras", 30, "Doña Marta", "Tacarimena", 2500, "Tubérculos y plátano"),
        new Oferta(3, "Huevos AA", "panales", 8, "Finca El Charte", "El Charte", 16000, "Huevos y carnes"),
        new Oferta(4, "Plátano", "racimos", 40, "Doña Marta", "Tacarimena", 12000, "Tubérculos y plátano"),
        new Oferta(5, "Carne de cerdo", "libras", 25, "Finca El Charte", "El Charte", 15000, "Huevos y carnes"),
        new Oferta(6, "Cuajada", "libras", 12, "Don Álvaro", "La Guafilla", 9000, "Lácteos"),
        new Oferta(7, "Mantequilla criolla", "libras", 6, "Don Álvaro", "La Guafilla", 11000, "Lácteos"),
        new Oferta(8, "Tomate chonto", "libras", 35, "Huerta Los Cedros", "Tacarimena", 3200, "Verduras y frutas"),
        new Oferta(9, "Cilantro", "manojos", 50, "Huerta Los Cedros", "Tacarimena", 1000, "Verduras y frutas"),
        new Oferta(10, "Maracuyá", "libras", 18, "Finca El Charte", "El Charte", 4500, "Verduras y frutas"),
        new Oferta(11, "Papaya", "unidades", 14, "Doña Marta", "Tacarimena", 6000, "Verduras y frutas"),
        new Oferta(12, "Pollo criollo", "unidades", 0, "Finca El Charte", "El Charte", 38000, "Huevos y carnes"));

    /** RF-03: filtra por texto (producto o productor) y por vereda. */
    public List<Oferta> buscar(String texto, String vereda) {
        String t = normalizar(texto);
        return ofertas.stream()
            .filter(o -> vereda == null || vereda.isBlank() || o.getVereda().equalsIgnoreCase(vereda))
            .filter(o -> t.isEmpty() || normalizar(o.getProducto()).contains(t)
                      || normalizar(o.getProductor()).contains(t))
            .collect(Collectors.toList());
    }

    public Optional<Oferta> porId(long id) {
        return ofertas.stream().filter(o -> o.getId() == id).findFirst();
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase().trim();
    }
}
