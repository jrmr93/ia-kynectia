import { Router } from 'express';
import { z } from 'zod';
import { ExercisesController } from './exercises.controller';
import { requireAuth, requirePermission } from '../../middleware/auth';
import { validateRequest } from '../../middleware/validate';

const router = Router();

const createExerciseSchema = z.object({
  name: z.string().min(2, 'El nombre debe tener al menos 2 caracteres'),
  description: z.string().optional(),
  executionInstructions: z.string().optional(),
  difficultyLevel: z.enum(['beginner', 'intermediate', 'advanced']),
  exerciseType: z.enum(['strength', 'cardio', 'flexibility', 'hiit', 'plyometric']),
  demoImageUrl: z.string().url('URL inválida').optional().or(z.literal('')),
  demoVideoUrl: z.string().url('URL inválida').optional().or(z.literal('')),
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

router.use(requireAuth);

router.get('/', ExercisesController.list);
router.get('/:id', ExercisesController.getById);
router.post('/', requirePermission('exercises:manage'), validateRequest({ body: createExerciseSchema }), ExercisesController.create);
router.put('/:id', requirePermission('exercises:manage'), validateRequest({ body: updateExerciseSchema }), ExercisesController.update);
router.patch('/:id/toggle-active', requirePermission('exercises:manage'), validateRequest({ body: toggleActiveSchema }), ExercisesController.toggleActive);
router.delete('/:id', requirePermission('exercises:manage'), ExercisesController.delete);

export default router;
