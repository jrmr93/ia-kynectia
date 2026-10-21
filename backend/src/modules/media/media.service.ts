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
}
