import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { env } from '../config/env';
import { pool } from '../database/pool';
import { sendError } from '../utils/response';

export interface AuthenticatedAdmin {
  id: number;
  email: string;
  fullName: string;
  roleId: number;
  roleName: string;
  permissions: string[];
}

export interface AuthenticatedUser {
  id: number;
  email: string;
  fullName: string;
  role: string;
}

declare global {
  namespace Express {
    interface Request {
      admin?: AuthenticatedAdmin;
      user?: AuthenticatedUser;
    }
  }
}

export const requireAuth = async (
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      sendError(res, 'Acceso denegado. Token no proporcionado o formato inválido.', 401);
      return;
    }

    const token = authHeader.split(' ')[1];
    const decoded = jwt.verify(token, env.JWT_SECRET) as { adminId?: number; userId?: number; role?: string };

    if (decoded.userId) {
      const { rows: userRows } = await pool.query(
        'SELECT id, full_name, email, is_active FROM users WHERE id = $1 AND deleted_at IS NULL;',
        [decoded.userId]
      );
      if (userRows.length === 0) {
        sendError(res, 'Usuario atleta no encontrado o eliminado.', 401);
        return;
      }
      if (!userRows[0].is_active) {
        sendError(res, 'Cuenta de usuario inactiva.', 403);
        return;
      }
      req.user = {
        id: userRows[0].id,
        email: userRows[0].email,
        fullName: userRows[0].full_name,
        role: decoded.role || 'athlete',
      };
      next();
      return;
    }

    if (!decoded.adminId) {
      sendError(res, 'Token no válido para la operación.', 401);
      return;
    }

    const query = `
      SELECT a.id, a.full_name, a.email, a.role_id, a.is_active,
             r.name as role_name,
             COALESCE(array_agg(p.code) FILTER (WHERE p.code IS NOT NULL), '{}') as permissions
      FROM admin_users a
      JOIN roles r ON a.role_id = r.id
      LEFT JOIN role_permissions rp ON r.id = rp.role_id
      LEFT JOIN permissions p ON rp.permission_id = p.id
      WHERE a.id = $1 AND a.deleted_at IS NULL
      GROUP BY a.id, r.name;
    `;

    const { rows } = await pool.query(query, [decoded.adminId]);
    if (rows.length === 0) {
      sendError(res, 'Administrador no encontrado o eliminado.', 401);
      return;
    }

    const admin = rows[0];
    if (!admin.is_active) {
      sendError(res, 'Cuenta de administrador inactiva o deshabilitada.', 403);
      return;
    }

    req.admin = {
      id: admin.id,
      email: admin.email,
      fullName: admin.full_name,
      roleId: admin.role_id,
      roleName: admin.role_name,
      permissions: admin.permissions || [],
    };

    next();
  } catch (error: any) {
    if (error.name === 'TokenExpiredError') {
      sendError(res, 'La sesión ha expirado. Por favor ingresa nuevamente.', 401);
      return;
    }
    sendError(res, 'Token inválido o corrupto.', 401);
  }
};

export const requirePermission = (permissionCode: string) => {
  return (req: Request, res: Response, next: NextFunction): void => {
    if (!req.admin) {
      sendError(res, 'No autenticado.', 401);
      return;
    }

    // Superadmin tiene todos los permisos
    if (req.admin.roleName === 'superadmin') {
      return next();
    }

    if (!req.admin.permissions.includes(permissionCode)) {
      sendError(
        res,
        `Permiso denegado. Se requiere el permiso [${permissionCode}] para esta operación.`,
        403
      );
      return;
    }

    next();
  };
};

export const requireRole = (...roles: string[]) => {
  return (req: Request, res: Response, next: NextFunction): void => {
    if (!req.admin) {
      sendError(res, 'No autenticado.', 401);
      return;
    }

    if (!roles.includes(req.admin.roleName)) {
      sendError(
        res,
        `Acceso restringido. Esta acción requiere uno de los siguientes roles: ${roles.join(', ')}`,
        403
      );
      return;
    }

    next();
  };
};
