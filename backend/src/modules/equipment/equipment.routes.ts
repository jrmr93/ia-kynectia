import { Router } from 'express';
import { z } from 'zod';
import { EquipmentController } from './equipment.controller';
import { requireAuth, requirePermission } from '../../middleware/auth';
import { validateRequest } from '../../middleware/validate';

const router = Router();

const equipmentSchema = z.object({
  name: z.string().min(2, 'El nombre debe tener al menos 2 caracteres'),
  description: z.string().optional(),
  category: z.enum(['free_weights', 'machines', 'bodyweight', 'accessories', 'resistance']),
  imageUrl: z.string().url('Debe ser una URL válida').optional().or(z.literal('')),
  isActive: z.boolean().optional(),
});

const toggleActiveSchema = z.object({
  isActive: z.boolean(),
});

router.use(requireAuth);

router.get('/', EquipmentController.list);
router.get('/:id', EquipmentController.getById);
router.post('/', requirePermission('equipment:manage'), validateRequest({ body: equipmentSchema }), EquipmentController.create);
router.put('/:id', requirePermission('equipment:manage'), validateRequest({ body: equipmentSchema.partial() }), EquipmentController.update);
router.patch('/:id/toggle-active', requirePermission('equipment:manage'), validateRequest({ body: toggleActiveSchema }), EquipmentController.toggleActive);
router.delete('/:id', requirePermission('equipment:manage'), EquipmentController.delete);

export default router;
