import React, { useState } from 'react';
import type { MuscleGroup } from '../types';
import { Activity, Info, Check, Edit2 } from 'lucide-react';

export type AnatomicalZone =
  | 'chest'
  | 'shoulders'
  | 'biceps'
  | 'triceps'
  | 'forearms'
  | 'core'
  | 'back'
  | 'glutes'
  | 'quads'
  | 'hamstrings'
  | 'calves';

interface HumanBodyVisualizerProps {
  muscles: MuscleGroup[];
  selectedMuscleId?: number | null;
  hoveredMuscleId?: number | null;
  onSelectMuscle?: (muscle: MuscleGroup | null) => void;
  onOpenEdit?: (muscle: MuscleGroup) => void;
}

// Mapea los nombres de la base de datos a zonas anatómicas
export const mapMuscleToZone = (name: string): AnatomicalZone[] => {
  const n = name.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');

  if (n.includes('pectoral') || n.includes('pecho') || n.includes('chest')) {
    return ['chest'];
  }
  if (n.includes('deltoide') || n.includes('hombro') || n.includes('shoulder')) {
    return ['shoulders'];
  }
  if (n.includes('bicep')) {
    return ['biceps'];
  }
  if (n.includes('tricep')) {
    return ['triceps'];
  }
  if (n.includes('antebrazo') || n.includes('forearm')) {
    return ['forearms'];
  }
  if (n.includes('abdomen') || n.includes('core') || n.includes('abdominal') || n.includes('oblicuo')) {
    return ['core'];
  }
  if (n.includes('espalda') || n.includes('dorsal') || n.includes('trapecio') || n.includes('back') || n.includes('lumbar')) {
    return ['back'];
  }
  if (n.includes('gluteo') || n.includes('glute')) {
    return ['glutes'];
  }
  if (n.includes('cuadric') || n.includes('quad')) {
    return ['quads'];
  }
  if (n.includes('isquio') || n.includes('femoral') || n.includes('hamstring')) {
    return ['hamstrings'];
  }
  if (n.includes('pantorrilla') || n.includes('gemelo') || n.includes('soleo') || n.includes('calf')) {
    return ['calves'];
  }
  return [];
};

export const HumanBodyVisualizer: React.FC<HumanBodyVisualizerProps> = ({
  muscles,
  selectedMuscleId,
  hoveredMuscleId,
  onSelectMuscle,
  onOpenEdit,
}) => {
  const [viewMode, setViewMode] = useState<'both' | 'anterior' | 'posterior'>('both');
  const [hoveredZone, setHoveredZone] = useState<AnatomicalZone | null>(null);

  // Determinar qué zonas están activas según el músculo seleccionado o en hover
  const activeMuscle = muscles.find((m) => m.id === (hoveredMuscleId || selectedMuscleId));
  const activeZones = activeMuscle ? mapMuscleToZone(activeMuscle.name) : [];

  // Buscar el objeto MuscleGroup que corresponde a una zona
  const getMuscleForZone = (zone: AnatomicalZone): MuscleGroup | undefined => {
    return muscles.find((m) => mapMuscleToZone(m.name).includes(zone));
  };

  const handleZoneClick = (zone: AnatomicalZone) => {
    const matched = getMuscleForZone(zone);
    if (matched) {
      if (selectedMuscleId === matched.id) {
        onSelectMuscle?.(null);
      } else {
        onSelectMuscle?.(matched);
      }
    }
  };

  const handleMouseEnter = (zone: AnatomicalZone, _e?: React.MouseEvent) => {
    setHoveredZone(zone);
  };

  const handleMouseLeave = () => {
    setHoveredZone(null);
  };

  const getZoneFill = (zone: AnatomicalZone) => {
    const isSelected = activeZones.includes(zone);
    const isHovered = hoveredZone === zone;

    if (isHovered) return '#10b981'; // Verde Neón al pasar cursor
    if (isSelected) return '#00f2fe'; // Azul/Cyan Neón activo
    return '#1c2436'; // Base anatómica oscura sutil
  };

  const getZoneStroke = (zone: AnatomicalZone) => {
    const isSelected = activeZones.includes(zone);
    const isHovered = hoveredZone === zone;

    if (isHovered) return '#34d399';
    if (isSelected) return '#38bdf8';
    return '#2d3748';
  };

  const getZoneOpacity = (zone: AnatomicalZone) => {
    const isSelected = activeZones.includes(zone);
    const isHovered = hoveredZone === zone;
    if (isSelected || isHovered) return 0.95;
    return 0.75;
  };

  const hoveredMuscleObj = hoveredZone ? getMuscleForZone(hoveredZone) : null;

  return (
    <div
      className="card"
      style={{
        padding: '20px 24px',
        marginBottom: '24px',
        background: 'linear-gradient(180deg, rgba(17, 24, 39, 0.9) 0%, rgba(10, 15, 26, 0.95) 100%)',
        border: '1px solid rgba(0, 242, 254, 0.15)',
        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.4)',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Fondo decorativo con rejilla futurista */}
      <div
        style={{
          position: 'absolute',
          top: 0,
          right: 0,
          width: '300px',
          height: '300px',
          background: 'radial-gradient(circle, rgba(0, 242, 254, 0.05) 0%, transparent 70%)',
          pointerEvents: 'none',
        }}
      />

      {/* Barra Superior con Selector de Vista y Estado */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '12px',
          marginBottom: '16px',
          borderBottom: '1px solid rgba(255, 255, 255, 0.06)',
          paddingBottom: '14px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div
            style={{
              width: '34px',
              height: '34px',
              borderRadius: '8px',
              background: 'rgba(0, 242, 254, 0.1)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--accent-cyan)',
            }}
          >
            <Activity size={18} />
          </div>
          <div>
            <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#ffffff', margin: 0 }}>
              Explorador Anatómico Interactivo
            </h3>
            <p style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', margin: 0 }}>
              Haz clic en cualquier grupo muscular del cuerpo para seleccionarlo, filtrarlo o editarlo.
            </p>
          </div>
        </div>

        {/* Controles de Vista */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div
            style={{
              display: 'inline-flex',
              background: '#0d131f',
              padding: '3px',
              borderRadius: '8px',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <button
              type="button"
              onClick={() => setViewMode('both')}
              className={`btn btn-sm ${viewMode === 'both' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ padding: '4px 10px', fontSize: '0.75rem', border: 'none' }}
            >
              Frente y Espalda
            </button>
            <button
              type="button"
              onClick={() => setViewMode('anterior')}
              className={`btn btn-sm ${viewMode === 'anterior' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ padding: '4px 10px', fontSize: '0.75rem', border: 'none' }}
            >
              Frente (Anterior)
            </button>
            <button
              type="button"
              onClick={() => setViewMode('posterior')}
              className={`btn btn-sm ${viewMode === 'posterior' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ padding: '4px 10px', fontSize: '0.75rem', border: 'none' }}
            >
              Espalda (Posterior)
            </button>
          </div>

          {selectedMuscleId && (
            <button
              type="button"
              onClick={() => onSelectMuscle?.(null)}
              className="btn btn-secondary btn-sm"
              style={{ fontSize: '0.75rem', padding: '4px 10px' }}
            >
              Mostrar Todos
            </button>
          )}
        </div>
      </div>

      {/* Contenedor Principal de Siluetas Anatómicas */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          gap: '32px',
          padding: '10px 0',
          position: 'relative',
        }}
      >
        {/* SVG Filtros para Efectos de Resplandor Neón */}
        <svg width="0" height="0" style={{ position: 'absolute' }}>
          <defs>
            <filter id="neon-glow" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="3" result="blur" />
              <feComposite in="SourceGraphic" in2="blur" operator="over" />
            </filter>
          </defs>
        </svg>

        {/* VISTA ANTERIOR (FRENTE) */}
        {(viewMode === 'both' || viewMode === 'anterior') && (
          <div style={{ textAlign: 'center' }}>
            <span
              style={{
                fontSize: '0.75rem',
                fontWeight: 600,
                color: 'var(--text-muted)',
                letterSpacing: '0.08em',
                textTransform: 'uppercase',
                display: 'block',
                marginBottom: '8px',
              }}
            >
              Vista Anterior (Frente)
            </span>
            <svg
              viewBox="0 0 200 420"
              style={{
                width: '180px',
                height: '380px',
                filter: 'drop-shadow(0 6px 16px rgba(0,0,0,0.5))',
                cursor: 'pointer',
              }}
            >
              {/* Silueta Base de fondo */}
              <g style={{ fill: '#0f1422', stroke: '#1f293d', strokeWidth: 1.2 }}>
                {/* Cabeza */}
                <circle cx="100" cy="30" r="16" />
                {/* Cuello */}
                <path d="M 94,46 L 94,56 L 106,56 L 106,46 Z" />
                {/* Manos */}
                <ellipse cx="37" cy="226" rx="6" ry="12" />
                <ellipse cx="163" cy="226" rx="6" ry="12" />
                {/* Pies */}
                <ellipse cx="80" cy="402" rx="10" ry="7" />
                <ellipse cx="120" cy="402" rx="10" ry="7" />
              </g>

              {/* GRUPO: HOMBROS / DELTOIDES (ANTERIOR) */}
              <g
                onClick={() => handleZoneClick('shoulders')}
                onMouseEnter={(e) => handleMouseEnter('shoulders', e)}
                onMouseLeave={handleMouseLeave}
                style={{
                  fill: getZoneFill('shoulders'),
                  stroke: getZoneStroke('shoulders'),
                  strokeWidth: 1.5,
                  opacity: getZoneOpacity('shoulders'),
                  transition: 'all 0.2s ease',
                }}
              >
                {/* Deltoides Izquierdo */}
                <path d="M 76,58 C 65,60 55,70 54,82 C 53,92 60,98 68,96 C 73,90 76,78 77,68 Z" />
                {/* Deltoides Derecho */}
                <path d="M 124,58 C 135,60 145,70 146,82 C 147,92 140,98 132,96 C 127,90 124,78 123,68 Z" />
              </g>

              {/* GRUPO: PECTORALES (PECHO) */}
              <g
                onClick={() => handleZoneClick('chest')}
                onMouseEnter={(e) => handleMouseEnter('chest', e)}
                onMouseLeave={handleMouseLeave}
                style={{
                  fill: getZoneFill('chest'),
                  stroke: getZoneStroke('chest'),
                  strokeWidth: 1.5,
                  opacity: getZoneOpacity('chest'),
                  transition: 'all 0.2s ease',
                }}
              >
                {/* Pectoral Izquierdo */}
                <path d="M 98,68 C 88,68 76,74 74,86 C 72,106 82,118 97,118 C 98,110 98,82 98,68 Z" />
                {/* Pectoral Derecho */}
                <path d="M 102,68 C 112,68 124,74 126,86 C 128,106 118,118 103,118 C 102,110 102,82 102,68 Z" />
              </g>

              {/* GRUPO: BÍCEPS */}
              <g
                onClick={() => handleZoneClick('biceps')}
                onMouseEnter={(e) => handleMouseEnter('biceps', e)}
                onMouseLeave={handleMouseLeave}
                style={{
                  fill: getZoneFill('biceps'),
                  stroke: getZoneStroke('biceps'),
                  strokeWidth: 1.5,
                  opacity: getZoneOpacity('biceps'),
                  transition: 'all 0.2s ease',
                }}
              >
                {/* Bíceps Izquierdo */}
                <path d="M 64,98 C 56,102 53,116 54,134 C 55,142 62,146 66,142 C 68,132 68,114 64,98 Z" />
                {/* Bíceps Derecho */}
                <path d="M 136,98 C 144,102 147,116 146,134 C 145,142 138,146 134,142 C 132,132 132,114 136,98 Z" />
              </g>

              {/* GRUPO: ANTEBRAZOS (ANTERIOR) */}
              <g
                onClick={() => handleZoneClick('forearms')}
                onMouseEnter={(e) => handleMouseEnter('forearms', e)}
                onMouseLeave={handleMouseLeave}
                style={{
                  fill: getZoneFill('forearms'),
                  stroke: getZoneStroke('forearms'),
                  strokeWidth: 1.5,
                  opacity: getZoneOpacity('forearms'),
                  transition: 'all 0.2s ease',
                }}
              >
                {/* Antebrazo Izquierdo */}
                <path d="M 54,146 C 46,156 42,176 40,204 C 40,214 46,216 48,212 C 54,196 60,172 62,150 Z" />
                {/* Antebrazo Derecho */}
                <path d="M 146,146 C 154,156 158,176 160,204 C 160,214 154,216 152,212 C 146,196 140,172 138,150 Z" />
              </g>

              {/* GRUPO: ABDOMEN Y CORE (ANTERIOR) */}
              <g
                onClick={() => handleZoneClick('core')}
                onMouseEnter={(e) => handleMouseEnter('core', e)}
                onMouseLeave={handleMouseLeave}
                style={{
                  fill: getZoneFill('core'),
                  stroke: getZoneStroke('core'),
                  strokeWidth: 1.5,
                  opacity: getZoneOpacity('core'),
                  transition: 'all 0.2s ease',
                }}
              >
                {/* Recto Abdominal Superior */}
                <path d="M 88,122 L 98,122 L 98,140 L 86,140 Z" />
                <path d="M 102,122 L 112,122 L 114,140 L 102,140 Z" />
                {/* Recto Abdominal Medio */}
                <path d="M 86,143 L 98,143 L 98,162 L 86,162 Z" />
                <path d="M 102,143 L 114,143 L 114,162 L 102,162 Z" />
                {/* Recto Abdominal Inferior */}
                <path d="M 87,165 L 98,165 L 98,186 L 88,186 Z" />
                <path d="M 102,165 L 113,165 L 112,186 L 102,186 Z" />
                {/* Oblicuos Izq y Der */}
                <path d="M 74,124 C 72,144 76,168 84,184 L 84,124 Z" />
                <path d="M 126,124 C 128,144 124,168 116,184 L 116,124 Z" />
              </g>

              {/* Pelvis / Centro */}
              <path d="M 86,188 L 114,188 L 108,212 L 92,212 Z" fill="#141a29" stroke="#253047" strokeWidth="1" />

              {/* GRUPO: CUÁDRICEPS */}
              <g
                onClick={() => handleZoneClick('quads')}
                onMouseEnter={(e) => handleMouseEnter('quads', e)}
                onMouseLeave={handleMouseLeave}
                style={{
                  fill: getZoneFill('quads'),
                  stroke: getZoneStroke('quads'),
                  strokeWidth: 1.5,
                  opacity: getZoneOpacity('quads'),
                  transition: 'all 0.2s ease',
                }}
              >
                {/* Cuádriceps Izquierdo */}
                <path d="M 76,212 C 68,226 66,260 70,296 C 76,302 88,302 92,296 C 96,260 94,228 90,212 Z" />
                {/* Cuádriceps Derecho */}
                <path d="M 124,212 C 132,226 134,260 130,296 C 124,302 112,302 108,296 C 104,260 106,228 110,212 Z" />
              </g>

              {/* Rodillas */}
              <circle cx="81" cy="306" r="6" fill="#161e30" stroke="#2a3752" strokeWidth="1" />
              <circle cx="119" cy="306" r="6" fill="#161e30" stroke="#2a3752" strokeWidth="1" />

              {/* GRUPO: PANTORRILLAS / TIBIALES (ANTERIOR) */}
              <g
                onClick={() => handleZoneClick('calves')}
                onMouseEnter={(e) => handleMouseEnter('calves', e)}
                onMouseLeave={handleMouseLeave}
                style={{
                  fill: getZoneFill('calves'),
                  stroke: getZoneStroke('calves'),
                  strokeWidth: 1.5,
                  opacity: getZoneOpacity('calves'),
                  transition: 'all 0.2s ease',
                }}
              >
                {/* Espinilla / Gemelo Anterior Izquierdo */}
                <path d="M 76,316 C 70,328 70,358 76,388 C 82,390 86,390 88,388 C 90,360 88,332 86,316 Z" />
                {/* Espinilla / Gemelo Anterior Derecho */}
                <path d="M 124,316 C 130,328 130,358 124,388 C 118,390 114,390 112,388 C 110,360 112,332 114,316 Z" />
              </g>
            </svg>
          </div>
        )}

        {/* VISTA POSTERIOR (ESPALDA) */}
        {(viewMode === 'both' || viewMode === 'posterior') && (
          <div style={{ textAlign: 'center' }}>
            <span
              style={{
                fontSize: '0.75rem',
                fontWeight: 600,
                color: 'var(--text-muted)',
                letterSpacing: '0.08em',
                textTransform: 'uppercase',
                display: 'block',
                marginBottom: '8px',
              }}
            >
              Vista Posterior (Espalda)
            </span>
            <svg
              viewBox="0 0 200 420"
              style={{
                width: '180px',
                height: '380px',
                filter: 'drop-shadow(0 6px 16px rgba(0,0,0,0.5))',
                cursor: 'pointer',
              }}
            >
              {/* Silueta Base de fondo */}
              <g style={{ fill: '#0f1422', stroke: '#1f293d', strokeWidth: 1.2 }}>
                {/* Cabeza posterior */}
                <circle cx="100" cy="30" r="16" />
                {/* Manos */}
                <ellipse cx="37" cy="226" rx="6" ry="12" />
                <ellipse cx="163" cy="226" rx="6" ry="12" />
                {/* Pies */}
                <ellipse cx="80" cy="402" rx="9" ry="6" />
                <ellipse cx="120" cy="402" rx="9" ry="6" />
              </g>

              {/* GRUPO: HOMBROS / DELTOIDES (POSTERIOR) */}
              <g
                onClick={() => handleZoneClick('shoulders')}
                onMouseEnter={(e) => handleMouseEnter('shoulders', e)}
                onMouseLeave={handleMouseLeave}
                style={{
                  fill: getZoneFill('shoulders'),
                  stroke: getZoneStroke('shoulders'),
                  strokeWidth: 1.5,
                  opacity: getZoneOpacity('shoulders'),
                  transition: 'all 0.2s ease',
                }}
              >
                {/* Deltoides Posterior Izquierdo */}
                <path d="M 76,58 C 65,60 55,70 54,82 C 53,92 60,98 68,96 C 73,90 76,78 77,68 Z" />
                {/* Deltoides Posterior Derecho */}
                <path d="M 124,58 C 135,60 145,70 146,82 C 147,92 140,98 132,96 C 127,90 124,78 123,68 Z" />
              </g>

              {/* GRUPO: ESPALDA Y DORSALES (INCLUYE TRAPECIOS) */}
              <g
                onClick={() => handleZoneClick('back')}
                onMouseEnter={(e) => handleMouseEnter('back', e)}
                onMouseLeave={handleMouseLeave}
                style={{
                  fill: getZoneFill('back'),
                  stroke: getZoneStroke('back'),
                  strokeWidth: 1.5,
                  opacity: getZoneOpacity('back'),
                  transition: 'all 0.2s ease',
                }}
              >
                {/* Trapecio Superior y Medio */}
                <path d="M 94,46 L 106,46 L 118,66 L 100,102 L 82,66 Z" />
                {/* Dorsal Izquierdo (Lats) */}
                <path d="M 80,68 C 74,78 72,118 84,166 L 98,166 L 98,104 Z" />
                {/* Dorsal Derecho (Lats) */}
                <path d="M 120,68 C 126,78 128,118 116,166 L 102,166 L 102,104 Z" />
                {/* Erectores Espinales / Lumbar */}
                <path d="M 88,168 L 98,168 L 98,194 L 88,194 Z" />
                <path d="M 102,168 L 112,168 L 112,194 L 102,194 Z" />
              </g>

              {/* GRUPO: TRÍCEPS */}
              <g
                onClick={() => handleZoneClick('triceps')}
                onMouseEnter={(e) => handleMouseEnter('triceps', e)}
                onMouseLeave={handleMouseLeave}
                style={{
                  fill: getZoneFill('triceps'),
                  stroke: getZoneStroke('triceps'),
                  strokeWidth: 1.5,
                  opacity: getZoneOpacity('triceps'),
                  transition: 'all 0.2s ease',
                }}
              >
                {/* Tríceps Izquierdo */}
                <path d="M 64,98 C 55,104 53,120 54,142 C 58,146 64,146 66,142 C 68,128 68,110 64,98 Z" />
                {/* Tríceps Derecho */}
                <path d="M 136,98 C 145,104 147,120 146,142 C 142,146 136,146 134,142 C 132,128 132,110 136,98 Z" />
              </g>

              {/* GRUPO: ANTEBRAZOS (POSTERIOR) */}
              <g
                onClick={() => handleZoneClick('forearms')}
                onMouseEnter={(e) => handleMouseEnter('forearms', e)}
                onMouseLeave={handleMouseLeave}
                style={{
                  fill: getZoneFill('forearms'),
                  stroke: getZoneStroke('forearms'),
                  strokeWidth: 1.5,
                  opacity: getZoneOpacity('forearms'),
                  transition: 'all 0.2s ease',
                }}
              >
                {/* Antebrazo Izquierdo Posterior */}
                <path d="M 54,146 C 46,156 42,176 40,204 C 40,214 46,216 48,212 C 54,196 60,172 62,150 Z" />
                {/* Antebrazo Derecho Posterior */}
                <path d="M 146,146 C 154,156 158,176 160,204 C 160,214 154,216 152,212 C 146,196 140,172 138,150 Z" />
              </g>

              {/* GRUPO: GLÚTEOS */}
              <g
                onClick={() => handleZoneClick('glutes')}
                onMouseEnter={(e) => handleMouseEnter('glutes', e)}
                onMouseLeave={handleMouseLeave}
                style={{
                  fill: getZoneFill('glutes'),
                  stroke: getZoneStroke('glutes'),
                  strokeWidth: 1.5,
                  opacity: getZoneOpacity('glutes'),
                  transition: 'all 0.2s ease',
                }}
              >
                {/* Glúteo Izquierdo */}
                <path d="M 72,194 C 68,206 68,232 76,248 C 84,250 96,246 98,234 C 98,208 96,194 88,194 Z" />
                {/* Glúteo Derecho */}
                <path d="M 128,194 C 132,206 132,232 124,248 C 116,250 104,246 102,234 C 102,208 104,194 112,194 Z" />
              </g>

              {/* GRUPO: ISQUIOTIBIALES (FEMORALES) */}
              <g
                onClick={() => handleZoneClick('hamstrings')}
                onMouseEnter={(e) => handleMouseEnter('hamstrings', e)}
                onMouseLeave={handleMouseLeave}
                style={{
                  fill: getZoneFill('hamstrings'),
                  stroke: getZoneStroke('hamstrings'),
                  strokeWidth: 1.5,
                  opacity: getZoneOpacity('hamstrings'),
                  transition: 'all 0.2s ease',
                }}
              >
                {/* Isquiotibial Izquierdo */}
                <path d="M 76,250 C 70,262 70,284 76,300 C 82,302 88,302 90,298 C 94,282 94,260 92,250 Z" />
                {/* Isquiotibial Derecho */}
                <path d="M 124,250 C 130,262 130,284 124,300 C 118,302 112,302 110,298 C 106,282 106,260 108,250 Z" />
              </g>

              {/* Hueco Poplíteo (tras rodillas) */}
              <circle cx="81" cy="308" r="5" fill="#161e30" stroke="#2a3752" strokeWidth="1" />
              <circle cx="119" cy="308" r="5" fill="#161e30" stroke="#2a3752" strokeWidth="1" />

              {/* GRUPO: PANTORRILLAS (GEMELOS POSTERIORES) */}
              <g
                onClick={() => handleZoneClick('calves')}
                onMouseEnter={(e) => handleMouseEnter('calves', e)}
                onMouseLeave={handleMouseLeave}
                style={{
                  fill: getZoneFill('calves'),
                  stroke: getZoneStroke('calves'),
                  strokeWidth: 1.5,
                  opacity: getZoneOpacity('calves'),
                  transition: 'all 0.2s ease',
                }}
              >
                {/* Gemelo Izquierdo (Gastrocnemio) */}
                <path d="M 75,316 C 68,328 68,354 75,386 C 82,388 86,388 89,386 C 92,354 90,328 87,316 Z" />
                {/* Gemelo Derecho (Gastrocnemio) */}
                <path d="M 125,316 C 132,328 132,354 125,386 C 118,388 114,388 111,386 C 108,354 110,328 113,316 Z" />
              </g>
            </svg>
          </div>
        )}
      </div>

      {/* Tooltip flotante informativo si hay hover */}
      {hoveredZone && (
        <div
          style={{
            position: 'absolute',
            bottom: '16px',
            left: '50%',
            transform: 'translateX(-50%)',
            background: 'rgba(15, 23, 42, 0.95)',
            border: '1px solid var(--accent-cyan)',
            boxShadow: '0 4px 20px rgba(0, 242, 254, 0.25)',
            borderRadius: '8px',
            padding: '8px 16px',
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            pointerEvents: 'none',
            zIndex: 10,
          }}
        >
          <div
            style={{
              width: '10px',
              height: '10px',
              borderRadius: '50%',
              background: '#10b981',
              boxShadow: '0 0 8px #10b981',
            }}
          />
          <div>
            <span style={{ fontSize: '0.85rem', fontWeight: 700, color: '#ffffff' }}>
              {hoveredMuscleObj ? hoveredMuscleObj.name : hoveredZone.toUpperCase()}
            </span>
            {hoveredMuscleObj && (
              <span style={{ fontSize: '0.75rem', color: 'var(--accent-cyan)', marginLeft: '8px' }}>
                ({hoveredMuscleObj.exercises_count || 0} ejercicios vinculados)
              </span>
            )}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
              (Clic para filtrar)
            </span>
            {hoveredMuscleObj && onOpenEdit && (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  onOpenEdit(hoveredMuscleObj);
                }}
                className="btn btn-secondary btn-sm"
                style={{
                  padding: '3px 8px',
                  fontSize: '0.72rem',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
                title="Editar este grupo muscular"
              >
                <Edit2 size={11} />
                <span>Editar</span>
              </button>
            )}
          </div>
        </div>
      )}

      {/* Barra Inferior con Píldoras de Grupos Musculares Disponibles */}
      <div
        style={{
          marginTop: '16px',
          paddingTop: '14px',
          borderTop: '1px solid rgba(255, 255, 255, 0.06)',
          display: 'flex',
          flexWrap: 'wrap',
          gap: '8px',
          alignItems: 'center',
        }}
      >
        <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
          <Info size={14} /> Filtro rápido por zona:
        </span>

        {muscles.map((m) => {
          const isSelected = selectedMuscleId === m.id;
          const isHovered = hoveredMuscleId === m.id;
          return (
            <button
              key={m.id}
              type="button"
              onClick={() => onSelectMuscle?.(isSelected ? null : m)}
              onMouseEnter={() => {
                const zones = mapMuscleToZone(m.name);
                if (zones.length > 0) setHoveredZone(zones[0]);
              }}
              onMouseLeave={() => setHoveredZone(null)}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '4px 10px',
                borderRadius: '6px',
                fontSize: '0.75rem',
                fontWeight: 600,
                border: isSelected ? '1px solid var(--accent-cyan)' : '1px solid rgba(255, 255, 255, 0.08)',
                background: isSelected
                  ? 'rgba(0, 242, 254, 0.15)'
                  : isHovered
                  ? 'rgba(255, 255, 255, 0.08)'
                  : 'rgba(255, 255, 255, 0.02)',
                color: isSelected ? '#ffffff' : 'var(--text-secondary)',
                cursor: 'pointer',
                transition: 'all 0.15s ease',
              }}
            >
              {isSelected && <Check size={12} color="var(--accent-cyan)" />}
              <span>{m.name}</span>
              <span
                style={{
                  background: 'rgba(255, 255, 255, 0.1)',
                  padding: '1px 5px',
                  borderRadius: '10px',
                  fontSize: '0.68rem',
                  color: isSelected ? 'var(--accent-cyan)' : 'var(--text-muted)',
                }}
              >
                {m.exercises_count || 0}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
};
