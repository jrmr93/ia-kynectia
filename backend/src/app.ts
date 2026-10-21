import express, { Application, Request, Response } from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import path from 'path';
import rateLimit from 'express-rate-limit';

import { env } from './config/env';
import { errorHandler } from './middleware/errorHandler';
import { sendError, sendSuccess } from './utils/response';

import authRoutes from './modules/auth/auth.routes';
import dashboardRoutes from './modules/dashboard/dashboard.routes';
import usersRoutes from './modules/users/users.routes';
import musclesRoutes from './modules/muscles/muscles.routes';
import equipmentRoutes from './modules/equipment/equipment.routes';
import exercisesRoutes from './modules/exercises/exercises.routes';
import workoutsRoutes from './modules/workouts/workouts.routes';
import plansRoutes from './modules/plans/plans.routes';
import adsRoutes from './modules/ads/ads.routes';
import mediaRoutes from './modules/media/media.routes';
import auditRoutes from './modules/audit/audit.routes';

export const createApp = (): Application => {
  const app = express();

  // 1. Seguridad HTTP y CORS
  app.use(
    helmet({
      crossOriginResourcePolicy: { policy: 'cross-origin' },
    })
  );

  app.use(
    cors({
      origin: [env.CLIENT_URL, 'http://localhost:5173', 'http://127.0.0.1:5173', 'http://localhost:3000'],
      credentials: true,
      methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
      allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With'],
    })
  );

  // 2. Rate Limiting general para la API
  const limiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 1000,
    standardHeaders: true,
    legacyHeaders: false,
    message: { success: false, message: 'Demasiadas solicitudes desde esta IP, por favor intenta más tarde.' },
  });
  app.use(limiter);

  // 3. Middlewares de parseo y logging
  app.use(express.json({ limit: '10mb' }));
  app.use(express.urlencoded({ extended: true, limit: '10mb' }));

  if (env.NODE_ENV !== 'test') {
    app.use(morgan('dev'));
  }

  // 4. Servir archivos subidos estáticos
  const uploadsPath = path.resolve(__dirname, '../uploads');
  app.use('/uploads', express.static(uploadsPath));

  // 5. Healthcheck
  app.get('/api/health', (_req: Request, res: Response) => {
    sendSuccess(res, { status: 'healthy', timestamp: new Date().toISOString() }, 'Kynectia Admin API activa.');
  });

  // 6. Rutas de la API v1
  const apiV1 = express.Router();
  apiV1.use('/auth', authRoutes);
  apiV1.use('/dashboard', dashboardRoutes);
  apiV1.use('/users', usersRoutes);
  apiV1.use('/muscles', musclesRoutes);
  apiV1.use('/equipment', equipmentRoutes);
  apiV1.use('/exercises', exercisesRoutes);
  apiV1.use('/workouts', workoutsRoutes);
  apiV1.use('/plans', plansRoutes);
  apiV1.use('/ads', adsRoutes);
  apiV1.use('/media', mediaRoutes);
  apiV1.use('/audit', auditRoutes);

  app.use('/api/v1', apiV1);

  // 7. Ruta 404
  app.use((req: Request, res: Response) => {
    sendError(res, `Recurso no encontrado: ${req.method} ${req.originalUrl}`, 404);
  });

  // 8. Manejador de errores
  app.use(errorHandler);

  return app;
};
