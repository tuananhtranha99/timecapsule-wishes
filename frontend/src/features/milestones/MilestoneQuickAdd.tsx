import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { X, Save, Loader2, Calendar, Tag, FileText } from 'lucide-react';
import { apiClient } from '../../api/apiClient';
import type { Milestone, MilestoneCategory, ApiResponse } from '../../types';

interface MilestoneQuickAddProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (milestone: Milestone) => void;
  recipientId: string;
  milestoneToEdit?: Milestone | null;
}

const CATEGORIES: MilestoneCategory[] = [
  'CAREER',
  'TRAVEL',
  'HEALTH',
  'RELATIONSHIP',
  'ACHIEVEMENT',
  'OTHER',
];

export const MilestoneQuickAdd: React.FC<MilestoneQuickAddProps> = ({
  isOpen,
  onClose,
  onSuccess,
  recipientId,
  milestoneToEdit,
}) => {
  const { t } = useTranslation();
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<MilestoneCategory>('ACHIEVEMENT');
  const [occurredAt, setOccurredAt] = useState(new Date().toISOString().split('T')[0]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (milestoneToEdit) {
      setDescription(milestoneToEdit.description || '');
      setCategory(milestoneToEdit.category || 'ACHIEVEMENT');
      setOccurredAt(milestoneToEdit.occurredAt || new Date().toISOString().split('T')[0]);
    } else {
      setDescription('');
      setCategory('ACHIEVEMENT');
      setOccurredAt(new Date().toISOString().split('T')[0]);
    }
    setErrorMsg(null);
  }, [milestoneToEdit, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setIsLoading(true);

    const payload = {
      description,
      category,
      occurredAt,
    };

    try {
      if (milestoneToEdit) {
        const res = await apiClient.put<ApiResponse<Milestone>>(`/milestones/${milestoneToEdit.id}`, payload);
        onSuccess(res.data.data);
      } else {
        const res = await apiClient.post<ApiResponse<Milestone>>(`/recipients/${recipientId}/milestones`, payload);
        onSuccess(res.data.data);
      }
      onClose();
    } catch (err: any) {
      if (err.response?.data?.validationErrors) {
        setErrorMsg(Object.values(err.response.data.validationErrors)[0] as string);
      } else if (err.response?.data?.message) {
        setErrorMsg(err.response.data.message);
      } else {
        setErrorMsg(t('common.error'));
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <h3>{milestoneToEdit ? t('milestones.editTitle') : t('milestones.createTitle')}</h3>
          <button onClick={onClose} className="btn btn-secondary" style={{ padding: '0.35rem', borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        {errorMsg && (
          <div style={{
            padding: '0.75rem',
            background: 'rgba(239, 68, 68, 0.15)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            borderRadius: 'var(--radius-sm)',
            color: '#f87171',
            fontSize: '0.85rem',
            marginBottom: '1rem',
          }}>
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              <FileText size={14} color="#a855f7" />
              {t('milestones.description')} *
            </label>
            <textarea
              className="form-textarea"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
              maxLength={1000}
              placeholder="e.g. Vừa hoàn thành đường chạy Marathon 21km, Đậu visa học bổng Thạc sĩ..."
              rows={3}
            />
          </div>

          <div className="form-group">
            <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              <Tag size={14} color="#22d3ee" />
              {t('milestones.category')} *
            </label>
            <select
              className="form-select"
              value={category}
              onChange={(e) => setCategory(e.target.value as MilestoneCategory)}
            >
              {CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>
                  {t(`milestones.categories.${cat}`)}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              <Calendar size={14} color="#f59e0b" />
              {t('milestones.occurredAt')} *
            </label>
            <input
              type="date"
              className="form-input"
              value={occurredAt}
              onChange={(e) => setOccurredAt(e.target.value)}
              required
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
            <button type="button" onClick={onClose} className="btn btn-secondary">
              {t('common.cancel')}
            </button>
            <button type="submit" className="btn btn-primary" disabled={isLoading}>
              {isLoading ? <Loader2 size={16} className="spinner" /> : <Save size={16} />}
              <span>{t('common.save')}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
