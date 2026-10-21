import { Router } from 'express';
import { z } from 'zod';
import { AdsController } from './ads.controller';
import { requireAuth, requirePermission } from '../../middleware/auth';
import { validateRequest } from '../../middleware/validate';

const router = Router();

const adSchema = z.object({
  title: z.string().min(2, 'El título debe tener al menos 2 caracteres'),
  description: z.string().optional(),
  mediaUrl: z.string().url('Debe ser una URL de imagen válida'),
  destinationUrl: z.string().url('Debe ser una URL de destino válida').optional().or(z.literal('')),
  placement: z.enum(['banner_home', 'interstitial_workout', 'sidebar', 'footer']),
  priority: z.number().int().min(1).default(1),
  startsAt: z.string(),
  endsAt: z.string(),
  isActive: z.boolean().optional(),
});

const toggleActiveSchema = z.object({
  isActive: z.boolean(),
});

router.use(requireAuth);

router.get('/', AdsController.list);
router.get('/:id', AdsController.getById);
router.post('/', requirePermission('ads:manage'), validateRequest({ body: adSchema }), AdsController.create);
router.put('/:id', requirePermission('ads:manage'), validateRequest({ body: adSchema.partial() }), AdsController.update);
router.patch('/:id/toggle-active', requirePermission('ads:manage'), validateRequest({ body: toggleActiveSchema }), AdsController.toggleActive);
router.delete('/:id', requirePermission('ads:manage'), AdsController.delete);

export default router;
