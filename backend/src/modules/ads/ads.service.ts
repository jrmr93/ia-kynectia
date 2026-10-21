import { pool } from '../../database/pool';
import { recordAuditLog } from '../../utils/logger';

export interface AdDto {
  title: string;
  description?: string;
  mediaUrl: string;
  destinationUrl?: string;
  placement: 'banner_home' | 'interstitial_workout' | 'sidebar' | 'footer';
  priority?: number;
  startsAt: string;
  endsAt: string;
  isActive?: boolean;
}

export class AdsService {
  static async list(placement?: string, isActive?: boolean) {
    const conditions: string[] = ['deleted_at IS NULL'];
    const params: any[] = [];

    if (placement) {
      params.push(placement);
      conditions.push(`placement = $${params.length}`);
    }

    if (isActive !== undefined) {
      params.push(isActive);
      conditions.push(`is_active = $${params.length}`);
    }

    const whereClause = `WHERE ${conditions.join(' AND ')}`;
    const query = `
      SELECT id, title, description, media_url, destination_url, placement,
             priority, starts_at, ends_at, is_active, impressions_count, clicks_count,
             created_at, updated_at
      FROM ads
      ${whereClause}
      ORDER BY priority DESC, created_at DESC;
    `;

    const { rows } = await pool.query(query, params);
    return rows;
  }

  static async getById(id: number) {
    const { rows } = await pool.query('SELECT * FROM ads WHERE id = $1 AND deleted_at IS NULL', [id]);
    if (rows.length === 0) {
      throw { statusCode: 404, message: 'Anuncio no encontrado.' };
    }
    return rows[0];
  }

  static async create(dto: AdDto, adminId?: number, adminEmail?: string) {
    const query = `
      INSERT INTO ads (
        title, description, media_url, destination_url, placement, priority, starts_at, ends_at, is_active
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
      RETURNING *;
    `;
    const values = [
      dto.title.trim(),
      dto.description || null,
      dto.mediaUrl,
      dto.destinationUrl || null,
      dto.placement,
      dto.priority || 1,
      dto.startsAt,
      dto.endsAt,
      dto.isActive !== undefined ? dto.isActive : true,
    ];

    const { rows } = await pool.query(query, values);
    const created = rows[0];

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'CREATE',
      entityType: 'ads',
      entityId: created.id,
      newValues: created,
    });

    return created;
  }

  static async update(id: number, dto: Partial<AdDto>, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);

    const query = `
      UPDATE ads
      SET title = COALESCE($1, title),
          description = COALESCE($2, description),
          media_url = COALESCE($3, media_url),
          destination_url = COALESCE($4, destination_url),
          placement = COALESCE($5, placement),
          priority = COALESCE($6, priority),
          starts_at = COALESCE($7, starts_at),
          ends_at = COALESCE($8, ends_at),
          is_active = COALESCE($9, is_active)
      WHERE id = $10 AND deleted_at IS NULL
      RETURNING *;
    `;
    const values = [
      dto.title ? dto.title.trim() : null,
      dto.description,
      dto.mediaUrl,
      dto.destinationUrl,
      dto.placement,
      dto.priority,
      dto.startsAt,
      dto.endsAt,
      dto.isActive,
      id,
    ];

    const { rows } = await pool.query(query, values);
    const updated = rows[0];

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'UPDATE',
      entityType: 'ads',
      entityId: id,
      oldValues: existing,
      newValues: updated,
    });

    return updated;
  }

  static async toggleActive(id: number, isActive: boolean, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);
    const { rows } = await pool.query(
      'UPDATE ads SET is_active = $1 WHERE id = $2 AND deleted_at IS NULL RETURNING id, title, is_active;',
      [isActive, id]
    );

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'STATUS_CHANGE',
      entityType: 'ads',
      entityId: id,
      oldValues: { isActive: existing.is_active },
      newValues: { isActive },
    });

    return rows[0];
  }

  static async delete(id: number, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);

    await pool.query('UPDATE ads SET deleted_at = NOW(), is_active = FALSE WHERE id = $1;', [id]);

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'DELETE',
      entityType: 'ads',
      entityId: id,
      oldValues: existing,
      newValues: { deleted_at: new Date() },
    });

    return { message: 'Anuncio eliminado exitosamente.' };
  }
}
