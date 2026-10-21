import React, { useEffect, useState } from 'react';
import { apiRequest } from '../api/client';
import type { MuscleGroup } from '../types';
import { useToast } from '../context/ToastContext';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import {
  Layers,
  Plus,
  Edit2,
  Trash2,
  Power,
  RefreshCw,
  Search,
  Activity,
  Image as ImageIcon,
} from 'lucide-react';

export const MusclesView: React.FC = () => {
  const { showToast } = useToast();

  const [muscles, setMuscles] = useState<MuscleGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingMuscle, setEditingMuscle] = useState<MuscleGroup | null>(null);
  const [deleteMuscle, setDeleteMuscle] = useState<MuscleGroup | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    imageUrl: '',
    isActive: true,
  });

  const fetchMuscles = async () => {
    setLoading(true);
    try {
      const res = await apiRequest<MuscleGroup[]>('/muscles', {
        params: search ? { search } : undefined,
      });
      setMuscles(res.data);
    } catch (err: any) {
      showToast('error', err.message || 'Error al obtener grupos musculares.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMuscles();
  }, [search]);

  const handleOpenCreate = () => {
    setFormData({ name: '', description: '', imageUrl: '', isActive: true });
    setIsCreateOpen(true);
  };

  const handleOpenEdit = (m: MuscleGroup) => {
    setEditingMuscle(m);
    setFormData({
      name: m.name,
      description: m.description || '',
      imageUrl: m.image_url || '',
      isActive: m.is_active,
    });
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionLoading(true);
    try {
      if (editingMuscle) {
        await apiRequest(`/muscles/${editingMuscle.id}`, {
          method: 'PUT',
          body: formData,
        });
        showToast('success', 'Grupo muscular actualizado con éxito.');
        setEditingMuscle(null);
      } else {
        await apiRequest('/muscles', {
          method: 'POST',
          body: formData,
        });
        showToast('success', 'Grupo muscular creado.');
        setIsCreateOpen(false);
      }
      fetchMuscles();
    } catch (err: any) {
      showToast('error', err.message || 'Error al guardar grupo muscular.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleActive = async (m: MuscleGroup) => {
    try {
      await apiRequest(`/muscles/${m.id}/toggle-active`, {
        method: 'PATCH',
        body: { isActive: !m.is_active },
      });
      showToast('success', `Grupo muscular ${!m.is_active ? 'activado' : 'desactivado'}.`);
      fetchMuscles();
    } catch (err: any) {
      showToast('error', err.message || 'Error al actualizar estado.');
    }
  };

  const handleDelete = async () => {
    if (!deleteMuscle) return;
    setActionLoading(true);
    try {
      await apiRequest(`/muscles/${deleteMuscle.id}`, { method: 'DELETE' });
      showToast('success', 'Grupo muscular eliminado.');
      setDeleteMuscle(null);
      fetchMuscles();
    } catch (err: any) {
      showToast('error', err.message || 'Error al eliminar grupo muscular.');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="page-container">
      {/* Encabezado */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#ffffff' }}>Grupos Musculares</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            Anatomía deportiva y segmentación para categorización de ejercicios ({muscles.length} grupos).
          </p>
        </div>
        <button onClick={handleOpenCreate} className="btn btn-primary">
          <Plus size={18} />
          <span>Nuevo Grupo Muscular</span>
        </button>
      </div>

      {/* Buscador */}
      <div className="filter-bar">
        <div className="search-input-wrapper">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            className="form-input"
            placeholder="Buscar grupo muscular..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <button onClick={fetchMuscles} className="btn btn-secondary btn-sm" title="Recargar">
          <RefreshCw size={16} className={loading ? 'spin-animation' : ''} />
        </button>
      </div>

      {/* Grid de Cards de Grupos Musculares */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>
          <RefreshCw size={28} className="spin-animation" style={{ color: 'var(--accent-cyan)', marginBottom: '12px' }} />
          <p>Cargando grupos musculares...</p>
        </div>
      ) : muscles.length === 0 ? (
        <div className="empty-state">
          <Layers size={48} className="empty-state-icon" />
          <h4 className="empty-state-title">No hay grupos musculares</h4>
          <p className="empty-state-desc">Comienza creando el primer grupo muscular o ajusta los términos de búsqueda.</p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '20px' }}>
          {muscles.map((m) => (
            <div
              key={m.id}
              className="card"
              style={{
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                padding: '0',
                position: 'relative',
              }}
            >
              {/* Imagen Ilustrativa */}
              <div
                style={{
                  height: '140px',
                  width: '100%',
                  background: m.image_url
                    ? `linear-gradient(180deg, rgba(0,0,0,0.1) 0%, rgba(17,22,34,0.95) 100%), url(${m.image_url}) center/cover`
                    : 'linear-gradient(135deg, #1e293b, #0f172a)',
                  display: 'flex',
                  alignItems: 'flex-end',
                  padding: '16px 20px',
                }}
              >
                <div>
                  <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#ffffff' }}>{m.name}</h3>
                </div>
              </div>

              {/* Contenido */}
              <div style={{ padding: '20px', flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.86rem', lineHeight: '1.5', marginBottom: '16px' }}>
                  {m.description || 'Sin descripción detallada.'}
                </p>

                <div>
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '10px 14px',
                      background: 'rgba(255, 255, 255, 0.02)',
                      borderRadius: '8px',
                      marginBottom: '16px',
                      border: '1px solid var(--border-subtle)',
                    }}
                  >
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Activity size={14} color="var(--accent-cyan)" /> Ejercicios asociados:
                    </span>
                    <span style={{ fontWeight: 700, color: 'var(--accent-cyan)' }}>
                      {m.exercises_count || 0}
                    </span>
                  </div>

                  {/* Acciones */}
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <span className={`badge ${m.is_active ? 'badge-success' : 'badge-danger'}`}>
                      {m.is_active ? 'Activo' : 'Inactivo'}
                    </span>

                    <div style={{ display: 'flex', gap: '6px' }}>
                      <button
                        onClick={() => handleOpenEdit(m)}
                        className="btn btn-secondary btn-sm"
                        title="Editar grupo"
                      >
                        <Edit2 size={14} />
                      </button>
                      <button
                        onClick={() => handleToggleActive(m)}
                        className="btn btn-secondary btn-sm"
                        title={m.is_active ? 'Desactivar' : 'Activar'}
                        style={{ color: m.is_active ? '#f59e0b' : '#10b981' }}
                      >
                        <Power size={14} />
                      </button>
                      <button
                        onClick={() => setDeleteMuscle(m)}
                        className="btn btn-danger btn-sm"
                        title="Eliminar grupo"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal Crear / Editar */}
      <Modal
        isOpen={isCreateOpen || editingMuscle !== null}
        onClose={() => {
          setIsCreateOpen(false);
          setEditingMuscle(null);
        }}
        title={editingMuscle ? `Editar Grupo: ${editingMuscle.name}` : 'Crear Grupo Muscular'}
      >
        <form onSubmit={handleSave}>
          <div className="form-group">
            <label className="form-label">Nombre del Grupo Muscular *</label>
            <input
              type="text"
              className="form-input"
              required
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="Ej. Deltoides (Hombros)"
            />
          </div>

          <div className="form-group">
            <label className="form-label">URL de Imagen Ilustrativa</label>
            <div style={{ position: 'relative' }}>
              <ImageIcon
                size={18}
                color="var(--text-muted)"
                style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }}
              />
              <input
                type="url"
                className="form-input"
                style={{ paddingLeft: '40px' }}
                value={formData.imageUrl}
                onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                placeholder="https://images.unsplash.com/photo-..."
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Descripción Anatómica</label>
            <textarea
              className="form-textarea"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Detalla la función anatómica, inserciones o rol en los ejercicios de empuje/tracción..."
            />
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

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '20px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => {
                setIsCreateOpen(false);
                setEditingMuscle(null);
              }}
              disabled={actionLoading}
            >
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={actionLoading}>
              {actionLoading ? 'Guardando...' : editingMuscle ? 'Actualizar Grupo' : 'Crear Grupo'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Confirmación Eliminar */}
      <ConfirmDialog
        isOpen={deleteMuscle !== null}
        title="Eliminar Grupo Muscular"
        message={`¿Estás seguro de eliminar el grupo muscular "${deleteMuscle?.name}"? Esta acción solo es permitida si no existen ejercicios vinculados.`}
        confirmLabel="Eliminar Definitivamente"
        loading={actionLoading}
        onConfirm={handleDelete}
        onCancel={() => setDeleteMuscle(null)}
      />
    </div>
  );
};
