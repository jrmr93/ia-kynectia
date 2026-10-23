import { Request, Response, NextFunction } from 'express';
import { MediaService } from './media.service';
import { sendSuccess, sendError, parseIdParam } from '../../utils/response';

export class MediaController {
  static async upload(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      if (!req.file) {
        sendError(res, 'No se ha adjuntado ningún archivo.', 400);
        return;
      }

      const assetType = req.file.mimetype.startsWith('video/')
        ? 'video'
        : req.file.mimetype.startsWith('image/')
        ? 'image'
        : 'document';

      const asset = await MediaService.registerAsset(req.file, assetType);
      const fileUrl = `${req.protocol}://${req.get('host')}${asset.file_path}`;

      sendSuccess(res, { ...asset, fullUrl: fileUrl }, 'Archivo multimedia subido con éxito.', 201);
    } catch (error) {
      next(error);
    }
  }

  static async list(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { type, limit } = req.query;
      const assets = await MediaService.listAssets(
        type as string,
        limit ? parseInt(limit as string, 10) : 50
      );
      sendSuccess(res, assets, 'Archivos multimedia obtenidos.');
    } catch (error) {
      next(error);
    }
  }

  static async delete(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const id = parseIdParam(req.params.id);
      const result = await MediaService.deleteAsset(id);
      sendSuccess(res, result, result.message);
    } catch (error) {
      next(error);
    }
  }

  static async deleteByUrl(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { fileUrl, filePath } = req.body;
      const target = fileUrl || filePath;
      if (!target) {
        sendError(res, 'Debe especificar la ruta o URL del archivo a eliminar.', 400);
        return;
      }
      const result = await MediaService.deleteFileByUrl(target);
      sendSuccess(res, result, result.message);
    } catch (error) {
      next(error);
    }
  }
}
