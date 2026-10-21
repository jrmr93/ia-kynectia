import { Request, Response, NextFunction } from 'express';
import { EquipmentService } from './equipment.service';
import { sendSuccess, parseIdParam } from '../../utils/response';

export class EquipmentController {
  static async list(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { search, category, isActive } = req.query;
      const items = await EquipmentService.list(
        search as string,
        category as string,
        isActive !== undefined ? isActive === 'true' : undefined
      );
      sendSuccess(res, items, 'Implementos deportivos obtenidos correctamente.');
    } catch (error) {
      next(error);
    }
  }

  static async getById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const item = await EquipmentService.getById(id);
      sendSuccess(res, item, 'Detalle del implemento obtenido.');
    } catch (error) {
      next(error);
    }
  }

  static async create(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const item = await EquipmentService.create(req.body, req.admin?.id, req.admin?.email);
      sendSuccess(res, item, 'Implemento registrado con éxito.', 201);
    } catch (error) {
      next(error);
    }
  }

  static async update(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const item = await EquipmentService.update(id, req.body, req.admin?.id, req.admin?.email);
      sendSuccess(res, item, 'Implemento actualizado correctamente.');
    } catch (error) {
      next(error);
    }
  }

  static async toggleActive(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const { isActive } = req.body;
      const item = await EquipmentService.toggleActive(id, isActive, req.admin?.id, req.admin?.email);
      sendSuccess(res, item, `Implemento ${isActive ? 'activado' : 'desactivado'}.`);
    } catch (error) {
      next(error);
    }
  }

  static async delete(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const result = await EquipmentService.delete(id, req.admin?.id, req.admin?.email);
      sendSuccess(res, result, result.message);
    } catch (error) {
      next(error);
    }
  }
}
