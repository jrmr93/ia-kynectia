import { pool } from '../../database/pool';
import { PaginationMeta } from '../../utils/response';

export interface AuditFilterOptions {
  action?: string;
  entityType?: string;
  adminId?: number;
  page?: number;
  limit?: number;
}

export class AuditService {
  static async list(options: AuditFilterOptions) {
    const page = Math.max(1, options.page || 1);
    const limit = Math.min(100, Math.max(1, options.limit || 20));
    const offset = (page - 1) * limit;

    const conditions: string[] = [];
    const params: any[] = [];

    if (options.action) {
      params.push(options.action);
      conditions.push(`action = $${params.length}`);
    }

    if (options.entityType) {
      params.push(options.entityType);
      conditions.push(`entity_type = $${params.length}`);
    }

    if (options.adminId) {
      params.push(options.adminId);
      conditions.push(`admin_id = $${params.length}`);
    }

    const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';

    const countQuery = `SELECT COUNT(*)::int as total FROM audit_logs ${whereClause};`;
    const { rows: countRows } = await pool.query(countQuery, params);
    const total = countRows[0].total;

    const query = `
      SELECT id, admin_id, admin_email, action, entity_type, entity_id,
             old_values, new_values, ip_address, user_agent, created_at
      FROM audit_logs
      ${whereClause}
      ORDER BY created_at DESC
      LIMIT $${params.length + 1} OFFSET $${params.length + 2};
    `;

    const { rows } = await pool.query(query, [...params, limit, offset]);

    const pagination: PaginationMeta = {
      page,
      limit,
      total,
      totalPages: Math.ceil(total / limit),
    };

    return { logs: rows, pagination };
  }
}
