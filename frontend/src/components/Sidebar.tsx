import React from 'react';
import {
  LayoutDashboard,
  Users,
  Dumbbell,
  Layers,
  Activity,
  CalendarCheck2,
  CreditCard,
  Megaphone,
  ShieldAlert,
} from 'lucide-react';
import { KynectiaLogo } from './KynectiaLogo';

export type NavigationTab =
  | 'dashboard'
  | 'users'
  | 'muscles'
  | 'equipment'
  | 'exercises'
  | 'workouts'
  | 'plans'
  | 'ads'
  | 'audit';

interface SidebarProps {
  activeTab: NavigationTab;
  onTabChange: (tab: NavigationTab) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ activeTab, onTabChange }) => {
  const menuItems = [
    { id: 'dashboard' as NavigationTab, label: 'Dashboard', icon: LayoutDashboard },
    { id: 'users' as NavigationTab, label: 'Usuarios', icon: Users },
    { id: 'muscles' as NavigationTab, label: 'Grupos Musculares', icon: Layers },
    { id: 'equipment' as NavigationTab, label: 'Implementos', icon: Dumbbell },
    { id: 'exercises' as NavigationTab, label: 'Catálogo Ejercicios', icon: Activity },
    { id: 'workouts' as NavigationTab, label: 'Rutinas', icon: CalendarCheck2 },
    { id: 'plans' as NavigationTab, label: 'Planes y Precios', icon: CreditCard },
    { id: 'ads' as NavigationTab, label: 'Publicidad', icon: Megaphone },
    { id: 'audit' as NavigationTab, label: 'Auditoría', icon: ShieldAlert },
  ];

  return (
    <aside
      style={{
        width: '260px',
        background: 'var(--grad-sidebar)',
        borderRight: '1px solid var(--border-subtle)',
        display: 'flex',
        flexDirection: 'column',
        height: '100vh',
      }}
    >
      {/* Brand Header */}
      <div
        style={{
          padding: '22px 18px',
          display: 'flex',
          alignItems: 'center',
          borderBottom: '1px solid var(--border-subtle)',
        }}
      >
        <KynectiaLogo size={36} showTagline={true} />
      </div>

      {/* Navegación */}
      <nav style={{ flex: 1, padding: '16px 12px', overflowY: 'auto' }}>
        <p style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.08em', padding: '8px 12px' }}>
          Gestión de Plataforma
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          {menuItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => onTabChange(item.id)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  width: '100%',
                  padding: '11px 14px',
                  borderRadius: '10px',
                  border: '1px solid',
                  borderColor: isActive ? 'rgba(0, 242, 254, 0.3)' : 'transparent',
                  background: isActive ? 'rgba(0, 242, 254, 0.08)' : 'transparent',
                  color: isActive ? 'var(--accent-cyan)' : 'var(--text-secondary)',
                  cursor: 'pointer',
                  textAlign: 'left',
                  fontSize: '0.9rem',
                  fontWeight: isActive ? 600 : 500,
                  transition: 'all var(--transition-fast)',
                }}
                onMouseEnter={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.background = 'rgba(255, 255, 255, 0.03)';
                    e.currentTarget.style.color = '#ffffff';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.background = 'transparent';
                    e.currentTarget.style.color = 'var(--text-secondary)';
                  }
                }}
              >
                <Icon size={19} color={isActive ? 'var(--accent-cyan)' : 'var(--text-muted)'} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </div>
      </nav>

      {/* Footer del Sidebar con estado de conexión */}
      <div
        style={{
          padding: '16px 20px',
          borderTop: '1px solid var(--border-subtle)',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
        }}
      >
        <div
          style={{
            width: '8px',
            height: '8px',
            borderRadius: '50%',
            background: 'var(--accent-green)',
            boxShadow: '0 0 8px #10b981',
          }}
        />
        <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
          PostgreSQL 18 Activo
        </span>
      </div>
    </aside>
  );
};
