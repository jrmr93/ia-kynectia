import { Request, Response, NextFunction } from 'express';
import { AuthService } from './auth.service';
import { sendSuccess } from '../../utils/response';
import { recordAuditLog } from '../../utils/logger';

export class AuthController {
  static async login(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { email, password } = req.body;
      const ipAddress = req.ip || req.socket.remoteAddress;
      const userAgent = req.headers['user-agent'];

      const result = await AuthService.login(email, password, ipAddress, userAgent);
      sendSuccess(res, result, 'Inicio de sesión exitoso.');
    } catch (error) {
      next(error);
    }
  }

  static async me(req: Request, res: Response): Promise<void> {
    sendSuccess(res, req.admin, 'Perfil administrativo obtenido.');
  }

  static async changePassword(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { oldPassword, newPassword } = req.body;
      const result = await AuthService.changePassword(req.admin!.id, oldPassword, newPassword);
      sendSuccess(res, result, 'Contraseña cambiada exitosamente.');
    } catch (error) {
      next(error);
    }
  }

  static async logout(req: Request, res: Response): Promise<void> {
    if (req.admin) {
      await recordAuditLog({
        adminId: req.admin.id,
        adminEmail: req.admin.email,
        action: 'LOGOUT',
        entityType: 'admin_users',
        entityId: req.admin.id,
        ipAddress: req.ip || req.socket.remoteAddress,
        userAgent: req.headers['user-agent'],
      });
    }
    sendSuccess(res, null, 'Sesión finalizada correctamente.');
  }
}
