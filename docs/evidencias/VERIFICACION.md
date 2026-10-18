# Verificación de la entrega — 18 de octubre de 2026

Entorno: Windows, JDK 25.0.1, Maven 3.9.16, Spring Boot 4.1.1.

| Verificación | Resultado |
| --- | --- |
| `mvn verify` | BUILD SUCCESS; 77 tests, 0 fallos, 0 errores, 0 omitidos |
| Tests de servicio | 6 correctos |
| Integración/MockMvc | 71 correctos, incluyendo parametrizados |
| Colección Postman ejecutada con Newman | 52 solicitudes; 126 comprobaciones; 0 fallos |
| Script `scripts/smoke_web.py` contra el WAR | 61 comprobaciones HTTP correctas |
| Arranque `java -jar target/sistema-gestion-pedidos.war` | Servidor iniciado; JSP y JSF accesibles |
| Persistencia después de reiniciar | Cliente creado por JSF antes del reinicio continúa en el listado después del reinicio |
| Revisión visual | Login, resumen, clientes, JSF y pedido confirmado revisados en navegador |
| JSP sin scriptlets | No se encontraron bloques Java `<% ... %>`; solo directivas JSP, JSTL y EL |

Las pruebas automatizadas de backend se ejecutaron contra H2 en memoria aislada. Postman, las pruebas web y la comprobación de reinicio se realizaron contra H2 **en disco**, con una base de pruebas separada de la ruta predeterminada `data/pedidos`.

Se revisó visualmente un pedido de ejemplo con 12 unidades a S/ 8.50 y 24 unidades a S/ 3.20, por un total de S/ 178.80. Su confirmación se realizó desde la interfaz web. Estos datos de la vista previa no se incluyen en el ZIP del código: la aplicación carga los ejemplos documentados en el README al abrir una base nueva.

La configuración opcional para PostgreSQL está incluida, pero no fue ejecutada contra un servidor PostgreSQL en esta verificación.

Los reportes de JUnit adjuntos y el resumen de Newman contienen los resultados de la ejecución. Los tokens y las contraseñas de las respuestas de prueba no se distribuyen en esos reportes.
