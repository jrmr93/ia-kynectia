import { Request, Response, NextFunction } from 'express';
import { PlansService } from './plans.service';
import { sendSuccess, parseIdParam } from '../../utils/response';

export class PlansController {
  static async list(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { isActive } = req.query;
      const plans = await PlansService.list(isActive !== undefined ? isActive === 'true' : undefined);
      sendSuccess(res, plans, 'Planes comerciales obtenidos.');
    } catch (error) {
      next(error);
    }
  }

  static async getById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const plan = await PlansService.getById(id);
      sendSuccess(res, plan, 'Detalle del plan obtenido.');
    } catch (error) {
      next(error);
    }
  }

  static async create(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const plan = await PlansService.create(req.body, req.admin?.id, req.admin?.email);
      sendSuccess(res, plan, 'Plan comercial creado con éxito.', 201);
    } catch (error) {
      next(error);
    }
  }

  static async update(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const plan = await PlansService.update(id, req.body, req.admin?.id, req.admin?.email);
      sendSuccess(res, plan, 'Plan actualizado.');
    } catch (error) {
      next(error);
    }
  }

  static async toggleActive(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const { isActive } = req.body;
      const plan = await PlansService.toggleActive(id, isActive, req.admin?.id, req.admin?.email);
      sendSuccess(res, plan, `Plan ${isActive ? 'activado' : 'desactivado'}.`);
    } catch (error) {
      next(error);
    }
  }

  static async delete(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const result = await PlansService.delete(id, req.admin?.id, req.admin?.email);
      sendSuccess(res, result, result.message);
    } catch (error) {
      next(error);
    }
  }

  static async listSubscriptions(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { status, page, limit } = req.query;
      const result = await PlansService.listSubscriptions(
        status as string,
        page ? parseInt(page as string, 10) : 1,
        limit ? parseInt(limit as string, 10) : 10
      );
      sendSuccess(res, result.subscriptions, 'Suscripciones obtenidas.', 200, result.pagination);
    } catch (error) {
      next(error);
    }
  }
}
