import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { Sidebar } from './components/Sidebar';
import type { NavigationTab } from './components/Sidebar';
import { Header } from './components/Header';
import { LoginView } from './views/LoginView';
import { DashboardView } from './views/DashboardView';
import { UsersView } from './views/UsersView';
import { MusclesView } from './views/MusclesView';
import { EquipmentView } from './views/EquipmentView';
import { ExercisesView } from './views/ExercisesView';
import { WorkoutsView } from './views/WorkoutsView';
import { PlansView } from './views/PlansView';
import { AdsView } from './views/AdsView';
import { AuditView } from './views/AuditView';
import { RefreshCw } from 'lucide-react';

const AdminLayout: React.FC = () => {
  const { admin, loading } = useAuth();
  const [activeTab, setActiveTab] = useState<NavigationTab>('dashboard');

  if (loading) {
    return (
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100vh',
          width: '100vw',
          background: 'var(--bg-primary)',
          color: 'var(--text-muted)',
          gap: '16px',
        }}
      >
        <RefreshCw size={36} className="spin-animation" style={{ color: 'var(--accent-cyan)' }} />
        <p style={{ fontSize: '0.95rem' }}>Verificando sesión administrativa...</p>
      </div>
    );
  }

  if (!admin) {
    return <LoginView />;
  }

  const getHeaderInfo = () => {
    switch (activeTab) {
      case 'dashboard':
        return { title: 'Dashboard Principal', subtitle: 'Monitoreo de métricas en vivo y salud de la plataforma Kynectia' };
      case 'users':
        return { title: 'Gestión de Usuarios', subtitle: 'Administración de cuentas de atletas, historial físico y membresías' };
      case 'muscles':
        return { title: 'Grupos Musculares', subtitle: 'Clasificación anatómica para organización de ejercicios' };
      case 'equipment':
        return { title: 'Implementos Deportivos', subtitle: 'Control de pesas libres, máquinas guiadas y accesorios de gimnasio' };
      case 'exercises':
        return { title: 'Catálogo de Ejercicios', subtitle: 'Biblioteca de movimientos con técnica, seguridad y multimedia' };
      case 'workouts':
        return { title: 'Rutinas de Entrenamiento', subtitle: 'Diseño de programas estructurados, secuenciación y versionado' };
      case 'plans':
        return { title: 'Planes y Precios', subtitle: 'Estrategia comercial, tarifas y consulta de suscripciones activas' };
      case 'ads':
        return { title: 'Gestión de Publicidad', subtitle: 'Campañas promocionales, banners dinámicos y métricas de clics' };
      case 'audit':
        return { title: 'Auditoría y Seguridad', subtitle: 'Trazabilidad y registro inmutable de acciones administrativas en PostgreSQL' };
      default:
        return { title: 'Panel de Administración', subtitle: 'Kynectia Fitness Platform' };
    }
  };

  const headerInfo = getHeaderInfo();

  return (
    <div className="app-layout">
      <Sidebar activeTab={activeTab} onTabChange={setActiveTab} />
      <div className="main-content">
        <Header title={headerInfo.title} subtitle={headerInfo.subtitle} />
        {activeTab === 'dashboard' && <DashboardView />}
        {activeTab === 'users' && <UsersView />}
        {activeTab === 'muscles' && <MusclesView />}
        {activeTab === 'equipment' && <EquipmentView />}
        {activeTab === 'exercises' && <ExercisesView />}
        {activeTab === 'workouts' && <WorkoutsView />}
        {activeTab === 'plans' && <PlansView />}
        {activeTab === 'ads' && <AdsView />}
        {activeTab === 'audit' && <AuditView />}
      </div>
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <ToastProvider>
      <AuthProvider>
        <AdminLayout />
      </AuthProvider>
    </ToastProvider>
  );
};

export default App;
