import { Router } from 'express';
import { z } from 'zod';
import multer from 'multer';
import path from 'path';
import fs from 'fs';
import { ExercisesController } from './exercises.controller';
import { requireAuth, requirePermission } from '../../middleware/auth';
import { validateRequest } from '../../middleware/validate';

const router = Router();

// Directorio de almacenamiento seguro para videos de ejercicios
const videosDirectory = path.resolve(__dirname, '../../../uploads/videos');
if (!fs.existsSync(videosDirectory)) {
  fs.mkdirSync(videosDirectory, { recursive: true });
}

const videoStorage = multer.diskStorage({
  destination: (_req, _file, cb) => {
    cb(null, videosDirectory);
  },
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    const sanitizedBase = path
      .basename(file.originalname, ext)
      .replace(/[^a-zA-Z0-9_-]/g, '_')
      .toLowerCase();
    const uniqueSuffix = `${Date.now()}-${Math.round(Math.random() * 1e9)}`;
    cb(null, `exercise-${sanitizedBase}-${uniqueSuffix}${ext}`);
  },
});

const uploadVideo = multer({
  storage: videoStorage,
  limits: { fileSize: 100 * 1024 * 1024 }, // 100 MB máximo por video
  fileFilter: (_req, file, cb) => {
    const allowed = ['video/mp4', 'video/webm', 'video/quicktime', 'video/x-matroska', 'video/ogg'];
    if (allowed.includes(file.mimetype) || file.mimetype.startsWith('video/')) {
      cb(null, true);
    } else {
      cb(new Error(`Tipo de archivo no permitido. Solo se aceptan videos (mp4, webm, mov, etc.). Tipo recibido: ${file.mimetype}`));
    }
  },
});

const createExerciseSchema = z.object({
  name: z.string().min(2, 'El nombre debe tener al menos 2 caracteres'),
  description: z.string().optional(),
  executionInstructions: z.string().optional(),
  difficultyLevel: z.enum(['beginner', 'intermediate', 'advanced']),
  exerciseType: z.enum(['strength', 'cardio', 'flexibility', 'hiit', 'plyometric']),
  demoImageUrl: z.string().optional().or(z.literal('')),
  demoVideoUrl: z.string().optional().or(z.literal('')),
  safetyRecommendations: z.string().optional(),
  publicationStatus: z.enum(['draft', 'published', 'archived']).optional(),
  isActive: z.boolean().optional(),
  primaryMuscleGroupId: z.number().int().positive('El grupo muscular primario es obligatorio'),
  secondaryMuscleGroupIds: z.array(z.number().int().positive()).optional(),
  equipmentIds: z.array(z.number().int().positive()).optional(),
});

const updateExerciseSchema = createExerciseSchema.partial();

const toggleActiveSchema = z.object({
  isActive: z.boolean(),
});

router.get('/', ExercisesController.list);
router.get('/:id', ExercisesController.getById);

// Gestión y carga de videos
router.post('/upload-video', requireAuth, requirePermission('exercises:manage'), uploadVideo.single('video'), ExercisesController.uploadVideo);
router.delete('/:id/video', requireAuth, requirePermission('exercises:manage'), ExercisesController.deleteExerciseVideo);
router.post('/delete-video-file', requireAuth, requirePermission('exercises:manage'), ExercisesController.deleteVideoFile);

router.post('/', requireAuth, requirePermission('exercises:manage'), validateRequest({ body: createExerciseSchema }), ExercisesController.create);
router.put('/:id', requireAuth, requirePermission('exercises:manage'), validateRequest({ body: updateExerciseSchema }), ExercisesController.update);
router.patch('/:id/toggle-active', requireAuth, requirePermission('exercises:manage'), validateRequest({ body: toggleActiveSchema }), ExercisesController.toggleActive);
router.delete('/:id', requireAuth, requirePermission('exercises:manage'), ExercisesController.delete);

export default router;
