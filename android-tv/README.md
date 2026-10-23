# Kynectia Android TV 📺

Subsistema de pantalla gigante (10-foot UI) para **Android TV, Google TV y Smart TV Boxes**, sincronizado en tiempo real con la app móvil [android-mobile](../android-mobile) de Kynectia.

---

## 🚀 Características Principales

1. **Vinculación Dual Instantánea (QR & PIN):**
   - Al iniciar la app en la TV, genera automáticamente un código PIN de 6 dígitos numéricos y un código QR visual para pantalla grande.
   - La app móvil puede conectarse ingresando los 6 dígitos o mediante el payload QR.
2. **Entrenamiento en Pantalla Gigante (10-foot UI):**
   - **Panel Izquierdo:** Información del ejercicio actual en tamaño TV, número de serie, índice de avance (`Ejercicio X de Y`), ilustración/fotografía en alta resolución y barra de progreso de la rutina general.
   - **Panel Derecho (Modo Activo):** Serie actual (`SERIE X / Y`), repeticiones objetivo en formato gigante y carga en kilogramos con colores de alto contraste (Kynectia Lime & Orange).
   - **Panel Derecho (Modo Descanso):** Anillo de recuperación animado en 360° con cuenta regresiva circular gigante (`45s`), badge visual y preview de la siguiente serie a ejecutar.
3. **Resumen de Sesión (Pantalla de Victoria):**
   - Muestra trofeo de celebración, tiempo total de entrenamiento (`mm:ss`), total de series completadas y volumen total levantado en kilogramos (`kg`).
4. **Resiliencia de Red y Polling Fluido:**
   - Sincronización continua cada 800ms con reintento automático y sin bloqueos de interfaz gráfica.

---

## 🛠️ Estructura del Proyecto

```
android-tv/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Configuración Leanback y modo apaisado (Landscape)
│   │   ├── java/com/kynectia/tv/
│   │   │   ├── MainActivity.kt          # Host principal y enrutador de pantallas
│   │   │   ├── data/
│   │   │   │   ├── TvModels.kt          # Contratos de datos (Sesión, Estado en vivo, Resumen)
│   │   │   │   └── TvApiService.kt      # Cliente Retrofit y OkHttp
│   │   │   ├── theme/
│   │   │   │   ├── Color.kt             # Paleta deportiva Kynectia (Obsidian, Lime, Orange)
│   │   │   │   └── Theme.kt             # Tipografía y tema Material3 TV
│   │   │   └── ui/
│   │   │       ├── TvViewModel.kt       # Máquina de estados (Loading, Pairing, Live, Summary)
│   │   │       ├── pairing/
│   │   │       │   └── TvPairingScreen.kt # Pantalla de espera con PIN de 6 dígitos y QR
│   │   │       ├── workout/
│   │   │       │   └── TvLiveWorkoutScreen.kt # Pantalla de entrenamiento y timer circular
│   │   │       └── summary/
│   │   │           └── TvSummaryScreen.kt # Pantalla de resumen y estadísticas
│   │   └── res/                         # Iconos, vectores y recursos de la app
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 📲 Instalación y Ejecución

### 1. Compilar el APK de TV
```bash
cd android-tv
./gradlew assembleDebug
```
El archivo compilado se genera en:
`android-tv/app/build/outputs/apk/debug/app-debug.apk`

### 2. Instalar en tu Android TV o Emulador TV
Conecta tu televisor mediante depuración inalámbrica ADB o cable:
```bash
adb connect <IP_DE_TU_TV>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Lanzar la App en TV
```bash
adb shell am start -n com.kynectia.tv/.MainActivity
```
