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
      // Intentar login como usuario atleta de la plataforma
      const userQuery = `
        SELECT u.id, u.full_name, u.email, u.phone, u.fitness_goal, u.height_cm, u.weight_kg, u.is_active,
               (SELECT p.name FROM user_subscriptions s
                JOIN plans p ON s.plan_id = p.id
                WHERE s.user_id = u.id AND s.status = 'active'
                ORDER BY s.end_date DESC LIMIT 1) as plan_name
        FROM users u
        WHERE LOWER(u.email) = LOWER($1) AND u.deleted_at IS NULL;
      `;
      const { rows: userRows } = await pool.query(userQuery, [email]);
      if (userRows.length > 0) {
        const user = userRows[0];
        if (!user.is_active) {
          throw { statusCode: 403, message: 'Tu cuenta de usuario está inactiva. Contacta a soporte.' };
        }
        const token = jwt.sign(
          { userId: user.id, email: user.email, role: 'athlete' },
          env.JWT_SECRET,
          { expiresIn: env.JWT_EXPIRES_IN as any }
        );
        return {
          token,
          user: {
            id: user.id,
            fullName: user.full_name,
            email: user.email,
            phone: user.phone,
            fitnessGoal: user.fitness_goal,
            heightCm: user.height_cm ? parseFloat(user.height_cm) : null,
            weightKg: user.weight_kg ? parseFloat(user.weight_kg) : null,
            planName: user.plan_name || 'Plan Starter',
          },
        };
      }

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
