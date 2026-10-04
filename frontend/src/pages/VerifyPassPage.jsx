import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { QRCodeSVG } from 'qrcode.react';
import { CheckCircle, XCircle, AlertTriangle, Bus, User, Calendar, MapPin, Hash } from 'lucide-react';

const API_BASE = import.meta.env.VITE_API_URL || '/api';

const STATUS_CONFIG = {
  ACTIVE:   { color: '#16a34a', bg: '#dcfce7', border: '#86efac', icon: CheckCircle,    label: 'ACTIVE'   },
  EXPIRED:  { color: '#d97706', bg: '#fef3c7', border: '#fcd34d', icon: AlertTriangle,  label: 'EXPIRED'  },
  REVOKED:  { color: '#dc2626', bg: '#fee2e2', border: '#fca5a5', icon: XCircle,        label: 'REVOKED'  },
};

export default function VerifyPassPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [pass, setPass] = useState(null);
  const [photoUrl, setPhotoUrl] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token) { setError('No verification token provided.'); setLoading(false); return; }

    const fetchPass = async () => {
      try {
        const res = await fetch(`${API_BASE}/public/verify-pass?token=${encodeURIComponent(token)}`);
        if (!res.ok) {
          const body = await res.json().catch(() => ({}));
          throw new Error(body.message || 'Invalid or revoked bus pass.');
        }
        const data = await res.json();
        setPass(data);

        // Fetch photo
        const photoRes = await fetch(`${API_BASE}/public/pass-photo?token=${encodeURIComponent(token)}`);
        if (photoRes.ok) {
          const blob = await photoRes.blob();
          setPhotoUrl(URL.createObjectURL(blob));
        }
      } catch (err) {
        setError(err.message || 'Unable to verify this bus pass.');
      } finally {
        setLoading(false);
      }
    };

    fetchPass();
    return () => { if (photoUrl) URL.revokeObjectURL(photoUrl); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  const qrValue = token ? `${window.location.origin}/verify-pass?token=${token}` : '';
  const cfg = pass ? (STATUS_CONFIG[pass.status] || STATUS_CONFIG.ACTIVE) : null;

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        {/* College Header */}
        <div style={styles.header}>
          <img src="/logo.jpg" alt="LBRCE Logo" style={styles.logo} />
          <div>
            <h1 style={styles.collegeName}>LAKIREDDY BALI REDDY COLLEGE OF ENGINEERING</h1>
            <p style={styles.collegeSubtitle}>(AUTONOMOUS)</p>
            <p style={styles.collegeAddress}>L.B. Reddy Nagar, Mylavaram, N.T.R. District, Andhra Pradesh - 521230</p>
          </div>
        </div>

        <div style={styles.passTitle}>
          <Bus size={22} color="#1e40af" />
          <span>DIGITAL BUS PASS VERIFICATION</span>
        </div>

        {loading && (
          <div style={styles.stateBox}>
            <div style={styles.spinner} />
            <p style={{ color: '#64748b', marginTop: 16 }}>Verifying bus pass…</p>
          </div>
        )}

        {error && !loading && (
          <div style={{ ...styles.stateBox, ...styles.errorBox }}>
            <XCircle size={56} color="#dc2626" />
            <h2 style={{ color: '#dc2626', marginTop: 16, fontWeight: 700 }}>Verification Failed</h2>
            <p style={{ color: '#7f1d1d', marginTop: 8 }}>{error}</p>
          </div>
        )}

        {pass && !loading && (
          <>
            {/* Status Banner */}
            <div style={{ ...styles.statusBanner, background: cfg.bg, borderColor: cfg.border }}>
              <cfg.icon size={22} color={cfg.color} />
              <span style={{ color: cfg.color, fontWeight: 700, fontSize: 15, marginLeft: 8 }}>
                This bus pass is {cfg.label}
              </span>
            </div>

            {/* Main Card */}
            <div style={styles.card}>
              {/* Left: Photo + QR */}
              <div style={styles.leftCol}>
                <div style={styles.photoFrame}>
                  {photoUrl ? (
                    <img src={photoUrl} alt={pass.studentName} style={styles.photo} />
                  ) : (
                    <div style={styles.photoPlaceholder}>
                      <User size={48} color="#94a3b8" />
                      <span style={{ fontSize: 11, color: '#94a3b8', marginTop: 6 }}>No Photo</span>
                    </div>
                  )}
                </div>

                {qrValue && (
                  <div style={styles.qrBox}>
                    <QRCodeSVG value={qrValue} size={110} level="M" includeMargin={true} />
                    <p style={styles.qrCaption}>Scan to verify</p>
                  </div>
                )}
              </div>

              {/* Right: Details */}
              <div style={styles.rightCol}>
                <div style={styles.passNumberRow}>
                  <Hash size={14} color="#64748b" />
                  <span style={styles.passNumberLabel}>Pass No:</span>
                  <span style={styles.passNumber}>{pass.passNumber}</span>
                  <span style={{ ...styles.statusPill, background: cfg.bg, color: cfg.color, borderColor: cfg.border }}>
                    {cfg.label}
                  </span>
                </div>

                <h2 style={styles.studentName}>{pass.studentName}</h2>
                <p style={styles.rollNumber}>{pass.rollNumber}</p>

                <div style={styles.detailsGrid}>
                  <DetailItem icon={<User size={13} />} label="Branch" value={pass.branch} />
                  <DetailItem icon={<Calendar size={13} />} label="Year / Semester" value={`${pass.year || '—'} / ${pass.semester || '—'}`} />
                  <DetailItem icon={<Bus size={13} />} label="Bus Number" value={pass.busNumber} />
                  <DetailItem icon={<MapPin size={13} />} label="Boarding Point" value={pass.boardingPoint} />
                  <DetailItem icon={<MapPin size={13} />} label="Starting Point" value={pass.startingPoint} />
                  <DetailItem icon={<MapPin size={13} />} label="Ending Point" value={pass.endingPoint} />
                  <DetailItem icon={<Calendar size={13} />} label="Academic Year" value={pass.academicYear} />
                  <DetailItem icon={<Calendar size={13} />} label="Valid From" value={pass.validFrom} />
                  <DetailItem icon={<Calendar size={13} />} label="Valid Until" value={pass.validUntil} />
                </div>
              </div>
            </div>

            <p style={styles.footerNote}>
              This verification page confirms the authenticity of the bus pass issued by LBRCE Transport Department.
            </p>
          </>
        )}
      </div>
    </div>
  );
}

function DetailItem({ icon, label, value }) {
  return (
    <div style={styles.detailItem}>
      <span style={styles.detailIcon}>{icon}</span>
      <div>
        <p style={styles.detailLabel}>{label}</p>
        <p style={styles.detailValue}>{value || '—'}</p>
      </div>
    </div>
  );
}

const styles = {
  page: {
    minHeight: '100vh',
    background: 'linear-gradient(135deg, #eff6ff 0%, #f0f9ff 50%, #f5f3ff 100%)',
    display: 'flex',
    alignItems: 'flex-start',
    justifyContent: 'center',
    padding: '24px 16px 40px',
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
  },
  container: {
    width: '100%',
    maxWidth: 820,
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: 16,
    background: '#1e3a8a',
    borderRadius: '16px 16px 0 0',
    padding: '20px 28px',
    color: '#fff',
  },
  logo: {
    width: 64,
    height: 64,
    borderRadius: '50%',
    background: '#fff',
    padding: 4,
    flexShrink: 0,
    objectFit: 'contain',
  },
  collegeName: {
    margin: 0,
    fontSize: 18,
    fontWeight: 800,
    letterSpacing: 0.3,
    lineHeight: 1.3,
    color: '#fff',
  },
  collegeSubtitle: {
    margin: '2px 0 0',
    fontSize: 13,
    color: '#93c5fd',
    fontWeight: 600,
  },
  collegeAddress: {
    margin: '2px 0 0',
    fontSize: 11,
    color: '#bfdbfe',
  },
  passTitle: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    background: '#dbeafe',
    padding: '12px 28px',
    fontSize: 14,
    fontWeight: 700,
    color: '#1e40af',
    letterSpacing: 1,
    borderBottom: '2px solid #bfdbfe',
  },
  stateBox: {
    background: '#fff',
    borderRadius: '0 0 16px 16px',
    padding: '60px 28px',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    boxShadow: '0 4px 24px rgba(0,0,0,0.08)',
  },
  errorBox: {
    border: '2px solid #fca5a5',
  },
  spinner: {
    width: 48,
    height: 48,
    border: '4px solid #e0e7ff',
    borderTop: '4px solid #1e40af',
    borderRadius: '50%',
    animation: 'spin 0.8s linear infinite',
  },
  statusBanner: {
    display: 'flex',
    alignItems: 'center',
    padding: '12px 20px',
    borderRadius: 8,
    border: '1.5px solid',
    margin: '16px 0 0',
  },
  card: {
    background: '#fff',
    borderRadius: '0 0 16px 16px',
    boxShadow: '0 4px 24px rgba(0,0,0,0.08)',
    display: 'flex',
    gap: 0,
    overflow: 'hidden',
    marginTop: 0,
    border: '1.5px solid #e2e8f0',
    borderTop: 'none',
  },
  leftCol: {
    background: '#f8fafc',
    borderRight: '1.5px solid #e2e8f0',
    padding: '24px 20px',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 16,
    minWidth: 160,
  },
  photoFrame: {
    width: 130,
    height: 155,
    borderRadius: 10,
    border: '3px solid #bfdbfe',
    overflow: 'hidden',
    background: '#f1f5f9',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  photo: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  },
  photoPlaceholder: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    height: '100%',
  },
  qrBox: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    background: '#fff',
    borderRadius: 10,
    border: '1.5px solid #e2e8f0',
    padding: '10px 10px 6px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.05)',
  },
  qrCaption: {
    fontSize: 10,
    color: '#64748b',
    marginTop: 4,
    textAlign: 'center',
    fontWeight: 600,
    letterSpacing: 0.5,
  },
  rightCol: {
    flex: 1,
    padding: '24px 28px',
  },
  passNumberRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
    marginBottom: 10,
    flexWrap: 'wrap',
  },
  passNumberLabel: {
    fontSize: 12,
    color: '#64748b',
    fontWeight: 500,
  },
  passNumber: {
    fontSize: 13,
    fontWeight: 700,
    color: '#1e3a8a',
    letterSpacing: 0.5,
  },
  statusPill: {
    fontSize: 11,
    fontWeight: 700,
    padding: '2px 10px',
    borderRadius: 20,
    border: '1.5px solid',
    letterSpacing: 0.5,
  },
  studentName: {
    margin: '0 0 4px',
    fontSize: 22,
    fontWeight: 800,
    color: '#0f172a',
  },
  rollNumber: {
    margin: '0 0 16px',
    fontSize: 14,
    color: '#475569',
    fontWeight: 500,
  },
  detailsGrid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '10px 20px',
  },
  detailItem: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: 8,
    background: '#f8fafc',
    borderRadius: 8,
    padding: '8px 10px',
    border: '1px solid #e2e8f0',
  },
  detailIcon: {
    color: '#64748b',
    marginTop: 3,
    flexShrink: 0,
  },
  detailLabel: {
    margin: '0 0 2px',
    fontSize: 10,
    color: '#64748b',
    fontWeight: 600,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  detailValue: {
    margin: 0,
    fontSize: 13,
    color: '#1e293b',
    fontWeight: 600,
  },
  footerNote: {
    textAlign: 'center',
    fontSize: 11,
    color: '#94a3b8',
    marginTop: 16,
    padding: '0 12px',
  },
};
