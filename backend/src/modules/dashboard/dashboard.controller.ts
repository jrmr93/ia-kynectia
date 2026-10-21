import { Request, Response, NextFunction } from 'express';
import { DashboardService } from './dashboard.service';
import { sendSuccess } from '../../utils/response';

export class DashboardController {
  static async getStats(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const stats = await DashboardService.getSummaryStats();
      sendSuccess(res, stats, 'Métricas reales del panel administrativo obtenidas.');
    } catch (error) {
      next(error);
    }
  }
}
