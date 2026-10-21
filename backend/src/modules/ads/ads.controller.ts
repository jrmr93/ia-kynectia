import { Request, Response, NextFunction } from 'express';
import { AdsService } from './ads.service';
import { sendSuccess, parseIdParam } from '../../utils/response';

export class AdsController {
  static async list(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { placement, isActive } = req.query;
      const ads = await AdsService.list(
        placement as string,
        isActive !== undefined ? isActive === 'true' : undefined
      );
      sendSuccess(res, ads, 'Anuncios publicitarios obtenidos.');
    } catch (error) {
      next(error);
    }
  }

  static async getById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const ad = await AdsService.getById(id);
      sendSuccess(res, ad, 'Detalle del anuncio obtenido.');
    } catch (error) {
      next(error);
    }
  }

  static async create(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const ad = await AdsService.create(req.body, req.admin?.id, req.admin?.email);
      sendSuccess(res, ad, 'Anuncio publicitario creado.', 201);
    } catch (error) {
      next(error);
    }
  }

  static async update(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const ad = await AdsService.update(id, req.body, req.admin?.id, req.admin?.email);
      sendSuccess(res, ad, 'Anuncio actualizado.');
    } catch (error) {
      next(error);
    }
  }

  static async toggleActive(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const { isActive } = req.body;
      const ad = await AdsService.toggleActive(id, isActive, req.admin?.id, req.admin?.email);
      sendSuccess(res, ad, `Anuncio ${isActive ? 'activado' : 'desactivado'}.`);
    } catch (error) {
      next(error);
    }
  }

  static async delete(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const result = await AdsService.delete(id, req.admin?.id, req.admin?.email);
      sendSuccess(res, result, result.message);
    } catch (error) {
      next(error);
    }
  }
}
