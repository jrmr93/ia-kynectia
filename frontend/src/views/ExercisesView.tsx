import React, { useEffect, useState, useCallback } from 'react';
import { apiRequest, getMediaUrl } from '../api/client';
import type { Exercise, MuscleGroup, Equipment } from '../types';
import { useToast } from '../context/ToastContext';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { ImageUpload } from '../components/ImageUpload';
import { HumanBodyVisualizer } from '../components/HumanBodyVisualizer';
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
  Film,
  UploadCloud,
  CheckCircle2,
  Filter,
  X,
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
  const [selectedMuscleGroupId, setSelectedMuscleGroupId] = useState<number | null>(null);
  const [hoveredMuscleGroupId, setHoveredMuscleGroupId] = useState<number | null>(null);
  const [showBodyVisualizer, setShowBodyVisualizer] = useState(true);
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

  const [uploadingVideo, setUploadingVideo] = useState(false);
  const [videoInputMode, setVideoInputMode] = useState<'upload' | 'url'>('upload');

  const handleVideoFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('video/')) {
      showToast('error', 'Por favor selecciona un archivo de video válido (MP4, WebM, MOV).');
      return;
    }

    if (file.size > 100 * 1024 * 1024) {
      showToast('error', 'El video no debe exceder los 100MB.');
      return;
    }

    setUploadingVideo(true);
    try {
      const data = new FormData();
      data.append('video', file);

      const res = await apiRequest<{ videoUrl: string }>('/exercises/upload-video', {
        method: 'POST',
        body: data,
        isFormData: true,
      });

      setFormData((prev) => ({ ...prev, demoVideoUrl: res.data.videoUrl }));
      showToast('success', 'Video subido correctamente al almacenamiento.');
    } catch (err: any) {
      showToast('error', err.message || 'Error al subir el video.');
    } finally {
      setUploadingVideo(false);
      e.target.value = '';
    }
  };

  const handleRemoveVideo = async () => {
    if (!formData.demoVideoUrl) return;

    const confirmDelete = window.confirm(
      '¿Deseas eliminar este video? Se borrará permanentemente del almacenamiento para no consumir espacio en disco.'
    );
    if (!confirmDelete) return;

    setActionLoading(true);
    try {
      if (editingExercise?.id && editingExercise.demo_video_url === formData.demoVideoUrl) {
        await apiRequest(`/exercises/${editingExercise.id}/video`, { method: 'DELETE' });
      } else {
        await apiRequest('/exercises/delete-video-file', {
          method: 'POST',
          body: { videoUrl: formData.demoVideoUrl },
        });
      }
      setFormData((prev) => ({ ...prev, demoVideoUrl: '' }));
      showToast('success', 'Video eliminado y espacio liberado.');
      fetchExercises();
    } catch (err: any) {
      showToast('error', err.message || 'Error al eliminar el video.');
    } finally {
      setActionLoading(false);
    }
  };

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
      if (selectedMuscleGroupId) params.muscleGroupId = selectedMuscleGroupId;

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
  }, [page, search, difficultyFilter, typeFilter, statusFilter, selectedMuscleGroupId, showToast]);

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

  const selectedMuscleObj = muscleGroups.find((m) => m.id === selectedMuscleGroupId);

  return (
    <div className="page-container">
      {/* Encabezado */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#ffffff' }}>Catálogo de Ejercicios</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            Repositorio maestro de movimientos, instrucciones técnicas, seguridad y multimedia ({totalCount} registrados).
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            type="button"
            onClick={() => setShowBodyVisualizer(!showBodyVisualizer)}
            className="btn btn-secondary"
            title="Alternar vista del modelo anatómico interactivo"
          >
            <Activity size={16} color="var(--accent-cyan)" />
            <span>{showBodyVisualizer ? 'Ocultar Modelo Anatómico' : 'Ver Modelo Anatómico'}</span>
          </button>
          <button onClick={handleOpenCreate} className="btn btn-primary">
            <Plus size={18} />
            <span>Nuevo Ejercicio</span>
          </button>
        </div>
      </div>

      {/* Modelo Anatómico Interactivo para filtrado por grupo muscular */}
      {showBodyVisualizer && (
        <HumanBodyVisualizer
          muscles={muscleGroups}
          selectedMuscleId={selectedMuscleGroupId}
          hoveredMuscleId={hoveredMuscleGroupId}
          onSelectMuscle={(m) => {
            setSelectedMuscleGroupId(m ? m.id : null);
            setPage(1);
          }}
        />
      )}

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

        {selectedMuscleGroupId && selectedMuscleObj && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '6px 14px',
              background: 'rgba(0, 242, 254, 0.12)',
              borderRadius: '8px',
              border: '1px solid var(--accent-cyan)',
              color: 'var(--accent-cyan)',
              fontSize: '0.82rem',
              fontWeight: 600,
            }}
          >
            <Filter size={14} />
            <span>Músculo: {selectedMuscleObj.name}</span>
            <button
              type="button"
              onClick={() => {
                setSelectedMuscleGroupId(null);
                setPage(1);
              }}
              style={{
                background: 'none',
                border: 'none',
                color: 'var(--accent-cyan)',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                padding: 0,
              }}
              title="Quitar filtro de músculo"
            >
              <X size={15} />
            </button>
          </div>
        )}

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
                <tr
                  key={ex.id}
                  onMouseEnter={() => setHoveredMuscleGroupId((ex as any).primary_muscle_id || null)}
                  onMouseLeave={() => setHoveredMuscleGroupId(null)}
                >
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div
                        style={{
                          width: '46px',
                          height: '46px',
                          borderRadius: '8px',
                          background: ex.demo_image_url && !ex.demo_video_url
                            ? `url(${getMediaUrl(ex.demo_image_url)}) center/cover`
                            : 'rgba(0, 242, 254, 0.1)',
                          border: '1px solid var(--border-subtle)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          flexShrink: 0,
                          position: 'relative',
                          overflow: 'hidden',
                        }}
                      >
                        {ex.demo_video_url ? (
                          <video
                            src={getMediaUrl(ex.demo_video_url)}
                            muted
                            loop
                            autoPlay
                            playsInline
                            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                          />
                        ) : !ex.demo_image_url ? (
                          <Activity size={18} color="var(--accent-cyan)" />
                        ) : null}
                        {ex.demo_video_url && (
                          <div
                            style={{
                              position: 'absolute',
                              bottom: '2px',
                              right: '2px',
                              background: 'rgba(0,0,0,0.7)',
                              borderRadius: '3px',
                              padding: '1px 3px',
                            }}
                          >
                            <Film size={10} color="#00f2fe" />
                          </div>
                        )}
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

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
            <ImageUpload
              value={formData.demoImageUrl}
              onChange={(url) => setFormData({ ...formData, demoImageUrl: url })}
              label="Imagen Demostrativa"
              helperText="Sube la imagen ilustrativa del ejercicio a uploads/ o proporciona una URL."
              disabled={actionLoading}
            />

            <div className="form-group">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <label className="form-label" style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Film size={15} color="var(--accent-cyan)" />
                  Video Demostrativo
                </label>
                <button
                  type="button"
                  onClick={() => setVideoInputMode(videoInputMode === 'upload' ? 'url' : 'upload')}
                  style={{ background: 'none', border: 'none', color: 'var(--accent-cyan)', fontSize: '0.75rem', cursor: 'pointer', textDecoration: 'underline' }}
                >
                  {videoInputMode === 'upload' ? 'Ingresar URL manual' : 'Subir archivo de video'}
                </button>
              </div>

              {formData.demoVideoUrl ? (
                <div style={{
                  padding: '12px',
                  borderRadius: '10px',
                  background: 'rgba(0, 242, 254, 0.04)',
                  border: '1px solid rgba(0, 242, 254, 0.2)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '8px',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <span style={{ fontSize: '0.8rem', color: '#10b981', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <CheckCircle2 size={14} /> Video configurado
                    </span>
                    <button
                      type="button"
                      onClick={handleRemoveVideo}
                      disabled={actionLoading}
                      className="btn btn-sm btn-danger"
                      style={{ padding: '4px 10px', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                      title="Eliminar y liberar espacio en disco"
                    >
                      <Trash2 size={13} /> Eliminar video
                    </button>
                  </div>
                  <video
                    src={getMediaUrl(formData.demoVideoUrl)}
                    controls
                    loop
                    muted
                    playsInline
                    style={{
                      width: '100%',
                      maxHeight: '140px',
                      borderRadius: '8px',
                      background: '#000',
                      objectFit: 'contain',
                    }}
                  />
                  <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)', wordBreak: 'break-all' }}>
                    Almacenamiento: {formData.demoVideoUrl}
                  </span>
                </div>
              ) : videoInputMode === 'upload' ? (
                <label
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '20px 16px',
                    borderRadius: '10px',
                    border: '2px dashed var(--border-subtle)',
                    background: 'rgba(255, 255, 255, 0.02)',
                    cursor: uploadingVideo ? 'wait' : 'pointer',
                    transition: 'all 0.2s ease',
                    textAlign: 'center',
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.borderColor = 'var(--accent-cyan)')}
                  onMouseLeave={(e) => (e.currentTarget.style.borderColor = 'var(--border-subtle)')}
                >
                  <input
                    type="file"
                    accept="video/mp4,video/webm,video/quicktime,video/x-matroska,video/ogg"
                    style={{ display: 'none' }}
                    disabled={uploadingVideo}
                    onChange={handleVideoFileChange}
                  />
                  {uploadingVideo ? (
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
                      <RefreshCw size={24} className="spin" color="var(--accent-cyan)" />
                      <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                        Subiendo y almacenando video...
                      </span>
                    </div>
                  ) : (
                    <>
                      <UploadCloud size={28} color="var(--accent-cyan)" style={{ marginBottom: '6px' }} />
                      <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#ffffff' }}>
                        Subir video demostrativo
                      </span>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                        MP4, WebM o MOV (Hasta 100MB)
                      </span>
                    </>
                  )}
                </label>
              ) : (
                <div>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.demoVideoUrl}
                    onChange={(e) => setFormData({ ...formData, demoVideoUrl: e.target.value })}
                    placeholder="https://... o /uploads/videos/..."
                  />
                </div>
              )}
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
              {detailExercise.demo_video_url ? (
                <div style={{ width: '260px', height: '175px', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--border-subtle)', background: '#000', position: 'relative' }}>
                  <video
                    src={getMediaUrl(detailExercise.demo_video_url)}
                    autoPlay
                    loop
                    muted
                    playsInline
                    controls
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                  <div style={{ position: 'absolute', top: '8px', left: '8px', background: 'rgba(0,0,0,0.7)', borderRadius: '4px', padding: '2px 6px', fontSize: '0.68rem', color: '#00f2fe', display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <Film size={10} /> VIDEO DEMOSTRATIVO
                  </div>
                </div>
              ) : detailExercise.demo_image_url ? (
                <img
                  src={getMediaUrl(detailExercise.demo_image_url)}
                  alt={detailExercise.name}
                  style={{ width: '220px', height: '160px', objectFit: 'cover', borderRadius: '12px', border: '1px solid var(--border-subtle)' }}
                />
              ) : null}
              <div style={{ flex: 1, minWidth: '240px' }}>
                <div style={{ display: 'flex', gap: '8px', marginBottom: '10px' }}>
                  <span className="badge badge-info">{detailExercise.primary_muscle_name || (detailExercise as any).primary_muscle?.name}</span>
                  <span className="badge badge-purple">{detailExercise.difficulty_level}</span>
                  <span className="badge badge-success">{detailExercise.publication_status}</span>
                </div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: '1.5' }}>
                  {detailExercise.description || 'Sin descripción general.'}
                </p>
                {detailExercise.demo_video_url && (
                  <div style={{ marginTop: '12px', fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                    <span>Archivo de video: {detailExercise.demo_video_url}</span>
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
