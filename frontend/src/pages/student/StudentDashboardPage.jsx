import React, { useEffect, useState } from 'react';
import { Bus, MapPin, CreditCard, Ticket } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import DashboardCard from '../../components/common/DashboardCard';
import StatusBadge from '../../components/common/StatusBadge';
import api from '../../utils/axios';
import { useAuth } from '../../contexts/AuthContext';
import LoadingSpinner from '../../components/common/LoadingSpinner';

const StudentDashboardPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [inchargePhotoUrl, setInchargePhotoUrl] = useState('');

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const res = await api.get('/student/dashboard');
        const dashboard = res.data.data || res.data;
        setData(dashboard);
        if (dashboard.inchargePhotoAvailable) {
          const photo = await api.get('/student/incharge/photo', { responseType: 'blob' });
          setInchargePhotoUrl(URL.createObjectURL(photo.data));
        }
      } catch (err) {
        setError('Failed to load dashboard data');
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  if (loading) return <LoadingSpinner />;
  if (error) return <div className="text-red-500">{error}</div>;

  return (
    <div className="space-y-6">
      {/* Welcome banner */}
      <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <h2 className="text-2xl font-bold text-gray-800">Welcome back, {user?.name}!</h2>
        <p className="text-gray-500 mt-1">Roll Number: {user?.rollNumber || data?.rollNumber}</p>
      </div>

      {/* Quick actions */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-800 mb-3">Available Actions</h3>
        <div className="flex flex-wrap gap-3">
          {data?.canApply && <Link to="/student/apply" className="btn-primary">Apply for Bus</Link>}
          {data?.canTransfer && <Link to="/student/transfers" className="btn-primary">Request Bus Transfer</Link>}
          {data?.hasActiveApplication && data?.transportationStatus !== 'ALLOCATED' && (
            <Link to="/student/applications" className="btn-secondary">Track Pending Application</Link>
          )}
          {!data?.canApply && !data?.canTransfer && !data?.canComplain && !data?.hasActiveApplication && (
            <p className="text-sm text-gray-500">No transportation action is currently available.</p>
          )}
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">

        {/* Bus Number — navigate to application history */}
        <DashboardCard
          title="Bus Number"
          value={data?.busNumber || 'Not Assigned'}
          icon={Bus}
          color="blue"
          subtitle={data?.startingPoint && data?.endingPoint ? `${data.startingPoint} → ${data.endingPoint}` : undefined}
          to="/student/applications"
        />

        {/* Bus In-Charge info card (custom, not a DashboardCard) */}
        <div className="card hover:shadow-md transition-shadow">
          <p className="text-sm font-medium text-gray-500 mb-2">Bus In-Charge</p>
          {data?.inchargeName
            ? <div className="flex items-center gap-3">
                <div className="flex h-16 w-16 shrink-0 items-center justify-center overflow-hidden rounded-full border bg-gray-50 text-center text-xs text-gray-400">
                  {inchargePhotoUrl
                    ? <img src={inchargePhotoUrl} alt={`${data.inchargeName} profile`} className="h-full w-full object-cover" />
                    : 'Photo'}
                </div>
                <div className="min-w-0 space-y-0.5">
                  <p className="truncate text-base font-bold text-gray-800">{data.inchargeName}</p>
                  <p className="truncate text-xs text-gray-600">{data.inchargeDesignation || '—'} · {data.inchargeBranch || '—'}</p>
                  <p className="text-xs text-blue-800">{data.inchargePhoneNumber || '—'}</p>
                </div>
              </div>
            : <p className="text-sm text-gray-500">In-Charge details will appear after a bus is assigned.</p>
          }
        </div>

        {/* Boarding Point */}
        <DashboardCard
          title="Boarding Point"
          value={data?.boardingPoint || 'N/A'}
          icon={MapPin}
          color="purple"
          to="/student/applications"
        />

        {/* Fee Status — clickable, navigates to /student/fees */}
        <div
          className="card hover:shadow-md transition-shadow cursor-pointer hover:ring-2 hover:ring-green-300"
          onClick={() => navigate('/student/fees')}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') navigate('/student/fees'); }}
          title="View fee details"
        >
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <p className="text-sm font-medium text-gray-500 mb-1">Fee Status</p>
              <div className="mt-1 mb-3">
                <StatusBadge status={data?.feeStatus || 'UNPAID'} />
              </div>
              {data?.totalFee != null && (
                <div className="space-y-1 text-sm">
                  <div className="flex items-center justify-between gap-4">
                    <span className="text-gray-500">Total Fee:</span>
                    <span className="font-semibold text-gray-800">₹{Number(data.totalFee).toLocaleString('en-IN')}</span>
                  </div>
                  <div className="flex items-center justify-between gap-4">
                    <span className="text-gray-500">Pending Fee:</span>
                    <span className={`font-semibold ${(data.totalFee - (data.paidAmount || 0)) > 0 ? 'text-red-600' : 'text-green-600'}`}>
                      ₹{Number(Math.max(0, data.totalFee - (data.paidAmount || 0))).toLocaleString('en-IN')}
                    </span>
                  </div>
                </div>
              )}
              <p className="text-xs text-green-600 mt-2">Click to view fees →</p>
            </div>
            <div className="p-3 rounded-lg bg-green-50 text-green-600 ml-3">
              <CreditCard size={24} />
            </div>
          </div>
        </div>

        {/* Pass Status — clickable, navigates to /student/pass */}
        <div
          className="card hover:shadow-md transition-shadow cursor-pointer hover:ring-2 hover:ring-yellow-300"
          onClick={() => navigate('/student/pass')}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') navigate('/student/pass'); }}
          title="View your bus pass"
        >
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 mb-1">Pass Status</p>
              <div className="mt-1">
                <StatusBadge status={data?.passStatus || 'INACTIVE'} />
              </div>
              <p className="text-xs text-yellow-600 mt-2">Click to view pass →</p>
            </div>
            <div className="p-3 rounded-lg bg-yellow-50 text-yellow-600">
              <Ticket size={24} />
            </div>
          </div>
        </div>

      </div>
    </div>
  );
};

export default StudentDashboardPage;
