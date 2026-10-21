import { Request, Response, NextFunction } from 'express';
import { AuditService } from './audit.service';
import { sendSuccess } from '../../utils/response';

export class AuditController {
  static async list(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { action, entityType, adminId, page, limit } = req.query;
      const result = await AuditService.list({
        action: action as string,
        entityType: entityType as string,
        adminId: adminId ? parseInt(adminId as string, 10) : undefined,
        page: page ? parseInt(page as string, 10) : 1,
        limit: limit ? parseInt(limit as string, 10) : 20,
      });
      sendSuccess(res, result.logs, 'Historial de auditoría obtenido.', 200, result.pagination);
    } catch (error) {
      next(error);
    }
  }
}
