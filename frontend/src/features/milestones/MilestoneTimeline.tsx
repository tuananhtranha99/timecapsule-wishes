import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  ArrowLeft,
  PlusCircle,
  Sparkles,
  Calendar,
  Pencil,
  Trash2,
  Milestone as MilestoneIcon
} from 'lucide-react';
import { apiClient } from '../../api/apiClient';
import { MilestoneQuickAdd } from './MilestoneQuickAdd';
import type { Milestone, Recipient, ApiResponse, MilestoneCategory } from '../../types';

export const MilestoneTimeline: React.FC = () => {
  const { recipientId } = useParams<{ recipientId: string }>();
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [recipient, setRecipient] = useState<Recipient | null>(null);
  const [milestones, setMilestones] = useState<Milestone[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingMilestone, setEditingMilestone] = useState<Milestone | null>(null);

  const fetchData = async () => {
    if (!recipientId) return;
    try {
      setIsLoading(true);
      const [recRes, msRes] = await Promise.all([
        apiClient.get<ApiResponse<Recipient>>(`/recipients/${recipientId}`),
        apiClient.get<ApiResponse<Milestone[]>>(`/recipients/${recipientId}/milestones`),
      ]);
      setRecipient(recRes.data.data);
      setMilestones(msRes.data.data);
    } catch (err) {
      console.error('Failed to load milestone data', err);
      navigate('/recipients');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [recipientId]);

  const handleOpenCreate = () => {
    setEditingMilestone(null);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (milestone: Milestone) => {
    setEditingMilestone(milestone);
    setIsModalOpen(true);
  };

  const handleDelete = async (id: string) => {
    if (window.confirm(t('milestones.deleteConfirm'))) {
      try {
        await apiClient.delete(`/milestones/${id}`);
        setMilestones((prev) => prev.filter((m) => m.id !== id));
      } catch (err) {
        alert(t('common.error'));
      }
    }
  };

  const getCategoryBadgeClass = (category: MilestoneCategory) => {
    switch (category) {
      case 'CAREER': return 'badge-career';
      case 'TRAVEL': return 'badge-travel';
      case 'HEALTH': return 'badge-health';
      case 'RELATIONSHIP': return 'badge-relationship';
      case 'ACHIEVEMENT': return 'badge-achievement';
      default: return 'badge-other';
    }
  };

  if (isLoading) {
    return <div style={{ textAlign: 'center', padding: '4rem 0', color: 'var(--text-muted)' }}>{t('common.loading')}</div>;
  }

  return (
    <div>
      {/* Top Breadcrumb & Actions */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.5rem' }}>
        <button
          onClick={() => navigate('/recipients')}
          className="btn btn-secondary"
          style={{ padding: '0.45rem 0.8rem', fontSize: '0.85rem' }}
        >
          <ArrowLeft size={16} />
          <span>{t('common.back')}</span>
        </button>
      </div>

      {/* Recipient Header Profile Banner */}
      <div className="glass-panel" style={{
        padding: '1.75rem 2rem',
        marginBottom: '2.5rem',
        display: 'flex',
        flexWrap: 'wrap',
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: '1.25rem',
        background: 'linear-gradient(135deg, rgba(30, 36, 66, 0.75) 0%, rgba(18, 22, 40, 0.9) 100%)',
      }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', marginBottom: '0.35rem' }}>
            {t('milestones.title', { name: recipient?.name })}
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem' }}>
            {t('milestones.subtitle')}
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <button onClick={handleOpenCreate} className="btn btn-secondary" style={{ padding: '0.65rem 1.15rem' }}>
            <PlusCircle size={18} color="#22d3ee" />
            <span>{t('milestones.addBtn')}</span>
          </button>

          <button
            onClick={() => navigate(`/recipients/${recipientId}/wishes`)}
            className="btn btn-primary"
            style={{ padding: '0.65rem 1.15rem' }}
          >
            <Sparkles size={18} />
            <span>{t('recipients.generateWish')}</span>
          </button>
        </div>
      </div>

      {/* Timeline List */}
      {milestones.length === 0 ? (
        <div className="glass-panel" style={{ textAlign: 'center', padding: '4rem 2rem' }}>
          <div style={{ display: 'inline-flex', padding: '1rem', borderRadius: '50%', background: 'var(--grad-glow)', marginBottom: '1rem' }}>
            <MilestoneIcon size={36} color="#22d3ee" />
          </div>
          <h3>{t('milestones.empty')}</h3>
          <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem', marginBottom: '1.5rem', maxWidth: '420px', margin: '0.5rem auto 1.5rem auto' }}>
            {t('milestones.subtitle')}
          </p>
          <button onClick={handleOpenCreate} className="btn btn-primary">
            <PlusCircle size={18} />
            <span>{t('milestones.addBtn')}</span>
          </button>
        </div>
      ) : (
        <div style={{ position: 'relative', paddingLeft: '2rem' }}>
          {/* Vertical Glowing Line */}
          <div style={{
            position: 'absolute',
            left: '7px',
            top: '12px',
            bottom: '12px',
            width: '2px',
            background: 'linear-gradient(180deg, #6366f1 0%, #a855f7 50%, #ec4899 100%)',
            borderRadius: '999px',
            opacity: 0.6,
          }} />

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            {milestones.map((milestone) => (
              <div key={milestone.id} style={{ position: 'relative' }}>
                {/* Glowing Dot on Line */}
                <div style={{
                  position: 'absolute',
                  left: '-2rem',
                  top: '1.25rem',
                  width: '16px',
                  height: '16px',
                  borderRadius: '50%',
                  background: 'var(--grad-primary)',
                  boxShadow: '0 0 12px rgba(168, 85, 247, 0.8)',
                  border: '3px solid var(--bg-dark)',
                }} />

                {/* Milestone Card */}
                <div className="glass-panel" style={{
                  padding: '1.25rem 1.5rem',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                  gap: '1rem',
                }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.65rem' }}>
                      <span className={`badge ${getCategoryBadgeClass(milestone.category)}`}>
                        {t(`milestones.categories.${milestone.category}`)}
                      </span>

                      <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                        <Calendar size={13} />
                        {milestone.occurredAt}
                      </span>
                    </div>

                    <p style={{ fontSize: '1rem', color: 'var(--text-main)', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
                      {milestone.description}
                    </p>
                  </div>

                  {/* Actions */}
                  <div style={{ display: 'flex', gap: '0.35rem' }}>
                    <button
                      onClick={() => handleOpenEdit(milestone)}
                      className="btn btn-secondary"
                      style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem' }}
                      title="Edit"
                    >
                      <Pencil size={13} />
                    </button>
                    <button
                      onClick={() => handleDelete(milestone.id)}
                      className="btn btn-danger"
                      style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem' }}
                      title="Delete"
                    >
                      <Trash2 size={13} />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Modal Dialog */}
      {recipientId && (
        <MilestoneQuickAdd
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSuccess={fetchData}
          recipientId={recipientId}
          milestoneToEdit={editingMilestone}
        />
      )}
    </div>
  );
};
