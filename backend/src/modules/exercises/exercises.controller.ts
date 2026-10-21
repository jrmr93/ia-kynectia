import { Request, Response, NextFunction } from 'express';
import { ExercisesService } from './exercises.service';
import { sendSuccess, parseIdParam } from '../../utils/response';

export class ExercisesController {
  static async list(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { search, difficultyLevel, exerciseType, publicationStatus, muscleGroupId, equipmentId, isActive, page, limit } = req.query;
      const result = await ExercisesService.list({
        search: search as string,
        difficultyLevel: difficultyLevel as string,
        exerciseType: exerciseType as string,
        publicationStatus: publicationStatus as string,
        muscleGroupId: muscleGroupId ? parseInt(muscleGroupId as string, 10) : undefined,
        equipmentId: equipmentId ? parseInt(equipmentId as string, 10) : undefined,
        isActive: isActive !== undefined ? isActive === 'true' : undefined,
        page: page ? parseInt(page as string, 10) : 1,
        limit: limit ? parseInt(limit as string, 10) : 10,
      });
      sendSuccess(res, result.exercises, 'Catálogo de ejercicios obtenido.', 200, result.pagination);
    } catch (error) {
      next(error);
    }
  }

  static async getById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const exercise = await ExercisesService.getById(id);
      sendSuccess(res, exercise, 'Detalle de ejercicio obtenido.');
    } catch (error) {
      next(error);
    }
  }

  static async create(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const exercise = await ExercisesService.create(req.body, req.admin?.id, req.admin?.email);
      sendSuccess(res, exercise, 'Ejercicio creado y publicado con éxito.', 201);
    } catch (error) {
      next(error);
    }
  }

  static async update(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const exercise = await ExercisesService.update(id, req.body, req.admin?.id, req.admin?.email);
      sendSuccess(res, exercise, 'Ejercicio actualizado.');
    } catch (error) {
      next(error);
    }
  }

  static async toggleActive(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const { isActive } = req.body;
      const exercise = await ExercisesService.toggleActive(id, isActive, req.admin?.id, req.admin?.email);
      sendSuccess(res, exercise, `Ejercicio ${isActive ? 'activado' : 'desactivado'}.`);
    } catch (error) {
      next(error);
    }
  }

  static async delete(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const result = await ExercisesService.delete(id, req.admin?.id, req.admin?.email);
      sendSuccess(res, result, result.message);
    } catch (error) {
      next(error);
    }
  }
}
