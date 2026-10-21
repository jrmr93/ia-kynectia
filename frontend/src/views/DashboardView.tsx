import React, { useEffect, useState } from 'react';
import { apiRequest } from '../api/client';
import type { DashboardStats } from '../types';
import { useToast } from '../context/ToastContext';
import {
  Users,
  Activity,
  Layers,
  Dumbbell,
  CalendarCheck2,
  CreditCard,
  Megaphone,
  CheckCircle,
  RefreshCw,
  TrendingUp,
  Clock,
} from 'lucide-react';

export const DashboardView: React.FC = () => {
  const { showToast } = useToast();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchStats = async () => {
    setLoading(true);
    try {
      const res = await apiRequest<DashboardStats>('/dashboard/stats');
      setStats(res.data);
    } catch (err: any) {
      showToast('error', err.message || 'Error al cargar métricas del Dashboard.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
  }, []);

  if (loading && !stats) {
    return (
      <div className="page-container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
        <div style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
          <RefreshCw size={36} className="spin-animation" style={{ color: 'var(--accent-cyan)', marginBottom: '16px' }} />
          <p>Consultando métricas en tiempo real desde PostgreSQL...</p>
        </div>
      </div>
    );
  }

  const overview = stats?.overview;

  return (
    <div className="page-container">
      {/* Barra superior de acciones */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#ffffff' }}>Resumen General</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            Indicadores y métricas operativas obtenidas de PostgreSQL en tiempo real.
          </p>
        </div>
        <button onClick={fetchStats} className="btn btn-secondary btn-sm" disabled={loading}>
          <RefreshCw size={15} className={loading ? 'spin-animation' : ''} />
          <span>Actualizar Datos</span>
        </button>
      </div>

      {/* Grid de Métricas Principales */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: '16px',
          marginBottom: '28px',
        }}
      >
        {/* Usuarios Registrados */}
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '20px' }}>
          <div style={{ padding: '12px', borderRadius: '12px', background: 'rgba(56, 189, 248, 0.15)', color: '#38bdf8' }}>
            <Users size={26} />
          </div>
          <div>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase' }}>
              Total Usuarios
            </span>
            <h3 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', margin: '2px 0' }}>
              {overview?.totalUsers || 0}
            </h3>
            <span style={{ fontSize: '0.75rem', color: 'var(--accent-green)', display: 'flex', alignItems: 'center', gap: '4px' }}>
              <CheckCircle size={12} /> {overview?.activeUsers || 0} cuentas activas
            </span>
          </div>
        </div>

        {/* Ejercicios */}
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '20px' }}>
          <div style={{ padding: '12px', borderRadius: '12px', background: 'rgba(16, 185, 129, 0.15)', color: '#10b981' }}>
            <Activity size={26} />
          </div>
          <div>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase' }}>
              Catálogo Ejercicios
            </span>
            <h3 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', margin: '2px 0' }}>
              {overview?.totalExercises || 0}
            </h3>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              {overview?.publishedExercises || 0} publicados en plataforma
            </span>
          </div>
        </div>

        {/* Rutinas */}
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '20px' }}>
          <div style={{ padding: '12px', borderRadius: '12px', background: 'rgba(245, 158, 11, 0.15)', color: '#f59e0b' }}>
            <CalendarCheck2 size={26} />
          </div>
          <div>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase' }}>
              Rutinas Creadas
            </span>
            <h3 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', margin: '2px 0' }}>
              {overview?.totalRoutines || 0}
            </h3>
            <span style={{ fontSize: '0.75rem', color: 'var(--accent-cyan)' }}>
              {overview?.publishedRoutines || 0} publicadas y disponibles
            </span>
          </div>
        </div>

        {/* Suscripciones Vigentes */}
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '20px' }}>
          <div style={{ padding: '12px', borderRadius: '12px', background: 'rgba(168, 85, 247, 0.15)', color: '#a855f7' }}>
            <CreditCard size={26} />
          </div>
          <div>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase' }}>
              Suscripciones Activas
            </span>
            <h3 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', margin: '2px 0' }}>
              {overview?.activeSubscriptions || 0}
            </h3>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              En {overview?.totalPlans || 0} planes comerciales
            </span>
          </div>
        </div>
      </div>

      {/* Grid Secundario de Métricas y Anatomía */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '16px',
          marginBottom: '32px',
        }}
      >
        <div className="card" style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Grupos Musculares</span>
            <p style={{ fontSize: '1.4rem', fontWeight: 700, color: '#ffffff' }}>{overview?.totalMuscleGroups || 0}</p>
          </div>
          <Layers size={22} color="var(--accent-cyan)" />
        </div>

        <div className="card" style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Implementos Registrados</span>
            <p style={{ fontSize: '1.4rem', fontWeight: 700, color: '#ffffff' }}>{overview?.totalEquipment || 0}</p>
          </div>
          <Dumbbell size={22} color="var(--accent-cyan)" />
        </div>

        <div className="card" style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Anuncios Activos</span>
            <p style={{ fontSize: '1.4rem', fontWeight: 700, color: '#ffffff' }}>{overview?.activeAds || 0}</p>
          </div>
          <Megaphone size={22} color="var(--accent-orange)" />
        </div>

        <div className="card" style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Planes Comerciales</span>
            <p style={{ fontSize: '1.4rem', fontWeight: 700, color: '#ffffff' }}>{overview?.activePlans || 0}</p>
          </div>
          <TrendingUp size={22} color="var(--accent-green)" />
        </div>
      </div>

      {/* Sección Gráfica y de Distribución */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))',
          gap: '24px',
          marginBottom: '32px',
        }}
      >
        {/* Distribución por Dificultad */}
        <div className="card">
          <h4 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '16px', color: '#ffffff' }}>
            Distribución de Ejercicios por Dificultad
          </h4>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {stats?.charts?.exercisesByDifficulty?.map((item) => {
              const total = overview?.totalExercises || 1;
              const percent = Math.round((item.count / total) * 100);
              const color =
                item.difficulty_level === 'beginner'
                  ? 'var(--accent-green)'
                  : item.difficulty_level === 'intermediate'
                  ? 'var(--accent-cyan)'
                  : 'var(--accent-orange)';
              return (
                <div key={item.difficulty_level}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '6px' }}>
                    <span style={{ textTransform: 'capitalize', color: 'var(--text-secondary)' }}>
                      {item.difficulty_level === 'beginner' ? 'Principiante' : item.difficulty_level === 'intermediate' ? 'Intermedio' : 'Avanzado'}
                    </span>
                    <span style={{ fontWeight: 700, color: '#ffffff' }}>{item.count} ({percent}%)</span>
                  </div>
                  <div style={{ height: '8px', background: 'rgba(255, 255, 255, 0.05)', borderRadius: '4px', overflow: 'hidden' }}>
                    <div style={{ height: '100%', width: `${percent}%`, background: color, borderRadius: '4px' }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Suscripciones por Plan */}
        <div className="card">
          <h4 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '16px', color: '#ffffff' }}>
            Distribución de Suscripciones por Plan
          </h4>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {stats?.charts?.subscriptionsByPlan?.map((plan) => {
              return (
                <div
                  key={plan.plan_name}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 14px',
                    borderRadius: '8px',
                    background: 'rgba(255, 255, 255, 0.02)',
                    border: '1px solid var(--border-subtle)',
                  }}
                >
                  <div>
                    <p style={{ fontWeight: 600, color: '#ffffff', fontSize: '0.9rem' }}>{plan.plan_name}</p>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'capitalize' }}>
                      Ciclo: {plan.billing_cycle}
                    </span>
                  </div>
                  <span className="badge badge-success" style={{ fontSize: '0.82rem' }}>
                    {plan.count} activas
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Actividad Reciente de Auditoría */}
      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '18px' }}>
          <Clock size={18} color="var(--accent-cyan)" />
          <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#ffffff' }}>
            Actividad Administrativa Reciente
          </h4>
        </div>
        <div className="table-container">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Administrador</th>
                <th>Acción</th>
                <th>Módulo Afectado</th>
                <th>Registro ID</th>
                <th>Fecha y Hora</th>
              </tr>
            </thead>
            <tbody>
              {stats?.recentActivity && stats.recentActivity.length > 0 ? (
                stats.recentActivity.map((log) => (
                  <tr key={log.id}>
                    <td>
                      <span style={{ fontWeight: 600 }}>{log.admin_email || 'Sistema / Seeder'}</span>
                    </td>
                    <td>
                      <span
                        className={`badge ${
                          log.action === 'CREATE'
                            ? 'badge-success'
                            : log.action === 'UPDATE'
                            ? 'badge-info'
                            : log.action === 'DELETE'
                            ? 'badge-danger'
                            : 'badge-purple'
                        }`}
                      >
                        {log.action}
                      </span>
                    </td>
                    <td>
                      <code style={{ color: 'var(--accent-cyan)', background: 'rgba(0, 242, 254, 0.08)', padding: '2px 6px', borderRadius: '4px' }}>
                        {log.entity_type}
                      </code>
                    </td>
                    <td>{log.entity_id || '—'}</td>
                    <td style={{ color: 'var(--text-muted)' }}>
                      {new Date(log.created_at).toLocaleString()}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '30px', color: 'var(--text-muted)' }}>
                    No se registran eventos de auditoría todavía.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
