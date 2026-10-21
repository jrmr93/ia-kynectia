# Registro de Cambios (CHANGELOG)

Todos los cambios notables en el proyecto **Kynectia Admin** se documentan en este archivo.
El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/) y este proyecto se adhiere a [Semantic Versioning](https://semver.org/lang/es/).

---

## [1.0.0] - 2026-10-21

### Añadido
- **Arquitectura de Software y Base de Datos:**
  - Modelo relacional normalizado en PostgreSQL 18 con 17 tablas.
  - Migrador automático versionado con soporte para `up` y `down` (`src/database/migrator.ts`).
  - Seeders iniciales con roles, permisos, superadmin inicial, 10 grupos musculares, 8 implementos, 6 ejercicios maestros con relaciones N:M, 2 rutinas completas, planes comerciales y usuarios atletas (`src/database/seed.ts`).
  - Triggers PostgreSQL automáticos para mantenimiento del timestamp `updated_at`.
  - Integridad referencial con restricciones `ON DELETE RESTRICT` para evitar orfandad de registros.
- **Backend REST API (Node.js + Express + TypeScript):**
  - Módulo de Autenticación con JWT, bcrypt (12 rounds) y perfiles administrativos.
  - Middlewares de autorización granular RBAC (`requirePermission`, `requireRole`).
  - Middlewares de validación de entradas con esquemas Zod en `body`, `query` y `params`.
  - Manejo centralizado de errores con mapeo nativo de códigos de error PostgreSQL (23505, 23503, 22P02).
  - Módulo Dashboard con agregaciones SQL en tiempo real y métricas operativas.
  - Módulo Usuarios con paginación, búsqueda, filtros por estado y meta, detalle con suscripciones, toggle activo y anonimización/soft-delete.
  - Módulo Grupos Musculares con imágenes ilustrativas y verificación de dependencias.
  - Módulo Implementos y Equipos con categorización y conteo de ejercicios dependientes.
  - Módulo Catálogo de Ejercicios con relaciones N:M hacia grupos primarios/secundarios e implementos, instrucciones y recomendaciones de seguridad.
  - Módulo Rutinas de Entrenamiento con orden secuencial de ejercicios, series, repeticiones, descansos, cargas y versionado al publicar.
  - Módulo Planes y Precios con beneficios en JSONB, promociones y consulta de suscripciones activas.
  - Módulo Publicidad con control de banners, fechas de vigencia, ubicaciones y conteo de impresiones/clics.
  - Módulo Multimedia con subida local segura vía Multer y registro de metadatos en PostgreSQL.
  - Módulo de Auditoría inmutable con registro automático de transacciones administrativas.
- **Frontend Panel Administrativo (React 18 + Vite + TypeScript):**
  - Sistema de diseño CSS oscuro para fitness (`Plus Jakarta Sans` y `Outfit`, acentos cyan/esmeralda/ámbar).
  - Sidebar de navegación con indicador de estado de la base de datos PostgreSQL.
  - Header superior con información del administrador activo y botón de cierre de sesión.
  - Vistas interactivas completas para los 9 módulos administrativos.
  - Formularios modales con validaciones en cliente y respuestas del servidor.
  - Modales de confirmación para eliminaciones y acciones críticas.
  - Sistema de notificaciones emergentes Toast (éxito, error, informativo).
  - Paginación interactiva y filtros combinables en tiempo real.
- **Pruebas Automatizadas y Documentación:**
  - Suite de 15 pruebas de integración con Jest y Supertest en `src/tests/api.test.ts`.
  - Especificación OpenAPI 3.0 / Swagger en `docs/openapi.yaml`.
  - Diccionario y relaciones en `docs/database-schema.md`.
  - Manual de autenticación y seguridad en `docs/authentication.md`.
  - Guía completa de instalación y despliegue en `README.md`.
