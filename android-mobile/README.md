# Kynectia - Subsistema Móvil Android

Aplicación móvil nativa en Android para atletas y usuarios finales de la plataforma **Kynectia**, construida con **Jetpack Compose**, **Material 3**, **Coroutines**, **Navigation3** y **Retrofit/OkHttp**.

---

## 🚀 Características y Funcionalidades de Entrenamiento

1. **Exploración de Rutinas (`WorkoutsScreen`)**:
   - Catálogo de rutinas organizadas por dificultad (*Principiante*, *Intermedio*, *Avanzado*) y objetivos de entrenamiento (*Fuerza*, *Hipertrofia*, *Potencia*).
   - Buscador en tiempo real por título o descripción.
   - Tarjetas informativas con duración estimada, número de ejercicios y portada.

2. **Modo Entrenamiento Interactivo / Player (`WorkoutPlayerScreen`)**:
   - Cronómetro global de la sesión de entrenamiento (`⏱️ mm:ss`).
   - Barra de progreso por ejercicio (e.g. *Ejercicio 2 de 5*).
   - Tarjeta del ejercicio con demostración gráfica, grupo muscular primario e instrucciones técnicas de ejecución.
   - **Registro de Series**: indicador de serie actual vs total (e.g. *Serie 2 de 4*), selectores interactivos (+/-) de repeticiones logradas y peso levantado en kilogramos (`kg`).
   - **Temporizador de Descanso Inteligente**:
     - Cuenta regresiva visual interactiva entre series.
     - Botones de ajuste rápido (`+15s`, `-15s`) y botón para saltar descanso (`Saltar Descanso`).
     - Alerta háptica/vibración en el dispositivo al finalizar el tiempo de descanso.
   - Controles de navegación de rutina: pausa/reanudación, ejercicio anterior, salto de ejercicio y finalización anticipada.

3. **Resumen y Métricas de Sesión (`WorkoutSummaryScreen`)**:
   - Tarjeta de felicitación por entrenamiento completado.
   - Métricas calculadas:
     - Tiempo total transcurrido.
     - Series totales completadas.
     - Volumen total levantado (`reps × kg`).
   - Desglose detallado por ejercicio con las series y cargas registradas.

4. **Catálogo de Ejercicios y Anatomía (`ExercisesScreen`)**:
   - Lista completa de ejercicios con filtros por grupo muscular (Pectorales, Dorsales, Cuádriceps, Deltoides, Bíceps, etc.).
   - Modal detallado con instrucciones paso a paso, tips de seguridad y prevención de lesiones, y equipamiento necesario.

5. **Ajustes y Conexión Backend (`SettingsScreen`)**:
   - Editor dinámico de URL del Backend REST API:
     - Por defecto: `http://10.0.2.2:4000/api/v1/` (emulador oficial de Android).
     - Configurable a la IP local de tu máquina en red Wi-Fi (e.g. `http://192.168.1.50:4000/api/v1/`).
   - Botón *"Guardar y Probar"* que realiza un healthcheck en vivo contra `/api/health`.
   - Soporte **Offline / Mock Fallback**: si el backend está desconectado o en mantenimiento, la app conmuta automáticamente a datos locales para que el usuario pueda seguir entrenando sin interrupciones.

---

## 🛠️ Stack Tecnológico

- **Lenguaje:** Kotlin 2.3.20 (JVM Toolchain 17).
- **UI Toolkit:** Jetpack Compose con BOM `2026.03.01` + Material 3.
- **Navegación:** AndroidX Navigation3.
- **Networking:** Retrofit 2.11.0 + OkHttp 4.12.0 + `kotlinx.serialization.json` 1.7.3.
- **Carga de Imágenes:** Coil Compose 2.7.0.
- **Estilo de Diseño:** Dark Mode Fitness Premium con acentos de color Lima Kynectia (`#CCFF00`) y Naranja Eléctrico (`#FF5E3A`).

---

## 📦 Compilación y Ejecución

### 1. Requisitos previos
- JDK 17 (OpenJDK Temurin o similar).
- Android SDK (API 34+ / API 36).

### 2. Compilar APK de depuración
Desde la terminal en `android-mobile/`:
```bash
./gradlew assembleDebug
```
El archivo APK generado se encontrará en:
`android-mobile/app/build/outputs/apk/debug/app-debug.apk`

### 3. Ejecutar pruebas unitarias
```bash
./gradlew testDebugUnitTest
```

### 4. Abrir en Android Studio
Abre la carpeta `android-mobile/` como proyecto en **Android Studio**. Presiona el botón verde de **Run** (`Shift+F10`) seleccionando un emulador o dispositivo físico conectado por USB/Wi-Fi.
