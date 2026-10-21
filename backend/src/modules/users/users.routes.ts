import { Router } from 'express';
import { z } from 'zod';
import { UsersController } from './users.controller';
import { requireAuth, requirePermission } from '../../middleware/auth';
import { validateRequest } from '../../middleware/validate';

const router = Router();

const createUserSchema = z.object({
  fullName: z.string().min(2, 'El nombre debe tener al menos 2 caracteres'),
  email: z.string().email('Debe ser un correo electrónico válido'),
  phone: z.string().optional(),
  birthDate: z.string().optional(),
  gender: z.enum(['male', 'female', 'other', 'unspecified']).optional(),
  heightCm: z.number().positive().optional(),
  weightKg: z.number().positive().optional(),
  fitnessGoal: z.string().optional(),
  isActive: z.boolean().optional(),
  emailVerified: z.boolean().optional(),
});

const updateUserSchema = createUserSchema.partial();

const toggleActiveSchema = z.object({
  isActive: z.boolean(),
});

router.use(requireAuth);

router.get('/', UsersController.list);
router.get('/:id', UsersController.getById);
router.post('/', requirePermission('users:manage'), validateRequest({ body: createUserSchema }), UsersController.create);
router.put('/:id', requirePermission('users:manage'), validateRequest({ body: updateUserSchema }), UsersController.update);
router.patch('/:id/toggle-active', requirePermission('users:manage'), validateRequest({ body: toggleActiveSchema }), UsersController.toggleActive);
router.delete('/:id', requirePermission('users:manage'), UsersController.delete);

export default router;
