import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { History, Copy, Check, Calendar, GitCommit } from 'lucide-react';
import type { GeneratedWish } from '../../types';

interface WishHistoryProps {
  wishes: GeneratedWish[];
}

export const WishHistory: React.FC<WishHistoryProps> = ({ wishes }) => {
  const { t } = useTranslation();
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const handleCopy = async (id: string, text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedId(id);
      setTimeout(() => setCopiedId(null), 2000);
    } catch (err) {
      console.error('Failed to copy', err);
    }
  };

  return (
    <div style={{ marginTop: '2rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem' }}>
        <History size={20} color="#ec4899" />
        <h3>{t('wishes.historyTitle')}</h3>
      </div>

      {wishes.length === 0 ? (
        <div className="glass-panel" style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          {t('wishes.noHistory')}
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {wishes.map((w) => {
            const text = w.editedText || w.generatedText;
            const isCopied = copiedId === w.id;

            return (
              <div
                key={w.id}
                className="glass-panel"
                style={{ padding: '1.25rem 1.5rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <span className="badge badge-relationship">
                      {t(`wishes.occasions.${w.occasionType}`)}
                    </span>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                      <Calendar size={12} />
                      {new Date(w.createdAt).toLocaleDateString()}
                    </span>
                  </div>

                  <span style={{
                    fontSize: '0.8rem',
                    color: '#fbbf24',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.25rem',
                  }}>
                    <GitCommit size={13} />
                    {t('wishes.version', { version: w.version })}
                  </span>
                </div>

                <p style={{
                  fontSize: '0.95rem',
                  color: 'var(--text-main)',
                  lineHeight: 1.6,
                  whiteSpace: 'pre-wrap',
                  background: 'rgba(0, 0, 0, 0.2)',
                  padding: '0.85rem 1rem',
                  borderRadius: 'var(--radius-sm)',
                }}>
                  {text}
                </p>

                <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                  <button
                    onClick={() => handleCopy(w.id, text)}
                    className="btn btn-secondary"
                    style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
                  >
                    {isCopied ? <Check size={14} color="#34d399" /> : <Copy size={14} />}
                    <span>{isCopied ? t('wishes.copied') : t('wishes.copyBtn')}</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
