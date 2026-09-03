import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  UserPlus,
  Calendar,
  Heart,
  FileText,
  Sparkles,
  Milestone as MilestoneIcon,
  Pencil,
  Trash2,
  Search,
  Users
} from 'lucide-react';
import { apiClient } from '../../api/apiClient';
import { RecipientForm } from './RecipientForm';
import type { Recipient, ApiResponse } from '../../types';

export const RecipientListPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [recipients, setRecipients] = useState<Recipient[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingRecipient, setEditingRecipient] = useState<Recipient | null>(null);

  const fetchRecipients = async () => {
    try {
      setIsLoading(true);
      const res = await apiClient.get<ApiResponse<Recipient[]>>('/recipients');
      setRecipients(res.data.data);
    } catch (error) {
      console.error('Failed to load recipients', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchRecipients();
  }, []);

  const handleOpenCreate = () => {
    setEditingRecipient(null);
    setIsFormOpen(true);
  };

  const handleOpenEdit = (recipient: Recipient) => {
    setEditingRecipient(recipient);
    setIsFormOpen(true);
  };

  const handleDelete = async (id: string) => {
    if (window.confirm(t('recipients.deleteConfirm'))) {
      try {
        await apiClient.delete(`/recipients/${id}`);
        setRecipients((prev) => prev.filter((r) => r.id !== id));
      } catch (err) {
        alert(t('common.error'));
      }
    }
  };

  const handleFormSuccess = () => {
    fetchRecipients();
  };

  const filteredRecipients = recipients.filter((r) => {
    const q = searchQuery.toLowerCase();
    return (
      r.name.toLowerCase().includes(q) ||
      (r.relationship && r.relationship.toLowerCase().includes(q)) ||
      (r.notes && r.notes.toLowerCase().includes(q))
    );
  });

  const getInitials = (name: string) => {
    const parts = name.trim().split(' ');
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  };

  return (
    <div>
      {/* Header Section */}
      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: '1rem',
        marginBottom: '2rem',
      }}>
        <div>
          <h1 style={{ fontSize: '2rem', marginBottom: '0.25rem' }}>{t('recipients.title')}</h1>
          <p style={{ color: 'var(--text-muted)' }}>{t('recipients.subtitle')}</p>
        </div>

        <button onClick={handleOpenCreate} className="btn btn-primary" style={{ padding: '0.75rem 1.25rem' }}>
          <UserPlus size={18} />
          <span>{t('recipients.addBtn')}</span>
        </button>
      </div>

      {/* Search Bar */}
      {recipients.length > 0 && (
        <div style={{ position: 'relative', marginBottom: '1.75rem', maxWidth: '400px' }}>
          <Search size={18} color="var(--text-dim)" style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)' }} />
          <input
            type="text"
            className="form-input"
            style={{ paddingLeft: '2.75rem' }}
            placeholder="Search recipients..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      )}

      {/* Loading & Empty States */}
      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '4rem 0', color: 'var(--text-muted)' }}>
          {t('common.loading')}
        </div>
      ) : filteredRecipients.length === 0 ? (
        <div className="glass-panel" style={{ textAlign: 'center', padding: '4rem 2rem' }}>
          <div style={{ display: 'inline-flex', padding: '1rem', borderRadius: '50%', background: 'var(--grad-glow)', marginBottom: '1rem' }}>
            <Users size={36} color="#a855f7" />
          </div>
          <h3>{t('recipients.empty')}</h3>
          <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem', marginBottom: '1.5rem', maxWidth: '420px', margin: '0.5rem auto 1.5rem auto' }}>
            {t('app.subtitle')}
          </p>
          <button onClick={handleOpenCreate} className="btn btn-primary">
            <UserPlus size={18} />
            <span>{t('recipients.addBtn')}</span>
          </button>
        </div>
      ) : (
        /* Recipient Cards Grid */
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
          gap: '1.5rem',
        }}>
          {filteredRecipients.map((recipient) => (
            <div
              key={recipient.id}
              className="glass-panel"
              style={{
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                padding: '1.5rem',
                position: 'relative',
                overflow: 'hidden',
              }}
            >
              <div>
                {/* Header: Avatar + Name + Relationship */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
                  <div style={{
                    width: '48px',
                    height: '48px',
                    borderRadius: '50%',
                    background: 'var(--grad-primary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontWeight: 700,
                    fontSize: '1.1rem',
                    color: '#fff',
                    boxShadow: '0 4px 12px rgba(139, 92, 246, 0.4)',
                  }}>
                    {getInitials(recipient.name)}
                  </div>

                  <div style={{ flex: 1, minWidth: 0 }}>
                    <h3 style={{ fontSize: '1.2rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {recipient.name}
                    </h3>
                    {recipient.relationship && (
                      <span className="badge badge-relationship" style={{ marginTop: '0.25rem' }}>
                        <Heart size={10} />
                        {recipient.relationship}
                      </span>
                    )}
                  </div>
                </div>

                {/* Details */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginBottom: '1.25rem' }}>
                  {recipient.birthday && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
                      <Calendar size={14} color="#60a5fa" />
                      <span>{t('recipients.birthday')}: <strong style={{ color: 'var(--text-main)' }}>{recipient.birthday}</strong></span>
                    </div>
                  )}

                  {recipient.notes && (
                    <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.5rem', fontSize: '0.85rem', color: 'var(--text-dim)' }}>
                      <FileText size={14} color="#eab308" style={{ flexShrink: 0, marginTop: '2px' }} />
                      <p style={{ display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                        {recipient.notes}
                      </p>
                    </div>
                  )}
                </div>
              </div>

              {/* Action Buttons */}
              <div style={{
                display: 'flex',
                flexDirection: 'column',
                gap: '0.75rem',
                borderTop: '1px solid var(--border-subtle)',
                paddingTop: '1rem',
              }}>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button
                    onClick={() => navigate(`/recipients/${recipient.id}/milestones`)}
                    className="btn btn-secondary"
                    style={{ flex: 1, padding: '0.5rem', fontSize: '0.85rem' }}
                  >
                    <MilestoneIcon size={15} color="#22d3ee" />
                    <span>Cột mốc</span>
                  </button>

                  <button
                    onClick={() => navigate(`/recipients/${recipient.id}/wishes`)}
                    className="btn btn-primary"
                    style={{ flex: 1, padding: '0.5rem', fontSize: '0.85rem' }}
                  >
                    <Sparkles size={15} />
                    <span>{t('recipients.generateWish')}</span>
                  </button>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                  <button
                    onClick={() => handleOpenEdit(recipient)}
                    className="btn btn-secondary"
                    style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem' }}
                    title="Edit"
                  >
                    <Pencil size={14} />
                    <span>{t('common.edit')}</span>
                  </button>
                  <button
                    onClick={() => handleDelete(recipient.id)}
                    className="btn btn-danger"
                    style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem' }}
                    title="Delete"
                  >
                    <Trash2 size={14} />
                    <span>{t('common.delete')}</span>
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create/Edit Modal Form */}
      <RecipientForm
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        onSuccess={handleFormSuccess}
        recipientToEdit={editingRecipient}
      />
    </div>
  );
};
