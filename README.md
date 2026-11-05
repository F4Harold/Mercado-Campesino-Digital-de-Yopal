# Mercado Campesino Digital de Yopal

Plataforma de LlanoSoft S.A.S. (caso académico, Ingeniería de Software – Corporación Universitaria Remington, sede Yopal) para que los productores del Mercado Campesino publiquen su oferta semanal, los clientes reserven antes del viernes a las 6:00 p. m. y la Secretaría consulte reportes de ventas.

## Contenido

- `src/main/java/co/llanosoft/mercado/` – regla de negocio de reservas (RF-04, RF-05, RNF-06).
- `src/main/java/co/llanosoft/mercado/ServidorWeb.java` – servidor HTTP (solo JDK) con la API `/api/estado`, `/api/ofertas` y `/api/reservas`, y `RepositorioOfertas` con datos de ejemplo.
- `src/main/resources/public/` – interfaz web de tema oscuro: `index.html`, `styles.css` y `app.js` (hero con estadísticas, filtros por vereda, precio y existencias, categorías, tarjetas con favoritos, panel lateral con mis reservas y cajón de reserva en 3 pasos: cantidad, hora de recogida y pago).
- `src/test/java/co/llanosoft/mercado/` – 9 pruebas JUnit 5 (CP-01 a CP-09).
- `docs/` – diagramas de arquitectura, entidad-relación, casos de uso, secuencia «Reservar producto» y prototipos.

## Requisitos

- Java 17
- Maven 3.8 o superior

## Ejecutar las pruebas

```bash
mvn test
```

Resultado esperado: `Tests run: 9, Failures: 0, Errors: 0`.

## Ejecutar la aplicación

```bash
mvn compile exec:java
```

Abra http://localhost:8080. Sin Maven: `javac -d out src/main/java/co/llanosoft/mercado/*.java` y luego `java -cp out:src/main/resources co.llanosoft.mercado.ServidorWeb`.

Para demostrar el cierre sin esperar al viernes, `POST /api/reservas` acepta un campo opcional `"momento"` (por ejemplo `"2026-11-06T18:00:00"`).

## Estrategia de ramas

- `main`: versiones en producción (etiquetas v1.0.0…)
- `develop`: integración
- `feature/rf-xx-descripcion`: una rama por requisito
- `hotfix/descripcion`: correcciones urgentes

Cada pull request a `develop` requiere revisión de otro integrante. Commits con formato `tipo(RF-xx): descripción`.
