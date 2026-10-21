import { pool } from '../../database/pool';
import { PaginationMeta } from '../../utils/response';
import { recordAuditLog } from '../../utils/logger';

export interface UserFilterOptions {
  search?: string;
  isActive?: boolean;
  fitnessGoal?: string;
  page?: number;
  limit?: number;
}

export class UsersService {
  static async list(options: UserFilterOptions) {
    const page = Math.max(1, options.page || 1);
    const limit = Math.min(100, Math.max(1, options.limit || 10));
    const offset = (page - 1) * limit;

    const conditions: string[] = ['deleted_at IS NULL'];
    const params: any[] = [];

    if (options.search) {
      params.push(`%${options.search.trim().toLowerCase()}%`);
      conditions.push(`(LOWER(full_name) LIKE $${params.length} OR LOWER(email) LIKE $${params.length} OR phone LIKE $${params.length})`);
    }

    if (options.isActive !== undefined) {
      params.push(options.isActive);
      conditions.push(`is_active = $${params.length}`);
    }

    if (options.fitnessGoal) {
      params.push(options.fitnessGoal);
      conditions.push(`fitness_goal = $${params.length}`);
    }

    const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';

    const countQuery = `SELECT COUNT(*)::int as total FROM users ${whereClause};`;
    const { rows: countRows } = await pool.query(countQuery, params);
    const total = countRows[0].total;

    const query = `
      SELECT u.id, u.full_name, u.email, u.phone, u.avatar_url, u.birth_date,
             u.gender, u.height_cm, u.weight_kg, u.fitness_goal, u.is_active,
             u.email_verified, u.created_at, u.updated_at,
             (SELECT p.name FROM user_subscriptions s
              JOIN plans p ON s.plan_id = p.id
              WHERE s.user_id = u.id AND s.status = 'active' AND s.end_date >= NOW()
              ORDER BY s.end_date DESC LIMIT 1) as active_plan_name
      FROM users u
      ${whereClause}
      ORDER BY u.created_at DESC
      LIMIT $${params.length + 1} OFFSET $${params.length + 2};
    `;

    const { rows } = await pool.query(query, [...params, limit, offset]);

    const pagination: PaginationMeta = {
      page,
      limit,
      total,
      totalPages: Math.ceil(total / limit),
    };

    return { users: rows, pagination };
  }

  static async getById(id: number) {
    const userQuery = `
      SELECT u.id, u.full_name, u.email, u.phone, u.avatar_url, u.birth_date,
             u.gender, u.height_cm, u.weight_kg, u.fitness_goal, u.is_active,
             u.email_verified, u.created_at, u.updated_at
      FROM users u
      WHERE u.id = $1 AND u.deleted_at IS NULL;
    `;
    const { rows: userRows } = await pool.query(userQuery, [id]);
    if (userRows.length === 0) {
      throw { statusCode: 404, message: 'Usuario no encontrado.' };
    }

    const subsQuery = `
      SELECT s.id, s.status, s.start_date, s.end_date, s.price_paid, s.currency,
             p.name as plan_name, p.billing_cycle
      FROM user_subscriptions s
      JOIN plans p ON s.plan_id = p.id
      WHERE s.user_id = $1
      ORDER BY s.created_at DESC;
    `;
    const { rows: subsRows } = await pool.query(subsQuery, [id]);

    return {
      ...userRows[0],
      subscriptions: subsRows,
    };
  }

  static async create(data: any, adminId?: number, adminEmail?: string) {
    const query = `
      INSERT INTO users (
        full_name, email, phone, birth_date, gender, height_cm, weight_kg, fitness_goal, is_active, email_verified
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
      RETURNING *;
    `;
    const values = [
      data.fullName,
      data.email.toLowerCase().trim(),
      data.phone || null,
      data.birthDate || null,
      data.gender || 'unspecified',
      data.heightCm || null,
      data.weightKg || null,
      data.fitnessGoal || null,
      data.isActive !== undefined ? data.isActive : true,
      data.emailVerified !== undefined ? data.emailVerified : false,
    ];

    const { rows } = await pool.query(query, values);
    const createdUser = rows[0];

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'CREATE',
      entityType: 'users',
      entityId: createdUser.id,
      newValues: createdUser,
    });

    return createdUser;
  }

  static async update(id: number, data: any, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);

    const query = `
      UPDATE users
      SET full_name = COALESCE($1, full_name),
          email = COALESCE($2, email),
          phone = COALESCE($3, phone),
          birth_date = COALESCE($4, birth_date),
          gender = COALESCE($5, gender),
          height_cm = COALESCE($6, height_cm),
          weight_kg = COALESCE($7, weight_kg),
          fitness_goal = COALESCE($8, fitness_goal),
          is_active = COALESCE($9, is_active),
          email_verified = COALESCE($10, email_verified)
      WHERE id = $11 AND deleted_at IS NULL
      RETURNING *;
    `;
    const values = [
      data.fullName,
      data.email ? data.email.toLowerCase().trim() : null,
      data.phone,
      data.birthDate,
      data.gender,
      data.heightCm,
      data.weightKg,
      data.fitnessGoal,
      data.isActive,
      data.emailVerified,
      id,
    ];

    const { rows } = await pool.query(query, values);
    const updatedUser = rows[0];

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'UPDATE',
      entityType: 'users',
      entityId: id,
      oldValues: existing,
      newValues: updatedUser,
    });

    return updatedUser;
  }

  static async toggleActive(id: number, isActive: boolean, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);
    const { rows } = await pool.query(
      'UPDATE users SET is_active = $1 WHERE id = $2 AND deleted_at IS NULL RETURNING id, full_name, email, is_active;',
      [isActive, id]
    );

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'STATUS_CHANGE',
      entityType: 'users',
      entityId: id,
      oldValues: { isActive: existing.is_active },
      newValues: { isActive },
    });

    return rows[0];
  }

  static async softDeleteOrAnonymize(id: number, anonymize: boolean = false, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);

    if (anonymize) {
      const anonEmail = `anon_${id}_${Date.now()}@kynectia.deleted`;
      await pool.query(
        `UPDATE users
         SET full_name = 'Usuario Anonimizado',
             email = $1,
             phone = NULL,
             avatar_url = NULL,
             deleted_at = NOW(),
             is_active = FALSE
         WHERE id = $2;`,
        [anonEmail, id]
      );
    } else {
      await pool.query(
        `UPDATE users
         SET deleted_at = NOW(),
             is_active = FALSE
         WHERE id = $1;`,
        [id]
      );
    }

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'DELETE',
      entityType: 'users',
      entityId: id,
      oldValues: existing,
      newValues: { deleted_at: new Date(), anonymized: anonymize },
    });

    return { message: anonymize ? 'Usuario anonimizado y dado de baja.' : 'Usuario eliminado (soft-delete).' };
  }
}
