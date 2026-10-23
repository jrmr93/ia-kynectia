import { pool } from '../../database/pool';
import { recordAuditLog } from '../../utils/logger';
import { MediaService } from '../media/media.service';

export class EquipmentService {
  static async list(search?: string, category?: string, isActive?: boolean) {
    const conditions: string[] = ['deleted_at IS NULL'];
    const params: any[] = [];

    if (search) {
      params.push(`%${search.trim().toLowerCase()}%`);
      conditions.push(`LOWER(name) LIKE $${params.length}`);
    }

    if (category) {
      params.push(category);
      conditions.push(`category = $${params.length}`);
    }

    if (isActive !== undefined) {
      params.push(isActive);
      conditions.push(`is_active = $${params.length}`);
    }

    const whereClause = `WHERE ${conditions.join(' AND ')}`;
    const query = `
      SELECT eq.id, eq.name, eq.description, eq.category, eq.image_url, eq.is_active, eq.created_at, eq.updated_at,
             (SELECT COUNT(*)::int FROM exercise_equipment ee
              JOIN exercises e ON ee.exercise_id = e.id AND e.deleted_at IS NULL
              WHERE ee.equipment_id = eq.id) as exercises_count
      FROM equipment eq
      ${whereClause}
      ORDER BY eq.name ASC;
    `;

    const { rows } = await pool.query(query, params);
    return rows;
  }

  static async getById(id: number) {
    const query = `
      SELECT eq.*,
             COALESCE(json_agg(
               json_build_object(
                 'id', e.id,
                 'name', e.name,
                 'difficultyLevel', e.difficulty_level
               )
             ) FILTER (WHERE e.id IS NOT NULL), '[]'::json) as related_exercises
      FROM equipment eq
      LEFT JOIN exercise_equipment ee ON eq.id = ee.equipment_id
      LEFT JOIN exercises e ON ee.exercise_id = e.id AND e.deleted_at IS NULL
      WHERE eq.id = $1 AND eq.deleted_at IS NULL
      GROUP BY eq.id;
    `;

    const { rows } = await pool.query(query, [id]);
    if (rows.length === 0) {
      throw { statusCode: 404, message: 'Implemento deportivo no encontrado.' };
    }
    return rows[0];
  }

  static async create(data: { name: string; description?: string; category: string; imageUrl?: string; isActive?: boolean }, adminId?: number, adminEmail?: string) {
    const query = `
      INSERT INTO equipment (name, description, category, image_url, is_active)
      VALUES ($1, $2, $3, $4, $5)
      RETURNING *;
    `;
    const values = [
      data.name.trim(),
      data.description || null,
      data.category,
      data.imageUrl || null,
      data.isActive !== undefined ? data.isActive : true,
    ];

    const { rows } = await pool.query(query, values);
    const created = rows[0];

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'CREATE',
      entityType: 'equipment',
      entityId: created.id,
      newValues: created,
    });

    return created;
  }

  static async update(id: number, data: { name?: string; description?: string; category?: string; imageUrl?: string; isActive?: boolean }, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);

    // Si la imagen cambió o se removió y la anterior era un archivo local en uploads, eliminarlo del disco
    if (data.imageUrl !== undefined && existing.image_url && data.imageUrl !== existing.image_url && existing.image_url.includes('/uploads/')) {
      await MediaService.deleteFileByUrl(existing.image_url);
    }

    const query = `
      UPDATE equipment
      SET name = COALESCE($1, name),
          description = CASE WHEN $7::boolean THEN $2 ELSE description END,
          category = COALESCE($3, category),
          image_url = CASE WHEN $8::boolean THEN $4 ELSE image_url END,
          is_active = COALESCE($5, is_active)
      WHERE id = $6 AND deleted_at IS NULL
      RETURNING *;
    `;
    const values = [
      data.name ? data.name.trim() : null,
      data.description !== undefined ? (data.description || null) : null,
      data.category,
      data.imageUrl !== undefined ? (data.imageUrl || null) : null,
      data.isActive,
      id,
      data.description !== undefined,
      data.imageUrl !== undefined,
    ];

    const { rows } = await pool.query(query, values);
    const updated = rows[0];

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'UPDATE',
      entityType: 'equipment',
      entityId: id,
      oldValues: existing,
      newValues: updated,
    });

    return updated;
  }

  static async toggleActive(id: number, isActive: boolean, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);
    const { rows } = await pool.query(
      'UPDATE equipment SET is_active = $1 WHERE id = $2 AND deleted_at IS NULL RETURNING id, name, is_active;',
      [isActive, id]
    );

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'STATUS_CHANGE',
      entityType: 'equipment',
      entityId: id,
      oldValues: { isActive: existing.is_active },
      newValues: { isActive },
    });

    return rows[0];
  }

  static async delete(id: number, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);

    const checkDepsQuery = `
      SELECT COUNT(*)::int as count
      FROM exercise_equipment ee
      JOIN exercises e ON ee.exercise_id = e.id AND e.deleted_at IS NULL
      WHERE ee.equipment_id = $1;
    `;
    const { rows: depRows } = await pool.query(checkDepsQuery, [id]);

    if (depRows[0].count > 0) {
      throw {
        statusCode: 400,
        message: `No se puede eliminar el implemento "${existing.name}" porque se utiliza en ${depRows[0].count} ejercicio(s). Desactívalo en su lugar.`,
      };
    }

    // Si tiene imagen en uploads, eliminar archivo físico
    if (existing.image_url && existing.image_url.includes('/uploads/')) {
      await MediaService.deleteFileByUrl(existing.image_url);
    }

    await pool.query('UPDATE equipment SET deleted_at = NOW(), is_active = FALSE WHERE id = $1;', [id]);

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'DELETE',
      entityType: 'equipment',
      entityId: id,
      oldValues: existing,
      newValues: { deleted_at: new Date() },
    });

    return { message: 'Implemento eliminado exitosamente.' };
  }
}
