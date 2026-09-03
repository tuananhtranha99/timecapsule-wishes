import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Sparkles, LogOut, Globe, UserCheck } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';

export const Navbar: React.FC = () => {
  const { t, i18n } = useTranslation();
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const toggleLanguage = () => {
    const nextLang = i18n.language === 'vi' ? 'en' : 'vi';
    i18n.changeLanguage(nextLang);
    localStorage.setItem('language', nextLang);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="logo-text">
          <Sparkles className="logo-icon" size={24} color="#ec4899" />
          <span>Time-Capsule Wishes</span>
        </Link>

        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          {isAuthenticated && (
            <Link to="/recipients" className="btn btn-secondary" style={{ padding: '0.45rem 0.9rem', fontSize: '0.875rem' }}>
              {t('nav.recipients')}
            </Link>
          )}

          <button
            onClick={toggleLanguage}
            className="btn btn-secondary"
            title="Switch Language (VI / EN)"
            style={{ padding: '0.45rem 0.75rem', fontSize: '0.85rem' }}
          >
            <Globe size={16} />
            <span>{i18n.language.toUpperCase()}</span>
          </button>

          {isAuthenticated ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <span style={{ fontSize: '0.9rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                <UserCheck size={16} color="#a855f7" />
                <strong style={{ color: 'var(--text-main)' }}>{user?.displayName}</strong>
              </span>
              <button
                onClick={handleLogout}
                className="btn btn-danger"
                style={{ padding: '0.45rem 0.75rem', fontSize: '0.85rem' }}
                title={t('nav.logout')}
              >
                <LogOut size={16} />
                <span className="hide-mobile">{t('nav.logout')}</span>
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <Link to="/login" className="btn btn-secondary" style={{ padding: '0.45rem 0.9rem', fontSize: '0.875rem' }}>
                {t('nav.login')}
              </Link>
              <Link to="/register" className="btn btn-primary" style={{ padding: '0.45rem 0.9rem', fontSize: '0.875rem' }}>
                {t('nav.register')}
              </Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};
