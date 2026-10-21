import { Request, Response, NextFunction } from 'express';
import { WorkoutsService } from './workouts.service';
import { sendSuccess, parseIdParam } from '../../utils/response';

export class WorkoutsController {
  static async list(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { search, difficultyLevel, category, publicationStatus, goal, isActive, page, limit } = req.query;
      const result = await WorkoutsService.list({
        search: search as string,
        difficultyLevel: difficultyLevel as string,
        category: category as string,
        publicationStatus: publicationStatus as string,
        goal: goal as string,
        isActive: isActive !== undefined ? isActive === 'true' : undefined,
        page: page ? parseInt(page as string, 10) : 1,
        limit: limit ? parseInt(limit as string, 10) : 10,
      });
      sendSuccess(res, result.routines, 'Rutinas de entrenamiento obtenidas.', 200, result.pagination);
    } catch (error) {
      next(error);
    }
  }

  static async getById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const routine = await WorkoutsService.getById(id);
      sendSuccess(res, routine, 'Detalle de la rutina obtenido.');
    } catch (error) {
      next(error);
    }
  }

  static async create(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const routine = await WorkoutsService.create(req.body, req.admin?.id, req.admin?.email);
      sendSuccess(res, routine, 'Rutina creada exitosamente.', 201);
    } catch (error) {
      next(error);
    }
  }

  static async update(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const routine = await WorkoutsService.update(id, req.body, req.admin?.id, req.admin?.email);
      sendSuccess(res, routine, 'Rutina actualizada.');
    } catch (error) {
      next(error);
    }
  }

  static async setStatus(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const { status } = req.body;
      const routine = await WorkoutsService.setPublicationStatus(id, status, req.admin?.id, req.admin?.email);
      sendSuccess(res, routine, `Estado de la rutina actualizado a ${status}.`);
    } catch (error) {
      next(error);
    }
  }

  static async delete(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const result = await WorkoutsService.delete(id, req.admin?.id, req.admin?.email);
      sendSuccess(res, result, result.message);
    } catch (error) {
      next(error);
    }
  }
}
