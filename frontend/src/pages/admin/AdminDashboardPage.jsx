import React, { useEffect, useState } from 'react';
import { Users, Bus, UserCheck, ClipboardList, CreditCard, Ticket } from 'lucide-react';
import DashboardCard from '../../components/common/DashboardCard';
import api from '../../utils/axios';
import LoadingSpinner from '../../components/common/LoadingSpinner';

const AdminDashboardPage = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const res = await api.get('/admin/dashboard');
        setStats(res.data.data || res.data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, []);

  if (loading) return <LoadingSpinner />;

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-800">System Overview</h2>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        <DashboardCard title="Total Students" value={stats?.totalStudents || 0} icon={Users} color="blue" />
        <DashboardCard title="Total Buses" value={stats?.totalBuses || 0} icon={Bus} color="purple" subtitle={`${stats?.activeBuses || 0} Active`} />
        <DashboardCard title="Total Incharges" value={stats?.totalIncharges || 0} icon={UserCheck} color="green" />
        <DashboardCard title="Pending Applications" value={stats?.pendingApplications || 0} icon={ClipboardList} color="yellow" />
        <DashboardCard title="Total Capacity" value={stats?.totalCapacity || 0} icon={Bus} color="blue" subtitle={`${stats?.occupiedSeats || 0} Occupied`} />
        <DashboardCard title="Passes Generated" value={stats?.passesGenerated || 0} icon={Ticket} color="purple" />
        <DashboardCard title="Total Fee Collection" value={`₹${stats?.totalFeeCollection || 0}`} icon={CreditCard} color="green" />
        <DashboardCard title="Vacancy" value={stats?.vacancy || 0} icon={Users} color="yellow" />
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="card"><h3 className="font-semibold text-blue-900 mb-2">Students</h3><p>With transport: <b>{stats?.studentsWithTransportation||0}</b></p><p>Without transport: <b>{stats?.studentsWithoutTransportation||0}</b></p><p>Rejected applications: <b>{stats?.rejectedApplications||0}</b></p></div>
        <div className="card"><h3 className="font-semibold text-blue-900 mb-2">Transfers</h3><p>Waiting old In-Charge: <b>{stats?.transferRequested||0}</b></p><p>Waiting new In-Charge: <b>{stats?.waitingNewIncharge||0}</b></p><p>Completed: <b>{stats?.completedTransfers||0}</b></p></div>
        <div className="card"><h3 className="font-semibold text-blue-900 mb-2">Fees & Passes</h3><p>Expected: <b>₹{stats?.totalExpectedFees||0}</b></p><p>Pending: <b>₹{stats?.pendingFeeAmount||0}</b></p><p>Pass eligible: <b>{stats?.passEligibleStudents||0}</b></p></div>
      </div>
    </div>
  );
};

export default AdminDashboardPage;
