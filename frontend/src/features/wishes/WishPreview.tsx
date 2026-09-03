import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Copy, Check, Pencil, Save, Sparkles, Loader2, GitCommit } from 'lucide-react';
import { apiClient } from '../../api/apiClient';
import type { GeneratedWish, ApiResponse } from '../../types';

interface WishPreviewProps {
  wish: GeneratedWish;
  onWishUpdated: (updatedWish: GeneratedWish) => void;
}

export const WishPreview: React.FC<WishPreviewProps> = ({ wish, onWishUpdated }) => {
  const { t } = useTranslation();
  const [isEditing, setIsEditing] = useState(false);
  const [editedText, setEditedText] = useState(wish.editedText || wish.generatedText);
  const [isSaving, setIsSaving] = useState(false);
  const [copied, setCopied] = useState(false);

  const displayText = wish.editedText || wish.generatedText;

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(displayText);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch (err) {
      console.error('Failed to copy', err);
    }
  };

  const handleSaveRevision = async () => {
    if (!editedText.trim()) return;
    try {
      setIsSaving(true);
      const res = await apiClient.put<ApiResponse<GeneratedWish>>(`/wishes/${wish.id}`, {
        editedText,
      });
      onWishUpdated(res.data.data);
      setIsEditing(false);
    } catch (err) {
      alert(t('common.error'));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div
      className="glass-panel"
      style={{
        padding: '2rem',
        borderRadius: 'var(--radius-lg)',
        border: '1px solid rgba(236, 72, 153, 0.4)',
        boxShadow: '0 0 35px rgba(168, 85, 247, 0.25)',
        position: 'relative',
        background: 'linear-gradient(135deg, rgba(28, 22, 54, 0.85) 0%, rgba(15, 18, 38, 0.95) 100%)',
        marginBottom: '2.5rem',
      }}
    >
      {/* Header Badges */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '1.25rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.35rem',
            padding: '0.35rem 0.85rem',
            borderRadius: 'var(--radius-pill)',
            background: 'var(--grad-primary)',
            color: '#fff',
            fontSize: '0.8rem',
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: '0.05em',
          }}>
            <Sparkles size={13} />
            {t(`wishes.occasions.${wish.occasionType}`)}
          </span>

          <span style={{
            padding: '0.35rem 0.75rem',
            borderRadius: 'var(--radius-pill)',
            background: 'rgba(255, 255, 255, 0.08)',
            border: '1px solid var(--border-subtle)',
            fontSize: '0.8rem',
            fontWeight: 600,
            color: 'var(--text-muted)',
          }}>
            {wish.language === 'VI' ? '🇻🇳 Tiếng Việt' : '🇺🇸 English'}
          </span>
        </div>

        <span style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '0.35rem',
          fontSize: '0.85rem',
          color: '#fbbf24',
          background: 'rgba(245, 158, 11, 0.12)',
          border: '1px solid rgba(245, 158, 11, 0.3)',
          padding: '0.25rem 0.65rem',
          borderRadius: 'var(--radius-pill)',
          fontWeight: 600,
        }}>
          <GitCommit size={14} />
          {t('wishes.version', { version: wish.version })}
        </span>
      </div>

      {/* Wish Content Body */}
      {isEditing ? (
        <div style={{ marginBottom: '1.25rem' }}>
          <textarea
            className="form-textarea"
            style={{ minHeight: '160px', fontSize: '1.05rem', lineHeight: 1.7 }}
            value={editedText}
            onChange={(e) => setEditedText(e.target.value)}
          />
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '0.75rem' }}>
            <button onClick={() => setIsEditing(false)} className="btn btn-secondary">
              {t('common.cancel')}
            </button>
            <button onClick={handleSaveRevision} className="btn btn-primary" disabled={isSaving}>
              {isSaving ? <Loader2 size={16} className="spinner" /> : <Save size={16} />}
              <span>{t('wishes.saveRevisionBtn')}</span>
            </button>
          </div>
        </div>
      ) : (
        <div style={{
          padding: '1.5rem',
          background: 'rgba(0, 0, 0, 0.25)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid rgba(255, 255, 255, 0.05)',
          marginBottom: '1.5rem',
        }}>
          <p style={{
            fontSize: '1.15rem',
            lineHeight: 1.8,
            color: '#f8fafc',
            whiteSpace: 'pre-wrap',
            fontStyle: 'italic',
          }}>
            "{displayText}"
          </p>
        </div>
      )}

      {/* Actions */}
      {!isEditing && (
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', flexWrap: 'wrap' }}>
          <button
            onClick={() => setIsEditing(true)}
            className="btn btn-secondary"
            style={{ padding: '0.6rem 1rem' }}
          >
            <Pencil size={16} />
            <span>{t('wishes.editBtn')}</span>
          </button>

          <button
            onClick={handleCopy}
            className="btn btn-primary"
            style={{ padding: '0.6rem 1.25rem' }}
          >
            {copied ? <Check size={16} color="#34d399" /> : <Copy size={16} />}
            <span>{copied ? t('wishes.copied') : t('wishes.copyBtn')}</span>
          </button>
        </div>
      )}
    </div>
  );
};
