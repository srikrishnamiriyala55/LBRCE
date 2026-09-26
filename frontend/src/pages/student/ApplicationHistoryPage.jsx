import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';

const ApplicationHistoryPage = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const { addToast } = useToast();

  useEffect(() => {
    fetchApplications();
  }, [page]);

  const fetchApplications = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/student/applications?page=${page}&size=10`);
      setData(res.data.content || res.data);
      setTotalPages(res.data.totalPages || 1);
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
      <DataTable columns={columns} data={data} loading={loading} />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  );
};

export default ApplicationHistoryPage;
