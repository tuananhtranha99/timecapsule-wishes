import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  ArrowLeft,
  Sparkles,
  Loader2,
  AlertCircle,
  Sliders,
  CheckSquare,
  Square
} from 'lucide-react';
import { apiClient } from '../../api/apiClient';
import { WishPreview } from './WishPreview';
import { WishHistory } from './WishHistory';
import type {
  Recipient,
  Milestone,
  GeneratedWish,
  OccasionType,
  WishLanguage,
  ToneStyle,
  ApiResponse
} from '../../types';

const OCCASIONS: OccasionType[] = ['BIRTHDAY', 'TET', 'ANNIVERSARY', 'CUSTOM'];
const TONES: ToneStyle[] = ['WARM', 'SWEET', 'PLAYFUL', 'RESPECTFUL', 'CASUAL'];

export const GenerateWishPage: React.FC = () => {
  const { recipientId } = useParams<{ recipientId: string }>();
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();

  const [recipient, setRecipient] = useState<Recipient | null>(null);
  const [milestones, setMilestones] = useState<Milestone[]>([]);
  const [selectedMilestoneIds, setSelectedMilestoneIds] = useState<string[]>([]);
  const [occasionType, setOccasionType] = useState<OccasionType>('BIRTHDAY');
  const [language, setLanguage] = useState<WishLanguage>(
    i18n.language === 'en' ? 'EN' : 'VI'
  );
  const [pronounSelf, setPronounSelf] = useState('');
  const [pronounRecipient, setPronounRecipient] = useState('');
  const [toneStyle, setToneStyle] = useState<ToneStyle>('WARM');
  const [customPrompt, setCustomPrompt] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [currentWish, setCurrentWish] = useState<GeneratedWish | null>(null);
  const [wishHistory, setWishHistory] = useState<GeneratedWish[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const fetchData = async () => {
    if (!recipientId) return;
    try {
      setIsLoading(true);
      const [recRes, msRes, wishesRes] = await Promise.all([
        apiClient.get<ApiResponse<Recipient>>(`/recipients/${recipientId}`),
        apiClient.get<ApiResponse<Milestone[]>>(`/recipients/${recipientId}/milestones`),
        apiClient.get<ApiResponse<GeneratedWish[]>>(`/recipients/${recipientId}/wishes`),
      ]);

      setRecipient(recRes.data.data);
      const loadedMilestones = msRes.data.data;
      setMilestones(loadedMilestones);
      // Select all by default
      setSelectedMilestoneIds(loadedMilestones.map((m) => m.id));

      const loadedWishes = wishesRes.data.data;
      setWishHistory(loadedWishes);
      if (loadedWishes.length > 0) {
        setCurrentWish(loadedWishes[0]);
      }
    } catch (err) {
      console.error('Failed to load data for wish generation', err);
      navigate('/recipients');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [recipientId]);

  const toggleMilestone = (id: string) => {
    setSelectedMilestoneIds((prev) =>
      prev.includes(id) ? prev.filter((mId) => mId !== id) : [...prev, id]
    );
  };

  const handleSelectAll = () => {
    if (selectedMilestoneIds.length === milestones.length) {
      setSelectedMilestoneIds([]);
    } else {
      setSelectedMilestoneIds(milestones.map((m) => m.id));
    }
  };

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!recipientId) return;
    setErrorMsg(null);
    setIsGenerating(true);

    try {
      const res = await apiClient.post<ApiResponse<GeneratedWish>>('/wishes/generate', {
        recipientId,
        milestoneIds: selectedMilestoneIds,
        occasionType,
        language,
        pronounSelf: pronounSelf.trim() || null,
        pronounRecipient: pronounRecipient.trim() || null,
        toneStyle,
        customPrompt: customPrompt.trim() || null,
      });

      const newWish = res.data.data;
      setCurrentWish(newWish);
      setWishHistory((prev) => [newWish, ...prev]);

      // Scroll to preview smoothly
      window.scrollTo({ top: 320, behavior: 'smooth' });
    } catch (err: any) {
      if (err.response?.data?.message) {
        setErrorMsg(err.response.data.message);
      } else {
        setErrorMsg(t('common.error'));
      }
    } finally {
      setIsGenerating(false);
    }
  };

  const handleWishUpdated = (updated: GeneratedWish) => {
    setCurrentWish(updated);
    setWishHistory((prev) => prev.map((w) => (w.id === updated.id ? updated : w)));
  };

  if (isLoading) {
    return <div style={{ textAlign: 'center', padding: '4rem 0', color: 'var(--text-muted)' }}>{t('common.loading')}</div>;
  }

  return (
    <div>
      {/* Top Back Navigation */}
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

      {/* Header Banner */}
      <div className="glass-panel" style={{
        padding: '1.75rem 2rem',
        marginBottom: '2rem',
        background: 'linear-gradient(135deg, rgba(35, 25, 60, 0.8) 0%, rgba(15, 18, 40, 0.95) 100%)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.35rem' }}>
          <Sparkles size={26} color="#ec4899" />
          <h1 style={{ fontSize: '1.75rem' }}>
            {t('wishes.title')} — {recipient?.name}
          </h1>
        </div>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem' }}>
          {t('wishes.subtitle')}
        </p>
      </div>

      {/* Error Message */}
      {errorMsg && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          padding: '1rem',
          background: 'rgba(239, 68, 68, 0.15)',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          borderRadius: 'var(--radius-sm)',
          color: '#f87171',
          marginBottom: '1.5rem',
        }}>
          <AlertCircle size={20} />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Main Grid: Left Configuration Form, Right Result Preview */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: currentWish ? 'minmax(320px, 420px) 1fr' : '1fr',
        gap: '2rem',
        alignItems: 'start',
      }}>
        {/* Wish Generation Settings Form */}
        <div className="glass-panel" style={{ padding: '2rem' }}>
          <form onSubmit={handleGenerate}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem' }}>
              <Sliders size={18} color="#a855f7" />
              <h3>Tùy chọn sinh lời chúc</h3>
            </div>

            {/* Occasion */}
            <div className="form-group">
              <label className="form-label">{t('wishes.occasion')}</label>
              <select
                className="form-select"
                value={occasionType}
                onChange={(e) => setOccasionType(e.target.value as OccasionType)}
              >
                {OCCASIONS.map((occ) => (
                  <option key={occ} value={occ}>
                    {t(`wishes.occasions.${occ}`)}
                  </option>
                ))}
              </select>
            </div>

            {/* Language */}
            <div className="form-group">
              <label className="form-label">{t('wishes.language')}</label>
              <div style={{ display: 'flex', gap: '0.75rem' }}>
                <button
                  type="button"
                  onClick={() => setLanguage('VI')}
                  className={`btn ${language === 'VI' ? 'btn-primary' : 'btn-secondary'}`}
                  style={{ flex: 1, padding: '0.55rem' }}
                >
                  🇻🇳 Tiếng Việt
                </button>
                <button
                  type="button"
                  onClick={() => setLanguage('EN')}
                  className={`btn ${language === 'EN' ? 'btn-primary' : 'btn-secondary'}`}
                  style={{ flex: 1, padding: '0.55rem' }}
                >
                  🇺🇸 English
                </button>
              </div>
            </div>

            {/* Milestones Selection */}
            <div className="form-group" style={{ marginTop: '1.25rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <label className="form-label">{t('wishes.selectMilestones')}</label>
                {milestones.length > 0 && (
                  <button
                    type="button"
                    onClick={handleSelectAll}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#a855f7',
                      fontSize: '0.8rem',
                      cursor: 'pointer',
                      fontWeight: 600,
                    }}
                  >
                    {selectedMilestoneIds.length === milestones.length ? 'Bỏ chọn tất cả' : 'Chọn tất cả'}
                  </button>
                )}
              </div>

              {milestones.length === 0 ? (
                <div style={{
                  padding: '1rem',
                  background: 'rgba(245, 158, 11, 0.12)',
                  border: '1px solid rgba(245, 158, 11, 0.3)',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '0.85rem',
                  color: '#fbbf24',
                }}>
                  {t('wishes.zeroMilestonesAlert')}
                </div>
              ) : (
                <div style={{
                  maxHeight: '260px',
                  overflowY: 'auto',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '0.5rem',
                  paddingRight: '0.25rem',
                }}>
                  {milestones.map((m) => {
                    const isSelected = selectedMilestoneIds.includes(m.id);
                    return (
                      <div
                        key={m.id}
                        onClick={() => toggleMilestone(m.id)}
                        style={{
                          display: 'flex',
                          alignItems: 'flex-start',
                          gap: '0.75rem',
                          padding: '0.75rem',
                          borderRadius: 'var(--radius-sm)',
                          background: isSelected ? 'rgba(99, 102, 241, 0.15)' : 'rgba(255, 255, 255, 0.03)',
                          border: isSelected ? '1px solid rgba(99, 102, 241, 0.4)' : '1px solid var(--border-subtle)',
                          cursor: 'pointer',
                          transition: 'var(--transition)',
                        }}
                      >
                        <div style={{ marginTop: '2px', color: isSelected ? '#a855f7' : 'var(--text-dim)' }}>
                          {isSelected ? <CheckSquare size={18} /> : <Square size={18} />}
                        </div>
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <p style={{ fontSize: '0.875rem', color: 'var(--text-main)', lineHeight: 1.4 }}>
                            {m.description}
                          </p>
                          <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.25rem', fontSize: '0.75rem', color: 'var(--text-dim)' }}>
                            <span>{m.occurredAt}</span>
                            <span>•</span>
                            <span>{t(`milestones.categories.${m.category}`)}</span>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Pronouns */}
            <div style={{ marginTop: '1rem' }}>
              <label className="form-label" style={{ marginBottom: '0.4rem', display: 'block' }}>
                {t('wishes.pronouns')}
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginBottom: '0.25rem', display: 'block' }}>
                    {t('wishes.pronounSelf')}
                  </label>
                  <input
                    type="text"
                    className="form-input"
                    value={pronounSelf}
                    onChange={(e) => setPronounSelf(e.target.value)}
                    placeholder={t('wishes.pronounSelfPlaceholder')}
                  />
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginBottom: '0.25rem', display: 'block' }}>
                    {t('wishes.pronounRecipient')}
                  </label>
                  <input
                    type="text"
                    className="form-input"
                    value={pronounRecipient}
                    onChange={(e) => setPronounRecipient(e.target.value)}
                    placeholder={t('wishes.pronounRecipientPlaceholder')}
                  />
                </div>
              </div>
            </div>

            {/* Tone Style */}
            <div className="form-group" style={{ marginTop: '1rem' }}>
              <label className="form-label">{t('wishes.toneStyle')}</label>
              <select
                className="form-select"
                value={toneStyle}
                onChange={(e) => setToneStyle(e.target.value as ToneStyle)}
              >
                {TONES.map((tone) => (
                  <option key={tone} value={tone}>
                    {t(`wishes.tones.${tone}`)}
                  </option>
                ))}
              </select>
            </div>

            {/* Custom Prompt */}
            <div className="form-group" style={{ marginTop: '1rem' }}>
              <label className="form-label">{t('wishes.customPrompt')}</label>
              <textarea
                className="form-textarea"
                value={customPrompt}
                onChange={(e) => setCustomPrompt(e.target.value)}
                placeholder={t('wishes.customPromptPlaceholder')}
                rows={2}
              />
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', padding: '0.85rem', marginTop: '1rem' }}
              disabled={isGenerating}
            >
              {isGenerating ? (
                <>
                  <Loader2 size={18} className="spinner" />
                  <span>{t('wishes.generating')}</span>
                </>
              ) : (
                <>
                  <Sparkles size={18} />
                  <span>{t('wishes.generateBtn')}</span>
                </>
              )}
            </button>
          </form>
        </div>

        {/* Wish Preview Card */}
        <div>
          {currentWish && (
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
                <Sparkles size={20} color="#ec4899" />
                <h2>{t('wishes.resultTitle')}</h2>
              </div>
              <WishPreview wish={currentWish} onWishUpdated={handleWishUpdated} />
            </div>
          )}

          {/* Past Wishes History */}
          <WishHistory wishes={wishHistory} />
        </div>
      </div>
    </div>
  );
};
