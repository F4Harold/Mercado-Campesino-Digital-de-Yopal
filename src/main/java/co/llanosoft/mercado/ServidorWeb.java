package co.llanosoft.mercado;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Servidor HTTP sin dependencias: API REST mínima y archivos de la PWA (RF-03, RF-04, RF-05, RF-06). */
public class ServidorWeb {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Map<String, String> ESTATICOS = Map.of(
        "/", "index.html", "/index.html", "index.html",
        "/styles.css", "styles.css", "/app.js", "app.js");

    private final ServicioReservas servicio = new ServicioReservas();
    private final RepositorioOfertas repositorio = new RepositorioOfertas();
    private final SecureRandom azar = new SecureRandom();

    public static void main(String[] args) throws IOException {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new ServidorWeb().iniciar(puerto);
    }

    void iniciar(int puerto) throws IOException {
        HttpServer http = HttpServer.create(new InetSocketAddress(puerto), 0);
        http.createContext("/", this::atender);
        http.start();
        System.out.println("Mercado Campesino Digital en http://localhost:" + puerto);
    }

    private void atender(HttpExchange ex) throws IOException {
        try {
            String ruta = ex.getRequestURI().getPath();
            String metodo = ex.getRequestMethod();
            if (ruta.equals("/api/estado") && metodo.equals("GET")) estado(ex);
            else if (ruta.equals("/api/ofertas") && metodo.equals("GET")) ofertas(ex);
            else if (ruta.equals("/api/reservas") && metodo.equals("POST")) reservar(ex);
            else if (ESTATICOS.containsKey(ruta) && metodo.equals("GET")) estatico(ex, ESTATICOS.get(ruta));
            else responder(ex, 404, "text/plain; charset=utf-8", "No encontrado");
        } catch (RuntimeException e) {
            json(ex, 500, "{\"error\":\"Error interno\"}");
        }
    }

    /** RF-05: tiempo restante para el cierre del viernes a las 6:00 p. m. */
    private void estado(HttpExchange ex) throws IOException {
        LocalDateTime ahora = LocalDateTime.now(BOGOTA);
        LocalDateTime cierre = servicio.cierreDeReservas(ahora);
        long segundos = Duration.between(ahora, cierre).getSeconds();
        json(ex, 200, "{\"abierto\":" + (segundos > 0) + ",\"segundosParaCierre\":" + Math.max(segundos, 0) + "}");
    }

    private void ofertas(HttpExchange ex) throws IOException {
        Map<String, String> q = consulta(ex.getRequestURI().getRawQuery());
        List<Oferta> lista = repositorio.buscar(q.get("q"), q.get("vereda"));
        json(ex, 200, lista.stream().map(ServidorWeb::aJson).collect(Collectors.joining(",", "[", "]")));
    }

    /** RF-04, RF-05, RNF-06: reserva con control de inventario; "momento" opcional para demostraciones. */
    private void reservar(HttpExchange ex) throws IOException {
        String cuerpo = new String(ex.getRequestBody().readNBytes(4096), StandardCharsets.UTF_8);
        try {
            Oferta oferta = repositorio.porId(Long.parseLong(campo(cuerpo, "idOferta", "\\d+"))).orElse(null);
            int cantidad = Integer.parseInt(campo(cuerpo, "cantidad", "-?\\d+"));
            String m = campo(cuerpo, "momento", "[0-9T:.\\-]+");
            LocalDateTime momento = m.isEmpty() ? LocalDateTime.now(BOGOTA) : LocalDateTime.parse(m);
            int disponible = servicio.reservar(oferta, cantidad, momento);
            json(ex, 201, "{\"codigo\":\"" + codigoRecogida() + "\",\"disponible\":" + disponible + "}");
        } catch (ReservaRechazadaException | NumberFormatException | java.time.format.DateTimeParseException e) {
            String mensaje = e instanceof ReservaRechazadaException ? e.getMessage() : "Revise los datos de la reserva";
            json(ex, 409, "{\"error\":\"" + esc(mensaje) + "\"}");
        }
    }

    private String codigoRecogida() {
        StringBuilder sb = new StringBuilder("MC-");
        for (int i = 0; i < 4; i++) sb.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        return sb.toString();
    }

    private void estatico(HttpExchange ex, String archivo) throws IOException {
        try (InputStream in = ServidorWeb.class.getResourceAsStream("/public/" + archivo)) {
            if (in == null) { responder(ex, 404, "text/plain; charset=utf-8", "No encontrado"); return; }
            String tipo = archivo.endsWith(".css") ? "text/css" : archivo.endsWith(".js") ? "text/javascript" : "text/html";
            responder(ex, 200, tipo + "; charset=utf-8", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static String aJson(Oferta o) {
        return "{\"id\":" + o.getId() + ",\"producto\":\"" + esc(o.getProducto()) + "\",\"unidad\":\"" + esc(o.getUnidad())
            + "\",\"productor\":\"" + esc(o.getProductor()) + "\",\"vereda\":\"" + esc(o.getVereda())
            + "\",\"categoria\":\"" + esc(o.getCategoria()) + "\",\"precio\":" + o.getPrecio() + ",\"disponible\":" + o.getCantidadDisponible() + "}";
    }

    private static String campo(String json, String nombre, String patron) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*\"?(" + patron + ")\"?").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private static Map<String, String> consulta(String raw) {
        Map<String, String> mapa = new java.util.HashMap<>();
        if (raw == null) return mapa;
        for (String par : raw.split("&")) {
            String[] kv = par.split("=", 2);
            mapa.put(kv[0], kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return mapa;
    }

    private static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }

    private static void json(HttpExchange ex, int codigo, String cuerpo) throws IOException {
        responder(ex, codigo, "application/json; charset=utf-8", cuerpo);
    }

    private static void responder(HttpExchange ex, int codigo, String tipo, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", tipo);
        ex.sendResponseHeaders(codigo, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}
