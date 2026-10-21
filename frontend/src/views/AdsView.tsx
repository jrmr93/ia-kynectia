import React, { useEffect, useState } from 'react';
import { apiRequest } from '../api/client';
import type { Ad } from '../types';
import { useToast } from '../context/ToastContext';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import {
  Megaphone,
  Plus,
  Edit2,
  Trash2,
  Power,
  RefreshCw,
  ExternalLink,
  Eye,
  MousePointer,
  Calendar,
} from 'lucide-react';

export const AdsView: React.FC = () => {
  const { showToast } = useToast();

  const [ads, setAds] = useState<Ad[]>([]);
  const [loading, setLoading] = useState(true);
  const [placementFilter, setPlacementFilter] = useState('all');
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingAd, setEditingAd] = useState<Ad | null>(null);
  const [deleteAd, setDeleteAd] = useState<Ad | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    mediaUrl: '',
    destinationUrl: '',
    placement: 'banner_home' as Ad['placement'],
    priority: 1,
    startsAt: new Date().toISOString().split('T')[0],
    endsAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    isActive: true,
  });

  const fetchAds = async () => {
    setLoading(true);
    try {
      const params: any = {};
      if (placementFilter !== 'all') params.placement = placementFilter;

      const res = await apiRequest<Ad[]>('/ads', { params });
      setAds(res.data);
    } catch (err: any) {
      showToast('error', err.message || 'Error al obtener anuncios.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAds();
  }, [placementFilter]);

  const handleOpenCreate = () => {
    setEditingAd(null);
    setFormData({
      title: '',
      description: '',
      mediaUrl: '',
      destinationUrl: '',
      placement: 'banner_home',
      priority: 1,
      startsAt: new Date().toISOString().split('T')[0],
      endsAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      isActive: true,
    });
    setIsFormOpen(true);
  };

  const handleOpenEdit = (ad: Ad) => {
    setEditingAd(ad);
    setFormData({
      title: ad.title,
      description: ad.description || '',
      mediaUrl: ad.media_url,
      destinationUrl: ad.destination_url || '',
      placement: ad.placement,
      priority: ad.priority,
      startsAt: ad.starts_at.split('T')[0],
      endsAt: ad.ends_at.split('T')[0],
      isActive: ad.is_active,
    });
    setIsFormOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionLoading(true);
    try {
      if (editingAd) {
        await apiRequest(`/ads/${editingAd.id}`, { method: 'PUT', body: formData });
        showToast('success', 'Anuncio actualizado.');
      } else {
        await apiRequest('/ads', { method: 'POST', body: formData });
        showToast('success', 'Anuncio creado con éxito.');
      }
      setIsFormOpen(false);
      fetchAds();
    } catch (err: any) {
      showToast('error', err.message || 'Error al guardar el anuncio.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleActive = async (ad: Ad) => {
    try {
      await apiRequest(`/ads/${ad.id}/toggle-active`, {
        method: 'PATCH',
        body: { isActive: !ad.is_active },
      });
      showToast('success', `Anuncio ${!ad.is_active ? 'activado' : 'desactivado'}.`);
      fetchAds();
    } catch (err: any) {
      showToast('error', err.message || 'Error al cambiar estado.');
    }
  };

  const handleDelete = async () => {
    if (!deleteAd) return;
    setActionLoading(true);
    try {
      await apiRequest(`/ads/${deleteAd.id}`, { method: 'DELETE' });
      showToast('success', 'Anuncio eliminado correctamente.');
      setDeleteAd(null);
      fetchAds();
    } catch (err: any) {
      showToast('error', err.message || 'Error al eliminar anuncio.');
    } finally {
      setActionLoading(false);
    }
  };

  const getPlacementLabel = (p: string) => {
    switch (p) {
      case 'banner_home': return 'Banner Principal Inicio';
      case 'interstitial_workout': return 'Intersticial Post-Rutina';
      case 'sidebar': return 'Barra Lateral';
      case 'footer': return 'Pie de Aplicación';
      default: return p;
    }
  };

  return (
    <div className="page-container">
      {/* Encabezado */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#ffffff' }}>Gestión de Publicidad</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            Control de banners, marcas patrocinadas, fechas de vigencia y analíticas de impresiones/clics ({ads.length} anuncios).
          </p>
        </div>
        <button onClick={handleOpenCreate} className="btn btn-primary">
          <Plus size={18} />
          <span>Nuevo Anuncio</span>
        </button>
      </div>

      {/* Filtros */}
      <div className="filter-bar">
        <select
          className="form-select"
          value={placementFilter}
          onChange={(e) => setPlacementFilter(e.target.value)}
          style={{ width: '240px' }}
        >
          <option value="all">Todas las Ubicaciones</option>
          <option value="banner_home">Banner Principal Inicio</option>
          <option value="interstitial_workout">Intersticial Post-Rutina</option>
          <option value="sidebar">Barra Lateral</option>
          <option value="footer">Pie de Aplicación</option>
        </select>

        <button onClick={fetchAds} className="btn btn-secondary btn-sm" title="Recargar">
          <RefreshCw size={16} className={loading ? 'spin-animation' : ''} />
        </button>
      </div>

      {/* Grid de Banners y Anuncios */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>
          <RefreshCw size={28} className="spin-animation" style={{ color: 'var(--accent-cyan)', marginBottom: '12px' }} />
          <p>Cargando anuncios publicitarios...</p>
        </div>
      ) : ads.length === 0 ? (
        <div className="empty-state">
          <Megaphone size={48} className="empty-state-icon" />
          <h4 className="empty-state-title">No hay anuncios configurados</h4>
          <p className="empty-state-desc">Publica ofertas de productos, suplementación o indumentaria deportiva.</p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: '20px' }}>
          {ads.map((ad) => (
            <div
              key={ad.id}
              className="card"
              style={{
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                padding: '0',
              }}
            >
              {/* Media Preview */}
              <div
                style={{
                  height: '160px',
                  width: '100%',
                  background: `linear-gradient(180deg, rgba(0,0,0,0.1) 0%, rgba(17,22,34,0.95) 100%), url(${ad.media_url}) center/cover`,
                  display: 'flex',
                  alignItems: 'flex-end',
                  padding: '16px 20px',
                }}
              >
                <div>
                  <span className="badge badge-purple" style={{ marginBottom: '6px', fontSize: '0.7rem' }}>
                    {getPlacementLabel(ad.placement)}
                  </span>
                  <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#ffffff' }}>{ad.title}</h3>
                </div>
              </div>

              {/* Contenido */}
              <div style={{ padding: '20px', flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.86rem', lineHeight: '1.5', marginBottom: '16px' }}>
                  {ad.description || 'Sin descripción adicional.'}
                </p>

                <div>
                  {/* Fechas de Vigencia */}
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      fontSize: '0.78rem',
                      color: 'var(--text-muted)',
                      marginBottom: '14px',
                    }}
                  >
                    <Calendar size={14} color="var(--accent-cyan)" />
                    <span>
                      {new Date(ad.starts_at).toLocaleDateString()} — {new Date(ad.ends_at).toLocaleDateString()}
                    </span>
                  </div>

                  {/* Estadísticas de Conversión */}
                  <div
                    style={{
                      display: 'grid',
                      gridTemplateColumns: '1fr 1fr',
                      gap: '8px',
                      padding: '10px 14px',
                      background: 'rgba(255, 255, 255, 0.02)',
                      borderRadius: '8px',
                      marginBottom: '16px',
                      border: '1px solid var(--border-subtle)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8rem' }}>
                      <Eye size={14} color="var(--accent-cyan)" />
                      <span>{ad.impressions_count} vistas</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8rem' }}>
                      <MousePointer size={14} color="var(--accent-green)" />
                      <span>{ad.clicks_count} clics</span>
                    </div>
                  </div>

                  {/* Enlace y Acciones */}
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span className={`badge ${ad.is_active ? 'badge-success' : 'badge-danger'}`}>
                        {ad.is_active ? 'Activo' : 'Inactivo'}
                      </span>
                      {ad.destination_url && (
                        <a
                          href={ad.destination_url}
                          target="_blank"
                          rel="noreferrer"
                          title="Visitar enlace de destino"
                          style={{ color: 'var(--accent-cyan)', display: 'inline-flex', alignItems: 'center' }}
                        >
                          <ExternalLink size={15} />
                        </a>
                      )}
                    </div>

                    <div style={{ display: 'flex', gap: '6px' }}>
                      <button
                        onClick={() => handleOpenEdit(ad)}
                        className="btn btn-secondary btn-sm"
                        title="Editar anuncio"
                      >
                        <Edit2 size={14} />
                      </button>
                      <button
                        onClick={() => handleToggleActive(ad)}
                        className="btn btn-secondary btn-sm"
                        title={ad.is_active ? 'Desactivar anuncio' : 'Activar anuncio'}
                        style={{ color: ad.is_active ? '#f59e0b' : '#10b981' }}
                      >
                        <Power size={14} />
                      </button>
                      <button
                        onClick={() => setDeleteAd(ad)}
                        className="btn btn-danger btn-sm"
                        title="Eliminar anuncio"
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

      {/* Modal Crear / Editar Anuncio */}
      <Modal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        title={editingAd ? `Editar Anuncio: ${editingAd.title}` : 'Crear Nuevo Anuncio Publicitario'}
      >
        <form onSubmit={handleSave}>
          <div className="form-group">
            <label className="form-label">Título del Anuncio / Campaña *</label>
            <input
              type="text"
              className="form-input"
              required
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              placeholder="Ej. Oferta Lanzamiento Kynectia Apparel"
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Ubicación en la Plataforma *</label>
              <select
                className="form-select"
                value={formData.placement}
                onChange={(e) => setFormData({ ...formData, placement: e.target.value as any })}
              >
                <option value="banner_home">Banner Principal Inicio</option>
                <option value="interstitial_workout">Intersticial Post-Rutina</option>
                <option value="sidebar">Barra Lateral</option>
                <option value="footer">Pie de Aplicación</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Prioridad (1-10)</label>
              <input
                type="number"
                min={1}
                max={10}
                className="form-input"
                value={formData.priority}
                onChange={(e) => setFormData({ ...formData, priority: parseInt(e.target.value, 10) })}
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">URL de Imagen Multimedia *</label>
            <input
              type="url"
              className="form-input"
              required
              value={formData.mediaUrl}
              onChange={(e) => setFormData({ ...formData, mediaUrl: e.target.value })}
              placeholder="https://images.unsplash.com/photo-..."
            />
          </div>

          <div className="form-group">
            <label className="form-label">Enlace de Destino (Call to Action)</label>
            <input
              type="url"
              className="form-input"
              value={formData.destinationUrl}
              onChange={(e) => setFormData({ ...formData, destinationUrl: e.target.value })}
              placeholder="https://kynectia.com/tienda"
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Fecha de Inicio *</label>
              <input
                type="date"
                className="form-input"
                required
                value={formData.startsAt}
                onChange={(e) => setFormData({ ...formData, startsAt: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label className="form-label">Fecha de Finalización *</label>
              <input
                type="date"
                className="form-input"
                required
                value={formData.endsAt}
                onChange={(e) => setFormData({ ...formData, endsAt: e.target.value })}
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Descripción o Copy Promocional</label>
            <textarea
              className="form-textarea"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Texto complementario del anuncio..."
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '20px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsFormOpen(false)}
              disabled={actionLoading}
            >
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={actionLoading}>
              {actionLoading ? 'Guardando...' : editingAd ? 'Actualizar Anuncio' : 'Crear Anuncio'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Confirmación Eliminar */}
      <ConfirmDialog
        isOpen={deleteAd !== null}
        title="Eliminar Anuncio Publicitario"
        message={`¿Estás seguro de eliminar el anuncio "${deleteAd?.title}"? Esta acción removerá el banner de la plataforma.`}
        confirmLabel="Eliminar Definitivamente"
        loading={actionLoading}
        onConfirm={handleDelete}
        onCancel={() => setDeleteAd(null)}
      />
    </div>
  );
};
