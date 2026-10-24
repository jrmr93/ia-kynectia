import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { Lock, Mail, ArrowRight } from 'lucide-react';
import { KynectiaLogo } from '../components/KynectiaLogo';

export const LoginView: React.FC = () => {
  const { login } = useAuth();
  const { showToast } = useToast();

  const [email, setEmail] = useState('admin@kynectia.com');
  const [password, setPassword] = useState('AdminKynectia2026!');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      showToast('error', 'Por favor ingresa tu correo y contraseña.');
      return;
    }

    setLoading(true);
    try {
      await login(email, password);
      showToast('success', '¡Bienvenido al Panel de Administración de Kynectia!');
    } catch (err: any) {
      showToast('error', err.message || 'Error al iniciar sesión.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        width: '100vw',
        background: 'radial-gradient(ellipse at bottom, #0e1e38 0%, #0B0F14 100%)',
        position: 'relative',
        padding: '20px',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '460px',
          background: 'rgba(16, 22, 32, 0.9)',
          backdropFilter: 'blur(20px)',
          border: '1px solid var(--border)',
          borderRadius: '24px',
          padding: '40px 36px',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.8), 0 0 40px rgba(0, 224, 255, 0.1)',
        }}
      >
        {/* Brand Icon & Logo */}
        <div style={{ textAlign: 'center', marginBottom: '28px', display: 'flex', justifyContent: 'center' }}>
          <KynectiaLogo
            size={64}
            horizontal={false}
            showTagline={true}
            subtitle="Portal de Administración y Control Deportivo"
          />
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Correo Administrativo</label>
            <div style={{ position: 'relative' }}>
              <Mail
                size={18}
                color="var(--text-muted)"
                style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }}
              />
              <input
                type="email"
                className="form-input"
                style={{ paddingLeft: '40px' }}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@kynectia.com"
                required
              />
            </div>
          </div>

          <div className="form-group" style={{ marginBottom: '24px' }}>
            <label className="form-label">Contraseña</label>
            <div style={{ position: 'relative' }}>
              <Lock
                size={18}
                color="var(--text-muted)"
                style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }}
              />
              <input
                type="password"
                className="form-input"
                style={{ paddingLeft: '40px' }}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••••••"
                required
              />
            </div>
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', padding: '13px', fontSize: '1rem' }}
            disabled={loading}
          >
            {loading ? (
              'Verificando credenciales...'
            ) : (
              <>
                <span>Ingresar al Sistema</span>
                <ArrowRight size={18} />
              </>
            )}
          </button>
        </form>

        <div style={{ marginTop: '28px', textAlign: 'center', borderTop: '1px solid var(--border-subtle)', paddingTop: '20px' }}>
          <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            Credenciales iniciales precargadas para evaluación local
          </span>
        </div>
      </div>
    </div>
  );
};
