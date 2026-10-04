import React from 'react';
import { QRCodeSVG } from 'qrcode.react';
import StatusBadge from '../common/StatusBadge';

const PUBLIC_VERIFY_BASE = 'https://lbrce-btms-web.onrender.com/verify-pass?token=';

const FRONTEND_VERIFY_BASE =
  import.meta.env.VITE_PASS_FRONTEND_VERIFY_URL ||
  (typeof window !== 'undefined' &&
   window.location.hostname !== 'localhost' &&
   window.location.hostname !== '127.0.0.1'
    ? `${window.location.origin}/verify-pass?token=`
    : PUBLIC_VERIFY_BASE);

export default function PassCard({ pass, photoUrl }) {
  const qrValue =
    pass.verificationUrl ||
    (pass.verificationToken ? FRONTEND_VERIFY_BASE + pass.verificationToken : '');

  const item = (label, value) => (
    <div className="pass-detail-item">
      <p className="pass-detail-label">{label}</p>
      <p className="pass-detail-value">{value || '—'}</p>
    </div>
  );

  return (
    <div id="bus-pass" className="pass-card">
      {/* Header */}
      <div className="pass-header">
        <img src="/logo.jpg" alt="LBRCE Logo" className="pass-logo" />
        <div>
          <h2 className="pass-college-name">LAKIREDDY BALI REDDY COLLEGE OF ENGINEERING</h2>
          <p className="pass-subtitle">DIGITAL BUS PASS</p>
        </div>
      </div>

      <div className="pass-body">
        {/* Top section: photo + pass number/status */}
        <div className="pass-top-section">
          <div className="pass-photo-wrap">
            {photoUrl ? (
              <img
                src={photoUrl}
                alt={`${pass.studentName} photo`}
                className="pass-photo"
              />
            ) : (
              <div className="pass-photo-placeholder">Photo<br />unavailable</div>
            )}
          </div>
          <div className="pass-id-info">
            <p className="pass-detail-label">Pass Number</p>
            <p className="pass-pass-number">{pass.passNumber}</p>
            <div className="pass-status-wrap">
              <StatusBadge status={pass.status || 'ACTIVE'} />
            </div>
            {qrValue && (
              <div className="pass-qr-inline">
                <QRCodeSVG value={qrValue} size={100} level="M" includeMargin={true} />
                <p className="pass-qr-caption">Scan to verify</p>
              </div>
            )}
          </div>
        </div>

        {/* Details grid */}
        <div className="pass-details-grid">
          {item('Student Name', pass.studentName)}
          {item('Roll Number', pass.rollNumber)}
          {item('Branch', pass.branch)}
          {item('Year / Semester', `${pass.year || '—'} / ${pass.semester || '—'}`)}
          {item('Phone Number', pass.phoneNumber)}
          {item('Bus Number', pass.busNumber)}
          {item('Starting Point', pass.startingPoint)}
          {item('Ending Point', pass.endingPoint)}
          {item('Boarding Point', pass.boardingPoint)}
          {item('Academic Year', pass.academicYear)}
          {item('Valid From', pass.validFrom)}
          {item('Valid Until', pass.validUntil)}
        </div>
      </div>

      {/* Footer */}
      <div className="pass-footer">
        System-generated digital bus pass. Verify authenticity by scanning the QR code.
      </div>
    </div>
  );
}
