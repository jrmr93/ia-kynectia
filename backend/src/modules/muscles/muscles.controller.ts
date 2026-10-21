import { Request, Response, NextFunction } from 'express';
import { MusclesService } from './muscles.service';
import { sendSuccess, parseIdParam } from '../../utils/response';

export class MusclesController {
  static async list(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { search, isActive } = req.query;
      const muscles = await MusclesService.list(
        search as string,
        isActive !== undefined ? isActive === 'true' : undefined
      );
      sendSuccess(res, muscles, 'Grupos musculares obtenidos correctamente.');
    } catch (error) {
      next(error);
    }
  }

  static async getById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const muscle = await MusclesService.getById(id);
      sendSuccess(res, muscle, 'Detalle de grupo muscular obtenido.');
    } catch (error) {
      next(error);
    }
  }

  static async create(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const muscle = await MusclesService.create(req.body, req.admin?.id, req.admin?.email);
      sendSuccess(res, muscle, 'Grupo muscular creado exitosamente.', 201);
    } catch (error) {
      next(error);
    }
  }

  static async update(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const muscle = await MusclesService.update(id, req.body, req.admin?.id, req.admin?.email);
      sendSuccess(res, muscle, 'Grupo muscular actualizado.');
    } catch (error) {
      next(error);
    }
  }

  static async toggleActive(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const { isActive } = req.body;
      const muscle = await MusclesService.toggleActive(id, isActive, req.admin?.id, req.admin?.email);
      sendSuccess(res, muscle, `Grupo muscular ${isActive ? 'activado' : 'desactivado'}.`);
    } catch (error) {
      next(error);
    }
  }

  static async delete(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const result = await MusclesService.delete(id, req.admin?.id, req.admin?.email);
      sendSuccess(res, result, result.message);
    } catch (error) {
      next(error);
    }
  }
}
