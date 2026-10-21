import { Router } from 'express';
import { z } from 'zod';
import { MusclesController } from './muscles.controller';
import { requireAuth, requirePermission } from '../../middleware/auth';
import { validateRequest } from '../../middleware/validate';

const router = Router();

const muscleSchema = z.object({
  name: z.string().min(2, 'El nombre debe tener al menos 2 caracteres'),
  description: z.string().optional(),
  imageUrl: z.string().url('Debe ser una URL válida').optional().or(z.literal('')),
  isActive: z.boolean().optional(),
});

const toggleActiveSchema = z.object({
  isActive: z.boolean(),
});

router.use(requireAuth);

router.get('/', MusclesController.list);
router.get('/:id', MusclesController.getById);
router.post('/', requirePermission('muscles:manage'), validateRequest({ body: muscleSchema }), MusclesController.create);
router.put('/:id', requirePermission('muscles:manage'), validateRequest({ body: muscleSchema.partial() }), MusclesController.update);
router.patch('/:id/toggle-active', requirePermission('muscles:manage'), validateRequest({ body: toggleActiveSchema }), MusclesController.toggleActive);
router.delete('/:id', requirePermission('muscles:manage'), MusclesController.delete);

export default router;
