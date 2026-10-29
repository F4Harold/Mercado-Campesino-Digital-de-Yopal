# Mercado Campesino Digital de Yopal

Plataforma de LlanoSoft S.A.S. (caso académico, Ingeniería de Software – Corporación Universitaria Remington, sede Yopal) para que los productores del Mercado Campesino publiquen su oferta semanal, los clientes reserven antes del viernes a las 6:00 p. m. y la Secretaría consulte reportes de ventas.

## Contenido

- `src/main/java/co/llanosoft/mercado/` – regla de negocio de reservas (RF-04, RF-05, RNF-06).
- `src/test/java/co/llanosoft/mercado/` – 9 pruebas JUnit 5 (CP-01 a CP-09).
- `docs/` – diagramas de arquitectura, entidad-relación, casos de uso, secuencia «Reservar producto» y prototipos.

## Requisitos

- Java 25 LTS
- Maven 3.8 o superior

## Ejecutar las pruebas

```bash
mvn test
```

Resultado esperado: `Tests run: 9, Failures: 0, Errors: 0`.

## Estrategia de ramas

- `main`: versiones en producción (etiquetas v1.0.0…)
- `develop`: integración
- `feature/rf-xx-descripcion`: una rama por requisito
- `hotfix/descripcion`: correcciones urgentes

Cada pull request a `develop` requiere revisión de otro integrante. Commits con formato `tipo(RF-xx): descripción`.
