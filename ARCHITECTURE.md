# Kynectia - Arquitectura y Plan de Implementación del Panel de Administración

## 1. Diagnóstico del Entorno de Trabajo
- **Espacio de trabajo:** `ia-kynectia` (repositorio Git inicializado y limpio).
- **Entorno de ejecución:** Node.js v24.18.0, npm v11.16.0 en Windows x64.
- **Base de datos disponible:** PostgreSQL (servicios activos en puertos 5432 y 5433, cliente `psql` disponible).
- **Estado de código previo:** No existen archivos o dependencias heredadas; se procede con una arquitectura limpia, modular y desacoplada.

---

## 2. Propuesta de Arquitectura de Software

### 2.1 Estructura Multimodular Desacoplada
Para garantizar mantenibilidad, separación estricta de responsabilidades e independencia de despliegue:

```
ia-kynectia/
├── backend/                  # Servidor API REST Node.js + TypeScript
│   ├── src/
│   │   ├── config/           # Variables de entorno y configuración de DB/Uploads
│   │   ├── database/         # Pool de conexiones PostgreSQL, migraciones y seeders
│   │   ├── middleware/       # Autenticación JWT, RBAC, validación Zod, rate limiting, audit
│   │   ├── modules/          # Módulos de negocio (rutas, controladores, servicios, repositorios)
│   │   │   ├── auth/         # Login, perfil, sesiones, refresh tokens
│   │   │   ├── users/        # Gestión de usuarios, estados, roles y suscripciones
│   │   │   ├── muscles/      # Grupos musculares y anatomía
│   │   │   ├── exercises/    # Catálogo de ejercicios, relaciones N:M
│   │   │   ├── equipment/    # Implementos y equipamiento de entrenamiento
│   │   │   ├── workouts/     # Rutinas, orden de ejecución, sets, reps, versionado
│   │   │   ├── plans/        # Planes de suscripción, precios, promociones
│   │   │   ├── ads/          # Publicidad, ubicaciones, prioridades, vigencia
│   │   │   ├── media/        # Metadatos multimedia y gestor de subidas
│   │   │   ├── dashboard/    # Agregaciones y métricas en tiempo real desde PostgreSQL
│   │   │   └── audit/        # Historial de auditoría administrativa
│   │   ├── utils/            # Hashing seguro (bcrypt), logger, respuestas estándar
│   │   └── app.ts            # Configuración de Express, CORS, Helmet, JSON parser
│   ├── migrations/           # Scripts SQL versionados (up/down)
│   ├── seeds/                # Datos semilla iniciales (admin, grupos musculares, implementos)
│   ├── tests/                # Pruebas automatizadas (Jest / Supertest)
│   ├── package.json
│   └── tsconfig.json
│
├── frontend/                 # Panel Web Administrativo (React + Vite + TypeScript)
│   ├── src/
│   │   ├── api/              # Cliente HTTP tipado con interceptores de auth
│   │   ├── assets/           # Iconos, logos e imágenes de branding
│   │   ├── components/       # Componentes UI reutilizables (Tablas, Modales, Badges, Filtros, Toasts)
│   │   ├── context/          # Contexto de autenticación y notificaciones
│   │   ├── layout/           # Sidebar lateral, Header con datos del admin, Breadcrumbs
│   │   ├── pages/            # Vistas (Dashboard, Usuarios, Músculos, Ejercicios, Equipos, Rutinas, Planes, Anuncios, Auditoría)
│   │   ├── styles/           # Sistema de diseño con CSS moderno (dark mode fitness, acentos energéticos)
│   │   └── types/            # Definiciones de tipos TypeScript compartidos
│   ├── package.json
│   └── vite.config.ts
│
├── docs/                     # Documentación técnica
│   ├── README.md
│   ├── openapi.yaml          # Especificación OpenAPI / Swagger 3.0
│   ├── database-schema.md    # Diccionario de datos y diagrama ER
│   ├── authentication.md     # Mecanismos de seguridad y permisos
│   └── CHANGELOG.md
│
└── .env.example              # Plantilla de variables de entorno globales
```

---

## 3. Modelo de Datos Relacional (PostgreSQL)

El diseño relacional normalizado incluye integridad referencial, índices para optimizar consultas frecuentes, marcas de auditoría (`created_at`, `updated_at`, `deleted_at`) y restricciones de negocio:

### Entidades Principales:
1. **`roles` y `permissions`**: RBAC escalable (Superadmin, Entrenador Admin, Editor de Contenidos, Soporte).
2. **`admin_users`**: Credenciales hash (bcrypt con salado seguro), control de bloqueo, último acceso.
3. **`users`**: Atletas/usuarios de la plataforma (estado de cuenta, métricas corporales, datos de contacto).
4. **`muscle_groups`**: Anatomía deportiva (Pectorales, Dorsales, Cuádriceps, Deltoides, etc.), descripción, imagen, estado.
5. **`equipment`**: Implementos deportivos (mancuernas, barras, bandas, máquinas), categoría y estado.
6. **`exercises`**: Catálogo maestro (nombre, dificultad, tipo, instrucciones paso a paso, multimedia, tips de seguridad, publicación).
7. **`exercise_muscles`**: Tabla intermedia N:M con indicador `is_primary` (grupo muscular primario o secundario).
8. **`exercise_equipment`**: Tabla intermedia N:M entre ejercicios e implementos requeridos.
9. **`workout_routines`**: Rutinas predefinidas con versionado (`version`), objetivo, nivel, duración estimada, estado de publicación.
10. **`routine_exercises`**: Configuración de cada ejercicio en la rutina (orden `sequence_order`, series, repeticiones, descanso en segundos, carga/peso sugerido, notas técnicas).
11. **`plans`**: Oferta comercial (mensual, trimestral, anual), moneda, precio, beneficios JSONB, promociones vigentes.
12. **`user_subscriptions`**: Registro de suscripciones activas/expiradas/canceladas vinculadas a usuarios y planes.
13. **`ads`**: Módulo publicitario (título, imagen/multimedia, URL de destino, fechas de vigencia, ubicación en la app, prioridad de impresión, estado activo).
14. **`media_assets`**: Catálogo de archivos subidos (nombre original, nombre sanitizado, ruta de almacenamiento local o S3, mimetype, tamaño).
15. **`audit_logs`**: Registro inmutable de acciones administrativas (administrador, acción, tabla afectada, registro id, valores anteriores/nuevos en JSONB, IP, user-agent).

---

## 4. Plan de Implementación por Etapas

| Fase | Título | Entregables Principales |
|---|---|---|
| **Fase 1** | **Estructura Base y Configuración** | Inicialización de proyectos backend y frontend con TypeScript, linting, variables `.env.example`, setup de scripts. |
| **Fase 2** | **Base de Datos y Migraciones** | Scripts de migración SQL para las 15 tablas relacionales, índices, triggers para `updated_at`, seeders con datos iniciales y usuario superadmin. |
| **Fase 3** | **Seguridad, Autenticación y Auditoría** | JWT, hashing bcrypt con salt rounds 12, middleware de autenticación, control de permisos RBAC y logger de auditoría en PostgreSQL. |
| **Fase 4** | **Backend REST API Modular** | Controladores, servicios y validación (Zod) para: Usuarios, Grupos Musculares, Equipamiento, Ejercicios (N:M), Rutinas compuestas (N:M versionadas), Planes, Publicidad, Multimedia y Dashboard Analytics. |
| **Fase 5** | **Dashboard Principal & Métricas Reales** | Backend endpoints de agregación SQL (conteos exactos, porcentajes de activos, rutinas publicadas, métricas financieras teóricas) y vista con widgets y gráficos interactivos. |
| **Fase 6** | **Frontend UI & Módulos CRUD** | Interfaz completa en React: Sidebar, Header, Breadcrumbs, Tablas con búsqueda/filtros/paginación, formularios modales y completos, confirmaciones de borrado, toasts informativos. |
| **Fase 7** | **Pruebas y Verificación Integral** | Pruebas de integración de endpoints con Supertest, validación de integridad referencial de PostgreSQL y verificación de flujos en navegador. |
| **Fase 8** | **Documentación Técnica de Entrega** | Generación de `README.md`, `openapi.yaml`, `database-schema.md`, `authentication.md` y `CHANGELOG.md`. |
