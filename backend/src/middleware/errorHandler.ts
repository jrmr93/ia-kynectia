import { Request, Response, NextFunction } from 'express';
import { sendError } from '../utils/response';
import { env } from '../config/env';

export const errorHandler = (
  err: any,
  req: Request,
  res: Response,
  next: NextFunction
): void => {
  console.error(`[Error] ${req.method} ${req.originalUrl}:`, err);

  // PostgreSQL Unique Violation (23505)
  if (err.code === '23505') {
    const detail = err.detail || 'Un registro con estos datos únicos ya existe.';
    sendError(res, detail, 409, { pgCode: err.code });
    return;
  }

  // PostgreSQL Foreign Key Violation (23503)
  if (err.code === '23503') {
    const detail = err.detail || 'No se puede realizar la operación debido a dependencias con otros registros.';
    sendError(res, detail, 400, { pgCode: err.code });
    return;
  }

  // PostgreSQL Invalid Text Representation (22P02)
  if (err.code === '22P02') {
    sendError(res, 'El formato del identificador o dato enviado es inválido.', 400, { pgCode: err.code });
    return;
  }

  const statusCode = err.statusCode || 500;
  const message = err.message || 'Ocurrió un error interno en el servidor.';

  sendError(
    res,
    message,
    statusCode,
    env.NODE_ENV === 'development' ? { stack: err.stack } : undefined
  );
};
