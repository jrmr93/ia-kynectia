import { pool } from '../../database/pool';
import { recordAuditLog } from '../../utils/logger';

export interface PlanDto {
  name: string;
  description?: string;
  billingCycle: 'monthly' | 'quarterly' | 'biannual' | 'annual' | 'free';
  price: number;
  currency?: string;
  benefits?: string[];
  isActive?: boolean;
  discountPercent?: number;
  promoEndDate?: string;
}

export class PlansService {
  static async list(isActive?: boolean) {
    const conditions: string[] = ['deleted_at IS NULL'];
    const params: any[] = [];

    if (isActive !== undefined) {
      params.push(isActive);
      conditions.push(`is_active = $${params.length}`);
    }

    const whereClause = `WHERE ${conditions.join(' AND ')}`;
    const query = `
      SELECT p.id, p.name, p.description, p.billing_cycle, p.price, p.currency,
             p.benefits, p.is_active, p.discount_percent, p.promo_end_date,
             p.created_at, p.updated_at,
             (SELECT COUNT(*)::int FROM user_subscriptions s WHERE s.plan_id = p.id AND s.status = 'active') as active_subscriptions_count
      FROM plans p
      ${whereClause}
      ORDER BY p.price ASC;
    `;

    const { rows } = await pool.query(query, params);
    return rows;
  }

  static async getById(id: number) {
    const query = `
      SELECT p.*,
             COALESCE((
               SELECT json_agg(json_build_object(
                 'id', s.id,
                 'userId', u.id,
                 'userName', u.full_name,
                 'userEmail', u.email,
                 'status', s.status,
                 'startDate', s.start_date,
                 'endDate', s.end_date,
                 'pricePaid', s.price_paid
               ))
               FROM user_subscriptions s
               JOIN users u ON s.user_id = u.id
               WHERE s.plan_id = p.id
               LIMIT 20
             ), '[]'::json) as recent_subscriptions
      FROM plans p
      WHERE p.id = $1 AND p.deleted_at IS NULL;
    `;

    const { rows } = await pool.query(query, [id]);
    if (rows.length === 0) {
      throw { statusCode: 404, message: 'Plan no encontrado.' };
    }
    return rows[0];
  }

  static async create(dto: PlanDto, adminId?: number, adminEmail?: string) {
    const query = `
      INSERT INTO plans (
        name, description, billing_cycle, price, currency, benefits, is_active, discount_percent, promo_end_date
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
      RETURNING *;
    `;
    const values = [
      dto.name.trim(),
      dto.description || null,
      dto.billingCycle,
      dto.price,
      dto.currency || 'USD',
      JSON.stringify(dto.benefits || []),
      dto.isActive !== undefined ? dto.isActive : true,
      dto.discountPercent || 0.0,
      dto.promoEndDate || null,
    ];

    const { rows } = await pool.query(query, values);
    const created = rows[0];

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'CREATE',
      entityType: 'plans',
      entityId: created.id,
      newValues: created,
    });

    return created;
  }

  static async update(id: number, dto: Partial<PlanDto>, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);

    const query = `
      UPDATE plans
      SET name = COALESCE($1, name),
          description = COALESCE($2, description),
          billing_cycle = COALESCE($3, billing_cycle),
          price = COALESCE($4, price),
          currency = COALESCE($5, currency),
          benefits = COALESCE($6, benefits),
          is_active = COALESCE($7, is_active),
          discount_percent = COALESCE($8, discount_percent),
          promo_end_date = COALESCE($9, promo_end_date)
      WHERE id = $10 AND deleted_at IS NULL
      RETURNING *;
    `;
    const values = [
      dto.name ? dto.name.trim() : null,
      dto.description,
      dto.billingCycle,
      dto.price,
      dto.currency,
      dto.benefits ? JSON.stringify(dto.benefits) : null,
      dto.isActive,
      dto.discountPercent,
      dto.promoEndDate,
      id,
    ];

    const { rows } = await pool.query(query, values);
    const updated = rows[0];

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'UPDATE',
      entityType: 'plans',
      entityId: id,
      oldValues: existing,
      newValues: updated,
    });

    return updated;
  }

  static async toggleActive(id: number, isActive: boolean, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);
    const { rows } = await pool.query(
      'UPDATE plans SET is_active = $1 WHERE id = $2 AND deleted_at IS NULL RETURNING id, name, is_active;',
      [isActive, id]
    );

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'STATUS_CHANGE',
      entityType: 'plans',
      entityId: id,
      oldValues: { isActive: existing.is_active },
      newValues: { isActive },
    });

    return rows[0];
  }

  static async delete(id: number, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);

    // Verificar si hay suscripciones activas vinculadas
    const checkQuery = `SELECT COUNT(*)::int as count FROM user_subscriptions WHERE plan_id = $1 AND status = 'active';`;
    const { rows } = await pool.query(checkQuery, [id]);

    if (rows[0].count > 0) {
      throw {
        statusCode: 400,
        message: `El plan "${existing.name}" tiene ${rows[0].count} suscripción(es) activa(s). Desactívalo para evitar nuevas contrataciones sin romper el histórico.`,
      };
    }

    await pool.query('UPDATE plans SET deleted_at = NOW(), is_active = FALSE WHERE id = $1;', [id]);

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'DELETE',
      entityType: 'plans',
      entityId: id,
      oldValues: existing,
      newValues: { deleted_at: new Date() },
    });

    return { message: 'Plan eliminado exitosamente (soft-delete).' };
  }

  static async listSubscriptions(status?: string, page: number = 1, limit: number = 10) {
    const offset = (page - 1) * limit;
    const conditions: string[] = [];
    const params: any[] = [];

    if (status) {
      params.push(status);
      conditions.push(`s.status = $${params.length}`);
    }

    const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';

    const countQuery = `SELECT COUNT(*)::int as total FROM user_subscriptions s ${whereClause};`;
    const { rows: countRows } = await pool.query(countQuery, params);
    const total = countRows[0].total;

    const query = `
      SELECT s.id, s.status, s.start_date, s.end_date, s.auto_renew, s.price_paid, s.currency, s.notes, s.created_at,
             u.id as user_id, u.full_name as user_name, u.email as user_email,
             p.id as plan_id, p.name as plan_name, p.billing_cycle
      FROM user_subscriptions s
      JOIN users u ON s.user_id = u.id
      JOIN plans p ON s.plan_id = p.id
      ${whereClause}
      ORDER BY s.created_at DESC
      LIMIT $${params.length + 1} OFFSET $${params.length + 2};
    `;

    const { rows } = await pool.query(query, [...params, limit, offset]);

    return {
      subscriptions: rows,
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
      },
    };
  }
}
