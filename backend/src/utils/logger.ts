import { pool } from '../database/pool';

export interface AuditLogPayload {
  adminId?: number;
  adminEmail?: string;
  action: 'CREATE' | 'UPDATE' | 'DELETE' | 'LOGIN' | 'LOGOUT' | 'STATUS_CHANGE' | 'PUBLISH';
  entityType: string;
  entityId?: string | number;
  oldValues?: any;
  newValues?: any;
  ipAddress?: string;
  userAgent?: string;
}

export const recordAuditLog = async (payload: AuditLogPayload): Promise<void> => {
  try {
    await pool.query(
      `INSERT INTO audit_logs (
        admin_id, admin_email, action, entity_type, entity_id,
        old_values, new_values, ip_address, user_agent
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
      [
        payload.adminId || null,
        payload.adminEmail || null,
        payload.action,
        payload.entityType,
        payload.entityId ? String(payload.entityId) : null,
        payload.oldValues ? JSON.stringify(payload.oldValues) : null,
        payload.newValues ? JSON.stringify(payload.newValues) : null,
        payload.ipAddress || null,
        payload.userAgent || null,
      ]
    );
  } catch (error) {
    console.error('[Audit Log Error]: Fallo al registrar auditoría en base de datos:', error);
  }
};
