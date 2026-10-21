import React, { useEffect, useState, useCallback } from 'react';
import { apiRequest } from '../api/client';
import type { AuditLog } from '../types';
import { useToast } from '../context/ToastContext';
import { Modal } from '../components/Modal';
import {
  RefreshCw,
  Eye,
  ChevronLeft,
  ChevronRight,
  Filter,
} from 'lucide-react';

export const AuditView: React.FC = () => {
  const { showToast } = useToast();

  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionFilter, setActionFilter] = useState('all');
  const [entityFilter, setEntityFilter] = useState('all');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalCount, setTotalCount] = useState(0);

  const [detailLog, setDetailLog] = useState<AuditLog | null>(null);

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    try {
      const params: any = { page, limit: 12 };
      if (actionFilter !== 'all') params.action = actionFilter;
      if (entityFilter !== 'all') params.entityType = entityFilter;

      const res = await apiRequest<AuditLog[]>('/audit', { params });
      setLogs(res.data);
      if (res.pagination) {
        setTotalPages(res.pagination.totalPages);
        setTotalCount(res.pagination.total);
      }
    } catch (err: any) {
      showToast('error', err.message || 'Error al obtener registros de auditoría.');
    } finally {
      setLoading(false);
    }
  }, [page, actionFilter, entityFilter, showToast]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  return (
    <div className="page-container">
      {/* Encabezado */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#ffffff' }}>Auditoría y Seguridad</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            Registro inmutable de acciones, inicios de sesión y modificaciones de registros en PostgreSQL ({totalCount} eventos).
          </p>
        </div>
        <button onClick={fetchLogs} className="btn btn-secondary btn-sm" title="Recargar">
          <RefreshCw size={16} className={loading ? 'spin-animation' : ''} />
          <span>Actualizar Bitácora</span>
        </button>
      </div>

      {/* Filtros */}
      <div className="filter-bar">
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-muted)' }}>
            <Filter size={16} />
            <span style={{ fontSize: '0.85rem' }}>Filtros:</span>
          </div>

          <select
            className="form-select"
            value={actionFilter}
            onChange={(e) => {
              setActionFilter(e.target.value);
              setPage(1);
            }}
            style={{ width: '180px' }}
          >
            <option value="all">Todas las Acciones</option>
            <option value="CREATE">CREATE (Creación)</option>
            <option value="UPDATE">UPDATE (Modificación)</option>
            <option value="DELETE">DELETE (Baja / Eliminación)</option>
            <option value="STATUS_CHANGE">STATUS_CHANGE (Estado)</option>
            <option value="PUBLISH">PUBLISH (Publicación)</option>
            <option value="LOGIN">LOGIN (Sesión iniciada)</option>
          </select>

          <select
            className="form-select"
            value={entityFilter}
            onChange={(e) => {
              setEntityFilter(e.target.value);
              setPage(1);
            }}
            style={{ width: '180px' }}
          >
            <option value="all">Todos los Módulos</option>
            <option value="users">Usuarios</option>
            <option value="exercises">Ejercicios</option>
            <option value="muscle_groups">Grupos Musculares</option>
            <option value="equipment">Implementos</option>
            <option value="workout_routines">Rutinas</option>
            <option value="plans">Planes</option>
            <option value="ads">Publicidad</option>
            <option value="admin_users">Administradores</option>
          </select>
        </div>
      </div>

      {/* Tabla de Logs */}
      <div className="table-container">
        <table className="custom-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Administrador</th>
              <th>Acción</th>
              <th>Módulo Afectado</th>
              <th>Registro ID</th>
              <th>IP Origen</th>
              <th>Fecha y Hora</th>
              <th style={{ textAlign: 'right' }}>Payload</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                  <RefreshCw size={24} className="spin-animation" style={{ color: 'var(--accent-cyan)', marginBottom: '8px' }} />
                  <p>Consultando bitácora de auditoría...</p>
                </td>
              </tr>
            ) : logs.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                  No se encontraron eventos de auditoría con los filtros aplicados.
                </td>
              </tr>
            ) : (
              logs.map((log) => (
                <tr key={log.id}>
                  <td style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>#{log.id}</td>
                  <td>
                    <span style={{ fontWeight: 600, color: '#ffffff' }}>
                      {log.admin_email || 'Sistema / Seeder'}
                    </span>
                  </td>
                  <td>
                    <span
                      className={`badge ${
                        log.action === 'CREATE'
                          ? 'badge-success'
                          : log.action === 'UPDATE'
                          ? 'badge-info'
                          : log.action === 'DELETE'
                          ? 'badge-danger'
                          : log.action === 'STATUS_CHANGE'
                          ? 'badge-warning'
                          : 'badge-purple'
                      }`}
                    >
                      {log.action}
                    </span>
                  </td>
                  <td>
                    <code style={{ color: 'var(--accent-cyan)', background: 'rgba(0, 242, 254, 0.08)', padding: '2px 8px', borderRadius: '4px' }}>
                      {log.entity_type}
                    </code>
                  </td>
                  <td>{log.entity_id || '—'}</td>
                  <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    {log.ip_address || 'localhost'}
                  </td>
                  <td style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                    {new Date(log.created_at).toLocaleString()}
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <button
                      onClick={() => setDetailLog(log)}
                      className="btn btn-secondary btn-sm"
                      title="Ver diferencias (JSON diff)"
                    >
                      <Eye size={14} />
                      <span>Ver Datos</span>
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Paginación */}
        <div className="pagination">
          <span>
            Página {page} de {Math.max(1, totalPages)} ({totalCount} eventos registrados)
          </span>
          <div className="pagination-controls">
            <button
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page <= 1 || loading}
              className="btn btn-secondary btn-sm"
            >
              <ChevronLeft size={16} />
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={page >= totalPages || loading}
              className="btn btn-secondary btn-sm"
            >
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      </div>

      {/* Modal Ver Detalle de Auditoría */}
      <Modal
        isOpen={detailLog !== null}
        onClose={() => setDetailLog(null)}
        title={`Detalle de Auditoría: Evento #${detailLog?.id}`}
        size="large"
      >
        {detailLog && (
          <div>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: '14px',
                marginBottom: '20px',
                padding: '16px',
                background: 'rgba(255, 255, 255, 0.02)',
                borderRadius: '10px',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Administrador:</span>
                <p style={{ fontWeight: 600 }}>{detailLog.admin_email || 'Sistema'}</p>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Acción:</span>
                <p style={{ fontWeight: 700, color: 'var(--accent-cyan)' }}>{detailLog.action}</p>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Módulo / Tabla:</span>
                <p style={{ fontWeight: 600 }}>{detailLog.entity_type} (ID: {detailLog.entity_id})</p>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>User Agent:</span>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {detailLog.user_agent || 'N/A'}
                </p>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: detailLog.old_values ? '1fr 1fr' : '1fr', gap: '16px' }}>
              {detailLog.old_values && (
                <div>
                  <h4 style={{ fontSize: '0.85rem', fontWeight: 700, color: '#f87171', marginBottom: '8px' }}>
                    Valores Anteriores (Pre-Modificación)
                  </h4>
                  <pre
                    style={{
                      background: 'rgba(0, 0, 0, 0.4)',
                      padding: '12px',
                      borderRadius: '8px',
                      border: '1px solid rgba(239, 68, 68, 0.2)',
                      fontSize: '0.78rem',
                      overflowX: 'auto',
                      maxHeight: '260px',
                    }}
                  >
                    {JSON.stringify(detailLog.old_values, null, 2)}
                  </pre>
                </div>
              )}

              <div>
                <h4 style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--accent-green)', marginBottom: '8px' }}>
                  Valores Nuevos / Modificados
                </h4>
                <pre
                  style={{
                    background: 'rgba(0, 0, 0, 0.4)',
                    padding: '12px',
                    borderRadius: '8px',
                    border: '1px solid rgba(16, 185, 129, 0.2)',
                    fontSize: '0.78rem',
                    overflowX: 'auto',
                    maxHeight: '260px',
                  }}
                >
                  {JSON.stringify(detailLog.new_values, null, 2)}
                </pre>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
