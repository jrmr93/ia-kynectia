import { Router } from 'express';
import { z } from 'zod';
import { WorkoutsController } from './workouts.controller';
import { requireAuth, requirePermission } from '../../middleware/auth';
import { validateRequest } from '../../middleware/validate';

const router = Router();

const routineExerciseItemSchema = z.object({
  exerciseId: z.number().int().positive(),
  sequenceOrder: z.number().int().positive(),
  sets: z.number().int().positive(),
  reps: z.number().int().positive(),
  durationSeconds: z.number().int().nonnegative().optional(),
  restSeconds: z.number().int().nonnegative(),
  recommendedWeightKg: z.number().nonnegative().optional(),
  specificInstructions: z.string().optional(),
});

const createWorkoutSchema = z.object({
  name: z.string().min(2, 'El nombre debe tener al menos 2 caracteres'),
  description: z.string().optional(),
  coverImageUrl: z.string().url('URL inválida').optional().or(z.literal('')),
  goal: z.string().min(2, 'El objetivo es obligatorio'),
  difficultyLevel: z.enum(['beginner', 'intermediate', 'advanced']),
  category: z.string().min(2, 'La categoría es obligatoria'),
  estimatedDurationMinutes: z.number().int().positive(),
  publicationStatus: z.enum(['draft', 'published', 'archived']).optional(),
  isActive: z.boolean().optional(),
  exercises: z.array(routineExerciseItemSchema).default([]),
});

const updateWorkoutSchema = createWorkoutSchema.partial();

const statusSchema = z.object({
  status: z.enum(['draft', 'published', 'archived']),
});

router.get('/', WorkoutsController.list);
router.get('/:id', WorkoutsController.getById);
router.post('/', requireAuth, requirePermission('routines:manage'), validateRequest({ body: createWorkoutSchema }), WorkoutsController.create);
router.put('/:id', requireAuth, requirePermission('routines:manage'), validateRequest({ body: updateWorkoutSchema }), WorkoutsController.update);
router.patch('/:id/status', requireAuth, requirePermission('routines:manage'), validateRequest({ body: statusSchema }), WorkoutsController.setStatus);
router.delete('/:id', requireAuth, requirePermission('routines:manage'), WorkoutsController.delete);

export default router;
