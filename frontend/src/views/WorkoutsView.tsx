import React, { useEffect, useState, useCallback } from 'react';
import { apiRequest } from '../api/client';
import type { WorkoutRoutine, Exercise, RoutineExercise } from '../types';
import { useToast } from '../context/ToastContext';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import {
  CalendarCheck2,
  Plus,
  Eye,
  Edit2,
  Trash2,
  RefreshCw,
  Search,
  Clock,
  Send,
  Archive,
  ChevronLeft,
  ChevronRight,
  GripVertical,
  X,
} from 'lucide-react';

export const WorkoutsView: React.FC = () => {
  const { showToast } = useToast();

  const [routines, setRoutines] = useState<WorkoutRoutine[]>([]);
  const [exercisesList, setExercisesList] = useState<Exercise[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [difficultyFilter, setDifficultyFilter] = useState('all');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalCount, setTotalCount] = useState(0);

  // Modales
  const [detailRoutine, setDetailRoutine] = useState<WorkoutRoutine | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingRoutine, setEditingRoutine] = useState<WorkoutRoutine | null>(null);
  const [deleteRoutine, setDeleteRoutine] = useState<WorkoutRoutine | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  // Form State
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    coverImageUrl: '',
    goal: 'hypertrophy',
    difficultyLevel: 'intermediate' as WorkoutRoutine['difficulty_level'],
    category: 'full_body',
    estimatedDurationMinutes: 45,
    publicationStatus: 'draft' as WorkoutRoutine['publication_status'],
    exercises: [] as RoutineExercise[],
  });

  const fetchExercisesList = async () => {
    try {
      const res = await apiRequest<Exercise[]>('/exercises', { params: { limit: 100 } });
      setExercisesList(res.data);
    } catch (err: any) {
      console.error('Error al cargar catálogo de ejercicios para rutinas:', err);
    }
  };

  const fetchRoutines = useCallback(async () => {
    setLoading(true);
    try {
      const params: any = { page, limit: 8 };
      if (search) params.search = search;
      if (statusFilter !== 'all') params.publicationStatus = statusFilter;
      if (difficultyFilter !== 'all') params.difficultyLevel = difficultyFilter;

      const res = await apiRequest<WorkoutRoutine[]>('/workouts', { params });
      setRoutines(res.data);
      if (res.pagination) {
        setTotalPages(res.pagination.totalPages);
        setTotalCount(res.pagination.total);
      }
    } catch (err: any) {
      showToast('error', err.message || 'Error al obtener rutinas.');
    } finally {
      setLoading(false);
    }
  }, [page, search, statusFilter, difficultyFilter, showToast]);

  useEffect(() => {
    fetchExercisesList();
  }, []);

  useEffect(() => {
    fetchRoutines();
  }, [fetchRoutines]);

  const handleOpenCreate = () => {
    setEditingRoutine(null);
    setFormData({
      name: '',
      description: '',
      coverImageUrl: '',
      goal: 'hypertrophy',
      difficultyLevel: 'intermediate',
      category: 'upper_lower',
      estimatedDurationMinutes: 50,
      publicationStatus: 'draft',
      exercises: [],
    });
    setIsFormOpen(true);
  };

  const handleOpenEdit = async (r: WorkoutRoutine) => {
    try {
      const res = await apiRequest<WorkoutRoutine>(`/workouts/${r.id}`);
      const full = res.data;
      setEditingRoutine(full);
      setFormData({
        name: full.name,
        description: full.description || '',
        coverImageUrl: full.cover_image_url || '',
        goal: full.goal,
        difficultyLevel: full.difficulty_level,
        category: full.category,
        estimatedDurationMinutes: full.estimated_duration_minutes,
        publicationStatus: full.publication_status,
        exercises: full.exercises || [],
      });
      setIsFormOpen(true);
    } catch (err: any) {
      showToast('error', err.message || 'Error al obtener rutina.');
    }
  };

  const handleViewDetail = async (r: WorkoutRoutine) => {
    try {
      const res = await apiRequest<WorkoutRoutine>(`/workouts/${r.id}`);
      setDetailRoutine(res.data);
    } catch (err: any) {
      showToast('error', err.message || 'Error al obtener detalle de rutina.');
    }
  };

  const handleAddExerciseToRoutine = (exerciseId: number) => {
    const ex = exercisesList.find((e) => e.id === exerciseId);
    if (!ex) return;

    setFormData((prev) => ({
      ...prev,
      exercises: [
        ...prev.exercises,
        {
          exercise_id: ex.id,
          exercise_name: ex.name,
          sequence_order: prev.exercises.length + 1,
          sets: 4,
          reps: 10,
          duration_seconds: 0,
          rest_seconds: 60,
          recommended_weight_kg: 0,
          specific_instructions: '',
        },
      ],
    }));
  };

  const handleRemoveExercise = (index: number) => {
    setFormData((prev) => {
      const updated = prev.exercises.filter((_, i) => i !== index);
      // Re-secuenciar orden
      return {
        ...prev,
        exercises: updated.map((item, idx) => ({ ...item, sequence_order: idx + 1 })),
      };
    });
  };

  const handleUpdateExerciseParam = (index: number, field: string, value: any) => {
    setFormData((prev) => {
      const updated = [...prev.exercises];
      updated[index] = { ...updated[index], [field]: value };
      return { ...prev, exercises: updated };
    });
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (formData.exercises.length === 0) {
      showToast('error', 'Debes incluir al menos un ejercicio en la rutina.');
      return;
    }

    setActionLoading(true);
    try {
      const payload = {
        name: formData.name,
        description: formData.description,
        coverImageUrl: formData.coverImageUrl,
        goal: formData.goal,
        difficultyLevel: formData.difficultyLevel,
        category: formData.category,
        estimatedDurationMinutes: formData.estimatedDurationMinutes,
        publicationStatus: formData.publicationStatus,
        exercises: formData.exercises.map((item, idx) => ({
          exerciseId: item.exercise_id,
          sequenceOrder: idx + 1,
          sets: item.sets,
          reps: item.reps,
          durationSeconds: item.duration_seconds || 0,
          restSeconds: item.rest_seconds,
          recommendedWeightKg: item.recommended_weight_kg || 0,
          specificInstructions: item.specific_instructions || '',
        })),
      };

      if (editingRoutine) {
        await apiRequest(`/workouts/${editingRoutine.id}`, {
          method: 'PUT',
          body: payload,
        });
        showToast('success', 'Rutina actualizada y versionada.');
      } else {
        await apiRequest('/workouts', {
          method: 'POST',
          body: payload,
        });
        showToast('success', 'Rutina predefinida creada con éxito.');
      }
      setIsFormOpen(false);
      fetchRoutines();
    } catch (err: any) {
      showToast('error', err.message || 'Error al guardar rutina.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleSetStatus = async (r: WorkoutRoutine, status: 'draft' | 'published' | 'archived') => {
    try {
      await apiRequest(`/workouts/${r.id}/status`, {
        method: 'PATCH',
        body: { status },
      });
      showToast('success', `Rutina marcada como ${status}.`);
      fetchRoutines();
    } catch (err: any) {
      showToast('error', err.message || 'Error al cambiar estado de publicación.');
    }
  };

  const handleDelete = async () => {
    if (!deleteRoutine) return;
    setActionLoading(true);
    try {
      await apiRequest(`/workouts/${deleteRoutine.id}`, { method: 'DELETE' });
      showToast('success', 'Rutina eliminada (soft-delete).');
      setDeleteRoutine(null);
      fetchRoutines();
    } catch (err: any) {
      showToast('error', err.message || 'Error al eliminar rutina.');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="page-container">
      {/* Encabezado */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#ffffff' }}>Rutinas de Entrenamiento</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            Diseño, secuenciación de ejercicios, series, repeticiones y versionado ({totalCount} rutinas).
          </p>
        </div>
        <button onClick={handleOpenCreate} className="btn btn-primary">
          <Plus size={18} />
          <span>Crear Nueva Rutina</span>
        </button>
      </div>

      {/* Filtros */}
      <div className="filter-bar">
        <div className="search-input-wrapper">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            className="form-input"
            placeholder="Buscar rutina por nombre u objetivo..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(1);
            }}
          />
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <select
            className="form-select"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(1);
            }}
            style={{ width: '160px' }}
          >
            <option value="all">Todo Estado</option>
            <option value="published">Publicadas</option>
            <option value="draft">Borradores</option>
            <option value="archived">Archivadas</option>
          </select>

          <select
            className="form-select"
            value={difficultyFilter}
            onChange={(e) => {
              setDifficultyFilter(e.target.value);
              setPage(1);
            }}
            style={{ width: '160px' }}
          >
            <option value="all">Toda Dificultad</option>
            <option value="beginner">Principiante</option>
            <option value="intermediate">Intermedio</option>
            <option value="advanced">Avanzado</option>
          </select>

          <button onClick={fetchRoutines} className="btn btn-secondary btn-sm" title="Recargar">
            <RefreshCw size={16} className={loading ? 'spin-animation' : ''} />
          </button>
        </div>
      </div>

      {/* Tabla de Rutinas */}
      <div className="table-container">
        <table className="custom-table">
          <thead>
            <tr>
              <th>Rutina</th>
              <th>Objetivo & Categoría</th>
              <th>Dificultad</th>
              <th>Duración</th>
              <th>Versión</th>
              <th>Publicación</th>
              <th style={{ textAlign: 'right' }}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                  <RefreshCw size={24} className="spin-animation" style={{ color: 'var(--accent-cyan)', marginBottom: '8px' }} />
                  <p>Cargando rutinas...</p>
                </td>
              </tr>
            ) : routines.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                  No se encontraron rutinas registradas.
                </td>
              </tr>
            ) : (
              routines.map((r) => (
                <tr key={r.id}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div
                        style={{
                          width: '42px',
                          height: '42px',
                          borderRadius: '8px',
                          background: r.cover_image_url
                            ? `url(${r.cover_image_url}) center/cover`
                            : 'rgba(245, 158, 11, 0.1)',
                          border: '1px solid var(--border-subtle)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          flexShrink: 0,
                        }}
                      >
                        {!r.cover_image_url && <CalendarCheck2 size={18} color="var(--accent-orange)" />}
                      </div>
                      <div>
                        <p style={{ fontWeight: 600, color: '#ffffff' }}>{r.name}</p>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          {r.exercises_count || 0} ejercicio(s) incluidos
                        </span>
                      </div>
                    </div>
                  </td>
                  <td>
                    <p style={{ fontSize: '0.85rem', textTransform: 'capitalize' }}>{r.goal}</p>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'capitalize' }}>
                      {r.category}
                    </span>
                  </td>
                  <td>
                    <span
                      className={`badge ${
                        r.difficulty_level === 'beginner'
                          ? 'badge-success'
                          : r.difficulty_level === 'intermediate'
                          ? 'badge-info'
                          : 'badge-warning'
                      }`}
                    >
                      {r.difficulty_level}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem' }}>
                      <Clock size={14} color="var(--accent-cyan)" />
                      <span>{r.estimated_duration_minutes} min</span>
                    </div>
                  </td>
                  <td>
                    <span className="badge badge-purple">v{r.version}</span>
                  </td>
                  <td>
                    <span
                      className={`badge ${
                        r.publication_status === 'published'
                          ? 'badge-success'
                          : r.publication_status === 'draft'
                          ? 'badge-warning'
                          : 'badge-danger'
                      }`}
                    >
                      {r.publication_status}
                    </span>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '6px' }}>
                      <button
                        onClick={() => handleViewDetail(r)}
                        className="btn btn-secondary btn-sm"
                        title="Ver desglose de ejercicios"
                      >
                        <Eye size={14} />
                      </button>
                      <button
                        onClick={() => handleOpenEdit(r)}
                        className="btn btn-secondary btn-sm"
                        title="Editar rutina"
                      >
                        <Edit2 size={14} />
                      </button>
                      {r.publication_status !== 'published' ? (
                        <button
                          onClick={() => handleSetStatus(r, 'published')}
                          className="btn btn-secondary btn-sm"
                          title="Publicar rutina"
                          style={{ color: '#10b981' }}
                        >
                          <Send size={14} />
                        </button>
                      ) : (
                        <button
                          onClick={() => handleSetStatus(r, 'archived')}
                          className="btn btn-secondary btn-sm"
                          title="Archivar rutina"
                          style={{ color: '#f59e0b' }}
                        >
                          <Archive size={14} />
                        </button>
                      )}
                      <button
                        onClick={() => setDeleteRoutine(r)}
                        className="btn btn-danger btn-sm"
                        title="Eliminar rutina"
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
            Página {page} de {Math.max(1, totalPages)} ({totalCount} rutinas)
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

      {/* Modal Creador / Editor de Rutinas */}
      <Modal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        title={editingRoutine ? `Editar Rutina: ${editingRoutine.name} (v${editingRoutine.version})` : 'Constructor de Rutina'}
        size="large"
      >
        <form onSubmit={handleSave}>
          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Nombre de la Rutina *</label>
              <input
                type="text"
                className="form-input"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="Ej. Torso Potencia e Hipertrofia"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Duración Estimada (min) *</label>
              <input
                type="number"
                className="form-input"
                required
                min={10}
                max={240}
                value={formData.estimatedDurationMinutes}
                onChange={(e) => setFormData({ ...formData, estimatedDurationMinutes: parseInt(e.target.value, 10) })}
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Objetivo de Entrenamiento</label>
              <select
                className="form-select"
                value={formData.goal}
                onChange={(e) => setFormData({ ...formData, goal: e.target.value })}
              >
                <option value="hypertrophy">Hipertrofia Muscular</option>
                <option value="strength">Fuerza Máxima</option>
                <option value="fat_loss">Quema de Grasa</option>
                <option value="endurance">Resistencia</option>
                <option value="mobility">Movilidad y Salud</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Dificultad</label>
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
              <label className="form-label">Estado de Publicación</label>
              <select
                className="form-select"
                value={formData.publicationStatus}
                onChange={(e) => setFormData({ ...formData, publicationStatus: e.target.value as any })}
              >
                <option value="draft">Borrador</option>
                <option value="published">Publicado</option>
                <option value="archived">Archivado</option>
              </select>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">URL de Imagen de Portada</label>
            <input
              type="url"
              className="form-input"
              value={formData.coverImageUrl}
              onChange={(e) => setFormData({ ...formData, coverImageUrl: e.target.value })}
              placeholder="https://images.unsplash.com/photo-..."
            />
          </div>

          <div className="form-group">
            <label className="form-label">Descripción de la Rutina</label>
            <textarea
              className="form-textarea"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Describe el enfoque metabólico, descanso recomendado entre sesiones y calentamiento..."
            />
          </div>

          {/* Selector para Añadir Ejercicios */}
          <div style={{ marginTop: '24px', marginBottom: '16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
              <label className="form-label" style={{ margin: 0 }}>
                Ejercicios Configurados ({formData.exercises.length})
              </label>
              <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                <select
                  className="form-select"
                  style={{ width: '280px', padding: '6px 10px', fontSize: '0.85rem' }}
                  onChange={(e) => {
                    const id = parseInt(e.target.value, 10);
                    if (id) {
                      handleAddExerciseToRoutine(id);
                      e.target.value = '';
                    }
                  }}
                  defaultValue=""
                >
                  <option value="" disabled>+ Añadir ejercicio del catálogo...</option>
                  {exercisesList.map((ex) => (
                    <option key={ex.id} value={ex.id}>
                      {ex.name} ({ex.difficulty_level})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Lista Interactiva de Ejercicios */}
            {formData.exercises.length === 0 ? (
              <div
                style={{
                  padding: '30px',
                  textAlign: 'center',
                  background: 'rgba(0,0,0,0.2)',
                  borderRadius: '10px',
                  border: '1px dashed var(--border-subtle)',
                  color: 'var(--text-muted)',
                  fontSize: '0.88rem',
                }}
              >
                Usa el selector superior para añadir ejercicios a la rutina. Podrás ajustar series, repeticiones, descansos y cargas.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {formData.exercises.map((item, index) => (
                  <div
                    key={index}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '12px',
                      padding: '12px 14px',
                      background: 'rgba(255, 255, 255, 0.03)',
                      border: '1px solid var(--border-subtle)',
                      borderRadius: '10px',
                    }}
                  >
                    <GripVertical size={18} color="var(--text-muted)" />
                    <span style={{ fontWeight: 700, color: 'var(--accent-cyan)', width: '24px' }}>
                      #{index + 1}
                    </span>

                    <div style={{ flex: 1 }}>
                      <p style={{ fontWeight: 600, color: '#ffffff', fontSize: '0.9rem' }}>
                        {item.exercise_name || `Ejercicio ID: ${item.exercise_id}`}
                      </p>
                    </div>

                    <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Series:</span>
                        <input
                          type="number"
                          className="form-input"
                          style={{ width: '60px', padding: '4px 6px', textAlign: 'center' }}
                          min={1}
                          max={20}
                          value={item.sets}
                          onChange={(e) => handleUpdateExerciseParam(index, 'sets', parseInt(e.target.value, 10))}
                        />
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Reps:</span>
                        <input
                          type="number"
                          className="form-input"
                          style={{ width: '60px', padding: '4px 6px', textAlign: 'center' }}
                          min={1}
                          max={100}
                          value={item.reps}
                          onChange={(e) => handleUpdateExerciseParam(index, 'reps', parseInt(e.target.value, 10))}
                        />
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Descanso (s):</span>
                        <input
                          type="number"
                          className="form-input"
                          style={{ width: '65px', padding: '4px 6px', textAlign: 'center' }}
                          min={0}
                          step={15}
                          value={item.rest_seconds}
                          onChange={(e) => handleUpdateExerciseParam(index, 'rest_seconds', parseInt(e.target.value, 10))}
                        />
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Kg:</span>
                        <input
                          type="number"
                          className="form-input"
                          style={{ width: '65px', padding: '4px 6px', textAlign: 'center' }}
                          min={0}
                          step={0.5}
                          value={item.recommended_weight_kg || 0}
                          onChange={(e) => handleUpdateExerciseParam(index, 'recommended_weight_kg', parseFloat(e.target.value))}
                        />
                      </div>

                      <button
                        type="button"
                        onClick={() => handleRemoveExercise(index)}
                        style={{
                          background: 'transparent',
                          border: 'none',
                          color: '#ef4444',
                          cursor: 'pointer',
                          padding: '4px',
                        }}
                        title="Quitar de la rutina"
                      >
                        <X size={18} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsFormOpen(false)}
              disabled={actionLoading}
            >
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={actionLoading}>
              {actionLoading ? 'Guardando...' : editingRoutine ? 'Guardar Versión de Rutina' : 'Publicar / Crear Rutina'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal Ficha Detallada */}
      <Modal
        isOpen={detailRoutine !== null}
        onClose={() => setDetailRoutine(null)}
        title={`Desglose: ${detailRoutine?.name} (Versión ${detailRoutine?.version})`}
        size="large"
      >
        {detailRoutine && (
          <div>
            <div style={{ display: 'flex', gap: '16px', marginBottom: '20px', flexWrap: 'wrap' }}>
              {detailRoutine.cover_image_url && (
                <img
                  src={detailRoutine.cover_image_url}
                  alt={detailRoutine.name}
                  style={{ width: '240px', height: '140px', objectFit: 'cover', borderRadius: '12px', border: '1px solid var(--border-subtle)' }}
                />
              )}
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', gap: '8px', marginBottom: '10px' }}>
                  <span className="badge badge-success">{detailRoutine.publication_status}</span>
                  <span className="badge badge-purple">{detailRoutine.difficulty_level}</span>
                  <span className="badge badge-info">{detailRoutine.goal}</span>
                </div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: '1.5' }}>
                  {detailRoutine.description || 'Sin descripción.'}
                </p>
                <div style={{ marginTop: '10px', fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                  Duración aproximada: <strong style={{ color: '#ffffff' }}>{detailRoutine.estimated_duration_minutes} minutos</strong>
                </div>
              </div>
            </div>

            <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#ffffff', marginBottom: '12px' }}>
              Secuencia de Entrenamiento
            </h4>
            <div className="table-container">
              <table className="custom-table">
                <thead>
                  <tr>
                    <th>Orden</th>
                    <th>Ejercicio</th>
                    <th>Series x Repeticiones</th>
                    <th>Descanso</th>
                    <th>Carga Sugerida</th>
                  </tr>
                </thead>
                <tbody>
                  {detailRoutine.exercises?.map((item) => (
                    <tr key={item.id}>
                      <td style={{ fontWeight: 700, color: 'var(--accent-cyan)' }}>#{item.sequence_order}</td>
                      <td style={{ fontWeight: 600 }}>{item.exercise_name}</td>
                      <td>
                        <span className="badge badge-info">
                          {item.sets} series x {item.reps} reps
                        </span>
                      </td>
                      <td>{item.rest_seconds} segundos</td>
                      <td>{item.recommended_weight_kg ? `${item.recommended_weight_kg} kg` : 'Autocarga'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </Modal>

      {/* Confirmación Eliminar */}
      <ConfirmDialog
        isOpen={deleteRoutine !== null}
        title="Eliminar Rutina"
        message={`¿Estás seguro de dar de baja la rutina "${deleteRoutine?.name}"? Se realizará un soft-delete protegiendo los entrenamientos históricos de los usuarios.`}
        confirmLabel="Eliminar Rutina"
        loading={actionLoading}
        onConfirm={handleDelete}
        onCancel={() => setDeleteRoutine(null)}
      />
    </div>
  );
};
