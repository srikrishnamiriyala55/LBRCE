import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';

const ApplicationHistoryPage = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const { addToast } = useToast();

  useEffect(() => {
    fetchApplications();
  }, []);

  const fetchApplications = async () => {
    setLoading(true);
    try {
      const res = await api.get('/student/applications?unpaged=true');
      setData(res.data.content || res.data);
    } catch (err) {
      addToast('Failed to load applications', 'error');
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    { key: 'appliedAt', label: 'Date', render: (row) => {
      const value = row.appliedAt || row.applicationDate || row.createdAt;
      if (!value) return '—';
      const date = new Date(value);
      return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString();
    } },
    { key: 'busNumber', label: 'Bus Number', render: (row) => row.bus?.busNumber || row.busNumber },
    { key: 'boardingPoint', label: 'Boarding Point', render: (row) => row.boardingPoint?.stationName || row.boardingPointName },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
  ];

  return (
    <div className="card">
      <h2 className="text-xl font-bold text-gray-800 mb-6">Application History</h2>
      <DataTable columns={columns} data={data} loading={loading} searchable={false} />
    </div>
  );
};

export default ApplicationHistoryPage;
