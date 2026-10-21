import React, { useEffect, useState } from 'react';
import { apiRequest } from '../api/client';
import type { Equipment } from '../types';
import { useToast } from '../context/ToastContext';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import {
  Dumbbell,
  Plus,
  Edit2,
  Trash2,
  Power,
  RefreshCw,
  Search,
  Activity,
  Image as ImageIcon,
} from 'lucide-react';

export const EquipmentView: React.FC = () => {
  const { showToast } = useToast();

  const [equipmentList, setEquipmentList] = useState<Equipment[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('all');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<Equipment | null>(null);
  const [deleteItem, setDeleteItem] = useState<Equipment | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    category: 'free_weights' as Equipment['category'],
    imageUrl: '',
    isActive: true,
  });

  const fetchEquipment = async () => {
    setLoading(true);
    try {
      const params: any = {};
      if (search) params.search = search;
      if (categoryFilter !== 'all') params.category = categoryFilter;

      const res = await apiRequest<Equipment[]>('/equipment', { params });
      setEquipmentList(res.data);
    } catch (err: any) {
      showToast('error', err.message || 'Error al obtener implementos.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEquipment();
  }, [search, categoryFilter]);

  const handleOpenCreate = () => {
    setFormData({
      name: '',
      description: '',
      category: 'free_weights',
      imageUrl: '',
      isActive: true,
    });
    setIsCreateOpen(true);
  };

  const handleOpenEdit = (item: Equipment) => {
    setEditingItem(item);
    setFormData({
      name: item.name,
      description: item.description || '',
      category: item.category,
      imageUrl: item.image_url || '',
      isActive: item.is_active,
    });
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionLoading(true);
    try {
      if (editingItem) {
        await apiRequest(`/equipment/${editingItem.id}`, {
          method: 'PUT',
          body: formData,
        });
        showToast('success', 'Implemento actualizado correctamente.');
        setEditingItem(null);
      } else {
        await apiRequest('/equipment', {
          method: 'POST',
          body: formData,
        });
        showToast('success', 'Implemento registrado.');
        setIsCreateOpen(false);
      }
      fetchEquipment();
    } catch (err: any) {
      showToast('error', err.message || 'Error al guardar implemento.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleActive = async (item: Equipment) => {
    try {
      await apiRequest(`/equipment/${item.id}/toggle-active`, {
        method: 'PATCH',
        body: { isActive: !item.is_active },
      });
      showToast('success', `Implemento ${!item.is_active ? 'activado' : 'desactivado'}.`);
      fetchEquipment();
    } catch (err: any) {
      showToast('error', err.message || 'Error al cambiar estado.');
    }
  };

  const handleDelete = async () => {
    if (!deleteItem) return;
    setActionLoading(true);
    try {
      await apiRequest(`/equipment/${deleteItem.id}`, { method: 'DELETE' });
      showToast('success', 'Implemento eliminado correctamente.');
      setDeleteItem(null);
      fetchEquipment();
    } catch (err: any) {
      showToast('error', err.message || 'Error al eliminar implemento.');
    } finally {
      setActionLoading(false);
    }
  };

  const getCategoryLabel = (cat: string) => {
    switch (cat) {
      case 'free_weights': return 'Pesas Libres';
      case 'machines': return 'Máquinas Guiadas';
      case 'bodyweight': return 'Autocarga / Calistenia';
      case 'accessories': return 'Accesorios';
      case 'resistance': return 'Resistencia Elástica';
      default: return cat;
    }
  };

  return (
    <div className="page-container">
      {/* Encabezado */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#ffffff' }}>Implementos y Equipos</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            Inventario deportivo requerido para la ejecución de los entrenamientos ({equipmentList.length} equipos).
          </p>
        </div>
        <button onClick={handleOpenCreate} className="btn btn-primary">
          <Plus size={18} />
          <span>Nuevo Implemento</span>
        </button>
      </div>

      {/* Filtros */}
      <div className="filter-bar">
        <div className="search-input-wrapper">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            className="form-input"
            placeholder="Buscar por nombre de equipo..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <select
            className="form-select"
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            style={{ width: '180px' }}
          >
            <option value="all">Todas las Categorías</option>
            <option value="free_weights">Pesas Libres</option>
            <option value="machines">Máquinas Guiadas</option>
            <option value="bodyweight">Autocarga / Calistenia</option>
            <option value="accessories">Accesorios</option>
            <option value="resistance">Resistencia Elástica</option>
          </select>

          <button onClick={fetchEquipment} className="btn btn-secondary btn-sm" title="Recargar">
            <RefreshCw size={16} className={loading ? 'spin-animation' : ''} />
          </button>
        </div>
      </div>

      {/* Grid de Cards de Implementos */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>
          <RefreshCw size={28} className="spin-animation" style={{ color: 'var(--accent-cyan)', marginBottom: '12px' }} />
          <p>Cargando catálogo de implementos...</p>
        </div>
      ) : equipmentList.length === 0 ? (
        <div className="empty-state">
          <Dumbbell size={48} className="empty-state-icon" />
          <h4 className="empty-state-title">No hay implementos encontrados</h4>
          <p className="empty-state-desc">Comienza registrando mancuernas, barras o bancos en la plataforma.</p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '20px' }}>
          {equipmentList.map((item) => (
            <div
              key={item.id}
              className="card"
              style={{
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                padding: '0',
              }}
            >
              {/* Imagen Ilustrativa */}
              <div
                style={{
                  height: '130px',
                  width: '100%',
                  background: item.image_url
                    ? `linear-gradient(180deg, rgba(0,0,0,0.1) 0%, rgba(17,22,34,0.95) 100%), url(${item.image_url}) center/cover`
                    : 'linear-gradient(135deg, #1e293b, #0f172a)',
                  display: 'flex',
                  alignItems: 'flex-end',
                  padding: '16px 20px',
                }}
              >
                <div>
                  <span className="badge badge-purple" style={{ marginBottom: '6px', fontSize: '0.68rem' }}>
                    {getCategoryLabel(item.category)}
                  </span>
                  <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#ffffff' }}>{item.name}</h3>
                </div>
              </div>

              {/* Contenido */}
              <div style={{ padding: '18px 20px', flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.84rem', lineHeight: '1.5', marginBottom: '16px' }}>
                  {item.description || 'Sin descripción detallada.'}
                </p>

                <div>
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '8px 12px',
                      background: 'rgba(255, 255, 255, 0.02)',
                      borderRadius: '8px',
                      marginBottom: '16px',
                      border: '1px solid var(--border-subtle)',
                    }}
                  >
                    <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Activity size={13} color="var(--accent-cyan)" /> Ejercicios que lo usan:
                    </span>
                    <span style={{ fontWeight: 700, color: 'var(--accent-cyan)' }}>
                      {item.exercises_count || 0}
                    </span>
                  </div>

                  {/* Acciones */}
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <span className={`badge ${item.is_active ? 'badge-success' : 'badge-danger'}`}>
                      {item.is_active ? 'Activo' : 'Inactivo'}
                    </span>

                    <div style={{ display: 'flex', gap: '6px' }}>
                      <button
                        onClick={() => handleOpenEdit(item)}
                        className="btn btn-secondary btn-sm"
                        title="Editar implemento"
                      >
                        <Edit2 size={14} />
                      </button>
                      <button
                        onClick={() => handleToggleActive(item)}
                        className="btn btn-secondary btn-sm"
                        title={item.is_active ? 'Desactivar' : 'Activar'}
                        style={{ color: item.is_active ? '#f59e0b' : '#10b981' }}
                      >
                        <Power size={14} />
                      </button>
                      <button
                        onClick={() => setDeleteItem(item)}
                        className="btn btn-danger btn-sm"
                        title="Eliminar implemento"
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
        isOpen={isCreateOpen || editingItem !== null}
        onClose={() => {
          setIsCreateOpen(false);
          setEditingItem(null);
        }}
        title={editingItem ? `Editar Implemento: ${editingItem.name}` : 'Registrar Nuevo Implemento'}
      >
        <form onSubmit={handleSave}>
          <div className="form-group">
            <label className="form-label">Nombre del Implemento *</label>
            <input
              type="text"
              className="form-input"
              required
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="Ej. Barra Olímpica de 20kg"
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Categoría *</label>
              <select
                className="form-select"
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value as any })}
              >
                <option value="free_weights">Pesas Libres</option>
                <option value="machines">Máquinas Guiadas</option>
                <option value="bodyweight">Autocarga / Calistenia</option>
                <option value="accessories">Accesorios</option>
                <option value="resistance">Resistencia Elástica</option>
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

          <div className="form-group">
            <label className="form-label">URL de Imagen</label>
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
            <label className="form-label">Descripción y Usos Recomendados</label>
            <textarea
              className="form-textarea"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Especificaciones de peso, material, ajuste o compatibilidad..."
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '20px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => {
                setIsCreateOpen(false);
                setEditingItem(null);
              }}
              disabled={actionLoading}
            >
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={actionLoading}>
              {actionLoading ? 'Guardando...' : editingItem ? 'Actualizar Implemento' : 'Registrar Implemento'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Confirmación Eliminar */}
      <ConfirmDialog
        isOpen={deleteItem !== null}
        title="Eliminar Implemento"
        message={`¿Estás seguro de eliminar "${deleteItem?.name}"? Esta acción solo se ejecutará si no hay ejercicios dependientes en el sistema.`}
        confirmLabel="Eliminar Definitivamente"
        loading={actionLoading}
        onConfirm={handleDelete}
        onCancel={() => setDeleteItem(null)}
      />
    </div>
  );
};
