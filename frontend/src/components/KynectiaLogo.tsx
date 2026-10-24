import React from 'react';

interface KynectiaLogoProps {
  size?: number;
  showText?: boolean;
  showTagline?: boolean;
  subtitle?: string;
  className?: string;
  horizontal?: boolean;
}

export const KynectiaLogo: React.FC<KynectiaLogoProps> = ({
  size = 40,
  showText = true,
  showTagline = false,
  subtitle,
  className = '',
  horizontal = true,
}) => {
  return (
    <div
      className={className}
      style={{
        display: 'inline-flex',
        flexDirection: horizontal ? 'row' : 'column',
        alignItems: 'center',
        gap: horizontal ? `${Math.round(size * 0.3)}px` : `${Math.round(size * 0.2)}px`,
        textDecoration: 'none',
        userSelect: 'none',
      }}
    >
      {/* Isotipo Oficial: K estilizada con punto de cabeza atleta en gradiente Cian -> Azul */}
      <svg
        width={size}
        height={size}
        viewBox="0 0 100 100"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        style={{ flexShrink: 0, filter: 'drop-shadow(0 0 12px rgba(0, 224, 255, 0.35))' }}
      >
        <defs>
          <linearGradient id="kynectiaBrandGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#00E0FF" />
            <stop offset="100%" stopColor="#007BFF" />
          </linearGradient>
          <linearGradient id="kynectiaDotGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#00E0FF" />
            <stop offset="100%" stopColor="#00A2FF" />
          </linearGradient>
        </defs>

        {/* Punto / Cabeza de la silueta atlética */}
        <circle cx="76" cy="18" r="9" fill="url(#kynectiaDotGrad)" />

        {/* Columna vertical izquierda de la K */}
        <path
          d="M18 14H34V86H18V14Z"
          fill="url(#kynectiaBrandGrad)"
        />

        {/* Brazo diagonal superior de la K */}
        <path
          d="M34 50L64 24H84L48 56L34 50Z"
          fill="url(#kynectiaBrandGrad)"
        />

        {/* Pierna diagonal inferior de la K */}
        <path
          d="M42 51L78 86H60L34 60V50L42 51Z"
          fill="url(#kynectiaBrandGrad)"
        />
      </svg>

      {/* Tipografía Oficial KYNECTIA */}
      {showText && (
        <div style={{ display: 'flex', flexDirection: 'column', textAlign: horizontal ? 'left' : 'center' }}>
          <span
            style={{
              fontFamily: "'Exo 2', sans-serif",
              fontSize: `${Math.round(size * 0.55)}px`,
              fontWeight: 900,
              letterSpacing: '0.12em',
              color: '#FFFFFF',
              lineHeight: 1,
            }}
          >
            KYNECTIA
          </span>
          {showTagline && (
            <span
              style={{
                fontFamily: "'Exo 2', sans-serif",
                fontSize: `${Math.max(10, Math.round(size * 0.22))}px`,
                fontWeight: 700,
                letterSpacing: '0.16em',
                color: 'var(--accent-cyan)',
                marginTop: '4px',
                textTransform: 'uppercase',
              }}
            >
              ENTRENA • EVOLUCIONA • CONECTA
            </span>
          )}
          {subtitle && (
            <span
              style={{
                fontFamily: "'Exo 2', sans-serif",
                fontSize: `${Math.max(12, Math.round(size * 0.24))}px`,
                fontWeight: 500,
                color: 'var(--text-secondary)',
                marginTop: '6px',
              }}
            >
              {subtitle}
            </span>
          )}
        </div>
      )}
    </div>
  );
};
