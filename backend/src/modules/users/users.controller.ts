import { Request, Response, NextFunction } from 'express';
import { UsersService } from './users.service';
import { sendSuccess, parseIdParam } from '../../utils/response';

export class UsersController {
  static async list(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { search, isActive, fitnessGoal, page, limit } = req.query;
      const result = await UsersService.list({
        search: search as string,
        isActive: isActive !== undefined ? isActive === 'true' : undefined,
        fitnessGoal: fitnessGoal as string,
        page: page ? parseInt(page as string, 10) : 1,
        limit: limit ? parseInt(limit as string, 10) : 10,
      });
      sendSuccess(res, result.users, 'Usuarios obtenidos correctamente.', 200, result.pagination);
    } catch (error) {
      next(error);
    }
  }

  static async getById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const user = await UsersService.getById(id);
      sendSuccess(res, user, 'Detalle del usuario obtenido.');
    } catch (error) {
      next(error);
    }
  }

  static async create(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const user = await UsersService.create(req.body, req.admin?.id, req.admin?.email);
      sendSuccess(res, user, 'Usuario registrado con éxito.', 201);
    } catch (error) {
      next(error);
    }
  }

  static async update(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const user = await UsersService.update(id, req.body, req.admin?.id, req.admin?.email);
      sendSuccess(res, user, 'Información del usuario actualizada.');
    } catch (error) {
      next(error);
    }
  }

  static async toggleActive(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const { isActive } = req.body;
      const user = await UsersService.toggleActive(id, isActive, req.admin?.id, req.admin?.email);
      sendSuccess(res, user, `Usuario ${isActive ? 'activado' : 'desactivado'} con éxito.`);
    } catch (error) {
      next(error);
    }
  }

  static async delete(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const anonymize = req.query.anonymize === 'true';
      const result = await UsersService.softDeleteOrAnonymize(id, anonymize, req.admin?.id, req.admin?.email);
      sendSuccess(res, result, result.message);
    } catch (error) {
      next(error);
    }
  }
}
