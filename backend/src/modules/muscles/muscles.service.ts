import { pool } from '../../database/pool';
import { recordAuditLog } from '../../utils/logger';

export class MusclesService {
  static async list(search?: string, isActive?: boolean) {
    const conditions: string[] = ['deleted_at IS NULL'];
    const params: any[] = [];

    if (search) {
      params.push(`%${search.trim().toLowerCase()}%`);
      conditions.push(`LOWER(name) LIKE $${params.length}`);
    }

    if (isActive !== undefined) {
      params.push(isActive);
      conditions.push(`is_active = $${params.length}`);
    }

    const whereClause = `WHERE ${conditions.join(' AND ')}`;
    const query = `
      SELECT m.id, m.name, m.description, m.image_url, m.is_active, m.created_at, m.updated_at,
             (SELECT COUNT(*)::int FROM exercise_muscles em
              JOIN exercises e ON em.exercise_id = e.id AND e.deleted_at IS NULL
              WHERE em.muscle_group_id = m.id) as exercises_count
      FROM muscle_groups m
      ${whereClause}
      ORDER BY m.name ASC;
    `;

    const { rows } = await pool.query(query, params);
    return rows;
  }

  static async getById(id: number) {
    const query = `
      SELECT m.*,
             COALESCE(json_agg(
               json_build_object(
                 'id', e.id,
                 'name', e.name,
                 'difficultyLevel', e.difficulty_level,
                 'isPrimary', em.is_primary
               )
             ) FILTER (WHERE e.id IS NOT NULL), '[]'::json) as related_exercises
      FROM muscle_groups m
      LEFT JOIN exercise_muscles em ON m.id = em.muscle_group_id
      LEFT JOIN exercises e ON em.exercise_id = e.id AND e.deleted_at IS NULL
      WHERE m.id = $1 AND m.deleted_at IS NULL
      GROUP BY m.id;
    `;

    const { rows } = await pool.query(query, [id]);
    if (rows.length === 0) {
      throw { statusCode: 404, message: 'Grupo muscular no encontrado.' };
    }
    return rows[0];
  }

  static async create(data: { name: string; description?: string; imageUrl?: string; isActive?: boolean }, adminId?: number, adminEmail?: string) {
    const query = `
      INSERT INTO muscle_groups (name, description, image_url, is_active)
      VALUES ($1, $2, $3, $4)
      RETURNING *;
    `;
    const values = [
      data.name.trim(),
      data.description || null,
      data.imageUrl || null,
      data.isActive !== undefined ? data.isActive : true,
    ];

    const { rows } = await pool.query(query, values);
    const created = rows[0];

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'CREATE',
      entityType: 'muscle_groups',
      entityId: created.id,
      newValues: created,
    });

    return created;
  }

  static async update(id: number, data: { name?: string; description?: string; imageUrl?: string; isActive?: boolean }, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);

    const query = `
      UPDATE muscle_groups
      SET name = COALESCE($1, name),
          description = COALESCE($2, description),
          image_url = COALESCE($3, image_url),
          is_active = COALESCE($4, is_active)
      WHERE id = $5 AND deleted_at IS NULL
      RETURNING *;
    `;
    const values = [data.name ? data.name.trim() : null, data.description, data.imageUrl, data.isActive, id];

    const { rows } = await pool.query(query, values);
    const updated = rows[0];

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'UPDATE',
      entityType: 'muscle_groups',
      entityId: id,
      oldValues: existing,
      newValues: updated,
    });

    return updated;
  }

  static async toggleActive(id: number, isActive: boolean, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);
    const { rows } = await pool.query(
      'UPDATE muscle_groups SET is_active = $1 WHERE id = $2 AND deleted_at IS NULL RETURNING id, name, is_active;',
      [isActive, id]
    );

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'STATUS_CHANGE',
      entityType: 'muscle_groups',
      entityId: id,
      oldValues: { isActive: existing.is_active },
      newValues: { isActive },
    });

    return rows[0];
  }

  static async delete(id: number, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);

    // Verificar si existen dependencias con ejercicios activos
    const checkDepsQuery = `
      SELECT COUNT(*)::int as count
      FROM exercise_muscles em
      JOIN exercises e ON em.exercise_id = e.id AND e.deleted_at IS NULL
      WHERE em.muscle_group_id = $1;
    `;
    const { rows: depRows } = await pool.query(checkDepsQuery, [id]);

    if (depRows[0].count > 0) {
      throw {
        statusCode: 400,
        message: `No se puede eliminar el grupo muscular "${existing.name}" porque está asociado a ${depRows[0].count} ejercicio(s). Desactívalo o reasigna los ejercicios primero.`,
      };
    }

    // Soft delete
    await pool.query('UPDATE muscle_groups SET deleted_at = NOW(), is_active = FALSE WHERE id = $1;', [id]);

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'DELETE',
      entityType: 'muscle_groups',
      entityId: id,
      oldValues: existing,
      newValues: { deleted_at: new Date() },
    });

    return { message: 'Grupo muscular eliminado exitosamente.' };
  }
}
