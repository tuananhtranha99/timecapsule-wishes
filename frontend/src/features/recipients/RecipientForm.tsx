import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { X, Save, Loader2, User, Calendar, Heart, FileText } from 'lucide-react';
import { apiClient } from '../../api/apiClient';
import type { Recipient, ApiResponse } from '../../types';

interface RecipientFormProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (recipient: Recipient) => void;
  recipientToEdit?: Recipient | null;
}

export const RecipientForm: React.FC<RecipientFormProps> = ({
  isOpen,
  onClose,
  onSuccess,
  recipientToEdit,
}) => {
  const { t } = useTranslation();
  const [name, setName] = useState('');
  const [birthday, setBirthday] = useState('');
  const [relationship, setRelationship] = useState('');
  const [notes, setNotes] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (recipientToEdit) {
      setName(recipientToEdit.name || '');
      setBirthday(recipientToEdit.birthday || '');
      setRelationship(recipientToEdit.relationship || '');
      setNotes(recipientToEdit.notes || '');
    } else {
      setName('');
      setBirthday('');
      setRelationship('');
      setNotes('');
    }
    setErrorMsg(null);
  }, [recipientToEdit, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setIsLoading(true);

    const payload = {
      name,
      birthday: birthday || null,
      relationship: relationship || null,
      notes: notes || null,
    };

    try {
      if (recipientToEdit) {
        const res = await apiClient.put<ApiResponse<Recipient>>(`/recipients/${recipientToEdit.id}`, payload);
        onSuccess(res.data.data);
      } else {
        const res = await apiClient.post<ApiResponse<Recipient>>('/recipients', payload);
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
          <h3>{recipientToEdit ? t('recipients.editTitle') : t('recipients.createTitle')}</h3>
          <button
            onClick={onClose}
            className="btn btn-secondary"
            style={{ padding: '0.35rem', borderRadius: '50%' }}
          >
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
              <User size={14} color="#a855f7" />
              {t('recipients.name')} *
            </label>
            <input
              type="text"
              className="form-input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              maxLength={150}
              placeholder="e.g. Lan Hương"
            />
          </div>

          <div className="form-group">
            <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              <Calendar size={14} color="#3b82f6" />
              {t('recipients.birthday')}
            </label>
            <input
              type="date"
              className="form-input"
              value={birthday}
              onChange={(e) => setBirthday(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              <Heart size={14} color="#ec4899" />
              {t('recipients.relationship')}
            </label>
            <input
              type="text"
              className="form-input"
              value={relationship}
              onChange={(e) => setRelationship(e.target.value)}
              maxLength={100}
              placeholder="e.g. Bạn thân, Mẹ, Đồng nghiệp"
            />
          </div>

          <div className="form-group">
            <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              <FileText size={14} color="#eab308" />
              {t('recipients.notes')}
            </label>
            <textarea
              className="form-textarea"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              maxLength={1000}
              placeholder="e.g. Thích chạy bộ, mê cà phê espresso, đang học tiếng Nhật..."
              rows={3}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
            <button type="button" onClick={onClose} className="btn btn-secondary">
              {t('recipients.cancelBtn')}
            </button>
            <button type="submit" className="btn btn-primary" disabled={isLoading}>
              {isLoading ? <Loader2 size={16} className="spinner" /> : <Save size={16} />}
              <span>{t('recipients.saveBtn')}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
