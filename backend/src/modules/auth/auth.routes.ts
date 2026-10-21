import { Router } from 'express';
import { z } from 'zod';
import { AuthController } from './auth.controller';
import { requireAuth } from '../../middleware/auth';
import { validateRequest } from '../../middleware/validate';

const router = Router();

const loginSchema = z.object({
  email: z.string().email('Debe ser un correo electrónico válido'),
  password: z.string().min(6, 'La contraseña debe tener al menos 6 caracteres'),
});

const changePasswordSchema = z.object({
  oldPassword: z.string().min(1, 'La contraseña anterior es obligatoria'),
  newPassword: z.string().min(8, 'La nueva contraseña debe tener al menos 8 caracteres'),
});

router.post('/login', validateRequest({ body: loginSchema }), AuthController.login);
router.get('/me', requireAuth, AuthController.me);
router.post('/change-password', requireAuth, validateRequest({ body: changePasswordSchema }), AuthController.changePassword);
router.post('/logout', requireAuth, AuthController.logout);

export default router;
