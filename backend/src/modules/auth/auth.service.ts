import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { pool } from '../../database/pool';
import { env } from '../../config/env';
import { recordAuditLog } from '../../utils/logger';

export class AuthService {
  static async login(email: string, password: string, ipAddress?: string, userAgent?: string) {
    const query = `
      SELECT a.id, a.full_name, a.email, a.password_hash, a.role_id, a.is_active,
             r.name as role_name,
             COALESCE(array_agg(p.code) FILTER (WHERE p.code IS NOT NULL), '{}') as permissions
      FROM admin_users a
      JOIN roles r ON a.role_id = r.id
      LEFT JOIN role_permissions rp ON r.id = rp.role_id
      LEFT JOIN permissions p ON rp.permission_id = p.id
      WHERE LOWER(a.email) = LOWER($1) AND a.deleted_at IS NULL
      GROUP BY a.id, r.name;
    `;

    const { rows } = await pool.query(query, [email]);
    if (rows.length === 0) {
      throw { statusCode: 401, message: 'Credenciales inválidas. Verifica tu correo y contraseña.' };
    }

    const admin = rows[0];
    if (!admin.is_active) {
      throw { statusCode: 403, message: 'Tu cuenta administrativa está inactiva. Contacta a un superadministrador.' };
    }

    const isMatch = await bcrypt.compare(password, admin.password_hash);
    if (!isMatch) {
      throw { statusCode: 401, message: 'Credenciales inválidas. Verifica tu correo y contraseña.' };
    }

    // Actualizar last_login_at
    await pool.query('UPDATE admin_users SET last_login_at = NOW() WHERE id = $1', [admin.id]);

    const token = jwt.sign(
      { adminId: admin.id, role: admin.role_name },
      env.JWT_SECRET,
      { expiresIn: env.JWT_EXPIRES_IN as any }
    );

    await recordAuditLog({
      adminId: admin.id,
      adminEmail: admin.email,
      action: 'LOGIN',
      entityType: 'admin_users',
      entityId: admin.id,
      ipAddress,
      userAgent,
    });

    return {
      token,
      admin: {
        id: admin.id,
        fullName: admin.full_name,
        email: admin.email,
        roleId: admin.role_id,
        roleName: admin.role_name,
        permissions: admin.permissions || [],
      },
    };
  }

  static async changePassword(adminId: number, oldPassword: string, newPassword: string) {
    const { rows } = await pool.query('SELECT password_hash FROM admin_users WHERE id = $1', [adminId]);
    if (rows.length === 0) {
      throw { statusCode: 404, message: 'Administrador no encontrado.' };
    }

    const isMatch = await bcrypt.compare(oldPassword, rows[0].password_hash);
    if (!isMatch) {
      throw { statusCode: 400, message: 'La contraseña actual no es correcta.' };
    }

    const newHash = await bcrypt.hash(newPassword, env.BCRYPT_SALT_ROUNDS);
    await pool.query('UPDATE admin_users SET password_hash = $1 WHERE id = $2', [newHash, adminId]);

    await recordAuditLog({
      adminId,
      action: 'UPDATE',
      entityType: 'admin_users',
      entityId: adminId,
      newValues: { passwordChanged: true },
    });

    return { message: 'Contraseña actualizada exitosamente.' };
  }
}
