# Kynectia - Panel de Administración Web

Sistema administrativo integral, moderno y de alto rendimiento para **Kynectia**, plataforma de entrenamiento, fitness y salud deportiva.

Este proyecto abarca exclusivamente el **panel de administración web**, su **backend REST API** y su **base de datos relacional PostgreSQL**.

---

## 1. Stack Tecnológico

- **Backend:** Node.js (v24+) con Express y TypeScript.
- **Base de Datos:** PostgreSQL (v16 / v18) con migraciones versionadas y pool transaccional (`pg`).
- **Seguridad:** JWT (JSON Web Tokens), bcrypt (12 salt rounds), rate limiting, Helmet, validación con Zod y control granular de roles y permisos (RBAC).
- **Frontend:** React 18 / 19 con Vite, TypeScript, Vanilla/Modern CSS tokens (estética dark fitness de alto contraste) y Lucide Icons.
- **Pruebas:** Jest y Supertest para pruebas automatizadas de integración.
- **Control de Versiones:** Git.

---

## 2. Estructura del Repositorio

```
ia-kynectia/
├── backend/                  # Servidor API REST Node.js + TypeScript
│   ├── migrations/           # Scripts SQL de migración (up / down)
│   ├── src/
│   │   ├── config/           # Carga validada de variables de entorno (.env)
│   │   ├── database/         # Pool PostgreSQL, migrador y seeder
│   │   ├── middleware/       # Autenticación JWT, RBAC, validación Zod, errores
│   │   ├── modules/          # Módulos: auth, users, muscles, equipment, exercises,
│   │   │                     # workouts, plans, ads, media, audit, dashboard
│   │   ├── utils/            # Respuestas estándar, logger de auditoría
│   │   ├── app.ts            # Configuración de Express, CORS y rutas
│   │   └── index.ts          # Inicialización y arranque del servidor HTTP
│   └── tests/                # Suite de pruebas automatizadas con Supertest
│
├── frontend/                 # Panel Web Administrativo (React + Vite + TypeScript)
│   ├── src/
│   │   ├── api/              # Cliente HTTP tipado con interceptor de tokens
│   │   ├── components/       # Modales, ConfirmDialog, Sidebar, Header, Toasts
│   │   ├── context/          # Contextos de Autenticación y Notificaciones
│   │   ├── styles/           # Sistema de diseño CSS dark mode fitness
│   │   ├── types/            # Definiciones TypeScript de entidades
│   │   └── views/            # Vistas: Dashboard, Usuarios, Músculos, Equipos,
│   │                         # Ejercicios, Rutinas, Planes, Publicidad, Auditoría
│   ├── index.html
│   └── vite.config.ts
│
├── docs/                     # Documentación técnica de entrega
│   ├── openapi.yaml          # Especificación OpenAPI 3.0 / Swagger
│   ├── database-schema.md    # Diccionario de datos y relaciones de PostgreSQL
│   ├── authentication.md     # Mecanismos de seguridad y permisos RBAC
│   └── CHANGELOG.md          # Historial de versiones y cambios
│
├── .env.example              # Plantilla de variables de entorno
├── ARCHITECTURE.md           # Especificación de arquitectura y diseño
└── package.json              # Scripts de orquestación raíz
```

---

## 3. Prerrequisitos

- **Node.js:** Versión 18 o superior (probado en Node.js v24.18.0).
- **PostgreSQL:** Versión 14 o superior (probado en PostgreSQL 18.4) corriendo en el puerto 5432 o 5433.
- **npm:** Versión 9 o superior.

---

## 4. Instalación y Configuración

### 4.1 Clonar o situarse en el repositorio
```bash
cd "c:\Users\Justo Maldonado\Documents\git\ia-kynectia"
```

### 4.2 Variables de Entorno
Copia la plantilla `.env.example` en `backend/.env` y ajusta las credenciales de PostgreSQL si difieren de los valores predeterminados:
```bash
cp .env.example backend/.env
```

Contenido por defecto de `backend/.env`:
```env
PORT=4000
NODE_ENV=development
CLIENT_URL=http://localhost:5173

# Base de Datos PostgreSQL
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=postgres
DB_NAME=kynectia_db
DB_SSL=false

# Seguridad
JWT_SECRET=kynectia_super_secure_jwt_secret_key_2026_xyz!@#
JWT_EXPIRES_IN=8h
BCRYPT_SALT_ROUNDS=12

# Multimedia
UPLOAD_DIR=./uploads
MAX_FILE_SIZE_MB=25
```

### 4.3 Creación de la Base de Datos
Si no existe la base de datos `kynectia_db`, créala con `psql`:
```bash
psql -U postgres -c "CREATE DATABASE kynectia_db WITH ENCODING 'UTF8';"
```

### 4.4 Instalación de Dependencias
```bash
# Instalar backend
cd backend && npm install

# Instalar frontend
cd ../frontend && npm install
cd ..
```

---

## 5. Migraciones y Datos Semilla (Seeders)

El proyecto incluye un sistema de migraciones versionadas y datos semilla con información real para todos los módulos:

```bash
# 1. Ejecutar migraciones pendientes en PostgreSQL
npm run migrate

# 2. Poblar la base de datos con datos reales iniciales
npm run seed
```

> **Credenciales Administrativas Iniciales:**
> - **Correo:** `admin@kynectia.com`
> - **Contraseña:** `AdminKynectia2026!`
> - **Rol:** Superadministrador (Acceso total)

---

## 6. Ejecución del Proyecto en Desarrollo

Para ejecutar backend y frontend simultáneamente:

### Terminal 1 - Backend API:
```bash
npm run dev:backend
# Servidor activo en: http://localhost:4000
# Healthcheck: http://localhost:4000/api/health
```

### Terminal 2 - Frontend Panel:
```bash
npm run dev:frontend
# Panel web disponible en: http://localhost:5173
```

---

## 7. Pruebas Automatizadas de Integración

Ejecuta la suite de pruebas unitarias y de integración que validan el flujo de autenticación, la conexión a PostgreSQL, el cálculo de métricas del dashboard y las operaciones de los módulos:

```bash
npm test
```

---

## 8. Respaldo y Restauración de Base de Datos

### Generar respaldo (Backup SQL):
```bash
pg_dump -U postgres -d kynectia_db -F p -f kynectia_backup.sql
```

### Restaurar respaldo:
```bash
psql -U postgres -d kynectia_db -f kynectia_backup.sql
```

---

## 9. Módulos del Sistema Administrativo

1. **Dashboard Principal:** Indicadores reales calculados desde PostgreSQL (usuarios totales, activos, ejercicios publicados, rutinas, planes, suscripciones, anuncios) y gráficos de distribución por dificultad y ciclo comercial.
2. **Gestión de Usuarios:** CRUD, filtrado por metas físicas y estado, ficha técnica con suscripciones activas, activación/desactivación y anonimización de datos personales.
3. **Grupos Musculares:** Segmentación anatómica, galerías visuales, conteo de ejercicios relacionados y control de dependencias.
4. **Implementos y Equipos:** Inventario deportivo con categorización (pesas libres, máquinas, autocarga, etc.) y validación de relaciones.
5. **Catálogo de Ejercicios:** Movimientos detallados con grupo primario, grupos secundarios (N:M), implementos (N:M), instrucciones técnicas, consejos de seguridad y multimedia.
6. **Rutinas de Entrenamiento:** Constructor de programas con ejercicios secuenciados, series, repeticiones, descansos, cargas recomendadas y versionado de publicaciones.
7. **Planes y Precios:** Definición de oferta comercial, precios en USD, beneficios JSONB, promociones y consulta de suscripciones activas.
8. **Publicidad y Anuncios:** Gestión de campañas, fechas de vigencia, ubicaciones (banner, intersticial, lateral) y métricas de impresiones/clics.
9. **Auditoría y Seguridad:** Registro inmutable con diferencias JSON de cambios administrativos, IP y navegador del operador.
