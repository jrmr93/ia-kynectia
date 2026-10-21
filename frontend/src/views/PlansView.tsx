import React, { useEffect, useState } from 'react';
import { apiRequest } from '../api/client';
import type { Plan } from '../types';
import { useToast } from '../context/ToastContext';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import {
  CreditCard,
  Plus,
  Edit2,
  Trash2,
  Power,
  RefreshCw,
  CheckCircle,
  Users,
  Percent,
} from 'lucide-react';

export const PlansView: React.FC = () => {
  const { showToast } = useToast();

  const [plans, setPlans] = useState<Plan[]>([]);
  const [subscriptions, setSubscriptions] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState<'plans' | 'subscriptions'>('plans');
  const [loading, setLoading] = useState(true);

  // Modales
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingPlan, setEditingPlan] = useState<Plan | null>(null);
  const [deletePlan, setDeletePlan] = useState<Plan | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  // Form State
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    billingCycle: 'monthly' as Plan['billing_cycle'],
    price: 14.99,
    currency: 'USD',
    benefitsText: 'Acceso a catálogo completo\nRutinas ilimitadas\nSoporte prioritario',
    discountPercent: 0,
    isActive: true,
  });

  const fetchPlans = async () => {
    setLoading(true);
    try {
      const res = await apiRequest<Plan[]>('/plans');
      setPlans(res.data);
    } catch (err: any) {
      showToast('error', err.message || 'Error al obtener planes comerciales.');
    } finally {
      setLoading(false);
    }
  };

  const fetchSubscriptions = async () => {
    setLoading(true);
    try {
      const res = await apiRequest<any[]>('/plans/subscriptions');
      setSubscriptions(res.data);
    } catch (err: any) {
      showToast('error', err.message || 'Error al obtener suscripciones de usuarios.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'plans') {
      fetchPlans();
    } else {
      fetchSubscriptions();
    }
  }, [activeTab]);

  const handleOpenCreate = () => {
    setEditingPlan(null);
    setFormData({
      name: '',
      description: '',
      billingCycle: 'monthly',
      price: 19.99,
      currency: 'USD',
      benefitsText: 'Catálogo ilimitado de ejercicios\nRutinas personalizadas\nSoporte por chat',
      discountPercent: 0,
      isActive: true,
    });
    setIsFormOpen(true);
  };

  const handleOpenEdit = (plan: Plan) => {
    setEditingPlan(plan);
    const benefitsList = Array.isArray(plan.benefits) ? plan.benefits : [];
    setFormData({
      name: plan.name,
      description: plan.description || '',
      billingCycle: plan.billing_cycle,
      price: parseFloat(String(plan.price)),
      currency: plan.currency,
      benefitsText: benefitsList.join('\n'),
      discountPercent: parseFloat(String(plan.discount_percent || 0)),
      isActive: plan.is_active,
    });
    setIsFormOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionLoading(true);
    try {
      const benefits = formData.benefitsText
        .split('\n')
        .map((b) => b.trim())
        .filter((b) => b.length > 0);

      const payload = {
        name: formData.name,
        description: formData.description,
        billingCycle: formData.billingCycle,
        price: formData.price,
        currency: formData.currency,
        benefits,
        discountPercent: formData.discountPercent,
        isActive: formData.isActive,
      };

      if (editingPlan) {
        await apiRequest(`/plans/${editingPlan.id}`, { method: 'PUT', body: payload });
        showToast('success', 'Plan comercial actualizado.');
      } else {
        await apiRequest('/plans', { method: 'POST', body: payload });
        showToast('success', 'Nuevo plan creado exitosamente.');
      }
      setIsFormOpen(false);
      fetchPlans();
    } catch (err: any) {
      showToast('error', err.message || 'Error al guardar plan comercial.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleActive = async (plan: Plan) => {
    try {
      await apiRequest(`/plans/${plan.id}/toggle-active`, {
        method: 'PATCH',
        body: { isActive: !plan.is_active },
      });
      showToast('success', `Plan ${!plan.is_active ? 'activado' : 'desactivado'}.`);
      fetchPlans();
    } catch (err: any) {
      showToast('error', err.message || 'Error al actualizar estado del plan.');
    }
  };

  const handleDelete = async () => {
    if (!deletePlan) return;
    setActionLoading(true);
    try {
      await apiRequest(`/plans/${deletePlan.id}`, { method: 'DELETE' });
      showToast('success', 'Plan comercial eliminado (soft-delete).');
      setDeletePlan(null);
      fetchPlans();
    } catch (err: any) {
      showToast('error', err.message || 'Error al eliminar el plan.');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="page-container">
      {/* Encabezado */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#ffffff' }}>Planes, Precios y Suscripciones</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            Gestión comercial de membresías de Kynectia y seguimiento de atletas suscritos.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <div style={{ display: 'flex', background: 'rgba(255, 255, 255, 0.05)', borderRadius: '8px', padding: '4px' }}>
            <button
              onClick={() => setActiveTab('plans')}
              className={`btn btn-sm ${activeTab === 'plans' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ border: 'none' }}
            >
              Oferta Comercial ({plans.length})
            </button>
            <button
              onClick={() => setActiveTab('subscriptions')}
              className={`btn btn-sm ${activeTab === 'subscriptions' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ border: 'none' }}
            >
              Suscripciones de Atletas
            </button>
          </div>
          {activeTab === 'plans' && (
            <button onClick={handleOpenCreate} className="btn btn-primary">
              <Plus size={18} />
              <span>Nuevo Plan</span>
            </button>
          )}
        </div>
      </div>

      {activeTab === 'plans' ? (
        /* Vista de Cards de Planes Comerciales */
        loading ? (
          <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>
            <RefreshCw size={28} className="spin-animation" style={{ color: 'var(--accent-cyan)', marginBottom: '12px' }} />
            <p>Cargando planes comerciales...</p>
          </div>
        ) : plans.length === 0 ? (
          <div className="empty-state">
            <CreditCard size={48} className="empty-state-icon" />
            <h4 className="empty-state-title">No hay planes registrados</h4>
            <p className="empty-state-desc">Configura el primer plan mensual, trimestral o gratuito para los atletas.</p>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px' }}>
            {plans.map((p) => {
              const benefitsList = Array.isArray(p.benefits) ? p.benefits : [];
              return (
                <div
                  key={p.id}
                  className="card"
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    border: p.is_active ? '1px solid rgba(0, 242, 254, 0.25)' : '1px solid var(--border-subtle)',
                    position: 'relative',
                  }}
                >
                  {parseFloat(String(p.discount_percent)) > 0 && (
                    <div
                      style={{
                        position: 'absolute',
                        top: '16px',
                        right: '16px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px',
                        background: 'rgba(239, 68, 68, 0.2)',
                        color: '#f87171',
                        border: '1px solid rgba(239, 68, 68, 0.4)',
                        padding: '4px 8px',
                        borderRadius: '6px',
                        fontSize: '0.72rem',
                        fontWeight: 700,
                      }}
                    >
                      <Percent size={12} /> {p.discount_percent}% OFF
                    </div>
                  )}

                  <div>
                    <span
                      style={{
                        fontSize: '0.72rem',
                        fontWeight: 700,
                        textTransform: 'uppercase',
                        color: 'var(--accent-cyan)',
                        letterSpacing: '0.05em',
                      }}
                    >
                      Ciclo: {p.billing_cycle}
                    </span>
                    <h3 style={{ fontSize: '1.4rem', fontWeight: 700, color: '#ffffff', marginTop: '4px' }}>
                      {p.name}
                    </h3>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginTop: '6px', minHeight: '40px' }}>
                      {p.description || 'Sin descripción.'}
                    </p>

                    <div style={{ margin: '20px 0', display: 'flex', alignItems: 'baseline', gap: '6px' }}>
                      <span style={{ fontSize: '2.2rem', fontWeight: 800, color: '#ffffff' }}>
                        ${parseFloat(String(p.price)).toFixed(2)}
                      </span>
                      <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                        {p.currency} / {p.billing_cycle === 'free' ? 'gratis' : p.billing_cycle}
                      </span>
                    </div>

                    <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '16px', marginBottom: '20px' }}>
                      <p style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '10px' }}>
                        Beneficios Incluidos:
                      </p>
                      <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        {benefitsList.map((b, i) => (
                          <li key={i} style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.84rem', color: 'var(--text-secondary)' }}>
                            <CheckCircle size={15} color="var(--accent-green)" />
                            <span>{b}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>

                  <div>
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '10px 14px',
                        background: 'rgba(255, 255, 255, 0.02)',
                        borderRadius: '8px',
                        marginBottom: '16px',
                        border: '1px solid var(--border-subtle)',
                      }}
                    >
                      <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Users size={13} color="var(--accent-cyan)" /> Suscripciones activas:
                      </span>
                      <span style={{ fontWeight: 700, color: 'var(--accent-cyan)' }}>
                        {p.active_subscriptions_count || 0}
                      </span>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <span className={`badge ${p.is_active ? 'badge-success' : 'badge-danger'}`}>
                        {p.is_active ? 'Activo' : 'Inactivo'}
                      </span>

                      <div style={{ display: 'flex', gap: '6px' }}>
                        <button
                          onClick={() => handleOpenEdit(p)}
                          className="btn btn-secondary btn-sm"
                          title="Editar características o precio"
                        >
                          <Edit2 size={14} />
                        </button>
                        <button
                          onClick={() => handleToggleActive(p)}
                          className="btn btn-secondary btn-sm"
                          title={p.is_active ? 'Desactivar plan' : 'Activar plan'}
                          style={{ color: p.is_active ? '#f59e0b' : '#10b981' }}
                        >
                          <Power size={14} />
                        </button>
                        <button
                          onClick={() => setDeletePlan(p)}
                          className="btn btn-danger btn-sm"
                          title="Eliminar plan"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )
      ) : (
        /* Tabla de Suscripciones Activas */
        <div className="table-container">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Atleta</th>
                <th>Plan Contratado</th>
                <th>Ciclo</th>
                <th>Monto</th>
                <th>Vigencia</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {subscriptions.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    No hay registros de suscripciones actualmente.
                  </td>
                </tr>
              ) : (
                subscriptions.map((s) => (
                  <tr key={s.id}>
                    <td>
                      <p style={{ fontWeight: 600, color: '#ffffff' }}>{s.user_name}</p>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{s.user_email}</span>
                    </td>
                    <td>
                      <span className="badge badge-info">{s.plan_name}</span>
                    </td>
                    <td style={{ textTransform: 'capitalize' }}>{s.billing_cycle}</td>
                    <td>
                      <strong style={{ color: '#ffffff' }}>${parseFloat(s.price_paid).toFixed(2)}</strong> {s.currency}
                    </td>
                    <td style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                      {new Date(s.start_date).toLocaleDateString()} — {new Date(s.end_date).toLocaleDateString()}
                    </td>
                    <td>
                      <span className={`badge ${s.status === 'active' ? 'badge-success' : 'badge-warning'}`}>
                        {s.status}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal Crear / Editar Plan */}
      <Modal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        title={editingPlan ? `Editar Plan: ${editingPlan.name}` : 'Crear Plan Comercial'}
      >
        <form onSubmit={handleSave}>
          <div className="form-group">
            <label className="form-label">Nombre del Plan *</label>
            <input
              type="text"
              className="form-input"
              required
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="Ej. Plan Trimestral Élite"
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Ciclo de Facturación *</label>
              <select
                className="form-select"
                value={formData.billingCycle}
                onChange={(e) => setFormData({ ...formData, billingCycle: e.target.value as any })}
              >
                <option value="free">Gratuito</option>
                <option value="monthly">Mensual</option>
                <option value="quarterly">Trimestral (3 meses)</option>
                <option value="biannual">Semestral (6 meses)</option>
                <option value="annual">Anual (12 meses)</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Precio ({formData.currency}) *</label>
              <input
                type="number"
                step="0.01"
                min="0"
                className="form-input"
                required
                value={formData.price}
                onChange={(e) => setFormData({ ...formData, price: parseFloat(e.target.value) })}
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Descuento Promocional (%)</label>
              <input
                type="number"
                step="1"
                min="0"
                max="100"
                className="form-input"
                value={formData.discountPercent}
                onChange={(e) => setFormData({ ...formData, discountPercent: parseFloat(e.target.value) })}
              />
            </div>
            <div className="form-group">
              <label className="form-label">Estado</label>
              <select
                className="form-select"
                value={formData.isActive ? 'true' : 'false'}
                onChange={(e) => setFormData({ ...formData, isActive: e.target.value === 'true' })}
              >
                <option value="true">Activo para Venta</option>
                <option value="false">Desactivado</option>
              </select>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Descripción Comercial</label>
            <input
              type="text"
              className="form-input"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Breve reseña del público objetivo o ahorro..."
            />
          </div>

          <div className="form-group">
            <label className="form-label">Lista de Beneficios (Un beneficio por línea)</label>
            <textarea
              className="form-textarea"
              value={formData.benefitsText}
              onChange={(e) => setFormData({ ...formData, benefitsText: e.target.value })}
              placeholder="Acceso ilimitado a videos 4K&#10;Rutinas personalizadas ilimitadas&#10;Planes nutricionales"
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
              {actionLoading ? 'Guardando...' : editingPlan ? 'Actualizar Plan' : 'Crear Plan'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Confirmación Eliminar */}
      <ConfirmDialog
        isOpen={deletePlan !== null}
        title="Eliminar Plan Comercial"
        message={`¿Estás seguro de eliminar el plan "${deletePlan?.name}"? Esta acción solo es posible si no hay suscripciones activas asociadas.`}
        confirmLabel="Eliminar Definitivamente"
        loading={actionLoading}
        onConfirm={handleDelete}
        onCancel={() => setDeletePlan(null)}
      />
    </div>
  );
};
