import { Response } from 'express';

export interface PaginationMeta {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
}

export const sendSuccess = <T>(
  res: Response,
  data: T,
  message?: string,
  statusCode: number = 200,
  pagination?: PaginationMeta
) => {
  return res.status(statusCode).json({
    success: true,
    message,
    data,
    pagination,
  });
};

export const sendError = (
  res: Response,
  message: string,
  statusCode: number = 400,
  errors?: any
) => {
  return res.status(statusCode).json({
    success: false,
    message,
    errors,
  });
};

export const parseIdParam = (idParam: string | string[] | undefined): number => {
  const str = Array.isArray(idParam) ? idParam[0] : idParam;
  const num = parseInt(str || '', 10);
  if (isNaN(num)) {
    throw { statusCode: 400, message: 'Identificador numérico inválido.' };
  }
  return num;
};
