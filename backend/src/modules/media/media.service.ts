import fs from 'fs';
import path from 'path';
import { pool } from '../../database/pool';

export class MediaService {
  static async registerAsset(file: Express.Multer.File, assetType: 'image' | 'video' | 'document' = 'image') {
    const query = `
      INSERT INTO media_assets (
        original_name, stored_filename, file_path, mime_type, file_size_bytes, asset_type
      ) VALUES ($1, $2, $3, $4, $5, $6)
      RETURNING *;
    `;
    const relativePath = `/uploads/${file.filename}`;
    const values = [
      file.originalname,
      file.filename,
      relativePath,
      file.mimetype,
      file.size,
      assetType,
    ];

    const { rows } = await pool.query(query, values);
    return rows[0];
  }

  static async listAssets(assetType?: string, limit: number = 50) {
    const conditions: string[] = [];
    const params: any[] = [];

    if (assetType) {
      params.push(assetType);
      conditions.push(`asset_type = $${params.length}`);
    }

    const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';
    const query = `
      SELECT * FROM media_assets
      ${whereClause}
      ORDER BY created_at DESC
      LIMIT $${params.length + 1};
    `;

    const { rows } = await pool.query(query, [...params, limit]);
    return rows;
  }

  static async deleteAsset(id: number) {
    const { rows } = await pool.query('SELECT * FROM media_assets WHERE id = $1', [id]);
    if (rows.length === 0) {
      throw { statusCode: 404, message: 'Archivo multimedia no encontrado.' };
    }

    const asset = rows[0];
    const fullPath = path.resolve(__dirname, '../../../', asset.file_path.replace(/^\//, ''));
    if (fs.existsSync(fullPath)) {
      try {
        fs.unlinkSync(fullPath);
      } catch (err) {
        console.error('Error al eliminar archivo del disco:', err);
      }
    }

    await pool.query('DELETE FROM media_assets WHERE id = $1', [id]);
    return { message: 'Archivo eliminado con éxito.' };
  }

  static async deleteFileByUrl(urlOrPath: string) {
    if (!urlOrPath || typeof urlOrPath !== 'string') {
      return { success: false, message: 'Ruta no válida.' };
    }

    const uploadIndex = urlOrPath.indexOf('/uploads/');
    if (uploadIndex === -1 && !urlOrPath.startsWith('uploads/')) {
      return { success: false, message: 'El archivo no pertenece al directorio de uploads.' };
    }

    const relativeSubpath = uploadIndex !== -1 ? urlOrPath.substring(uploadIndex + 9) : urlOrPath.replace(/^uploads\//, '');
    const filename = path.basename(relativeSubpath);
    if (!filename) {
      return { success: false, message: 'Nombre de archivo inválido.' };
    }

    const uploadDir = path.resolve(__dirname, '../../../uploads');
    const targetPath = path.resolve(uploadDir, filename);

    if (!targetPath.startsWith(uploadDir)) {
      throw { statusCode: 400, message: 'Ruta de archivo no permitida.' };
    }

    let deletedFromDisk = false;
    if (fs.existsSync(targetPath)) {
      try {
        fs.unlinkSync(targetPath);
        deletedFromDisk = true;
      } catch (err) {
        console.error('Error al eliminar archivo del disco:', err);
      }
    }

    try {
      await pool.query(
        'DELETE FROM media_assets WHERE stored_filename = $1 OR file_path = $2 OR file_path LIKE $3',
        [filename, `/uploads/${filename}`, `%${filename}`]
      );
    } catch (dbErr) {
      console.error('Error al limpiar registro de media_assets:', dbErr);
    }

    return {
      success: true,
      deletedFromDisk,
      message: deletedFromDisk ? 'Archivo eliminado del directorio exitosamente.' : 'Registro limpiado (archivo no encontrado en disco).',
    };
  }
}
