import React, { useEffect, useState, useCallback } from 'react';
import { apiRequest } from '../api/client';
import type { User } from '../types';
import { useToast } from '../context/ToastContext';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import {
  Search,
  Plus,
  Eye,
  Edit2,
  Trash2,
  Power,
  UserCheck,
  UserX,
  ChevronLeft,
  ChevronRight,
  RefreshCw,
} from 'lucide-react';

export const UsersView: React.FC = () => {
  const { showToast } = useToast();

  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [isActiveFilter, setIsActiveFilter] = useState<string>('all');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalCount, setTotalCount] = useState(0);

  // Modales
  const [detailUser, setDetailUser] = useState<User | null>(null);
  const [editingUser, setEditingUser] = useState<Partial<User> | null>(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [deleteConfirmUser, setDeleteConfirmUser] = useState<User | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  // Form State
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    birthDate: '',
    gender: 'unspecified',
    heightCm: '',
    weightKg: '',
    fitnessGoal: 'hypertrophy',
    isActive: true,
  });

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const params: any = { page, limit: 8 };
      if (search) params.search = search;
      if (isActiveFilter !== 'all') params.isActive = isActiveFilter === 'active';

      const res = await apiRequest<User[]>('/users', { params });
      setUsers(res.data);
      if (res.pagination) {
        setTotalPages(res.pagination.totalPages);
        setTotalCount(res.pagination.total);
      }
    } catch (err: any) {
      showToast('error', err.message || 'Error al obtener usuarios.');
    } finally {
      setLoading(false);
    }
  }, [page, search, isActiveFilter, showToast]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleOpenCreate = () => {
    setFormData({
      fullName: '',
      email: '',
      phone: '',
      birthDate: '',
      gender: 'unspecified',
      heightCm: '',
      weightKg: '',
      fitnessGoal: 'hypertrophy',
      isActive: true,
    });
    setIsCreateOpen(true);
  };

  const handleOpenEdit = (user: User) => {
    setEditingUser(user);
    setFormData({
      fullName: user.full_name,
      email: user.email,
      phone: user.phone || '',
      birthDate: user.birth_date ? user.birth_date.split('T')[0] : '',
      gender: user.gender || 'unspecified',
      heightCm: user.height_cm ? String(user.height_cm) : '',
      weightKg: user.weight_kg ? String(user.weight_kg) : '',
      fitnessGoal: user.fitness_goal || 'hypertrophy',
      isActive: user.is_active,
    });
  };

  const handleViewDetail = async (user: User) => {
    try {
      const res = await apiRequest<User>(`/users/${user.id}`);
      setDetailUser(res.data);
    } catch (err: any) {
      showToast('error', err.message || 'Error al cargar detalle del usuario.');
    }
  };

  const handleSaveUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionLoading(true);
    try {
      const payload: any = {
        fullName: formData.fullName,
        email: formData.email,
        phone: formData.phone || undefined,
        birthDate: formData.birthDate || undefined,
        gender: formData.gender,
        fitnessGoal: formData.fitnessGoal,
        isActive: formData.isActive,
      };
      if (formData.heightCm) payload.heightCm = parseFloat(formData.heightCm);
      if (formData.weightKg) payload.weightKg = parseFloat(formData.weightKg);

      if (editingUser) {
        await apiRequest(`/users/${editingUser.id}`, { method: 'PUT', body: payload });
        showToast('success', 'Usuario actualizado correctamente.');
        setEditingUser(null);
      } else {
        await apiRequest('/users', { method: 'POST', body: payload });
        showToast('success', 'Usuario creado exitosamente.');
        setIsCreateOpen(false);
      }
      fetchUsers();
    } catch (err: any) {
      showToast('error', err.message || 'Error al guardar el usuario.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleActive = async (user: User) => {
    try {
      await apiRequest(`/users/${user.id}/toggle-active`, {
        method: 'PATCH',
        body: { isActive: !user.is_active },
      });
      showToast('success', `Usuario ${!user.is_active ? 'activado' : 'desactivado'}.`);
      fetchUsers();
    } catch (err: any) {
      showToast('error', err.message || 'Error al cambiar estado.');
    }
  };

  const handleDelete = async (anonymize: boolean) => {
    if (!deleteConfirmUser) return;
    setActionLoading(true);
    try {
      await apiRequest(`/users/${deleteConfirmUser.id}?anonymize=${anonymize}`, {
        method: 'DELETE',
      });
      showToast('success', anonymize ? 'Usuario anonimizado y desactivado.' : 'Usuario eliminado (soft-delete).');
      setDeleteConfirmUser(null);
      fetchUsers();
    } catch (err: any) {
      showToast('error', err.message || 'Error al eliminar usuario.');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="page-container">
      {/* Header de Sección */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#ffffff' }}>Gestión de Usuarios</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            Control de cuentas, objetivos físicos, perfiles y suscripciones vigentes ({totalCount} registros).
          </p>
        </div>
        <button onClick={handleOpenCreate} className="btn btn-primary">
          <Plus size={18} />
          <span>Nuevo Usuario</span>
        </button>
      </div>

      {/* Filtros y Búsqueda */}
      <div className="filter-bar">
        <div className="search-input-wrapper">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            className="form-input"
            placeholder="Buscar por nombre, correo o teléfono..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(1);
            }}
          />
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <select
            className="form-select"
            value={isActiveFilter}
            onChange={(e) => {
              setIsActiveFilter(e.target.value);
              setPage(1);
            }}
            style={{ width: '160px' }}
          >
            <option value="all">Todos los estados</option>
            <option value="active">Solo Activos</option>
            <option value="inactive">Solo Inactivos</option>
          </select>

          <button onClick={fetchUsers} className="btn btn-secondary btn-sm" title="Recargar">
            <RefreshCw size={16} className={loading ? 'spin-animation' : ''} />
          </button>
        </div>
      </div>

      {/* Tabla de Usuarios */}
      <div className="table-container">
        <table className="custom-table">
          <thead>
            <tr>
              <th>Usuario</th>
              <th>Contacto</th>
              <th>Objetivo</th>
              <th>Suscripción</th>
              <th>Estado</th>
              <th>Registro</th>
              <th style={{ textAlign: 'right' }}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                  <RefreshCw size={24} className="spin-animation" style={{ color: 'var(--accent-cyan)', marginBottom: '8px' }} />
                  <p>Cargando usuarios desde PostgreSQL...</p>
                </td>
              </tr>
            ) : users.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                  No se encontraron usuarios con los criterios de búsqueda aplicados.
                </td>
              </tr>
            ) : (
              users.map((u) => (
                <tr key={u.id}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div
                        style={{
                          width: '36px',
                          height: '36px',
                          borderRadius: '50%',
                          background: 'rgba(56, 189, 248, 0.1)',
                          color: 'var(--accent-cyan)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontWeight: 700,
                          fontSize: '0.85rem',
                        }}
                      >
                        {u.full_name[0]?.toUpperCase()}
                      </div>
                      <div>
                        <p style={{ fontWeight: 600, color: '#ffffff' }}>{u.full_name}</p>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>ID: #{u.id}</span>
                      </div>
                    </div>
                  </td>
                  <td>
                    <p style={{ fontSize: '0.85rem' }}>{u.email}</p>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{u.phone || 'Sin teléfono'}</span>
                  </td>
                  <td>
                    <span className="badge badge-info" style={{ textTransform: 'capitalize' }}>
                      {u.fitness_goal || 'General'}
                    </span>
                  </td>
                  <td>
                    {u.active_plan_name ? (
                      <span className="badge badge-success">{u.active_plan_name}</span>
                    ) : (
                      <span className="badge" style={{ background: 'rgba(255, 255, 255, 0.05)', color: 'var(--text-muted)' }}>
                        Sin suscripción
                      </span>
                    )}
                  </td>
                  <td>
                    <span className={`badge ${u.is_active ? 'badge-success' : 'badge-danger'}`}>
                      {u.is_active ? (
                        <>
                          <UserCheck size={12} /> Activo
                        </>
                      ) : (
                        <>
                          <UserX size={12} /> Inactivo
                        </>
                      )}
                    </span>
                  </td>
                  <td style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                    {new Date(u.created_at).toLocaleDateString()}
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '6px' }}>
                      <button
                        onClick={() => handleViewDetail(u)}
                        className="btn btn-secondary btn-sm"
                        title="Ver ficha técnica y suscripciones"
                      >
                        <Eye size={14} />
                      </button>
                      <button
                        onClick={() => handleOpenEdit(u)}
                        className="btn btn-secondary btn-sm"
                        title="Editar información"
                      >
                        <Edit2 size={14} />
                      </button>
                      <button
                        onClick={() => handleToggleActive(u)}
                        className="btn btn-secondary btn-sm"
                        title={u.is_active ? 'Desactivar cuenta' : 'Activar cuenta'}
                        style={{ color: u.is_active ? '#f59e0b' : '#10b981' }}
                      >
                        <Power size={14} />
                      </button>
                      <button
                        onClick={() => setDeleteConfirmUser(u)}
                        className="btn btn-danger btn-sm"
                        title="Eliminar o anonimizar"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Paginación */}
        <div className="pagination">
          <span>
            Página {page} de {Math.max(1, totalPages)} ({totalCount} usuarios)
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

      {/* Modal Crear / Editar */}
      <Modal
        isOpen={isCreateOpen || editingUser !== null}
        onClose={() => {
          setIsCreateOpen(false);
          setEditingUser(null);
        }}
        title={editingUser ? `Editar Usuario: ${editingUser.full_name}` : 'Crear Nuevo Usuario Atleta'}
      >
        <form onSubmit={handleSaveUser}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Nombre Completo *</label>
              <input
                type="text"
                className="form-input"
                required
                value={formData.fullName}
                onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                placeholder="Ej. Valeria Rivas"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Correo Electrónico *</label>
              <input
                type="email"
                className="form-input"
                required
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                placeholder="ejemplo@email.com"
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Teléfono</label>
              <input
                type="text"
                className="form-input"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                placeholder="+593 98 123 4567"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Fecha de Nacimiento</label>
              <input
                type="date"
                className="form-input"
                value={formData.birthDate}
                onChange={(e) => setFormData({ ...formData, birthDate: e.target.value })}
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Género</label>
              <select
                className="form-select"
                value={formData.gender}
                onChange={(e) => setFormData({ ...formData, gender: e.target.value })}
              >
                <option value="male">Masculino</option>
                <option value="female">Femenino</option>
                <option value="other">Otro</option>
                <option value="unspecified">No especificado</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Estatura (cm)</label>
              <input
                type="number"
                step="0.5"
                className="form-input"
                value={formData.heightCm}
                onChange={(e) => setFormData({ ...formData, heightCm: e.target.value })}
                placeholder="175"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Peso (kg)</label>
              <input
                type="number"
                step="0.1"
                className="form-input"
                value={formData.weightKg}
                onChange={(e) => setFormData({ ...formData, weightKg: e.target.value })}
                placeholder="74.5"
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Objetivo de Fitness</label>
              <select
                className="form-select"
                value={formData.fitnessGoal}
                onChange={(e) => setFormData({ ...formData, fitnessGoal: e.target.value })}
              >
                <option value="hypertrophy">Hipertrofia Muscular</option>
                <option value="strength">Fuerza y Potencia</option>
                <option value="fat_loss">Pérdida de Grasa</option>
                <option value="endurance">Resistencia Cardiovascular</option>
                <option value="toning">Tonificación y Salud General</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Estado</label>
              <select
                className="form-select"
                value={formData.isActive ? 'true' : 'false'}
                onChange={(e) => setFormData({ ...formData, isActive: e.target.value === 'true' })}
              >
                <option value="true">Activo</option>
                <option value="false">Inactivo</option>
              </select>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '20px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => {
                setIsCreateOpen(false);
                setEditingUser(null);
              }}
              disabled={actionLoading}
            >
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={actionLoading}>
              {actionLoading ? 'Guardando en BD...' : editingUser ? 'Actualizar Usuario' : 'Registrar Usuario'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal Ver Detalle */}
      <Modal
        isOpen={detailUser !== null}
        onClose={() => setDetailUser(null)}
        title={`Ficha del Usuario: ${detailUser?.full_name}`}
      >
        {detailUser && (
          <div>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: '14px',
                marginBottom: '24px',
                padding: '16px',
                background: 'rgba(255, 255, 255, 0.02)',
                borderRadius: '12px',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Correo:</span>
                <p style={{ fontWeight: 600 }}>{detailUser.email}</p>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Teléfono:</span>
                <p style={{ fontWeight: 600 }}>{detailUser.phone || 'No registrado'}</p>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Métricas Físicas:</span>
                <p style={{ fontWeight: 600 }}>
                  {detailUser.height_cm ? `${detailUser.height_cm} cm` : '—'} /{' '}
                  {detailUser.weight_kg ? `${detailUser.weight_kg} kg` : '—'}
                </p>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Objetivo:</span>
                <p style={{ fontWeight: 600, textTransform: 'capitalize' }}>{detailUser.fitness_goal || 'General'}</p>
              </div>
            </div>

            <h4 style={{ fontSize: '0.95rem', fontWeight: 700, marginBottom: '12px', color: '#ffffff' }}>
              Historial de Suscripciones
            </h4>
            {detailUser.subscriptions && detailUser.subscriptions.length > 0 ? (
              <div className="table-container">
                <table className="custom-table">
                  <thead>
                    <tr>
                      <th>Plan</th>
                      <th>Ciclo</th>
                      <th>Vigencia</th>
                      <th>Precio Pagado</th>
                      <th>Estado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {detailUser.subscriptions.map((s) => (
                      <tr key={s.id}>
                        <td style={{ fontWeight: 600 }}>{s.plan_name}</td>
                        <td style={{ textTransform: 'capitalize' }}>{s.billing_cycle}</td>
                        <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                          {new Date(s.start_date).toLocaleDateString()} — {new Date(s.end_date).toLocaleDateString()}
                        </td>
                        <td>${parseFloat(s.price_paid).toFixed(2)} {s.currency}</td>
                        <td>
                          <span className={`badge ${s.status === 'active' ? 'badge-success' : 'badge-warning'}`}>
                            {s.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                El usuario no posee suscripciones registradas actualmente.
              </p>
            )}
          </div>
        )}
      </Modal>

      {/* Diálogo Eliminar / Anonimizar */}
      <ConfirmDialog
        isOpen={deleteConfirmUser !== null}
        title="Opciones de Baja de Usuario"
        message={`¿Deseas dar de baja a "${deleteConfirmUser?.full_name}"? Puedes realizar un borrado lógico (soft-delete) o anonimizar completamente sus datos personales protegiendo el historial de entrenamientos.`}
        confirmLabel="Eliminar (Soft-delete)"
        cancelLabel="Cancelar"
        loading={actionLoading}
        onConfirm={() => handleDelete(false)}
        onCancel={() => setDeleteConfirmUser(null)}
      />
    </div>
  );
};
