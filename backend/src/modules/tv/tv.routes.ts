import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { validateRequest } from '../../middleware/validate';
import { sendSuccess, sendError } from '../../utils/response';

interface TvWorkoutState {
  routineName: string;
  exerciseName: string;
  demoImageUrl?: string;
  demoVideoUrl?: string;
  currentExerciseIndex: number;
  totalExercises: number;
  currentSetNumber: number;
  totalSets: number;
  reps: number;
  weightKg: number;
  isResting: boolean;
  restSecondsRemaining: number;
  restSecondsTotal: number;
  elapsedWorkoutSeconds: number;
  isPaused: boolean;
  isFinished: boolean;
  summary?: {
    totalDurationSeconds: number;
    totalSetsCompleted: number;
    totalVolumeKg: number;
  };
  lastUpdated: number;
}

export interface TvRemoteCommand {
  id: string;
  command: 'TOGGLE_PAUSE' | 'SKIP_REST' | 'NEXT_EXERCISE' | 'PREV_EXERCISE';
  timestamp: number;
}

interface TvSession {
  sessionId: string;
  pinCode: string;
  qrPayload: string;
  createdAt: number;
  expiresAt: number;
  isPaired: boolean;
  pairedAthleteName?: string;
  pairedAthleteEmail?: string;
  currentState?: TvWorkoutState;
  pendingCommands?: TvRemoteCommand[];
  rotateCount?: number;
  orientation?: string;
}


import fs from 'fs';
import path from 'path';

// Almacén de sesiones de TV activas
const tvSessions = new Map<string, TvSession>();
const pinIndex = new Map<string, string>(); // pinCode -> sessionId
const CACHE_FILE = path.join(process.cwd(), 'uploads', 'tv_sessions_cache.json');

function saveSessionsToDisk() {
  try {
    const list = Array.from(tvSessions.values());
    fs.writeFileSync(CACHE_FILE, JSON.stringify(list, null, 2), 'utf-8');
  } catch (err) {
    console.error('Error guardando caché de sesiones TV:', err);
  }
}

function loadSessionsFromDisk() {
  try {
    if (fs.existsSync(CACHE_FILE)) {
      const data = fs.readFileSync(CACHE_FILE, 'utf-8');
      const list = JSON.parse(data) as TvSession[];
      const now = Date.now();
      for (const s of list) {
        if (s && s.sessionId && s.expiresAt > now) {
          tvSessions.set(s.sessionId, s);
          if (s.pinCode) {
            pinIndex.set(s.pinCode, s.sessionId);
          }
        }
      }
    }
  } catch (err) {
    console.error('Error cargando caché de sesiones TV:', err);
  }
}

// Cargar sesiones persistentes al iniciar el servidor
loadSessionsFromDisk();

// Limpieza periódica de sesiones realmente expiradas (vigencia extendida de 30 días para recordar conexión)
const cleanupTimer = setInterval(() => {
  const now = Date.now();
  let changed = false;
  for (const [id, session] of tvSessions.entries()) {
    if (session.expiresAt < now) {
      pinIndex.delete(session.pinCode);
      tvSessions.delete(id);
      changed = true;
    }
  }
  if (changed) {
    saveSessionsToDisk();
  }
}, 30 * 60 * 1000);
if (cleanupTimer.unref) {
  cleanupTimer.unref();
}

export class TvService {
  static createSession(): TvSession {
    // Generar PIN aleatorio de 6 dígitos no repetido
    let pinCode: string;
    do {
      pinCode = Math.floor(100000 + Math.random() * 900000).toString();
    } while (pinIndex.has(pinCode));

    const sessionId = `tv_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
    const now = Date.now();
    // 30 días de vigencia para recordar la conexión hasta desconexión manual
    const expiresAt = now + 30 * 24 * 60 * 60 * 1000;

    const qrPayload = JSON.stringify({
      app: 'kynectia',
      type: 'tv_pairing',
      sessionId,
      pinCode,
    });

    const session: TvSession = {
      sessionId,
      pinCode,
      qrPayload,
      createdAt: now,
      expiresAt,
      isPaired: false,
    };

    tvSessions.set(sessionId, session);
    pinIndex.set(pinCode, sessionId);
    saveSessionsToDisk();

    return session;
  }

  static pairWithPin(pinCode: string, athleteName?: string, athleteEmail?: string): TvSession | null {
    const sessionId = pinIndex.get(pinCode.trim());
    if (!sessionId) return null;

    const session = tvSessions.get(sessionId);
    if (!session || session.expiresAt < Date.now()) return null;

    session.isPaired = true;
    session.pairedAthleteName = athleteName || 'Atleta Kynectia';
    session.pairedAthleteEmail = athleteEmail;
    session.expiresAt = Date.now() + 30 * 24 * 60 * 60 * 1000;
    saveSessionsToDisk();

    return session;
  }

  static pairWithSessionId(sessionId: string, athleteName?: string, athleteEmail?: string): TvSession | null {
    const session = tvSessions.get(sessionId);
    if (!session || session.expiresAt < Date.now()) return null;

    session.isPaired = true;
    session.pairedAthleteName = athleteName || 'Atleta Kynectia';
    session.pairedAthleteEmail = athleteEmail;
    session.expiresAt = Date.now() + 30 * 24 * 60 * 60 * 1000;
    saveSessionsToDisk();

    return session;
  }

  static updateState(sessionId: string, state: Omit<TvWorkoutState, 'lastUpdated'>): boolean {
    const session = tvSessions.get(sessionId);
    if (!session) return false;

    session.currentState = {
      ...state,
      lastUpdated: Date.now(),
    };
    session.expiresAt = Date.now() + 30 * 24 * 60 * 60 * 1000;
    saveSessionsToDisk();
    return true;
  }

  static getSession(sessionId: string): TvSession | null {
    const session = tvSessions.get(sessionId);
    if (!session || session.expiresAt < Date.now()) return null;
    // Renovar vigencia en cada lectura activa para mantener la sesión viva
    session.expiresAt = Date.now() + 30 * 24 * 60 * 60 * 1000;
    return session;
  }

  static disconnect(sessionId: string): boolean {
    const session = tvSessions.get(sessionId);
    if (session) {
      pinIndex.delete(session.pinCode);
      tvSessions.delete(sessionId);
      saveSessionsToDisk();
      return true;
    }
    return false;
  }

  static pushCommand(sessionId: string, command: 'TOGGLE_PAUSE' | 'SKIP_REST' | 'NEXT_EXERCISE' | 'PREV_EXERCISE'): boolean {
    const session = tvSessions.get(sessionId);
    if (!session) return false;

    if (!session.pendingCommands) session.pendingCommands = [];
    session.pendingCommands.push({
      id: `${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
      command,
      timestamp: Date.now()
    });

    if (session.currentState) {
      if (command === 'TOGGLE_PAUSE') {
        session.currentState.isPaused = !session.currentState.isPaused;
      } else if (command === 'SKIP_REST') {
        session.currentState.isResting = false;
        session.currentState.restSecondsRemaining = 0;
      }
      session.currentState.lastUpdated = Date.now();
    }
    return true;
  }

  static popPendingCommands(sessionId: string): TvRemoteCommand[] {
    const session = tvSessions.get(sessionId);
    if (!session || !session.pendingCommands) return [];
    const commands = [...session.pendingCommands];
    session.pendingCommands = [];
    return commands;
  }

  static rotateScreen(sessionId: string, orientation?: string): { rotateCount: number; orientation?: string } | null {
    const session = tvSessions.get(sessionId);
    if (!session || session.expiresAt < Date.now()) return null;

    session.rotateCount = (session.rotateCount || 0) + 1;
    if (orientation) {
      session.orientation = orientation;
    }
    saveSessionsToDisk();
    return {
      rotateCount: session.rotateCount,
      orientation: session.orientation,
    };
  }
}


// Router
const router = Router();

// 1. Iniciar sesión de TV (Genera PIN y QR para mostrar en pantalla grande)
router.post('/session', (_req: Request, res: Response) => {
  const session = TvService.createSession();
  sendSuccess(res, {
    sessionId: session.sessionId,
    pinCode: session.pinCode,
    qrPayload: session.qrPayload,
    expiresAt: session.expiresAt,
  }, 'Sesión de Android TV creada exitosamente.', 201);
});

// 2. Vincular desde la app móvil (vía código PIN o QR)
const pairSchema = z.object({
  pinCode: z.string().min(4).max(10).optional(),
  sessionId: z.string().optional(),
  athleteName: z.string().optional(),
  athleteEmail: z.string().optional(),
}).refine(data => data.pinCode || data.sessionId, {
  message: 'Debes proporcionar pinCode o sessionId para vincular.',
});

router.post('/pair', validateRequest({ body: pairSchema as any }), (req: Request, res: Response) => {
  const { pinCode, sessionId, athleteName, athleteEmail } = req.body;
  let session: TvSession | null = null;

  if (pinCode) {
    session = TvService.pairWithPin(pinCode, athleteName, athleteEmail);
  } else if (sessionId) {
    session = TvService.pairWithSessionId(sessionId, athleteName, athleteEmail);
  }

  if (!session) {
    sendError(res, 'Código PIN o sesión inválida o expirada.', 404);
    return;
  }

  sendSuccess(res, {
    sessionId: session.sessionId,
    pinCode: session.pinCode,
    isPaired: session.isPaired,
    pairedAthleteName: session.pairedAthleteName,
  }, 'Vinculación exitosa con la pantalla Android TV.');
});

// 3. Emitir estado del entrenamiento desde el móvil a la TV
const updateStateSchema = z.object({
  sessionId: z.string().min(1),
  routineName: z.string(),
  exerciseName: z.string(),
  demoImageUrl: z.string().nullable().optional(),
  demoVideoUrl: z.string().nullable().optional(),
  currentExerciseIndex: z.number().int(),
  totalExercises: z.number().int(),
  currentSetNumber: z.number().int(),
  totalSets: z.number().int(),
  reps: z.number().int(),
  weightKg: z.number(),
  isResting: z.boolean(),
  restSecondsRemaining: z.number().int(),
  restSecondsTotal: z.number().int(),
  elapsedWorkoutSeconds: z.number().int(),
  isPaused: z.boolean(),
  isFinished: z.boolean(),
  summary: z.object({
    totalDurationSeconds: z.number().int(),
    totalSetsCompleted: z.number().int(),
    totalVolumeKg: z.number(),
  }).nullable().optional(),
});

router.post('/state', validateRequest({ body: updateStateSchema }), (req: Request, res: Response) => {
  const { sessionId, ...state } = req.body;
  const ok = TvService.updateState(sessionId, state);
  if (!ok) {
    sendError(res, 'Sesión de TV no encontrada o inactiva.', 404);
    return;
  }
  sendSuccess(res, { updated: true }, 'Estado de entrenamiento actualizado en TV.');
});

// 4. Obtener estado en tiempo real (consultado periódicamente por la app TV y móvil)
router.get('/session/:sessionId', (req: Request, res: Response) => {
  const session = TvService.getSession(req.params.sessionId as string);
  if (!session) {
    sendError(res, 'Sesión de TV no encontrada o expirada.', 404);
    return;
  }

  sendSuccess(res, {
    sessionId: session.sessionId,
    pinCode: session.pinCode,
    isPaired: session.isPaired,
    pairedAthleteName: session.pairedAthleteName,
    state: session.currentState || null,
    pendingCommands: session.pendingCommands || [],
    rotateCount: session.rotateCount || 0,
    orientation: session.orientation || null,
  }, 'Estado actual de la sesión de TV obtenido.');
});

// 5. Enviar comando desde el control remoto de Android TV
const commandSchema = z.object({
  sessionId: z.string().min(1),
  command: z.enum(['TOGGLE_PAUSE', 'SKIP_REST', 'NEXT_EXERCISE', 'PREV_EXERCISE']),
});

router.post('/command', validateRequest({ body: commandSchema }), (req: Request, res: Response) => {
  const { sessionId, command } = req.body;
  const ok = TvService.pushCommand(sessionId, command);
  if (!ok) {
    sendError(res, 'Sesión de TV no encontrada o inactiva.', 404);
    return;
  }
  sendSuccess(res, { command, success: true }, 'Comando del control remoto procesado.');
});

// 6. Consumir comandos pendientes desde el móvil
router.post('/commands/consume/:sessionId', (req: Request, res: Response) => {
  const commands = TvService.popPendingCommands(req.params.sessionId as string);
  sendSuccess(res, { commands }, 'Comandos consumidos.');
});

// 7. Girar pantalla de la TV desde el móvil
router.post('/rotate/:sessionId', (req: Request, res: Response) => {
  const sessionId = req.params.sessionId as string;
  const orientation = req.body?.orientation;
  const result = TvService.rotateScreen(sessionId, orientation);
  if (!result) {
    sendError(res, 'Sesión de TV no encontrada o inactiva.', 404);
    return;
  }
  sendSuccess(res, result, 'Solicitud de giro de pantalla enviada a la TV.');
});

// 8. Desconectar sesión
router.post('/disconnect/:sessionId', (req: Request, res: Response) => {
  TvService.disconnect(req.params.sessionId as string);
  sendSuccess(res, { disconnected: true }, 'Sesión de TV finalizada.');
});

export default router;

