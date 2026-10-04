import React, { useEffect, useState } from 'react';
import { Bus, ClipboardList, AlertCircle, ArrowLeftRight, BadgeCheck, CreditCard } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import DashboardCard from '../../components/common/DashboardCard';
import api from '../../utils/axios';
import LoadingSpinner from '../../components/common/LoadingSpinner';

const InchargeDashboardPage = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const res = await api.get('/incharge/dashboard');
        setStats(res.data.data || res.data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  if (loading) return <LoadingSpinner />;

  return (
    <div className="space-y-6">
      <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <h2 className="text-xl font-bold text-gray-800">Assigned Bus: {stats?.busNumber || 'N/A'}</h2>
        <p className="text-gray-500 mt-1">
          {stats?.startingPoint && stats?.endingPoint
            ? `${stats.startingPoint} → ${stats.endingPoint}`
            : 'Starting and ending points unavailable'}
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <DashboardCard
          title="Occupancy (Filled / Capacity)"
          value={`${stats?.totalStudents || 0} / ${stats?.totalCapacity || 0}`}
          icon={Bus}
          color="blue"
          to="/incharge/students"
        />
        <DashboardCard
          title="Pending Applications"
          value={stats?.pendingApplications || 0}
          icon={ClipboardList}
          color="yellow"
          to="/incharge/applications"
          searchParams={{ status: 'PENDING' }}
        />
        <DashboardCard
          title="Vacancy"
          value={stats?.vacancy || 0}
          icon={AlertCircle}
          color="purple"
        />
        <DashboardCard
          title="Pending Transfer Releases"
          value={stats?.pendingTransferReleases || 0}
          icon={ArrowLeftRight}
          color="yellow"
          to="/incharge/transfers"
          searchParams={{ status: 'TRANSFER_REQUESTED' }}
        />
        <DashboardCard
          title="Pending Transfer Acceptances"
          value={stats?.pendingTransferAcceptances || 0}
          icon={ArrowLeftRight}
          color="blue"
          to="/incharge/transfers"
          searchParams={{ status: 'OLD_INCHARGE_APPROVED' }}
        />
        <DashboardCard
          title="Active Passes"
          value={stats?.activePasses || 0}
          icon={BadgeCheck}
          color="green"
          to="/incharge/passes"
        />
        <DashboardCard
          title="Fee Collected"
          value={`₹${stats?.collectedFee || 0}`}
          icon={CreditCard}
          color="green"
          to="/incharge/fees"
        />
      </div>

      <div className="card">
        <h3 className="font-bold mb-3">Boarding Points (Travel Order)</h3>
        <div className="flex flex-wrap items-center gap-2">
          {[...(stats?.boardingPoints || [])]
            .sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0) || a.stationName.localeCompare(b.stationName))
            .map((p, index) => (
              <React.Fragment key={p.id}>
                <span className="rounded bg-blue-50 px-3 py-2 text-sm text-blue-800">
                  <strong>{index}</strong> · {p.stationName}
                </span>
                {index < (stats?.boardingPoints?.length || 0) - 1 && (
                  <span className="text-gray-400">→</span>
                )}
              </React.Fragment>
            ))}
        </div>
      </div>
    </div>
  );
};

export default InchargeDashboardPage;
