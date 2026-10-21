import { Router } from 'express';
import { z } from 'zod';
import { PlansController } from './plans.controller';
import { requireAuth, requirePermission } from '../../middleware/auth';
import { validateRequest } from '../../middleware/validate';

const router = Router();

const planSchema = z.object({
  name: z.string().min(2, 'El nombre debe tener al menos 2 caracteres'),
  description: z.string().optional(),
  billingCycle: z.enum(['monthly', 'quarterly', 'biannual', 'annual', 'free']),
  price: z.number().min(0, 'El precio no puede ser negativo'),
  currency: z.string().length(3).optional(),
  benefits: z.array(z.string()).optional(),
  isActive: z.boolean().optional(),
  discountPercent: z.number().min(0).max(100).optional(),
  promoEndDate: z.string().optional(),
});

const toggleActiveSchema = z.object({
  isActive: z.boolean(),
});

router.use(requireAuth);

router.get('/subscriptions', PlansController.listSubscriptions);
router.get('/', PlansController.list);
router.get('/:id', PlansController.getById);
router.post('/', requirePermission('plans:manage'), validateRequest({ body: planSchema }), PlansController.create);
router.put('/:id', requirePermission('plans:manage'), validateRequest({ body: planSchema.partial() }), PlansController.update);
router.patch('/:id/toggle-active', requirePermission('plans:manage'), validateRequest({ body: toggleActiveSchema }), PlansController.toggleActive);
router.delete('/:id', requirePermission('plans:manage'), PlansController.delete);

export default router;
