import React, { useState, useRef } from 'react';
import { UploadCloud, Trash2, CheckCircle2, AlertCircle, RefreshCw, Eye, Image as ImageIcon } from 'lucide-react';
import { apiRequest, getMediaUrl } from '../api/client';
import { useToast } from '../context/ToastContext';

interface ImageUploadProps {
  value: string;
  onChange: (url: string) => void;
  label?: string;
  helperText?: string;
  disabled?: boolean;
}

export const ImageUpload: React.FC<ImageUploadProps> = ({
  value,
  onChange,
  label = 'Imagen Ilustrativa',
  helperText = 'Formatos soportados: JPG, PNG, WEBP o GIF. Máx. 10MB.',
  disabled = false,
}) => {
  const { showToast } = useToast();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const [uploading, setUploading] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const [previewError, setPreviewError] = useState(false);
  const [showUrlInput, setShowUrlInput] = useState(false);

  const previewUrl = value ? getMediaUrl(value) : '';

  const handleFileUpload = async (file: File) => {
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      showToast('error', 'El archivo seleccionado debe ser una imagen (JPG, PNG, WEBP, GIF).');
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      showToast('error', 'El archivo excede el tamaño máximo permitido de 10 MB.');
      return;
    }

    setUploading(true);
    setPreviewError(false);

    try {
      const formData = new FormData();
      formData.append('file', file);

      const res = await apiRequest<{ file_path: string; fullUrl: string }>('/media/upload', {
        method: 'POST',
        body: formData,
        isFormData: true,
      });

      if (res.data && res.data.file_path) {
        // Si ya existía un archivo local previo diferente al nuevo, eliminarlo físicamente
        if (value && value !== res.data.file_path && value.includes('/uploads/')) {
          try {
            await apiRequest('/media/delete-by-path', {
              method: 'POST',
              body: { fileUrl: value },
            });
          } catch (e) {
            console.warn('No se pudo remover el archivo anterior:', e);
          }
        }

        onChange(res.data.file_path);
        showToast('success', 'Imagen subida exitosamente al directorio uploads.');
      }
    } catch (err: any) {
      showToast('error', err.message || 'Error al subir la imagen al servidor.');
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleFileUpload(file);
    }
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragOver(false);
    if (disabled || uploading) return;

    const file = e.dataTransfer.files?.[0];
    if (file) {
      handleFileUpload(file);
    }
  };

  const handleRemoveImage = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (disabled || deleting) return;

    setDeleting(true);
    try {
      if (value && value.includes('/uploads/')) {
        await apiRequest('/media/delete-by-path', {
          method: 'POST',
          body: { fileUrl: value },
        });
        showToast('success', 'Imagen eliminada del directorio uploads con éxito.');
      } else {
        showToast('info', 'Referencia de imagen eliminada.');
      }
      onChange('');
      setPreviewError(false);
    } catch (err: any) {
      showToast('error', err.message || 'Error al eliminar el archivo del servidor.');
      // Aún así limpiamos el campo si el usuario lo desea
      onChange('');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="form-group" style={{ marginBottom: '18px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <label className="form-label" style={{ marginBottom: 0 }}>{label}</label>
        <button
          type="button"
          onClick={() => setShowUrlInput(!showUrlInput)}
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--accent-cyan)',
            fontSize: '0.78rem',
            cursor: 'pointer',
            textDecoration: 'underline',
            padding: 0,
          }}
        >
          {showUrlInput ? 'Volver a Subir Archivo' : 'O ingresar URL externa'}
        </button>
      </div>

      {showUrlInput ? (
        <div>
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
              value={value}
              onChange={(e) => {
                setPreviewError(false);
                onChange(e.target.value);
              }}
              placeholder="https://images.unsplash.com/photo-..."
              disabled={disabled}
            />
          </div>
          {value && (
            <div style={{ marginTop: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Vista previa:</span>
              <img
                src={previewUrl}
                alt="Vista previa"
                style={{ width: '36px', height: '36px', objectFit: 'cover', borderRadius: '4px', border: '1px solid var(--border-subtle)' }}
                onError={() => setPreviewError(true)}
              />
              <button
                type="button"
                onClick={handleRemoveImage}
                className="btn btn-secondary btn-sm"
                style={{ color: '#ef4444', padding: '2px 8px', fontSize: '0.75rem' }}
              >
                Limpiar
              </button>
            </div>
          )}
        </div>
      ) : (
        <div>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            style={{ display: 'none' }}
            onChange={handleFileChange}
            disabled={disabled || uploading}
          />

          {value ? (
            /* Vista previa con acciones de gestión */
            <div
              style={{
                position: 'relative',
                borderRadius: '10px',
                border: '1px solid var(--border-subtle)',
                overflow: 'hidden',
                background: '#0d131f',
                display: 'flex',
                alignItems: 'center',
                padding: '12px 16px',
                gap: '16px',
              }}
            >
              {/* Miniatura */}
              <div
                style={{
                  width: '90px',
                  height: '70px',
                  borderRadius: '8px',
                  overflow: 'hidden',
                  background: '#1a2234',
                  flexShrink: 0,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: '1px solid rgba(255, 255, 255, 0.08)',
                }}
              >
                {!previewError ? (
                  <img
                    src={previewUrl}
                    alt="Preview"
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    onError={() => setPreviewError(true)}
                  />
                ) : (
                  <div style={{ textAlign: 'center', color: '#f59e0b', fontSize: '0.7rem' }}>
                    <AlertCircle size={20} />
                    <div>Error img</div>
                  </div>
                )}
              </div>

              {/* Info y ruta */}
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px' }}>
                  <CheckCircle2 size={15} color="var(--accent-cyan)" />
                  <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#ffffff' }}>
                    {value.includes('/uploads/') ? 'Archivo en uploads/' : 'Imagen configurada'}
                  </span>
                </div>
                <p
                  style={{
                    fontSize: '0.74rem',
                    color: 'var(--text-secondary)',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    marginBottom: '8px',
                  }}
                  title={value}
                >
                  {value}
                </p>

                {/* Botones de acción */}
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={disabled || uploading || deleting}
                    className="btn btn-secondary btn-sm"
                    style={{ fontSize: '0.75rem', padding: '4px 10px' }}
                  >
                    <UploadCloud size={13} />
                    <span>Cambiar</span>
                  </button>

                  <a
                    href={previewUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="btn btn-secondary btn-sm"
                    style={{ fontSize: '0.75rem', padding: '4px 10px' }}
                    title="Ver imagen completa"
                  >
                    <Eye size={13} />
                    <span>Ver</span>
                  </a>

                  <button
                    type="button"
                    onClick={handleRemoveImage}
                    disabled={disabled || uploading || deleting}
                    className="btn btn-danger btn-sm"
                    style={{ fontSize: '0.75rem', padding: '4px 10px' }}
                    title="Eliminar imagen del directorio y desasociar"
                  >
                    {deleting ? (
                      <RefreshCw size={13} className="spin-animation" />
                    ) : (
                      <Trash2 size={13} />
                    )}
                    <span>{deleting ? 'Eliminando...' : 'Eliminar'}</span>
                  </button>
                </div>
              </div>
            </div>
          ) : (
            /* Área Drag & Drop para subir imagen */
            <div
              onDragOver={(e) => {
                e.preventDefault();
                setDragOver(true);
              }}
              onDragLeave={() => setDragOver(false)}
              onDrop={handleDrop}
              onClick={() => {
                if (!disabled && !uploading) {
                  fileInputRef.current?.click();
                }
              }}
              style={{
                borderRadius: '10px',
                border: `2px dashed ${dragOver ? 'var(--accent-cyan)' : 'rgba(255, 255, 255, 0.12)'}`,
                background: dragOver ? 'rgba(0, 242, 254, 0.05)' : 'rgba(255, 255, 255, 0.015)',
                padding: '24px 16px',
                textAlign: 'center',
                cursor: disabled || uploading ? 'not-allowed' : 'pointer',
                transition: 'all 0.2s ease',
              }}
            >
              {uploading ? (
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
                  <RefreshCw size={26} className="spin-animation" style={{ color: 'var(--accent-cyan)' }} />
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    Subiendo imagen al directorio uploads...
                  </span>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
                  <div
                    style={{
                      width: '42px',
                      height: '42px',
                      borderRadius: '50%',
                      background: 'rgba(0, 242, 254, 0.1)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: 'var(--accent-cyan)',
                    }}
                  >
                    <UploadCloud size={20} />
                  </div>
                  <div>
                    <span style={{ fontSize: '0.86rem', fontWeight: 600, color: '#ffffff' }}>
                      Haz clic para subir
                    </span>{' '}
                    <span style={{ fontSize: '0.84rem', color: 'var(--text-secondary)' }}>
                      o arrastra y suelta tu archivo aquí
                    </span>
                  </div>
                  <span style={{ fontSize: '0.74rem', color: 'var(--text-muted)' }}>
                    {helperText}
                  </span>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
