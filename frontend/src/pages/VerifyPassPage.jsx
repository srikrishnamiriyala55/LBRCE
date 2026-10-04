import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { CheckCircle, XCircle, AlertTriangle, ShieldCheck, Bus, MapPin, Calendar, Hash, User, Phone } from 'lucide-react';

const API_BASE = import.meta.env.VITE_API_URL || '/api';

const STATUS_CONFIG = {
  ACTIVE: {
    cls: 'vp-status-active',
    Icon: CheckCircle,
    label: 'ACTIVE',
    badgeText: 'OFFICIAL VERIFIED PASS • ACTIVE',
  },
  EXPIRED: {
    cls: 'vp-status-expired',
    Icon: AlertTriangle,
    label: 'EXPIRED',
    badgeText: 'VERIFIED PASS • EXPIRED',
  },
  REVOKED: {
    cls: 'vp-status-revoked',
    Icon: XCircle,
    label: 'REVOKED',
    badgeText: 'PASS REVOKED • INVALID',
  },
};

export default function VerifyPassPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [pass, setPass] = useState(null);
  const [photoUrl, setPhotoUrl] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token) {
      setError('No verification token provided.');
      setLoading(false);
      return;
    }

    let objectUrl = null;
    const fetchPass = async () => {
      try {
        const res = await fetch(`${API_BASE}/public/verify-pass?token=${encodeURIComponent(token)}`);
        if (!res.ok) {
          const body = await res.json().catch(() => ({}));
          throw new Error(body.message || 'Invalid or revoked bus pass.');
        }
        setPass(await res.json());

        const photoRes = await fetch(`${API_BASE}/public/pass-photo?token=${encodeURIComponent(token)}`);
        if (photoRes.ok) {
          objectUrl = URL.createObjectURL(await photoRes.blob());
          setPhotoUrl(objectUrl);
        }
      } catch (err) {
        setError(err.message || 'Unable to verify this bus pass.');
      } finally {
        setLoading(false);
      }
    };

    fetchPass();
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [token]);

  const cfg = pass ? STATUS_CONFIG[pass.status] || STATUS_CONFIG.ACTIVE : null;

  return (
    <div className="vp-page">
      <div className="vp-container">
        {/* ── Loading ────────────────────────────────────────── */}
        {loading && (
          <div className="vp-state-box">
            <div className="vp-spinner" />
            <p className="vp-state-msg">Verifying bus pass authenticity…</p>
          </div>
        )}

        {/* ── Error ──────────────────────────────────────────── */}
        {error && !loading && (
          <div className="vp-state-box vp-error-box">
            <XCircle size={54} color="#dc2626" />
            <h2 className="vp-error-title">Verification Failed</h2>
            <p className="vp-error-msg">{error}</p>
          </div>
        )}

        {/* ── Verified Pass Card ─────────────────────────────── */}
        {pass && !loading && (
          <div className="vp-card">
            {/* Header */}
            <div className="vp-header">
              <img src="/logo.jpg" alt="LBRCE Logo" className="vp-logo" />
              <div className="vp-header-text">
                <h1 className="vp-college-name">LAKIREDDY BALI REDDY COLLEGE OF ENGINEERING</h1>
                <p className="vp-college-sub">(AUTONOMOUS) • MYLAVARAM</p>
                <p className="vp-header-badge">DIGITAL BUS PASS VERIFICATION</p>
              </div>
            </div>

            {/* Seamless Verification Banner */}
            <div className={`vp-verified-banner ${cfg.cls}`}>
              <cfg.Icon size={18} className="vp-banner-icon" />
              <span>{cfg.badgeText}</span>
            </div>

            {/* Student Identity: Photo + Primary Info */}
            <div className="vp-identity-section">
              <div className="vp-photo-frame">
                {photoUrl ? (
                  <img src={photoUrl} alt={pass.studentName} className="vp-photo" />
                ) : (
                  <div className="vp-photo-placeholder">
                    <User size={36} color="#94a3b8" />
                    <span>No Photo</span>
                  </div>
                )}
              </div>

              <div className="vp-primary-info">
                <div className="vp-pass-num-badge">
                  <Hash size={12} />
                  <span>PASS: {pass.passNumber}</span>
                </div>
                <h2 className="vp-student-name">{pass.studentName}</h2>
                <div className="vp-roll-badge">{pass.rollNumber}</div>
                <p className="vp-branch-text">
                  {pass.branch || '—'} &bull; Year {pass.year || '—'} / Sem {pass.semester || '—'}
                </p>
              </div>
            </div>

            {/* Details Grid (Strictly 2 Columns for flawless mobile alignment) */}
            <div className="vp-details-grid">
              <VpItem icon={<Bus size={13} />} label="Bus Number" value={pass.busNumber} />
              <VpItem icon={<MapPin size={13} />} label="Boarding Point" value={pass.boardingPoint} />
              <VpItem icon={<MapPin size={13} />} label="Starting Point" value={pass.startingPoint} />
              <VpItem icon={<MapPin size={13} />} label="Ending Point" value={pass.endingPoint} />
              <VpItem icon={<Calendar size={13} />} label="Academic Year" value={pass.academicYear} />
              <VpItem icon={<Phone size={13} />} label="Phone Number" value={pass.phoneNumber} />
              <VpItem icon={<Calendar size={13} />} label="Valid From" value={pass.validFrom} />
              <VpItem icon={<Calendar size={13} />} label="Valid Until" value={pass.validUntil} />
            </div>

            {/* Verification Security Footer */}
            <div className="vp-footer">
              <div className="vp-footer-badge">
                <ShieldCheck size={14} className="text-blue-600" />
                <span>Verified Authenticity &bull; Transport Department, LBRCE</span>
              </div>
              <p className="vp-footer-note">
                System-verified digital pass issued by Lakireddy Bali Reddy College of Engineering.
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function VpItem({ icon, label, value }) {
  return (
    <div className="vp-grid-item">
      <div className="vp-item-header">
        <span className="vp-item-icon">{icon}</span>
        <span className="vp-item-label">{label}</span>
      </div>
      <p className="vp-item-value">{value || '—'}</p>
    </div>
  );
}
