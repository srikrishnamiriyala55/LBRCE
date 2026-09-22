import React, { useEffect, useState } from 'react';
import { Bus, MapPin, CreditCard, Ticket } from 'lucide-react';
import { Link } from 'react-router-dom';
import DashboardCard from '../../components/common/DashboardCard';
import StatusBadge from '../../components/common/StatusBadge';
import api from '../../utils/axios';
import { useAuth } from '../../contexts/AuthContext';
import LoadingSpinner from '../../components/common/LoadingSpinner';

const StudentDashboardPage = () => {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const res = await api.get('/student/dashboard');
        setData(res.data.data || res.data);
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
      <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <h2 className="text-2xl font-bold text-gray-800">Welcome back, {user?.name}!</h2>
        <p className="text-gray-500 mt-1">Roll Number: {user?.rollNumber || data?.rollNumber}</p>
      </div>

      <div className="card">
        <h3 className="text-lg font-semibold text-gray-800 mb-3">Available Actions</h3>
        <div className="flex flex-wrap gap-3">
          {data?.canApply && <Link to="/student/apply" className="btn-primary">Apply for Bus</Link>}
          {data?.canTransfer && <Link to="/student/transfers" className="btn-primary">Request Bus Transfer</Link>}
          {data?.canComplain && <Link to="/student/complaints" className="btn-secondary">Raise Complaint</Link>}
          {data?.hasActiveApplication && data?.transportationStatus !== 'ALLOCATED' && <Link to="/student/applications" className="btn-secondary">Track Pending Application</Link>}
          {!data?.canApply && !data?.canTransfer && !data?.canComplain && !data?.hasActiveApplication && <p className="text-sm text-gray-500">No transportation action is currently available.</p>}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <DashboardCard 
          title="Bus Number" 
          value={data?.busNumber || 'Not Assigned'} 
          icon={Bus} 
          color="blue" 
          subtitle={data?.startingPoint && data?.endingPoint ? `${data.startingPoint} → ${data.endingPoint}` : undefined}
        />
        <DashboardCard 
          title="Boarding Point" 
          value={data?.boardingPoint || 'N/A'} 
          icon={MapPin} 
          color="purple" 
        />
        <div className="card hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 mb-1">Fee Status</p>
              <div className="mt-1">
                <StatusBadge status={data?.feeStatus || 'UNPAID'} />
              </div>
            </div>
            <div className="p-3 rounded-lg bg-green-50 text-green-600">
              <CreditCard size={24} />
            </div>
          </div>
        </div>
        <div className="card hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 mb-1">Pass Status</p>
              <div className="mt-1">
                <StatusBadge status={data?.passStatus || 'INACTIVE'} />
              </div>
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
