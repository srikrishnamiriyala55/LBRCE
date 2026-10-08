import React, { useEffect, useState } from 'react';
import { Users, Bus, UserCheck, ClipboardList, CreditCard, Ticket } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import DashboardCard from '../../components/common/DashboardCard';
import api from '../../utils/axios';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';

const StatChip = ({ label, value, onClick }) => (
  <span
    onClick={onClick}
    className="cursor-pointer rounded-md bg-blue-50 px-2 py-0.5 text-sm text-blue-800 hover:bg-blue-100 hover:underline transition-colors"
    role="button"
    tabIndex={0}
    onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') onClick(); }}
    title={`View ${label}`}
  >
    {label}: <strong>{value || 0}</strong>
  </span>
);

const AdminDashboardPage = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const { addToast } = useToast();

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const res = await api.get('/admin/dashboard');
        const payload = res.data?.data?.data ?? res.data?.data ?? res.data;
        if (!payload || typeof payload !== 'object') throw new Error('Invalid dashboard response');
        setStats(payload);
      } catch (err) {
        console.error(err);
        addToast(err.response?.data?.message || 'Failed to load dashboard details', 'error');
        setStats({});
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, [addToast]);

  if (loading) return <LoadingSpinner />;

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-gray-800">System Overview</h2>
        <p className="text-sm text-gray-500">Operational values are for {stats?.activeAcademicYear || 'the active academic year'}; registered students, buses and In-Charges are organization totals.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        <DashboardCard
          title="Total Students"
          value={stats?.totalStudents || 0}
          icon={Users}
          color="blue"
          to="/admin/students"
        />
        <DashboardCard
          title="Total Buses"
          value={stats?.totalBuses || 0}
          icon={Bus}
          color="purple"
          subtitle={`${stats?.activeBuses || 0} Active`}
          to="/admin/buses"
        />
        <DashboardCard
          title="Total Incharges"
          value={stats?.totalIncharges || 0}
          icon={UserCheck}
          color="green"
          to="/admin/incharges"
        />
        <DashboardCard
          title="Pending Applications"
          value={stats?.pendingApplications || 0}
          icon={ClipboardList}
          color="yellow"
          to="/admin/applications"
          searchParams={{ status: 'PENDING' }}
        />
        <DashboardCard
          title="Total Capacity"
          value={stats?.totalCapacity || 0}
          icon={Bus}
          color="blue"
          subtitle={`${stats?.occupiedSeats || 0} Occupied`}
          to="/admin/buses"
        />
        <DashboardCard
          title="Passes Generated"
          value={stats?.passesGenerated || 0}
          icon={Ticket}
          color="purple"
          to="/admin/passes"
        />
        <DashboardCard
          title="Total Fee Collection"
          value={`₹${stats?.totalFeeCollection || 0}`}
          icon={CreditCard}
          color="green"
          to="/admin/fees"
        />
        <DashboardCard
          title="Vacancy"
          value={stats?.vacancy || 0}
          icon={Users}
          color="yellow"
          to="/admin/buses"
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        {/* Students summary */}
        <div className="card space-y-2">
          <h3 className="font-semibold text-blue-900 mb-2">Students</h3>
          <div className="flex flex-wrap gap-2">
            <StatChip
              label="With transport"
              value={stats?.studentsWithTransportation || 0}
              onClick={() => navigate('/admin/students')}
            />
            <StatChip
              label="Without transport"
              value={stats?.studentsWithoutTransportation || 0}
              onClick={() => navigate('/admin/students')}
            />
            <StatChip
              label="Rejected applications"
              value={stats?.rejectedApplications || 0}
              onClick={() => navigate('/admin/applications?status=REJECTED')}
            />
          </div>
        </div>

        {/* Transfers summary */}
        <div className="card space-y-2">
          <h3 className="font-semibold text-blue-900 mb-2">Transfers</h3>
          <div className="flex flex-wrap gap-2">
            <StatChip
              label="Waiting old In-Charge"
              value={stats?.transferRequested || 0}
              onClick={() => navigate('/admin/transfers?status=TRANSFER_REQUESTED')}
            />
            <StatChip
              label="Waiting new In-Charge"
              value={stats?.waitingNewIncharge || 0}
              onClick={() => navigate('/admin/transfers?status=OLD_INCHARGE_APPROVED')}
            />
            <StatChip
              label="Completed"
              value={stats?.completedTransfers || 0}
              onClick={() => navigate('/admin/transfers?status=TRANSFER_COMPLETED')}
            />
          </div>
        </div>

        {/* Fees & Passes summary */}
        <div className="card space-y-2">
          <h3 className="font-semibold text-blue-900 mb-2">Fees &amp; Passes</h3>
          <div className="flex flex-wrap gap-2">
            <StatChip
              label="Expected"
              value={`₹${stats?.totalExpectedFees || 0}`}
              onClick={() => navigate('/admin/fees')}
            />
            <StatChip
              label="Pending"
              value={`₹${stats?.pendingFeeAmount || 0}`}
              onClick={() => navigate('/admin/fees')}
            />
            <StatChip
              label="Pass eligible"
              value={stats?.passEligibleStudents || 0}
              onClick={() => navigate('/admin/passes')}
            />
          </div>
        </div>

        <div className="card space-y-2">
          <h3 className="font-semibold text-blue-900 mb-2">All Academic Years</h3>
          <div className="flex flex-wrap gap-2">
            <StatChip label="Applications" value={stats?.historicalApplications || 0} onClick={() => navigate('/admin/applications')} />
            <StatChip label="Allocations" value={stats?.historicalAllocations || 0} onClick={() => navigate('/admin/students')} />
            <StatChip label="Collected" value={`₹${stats?.historicalFeeCollection || 0}`} onClick={() => navigate('/admin/fees')} />
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboardPage;
