import React, { useEffect, useState, useCallback } from 'react';
import { apiRequest } from '../api/client';
import type { Exercise, MuscleGroup, Equipment } from '../types';
import { useToast } from '../context/ToastContext';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import {
  Activity,
  Plus,
  Eye,
  Edit2,
  Trash2,
  Power,
  RefreshCw,
  Search,
  ChevronLeft,
  ChevronRight,
  ShieldCheck,
  PlayCircle,
} from 'lucide-react';

export const ExercisesView: React.FC = () => {
  const { showToast } = useToast();

  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [muscleGroups, setMuscleGroups] = useState<MuscleGroup[]>([]);
  const [equipmentList, setEquipmentList] = useState<Equipment[]>([]);

  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [difficultyFilter, setDifficultyFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalCount, setTotalCount] = useState(0);

  // Modales
  const [detailExercise, setDetailExercise] = useState<Exercise | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingExercise, setEditingExercise] = useState<Exercise | null>(null);
  const [deleteExercise, setDeleteExercise] = useState<Exercise | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  // Form State
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    executionInstructions: '',
    difficultyLevel: 'intermediate' as Exercise['difficulty_level'],
    exerciseType: 'strength' as Exercise['exercise_type'],
    demoImageUrl: '',
    demoVideoUrl: '',
    safetyRecommendations: '',
    publicationStatus: 'published' as Exercise['publication_status'],
    isActive: true,
    primaryMuscleGroupId: 0,
    secondaryMuscleGroupIds: [] as number[],
    equipmentIds: [] as number[],
  });

  const fetchAuxiliaryData = async () => {
    try {
      const [musclesRes, equipRes] = await Promise.all([
        apiRequest<MuscleGroup[]>('/muscles'),
        apiRequest<Equipment[]>('/equipment'),
      ]);
      setMuscleGroups(musclesRes.data);
      setEquipmentList(equipRes.data);
    } catch (err: any) {
      console.error('Error al cargar datos auxiliares:', err);
    }
  };

  const fetchExercises = useCallback(async () => {
    setLoading(true);
    try {
      const params: any = { page, limit: 8 };
      if (search) params.search = search;
      if (difficultyFilter !== 'all') params.difficultyLevel = difficultyFilter;
      if (typeFilter !== 'all') params.exerciseType = typeFilter;
      if (statusFilter !== 'all') params.publicationStatus = statusFilter;

      const res = await apiRequest<Exercise[]>('/exercises', { params });
      setExercises(res.data);
      if (res.pagination) {
        setTotalPages(res.pagination.totalPages);
        setTotalCount(res.pagination.total);
      }
    } catch (err: any) {
      showToast('error', err.message || 'Error al obtener ejercicios.');
    } finally {
      setLoading(false);
    }
  }, [page, search, difficultyFilter, typeFilter, statusFilter, showToast]);

  useEffect(() => {
    fetchAuxiliaryData();
  }, []);

  useEffect(() => {
    fetchExercises();
  }, [fetchExercises]);

  const handleOpenCreate = () => {
    setEditingExercise(null);
    setFormData({
      name: '',
      description: '',
      executionInstructions: '',
      difficultyLevel: 'intermediate',
      exerciseType: 'strength',
      demoImageUrl: '',
      demoVideoUrl: '',
      safetyRecommendations: '',
      publicationStatus: 'published',
      isActive: true,
      primaryMuscleGroupId: muscleGroups[0]?.id || 0,
      secondaryMuscleGroupIds: [],
      equipmentIds: [],
    });
    setIsFormOpen(true);
  };

  const handleOpenEdit = async (ex: Exercise) => {
    try {
      const res = await apiRequest<Exercise>(`/exercises/${ex.id}`);
      const fullEx = res.data;
      setEditingExercise(fullEx);

      setFormData({
        name: fullEx.name,
        description: fullEx.description || '',
        executionInstructions: fullEx.execution_instructions || '',
        difficultyLevel: fullEx.difficulty_level,
        exerciseType: fullEx.exercise_type,
        demoImageUrl: fullEx.demo_image_url || '',
        demoVideoUrl: fullEx.demo_video_url || '',
        safetyRecommendations: fullEx.safety_recommendations || '',
        publicationStatus: fullEx.publication_status,
        isActive: fullEx.is_active,
        primaryMuscleGroupId: fullEx.primary_muscle_id || (fullEx as any).primary_muscle?.id || 0,
        secondaryMuscleGroupIds: fullEx.secondary_muscles ? fullEx.secondary_muscles.map((m) => m.id) : [],
        equipmentIds: fullEx.equipment ? fullEx.equipment.map((eq) => eq.id) : [],
      });
      setIsFormOpen(true);
    } catch (err: any) {
      showToast('error', err.message || 'Error al cargar detalle del ejercicio.');
    }
  };

  const handleViewDetail = async (ex: Exercise) => {
    try {
      const res = await apiRequest<Exercise>(`/exercises/${ex.id}`);
      setDetailExercise(res.data);
    } catch (err: any) {
      showToast('error', err.message || 'Error al obtener detalle.');
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.primaryMuscleGroupId) {
      showToast('error', 'Debes seleccionar un grupo muscular primario.');
      return;
    }

    setActionLoading(true);
    try {
      if (editingExercise) {
        await apiRequest(`/exercises/${editingExercise.id}`, {
          method: 'PUT',
          body: formData,
        });
        showToast('success', 'Ejercicio actualizado correctamente.');
      } else {
        await apiRequest('/exercises', {
          method: 'POST',
          body: formData,
        });
        showToast('success', 'Ejercicio registrado en el catálogo.');
      }
      setIsFormOpen(false);
      fetchExercises();
    } catch (err: any) {
      showToast('error', err.message || 'Error al guardar ejercicio.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleActive = async (ex: Exercise) => {
    try {
      await apiRequest(`/exercises/${ex.id}/toggle-active`, {
        method: 'PATCH',
        body: { isActive: !ex.is_active },
      });
      showToast('success', `Ejercicio ${!ex.is_active ? 'activado' : 'desactivado'}.`);
      fetchExercises();
    } catch (err: any) {
      showToast('error', err.message || 'Error al cambiar estado.');
    }
  };

  const handleDelete = async () => {
    if (!deleteExercise) return;
    setActionLoading(true);
    try {
      await apiRequest(`/exercises/${deleteExercise.id}`, { method: 'DELETE' });
      showToast('success', 'Ejercicio eliminado (soft-delete).');
      setDeleteExercise(null);
      fetchExercises();
    } catch (err: any) {
      showToast('error', err.message || 'Error al eliminar ejercicio.');
    } finally {
      setActionLoading(false);
    }
  };

  const toggleSecondaryMuscle = (id: number) => {
    setFormData((prev) => {
      const exists = prev.secondaryMuscleGroupIds.includes(id);
      return {
        ...prev,
        secondaryMuscleGroupIds: exists
          ? prev.secondaryMuscleGroupIds.filter((item) => item !== id)
          : [...prev.secondaryMuscleGroupIds, id],
      };
    });
  };

  const toggleEquipment = (id: number) => {
    setFormData((prev) => {
      const exists = prev.equipmentIds.includes(id);
      return {
        ...prev,
        equipmentIds: exists
          ? prev.equipmentIds.filter((item) => item !== id)
          : [...prev.equipmentIds, id],
      };
    });
  };

  return (
    <div className="page-container">
      {/* Encabezado */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#ffffff' }}>Catálogo de Ejercicios</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            Repositorio maestro de movimientos, instrucciones técnicas, seguridad y multimedia ({totalCount} registrados).
          </p>
        </div>
        <button onClick={handleOpenCreate} className="btn btn-primary">
          <Plus size={18} />
          <span>Nuevo Ejercicio</span>
        </button>
      </div>

      {/* Filtros */}
      <div className="filter-bar">
        <div className="search-input-wrapper">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            className="form-input"
            placeholder="Buscar por nombre o descripción..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(1);
            }}
          />
        </div>

        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <select
            className="form-select"
            value={difficultyFilter}
            onChange={(e) => {
              setDifficultyFilter(e.target.value);
              setPage(1);
            }}
            style={{ width: '150px' }}
          >
            <option value="all">Toda Dificultad</option>
            <option value="beginner">Principiante</option>
            <option value="intermediate">Intermedio</option>
            <option value="advanced">Avanzado</option>
          </select>

          <select
            className="form-select"
            value={typeFilter}
            onChange={(e) => {
              setTypeFilter(e.target.value);
              setPage(1);
            }}
            style={{ width: '150px' }}
          >
            <option value="all">Todo Tipo</option>
            <option value="strength">Fuerza</option>
            <option value="cardio">Cardio</option>
            <option value="flexibility">Flexibilidad / Core</option>
            <option value="hiit">HIIT</option>
            <option value="plyometric">Pliometría</option>
          </select>

          <select
            className="form-select"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(1);
            }}
            style={{ width: '150px' }}
          >
            <option value="all">Todo Estado</option>
            <option value="published">Publicado</option>
            <option value="draft">Borrador</option>
            <option value="archived">Archivado</option>
          </select>

          <button onClick={fetchExercises} className="btn btn-secondary btn-sm" title="Recargar">
            <RefreshCw size={16} className={loading ? 'spin-animation' : ''} />
          </button>
        </div>
      </div>

      {/* Tabla de Ejercicios */}
      <div className="table-container">
        <table className="custom-table">
          <thead>
            <tr>
              <th>Ejercicio</th>
              <th>Músculo Principal</th>
              <th>Dificultad</th>
              <th>Tipo</th>
              <th>Implementos</th>
              <th>Publicación</th>
              <th>Estado</th>
              <th style={{ textAlign: 'right' }}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                  <RefreshCw size={24} className="spin-animation" style={{ color: 'var(--accent-cyan)', marginBottom: '8px' }} />
                  <p>Cargando ejercicios desde la base de datos...</p>
                </td>
              </tr>
            ) : exercises.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                  No se encontraron ejercicios coincidentes con los filtros aplicados.
                </td>
              </tr>
            ) : (
              exercises.map((ex) => (
                <tr key={ex.id}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div
                        style={{
                          width: '42px',
                          height: '42px',
                          borderRadius: '8px',
                          background: ex.demo_image_url
                            ? `url(${ex.demo_image_url}) center/cover`
                            : 'rgba(0, 242, 254, 0.1)',
                          border: '1px solid var(--border-subtle)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          flexShrink: 0,
                        }}
                      >
                        {!ex.demo_image_url && <Activity size={18} color="var(--accent-cyan)" />}
                      </div>
                      <div>
                        <p style={{ fontWeight: 600, color: '#ffffff' }}>{ex.name}</p>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          {ex.routines_count || 0} rutina(s) activa(s)
                        </span>
                      </div>
                    </div>
                  </td>
                  <td>
                    <span className="badge badge-info">{ex.primary_muscle_name || 'Sin asignar'}</span>
                  </td>
                  <td>
                    <span
                      className={`badge ${
                        ex.difficulty_level === 'beginner'
                          ? 'badge-success'
                          : ex.difficulty_level === 'intermediate'
                          ? 'badge-info'
                          : 'badge-warning'
                      }`}
                    >
                      {ex.difficulty_level}
                    </span>
                  </td>
                  <td>
                    <span style={{ fontSize: '0.82rem', textTransform: 'capitalize', color: 'var(--text-secondary)' }}>
                      {ex.exercise_type}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', maxWidth: '160px' }}>
                      {ex.equipment && ex.equipment.length > 0 ? (
                        ex.equipment.map((eq) => (
                          <span
                            key={eq.id}
                            style={{
                              fontSize: '0.7rem',
                              padding: '2px 6px',
                              borderRadius: '4px',
                              background: 'rgba(255, 255, 255, 0.05)',
                              color: 'var(--text-secondary)',
                            }}
                          >
                            {eq.name}
                          </span>
                        ))
                      ) : (
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Ninguno</span>
                      )}
                    </div>
                  </td>
                  <td>
                    <span
                      className={`badge ${
                        ex.publication_status === 'published'
                          ? 'badge-success'
                          : ex.publication_status === 'draft'
                          ? 'badge-warning'
                          : 'badge-danger'
                      }`}
                    >
                      {ex.publication_status}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${ex.is_active ? 'badge-success' : 'badge-danger'}`}>
                      {ex.is_active ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '6px' }}>
                      <button
                        onClick={() => handleViewDetail(ex)}
                        className="btn btn-secondary btn-sm"
                        title="Ver instrucciones completas"
                      >
                        <Eye size={14} />
                      </button>
                      <button
                        onClick={() => handleOpenEdit(ex)}
                        className="btn btn-secondary btn-sm"
                        title="Editar ejercicio"
                      >
                        <Edit2 size={14} />
                      </button>
                      <button
                        onClick={() => handleToggleActive(ex)}
                        className="btn btn-secondary btn-sm"
                        title={ex.is_active ? 'Desactivar' : 'Activar'}
                        style={{ color: ex.is_active ? '#f59e0b' : '#10b981' }}
                      >
                        <Power size={14} />
                      </button>
                      <button
                        onClick={() => setDeleteExercise(ex)}
                        className="btn btn-danger btn-sm"
                        title="Eliminar ejercicio"
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
            Página {page} de {Math.max(1, totalPages)} ({totalCount} ejercicios)
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

      {/* Modal Formulario Completo Crear / Editar */}
      <Modal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        title={editingExercise ? `Modificar Ejercicio: ${editingExercise.name}` : 'Crear Nuevo Ejercicio Maestro'}
        size="large"
      >
        <form onSubmit={handleSave}>
          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Nombre del Ejercicio *</label>
              <input
                type="text"
                className="form-input"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="Ej. Press Militar con Barra"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Músculo Principal *</label>
              <select
                className="form-select"
                required
                value={formData.primaryMuscleGroupId}
                onChange={(e) => setFormData({ ...formData, primaryMuscleGroupId: parseInt(e.target.value, 10) })}
              >
                <option value="">Selecciona el músculo objetivo</option>
                {muscleGroups.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Nivel de Dificultad</label>
              <select
                className="form-select"
                value={formData.difficultyLevel}
                onChange={(e) => setFormData({ ...formData, difficultyLevel: e.target.value as any })}
              >
                <option value="beginner">Principiante</option>
                <option value="intermediate">Intermedio</option>
                <option value="advanced">Avanzado</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Tipo de Ejercicio</label>
              <select
                className="form-select"
                value={formData.exerciseType}
                onChange={(e) => setFormData({ ...formData, exerciseType: e.target.value as any })}
              >
                <option value="strength">Fuerza / Hipertrofia</option>
                <option value="cardio">Cardiovascular</option>
                <option value="flexibility">Flexibilidad / Core</option>
                <option value="hiit">HIIT Metabólico</option>
                <option value="plyometric">Pliometría / Potencia</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Publicación</label>
              <select
                className="form-select"
                value={formData.publicationStatus}
                onChange={(e) => setFormData({ ...formData, publicationStatus: e.target.value as any })}
              >
                <option value="published">Publicado</option>
                <option value="draft">Borrador</option>
                <option value="archived">Archivado</option>
              </select>
            </div>
          </div>

          {/* Músculos Secundarios (Selección Múltiple) */}
          <div className="form-group">
            <label className="form-label">Músculos Secundarios / Sinérgicos</label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', maxHeight: '110px', overflowY: 'auto', padding: '8px', background: 'rgba(0,0,0,0.2)', borderRadius: '8px' }}>
              {muscleGroups
                .filter((m) => m.id !== formData.primaryMuscleGroupId)
                .map((m) => {
                  const isSelected = formData.secondaryMuscleGroupIds.includes(m.id);
                  return (
                    <button
                      type="button"
                      key={m.id}
                      onClick={() => toggleSecondaryMuscle(m.id)}
                      className={`btn btn-sm ${isSelected ? 'btn-primary' : 'btn-secondary'}`}
                      style={{ fontSize: '0.78rem' }}
                    >
                      {m.name}
                    </button>
                  );
                })}
            </div>
          </div>

          {/* Implementos Requeridos */}
          <div className="form-group">
            <label className="form-label">Implementos y Equipos Necesarios</label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', maxHeight: '110px', overflowY: 'auto', padding: '8px', background: 'rgba(0,0,0,0.2)', borderRadius: '8px' }}>
              {equipmentList.map((eq) => {
                const isSelected = formData.equipmentIds.includes(eq.id);
                return (
                  <button
                    type="button"
                    key={eq.id}
                    onClick={() => toggleEquipment(eq.id)}
                    className={`btn btn-sm ${isSelected ? 'btn-primary' : 'btn-secondary'}`}
                    style={{ fontSize: '0.78rem' }}
                  >
                    {eq.name}
                  </button>
                );
              })}
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">URL de Imagen Demostrativa</label>
              <input
                type="url"
                className="form-input"
                value={formData.demoImageUrl}
                onChange={(e) => setFormData({ ...formData, demoImageUrl: e.target.value })}
                placeholder="https://images.unsplash.com/photo-..."
              />
            </div>
            <div className="form-group">
              <label className="form-label">URL de Video Demostrativo (MP4/WebM)</label>
              <input
                type="url"
                className="form-input"
                value={formData.demoVideoUrl}
                onChange={(e) => setFormData({ ...formData, demoVideoUrl: e.target.value })}
                placeholder="https://ejemplo.com/video.mp4"
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Instrucciones Paso a Paso de Ejecución</label>
            <textarea
              className="form-textarea"
              style={{ minHeight: '100px' }}
              value={formData.executionInstructions}
              onChange={(e) => setFormData({ ...formData, executionInstructions: e.target.value })}
              placeholder="1. Posición inicial...&#10;2. Fase excéntrica y control...&#10;3. Fase concéntrica y bloqueo..."
            />
          </div>

          <div className="form-group">
            <label className="form-label">Recomendaciones de Seguridad y Tips Posturales</label>
            <textarea
              className="form-textarea"
              value={formData.safetyRecommendations}
              onChange={(e) => setFormData({ ...formData, safetyRecommendations: e.target.value })}
              placeholder="Cuidado con hiperextender la zona lumbar; mantener codos en ángulo de 45°..."
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
              {actionLoading ? 'Guardando en BD...' : editingExercise ? 'Actualizar Ejercicio' : 'Registrar Ejercicio'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal Ficha Detallada */}
      <Modal
        isOpen={detailExercise !== null}
        onClose={() => setDetailExercise(null)}
        title={`Ficha Técnica: ${detailExercise?.name}`}
        size="large"
      >
        {detailExercise && (
          <div>
            <div style={{ display: 'flex', gap: '20px', marginBottom: '20px', flexWrap: 'wrap' }}>
              {detailExercise.demo_image_url && (
                <img
                  src={detailExercise.demo_image_url}
                  alt={detailExercise.name}
                  style={{ width: '220px', height: '160px', objectFit: 'cover', borderRadius: '12px', border: '1px solid var(--border-subtle)' }}
                />
              )}
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', gap: '8px', marginBottom: '10px' }}>
                  <span className="badge badge-info">{detailExercise.primary_muscle_name || (detailExercise as any).primary_muscle?.name}</span>
                  <span className="badge badge-purple">{detailExercise.difficulty_level}</span>
                  <span className="badge badge-success">{detailExercise.publication_status}</span>
                </div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: '1.5' }}>
                  {detailExercise.description || 'Sin descripción general.'}
                </p>
                {detailExercise.demo_video_url && (
                  <div style={{ marginTop: '12px' }}>
                    <a
                      href={detailExercise.demo_video_url}
                      target="_blank"
                      rel="noreferrer"
                      className="btn btn-secondary btn-sm"
                      style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                    >
                      <PlayCircle size={16} color="var(--accent-cyan)" /> Ver Video Demostrativo
                    </a>
                  </div>
                )}
              </div>
            </div>

            <div style={{ marginBottom: '18px' }}>
              <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: '#ffffff', marginBottom: '8px' }}>
                Instrucciones de Ejecución
              </h4>
              <div
                style={{
                  padding: '14px',
                  background: 'rgba(0,0,0,0.25)',
                  borderRadius: '8px',
                  border: '1px solid var(--border-subtle)',
                  whiteSpace: 'pre-line',
                  fontSize: '0.88rem',
                  color: 'var(--text-secondary)',
                }}
              >
                {detailExercise.execution_instructions || 'No se han cargado instrucciones paso a paso.'}
              </div>
            </div>

            {detailExercise.safety_recommendations && (
              <div style={{ marginBottom: '20px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
                  <ShieldCheck size={16} color="var(--accent-orange)" />
                  <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: '#ffffff' }}>
                    Recomendaciones de Seguridad
                  </h4>
                </div>
                <div
                  style={{
                    padding: '12px 14px',
                    background: 'rgba(245, 158, 11, 0.08)',
                    border: '1px solid rgba(245, 158, 11, 0.25)',
                    borderRadius: '8px',
                    fontSize: '0.85rem',
                    color: '#fde68a',
                  }}
                >
                  {detailExercise.safety_recommendations}
                </div>
              </div>
            )}
          </div>
        )}
      </Modal>

      {/* Confirmación Eliminar */}
      <ConfirmDialog
        isOpen={deleteExercise !== null}
        title="Eliminar Ejercicio del Catálogo"
        message={`¿Estás seguro de eliminar el ejercicio "${deleteExercise?.name}"? Si el ejercicio forma parte de una o más rutinas activas, la base de datos protegerá la integridad referencial y no permitirá la eliminación física.`}
        confirmLabel="Eliminar (Soft-delete)"
        loading={actionLoading}
        onConfirm={handleDelete}
        onCancel={() => setDeleteExercise(null)}
      />
    </div>
  );
};
