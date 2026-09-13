import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import StatusBadge from '../../components/common/StatusBadge';
import { useToast } from '../../components/common/Toast';

const FeeManagementPage = () => {
  const [fees, setFees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const { addToast } = useToast();

  useEffect(() => {
    fetchFees();
  }, [page]);

  const fetchFees = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/admin/fees?page=${page}&size=10`);
      setFees(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
    } catch (err) {
      addToast('Failed to load fees records', 'error');
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    { key: 'student', label: 'Student', render: (row) => (
      <div>
        <span className="font-bold text-blue-900">{row.rollNumber || 'N/A'}</span>
        {row.studentName && <span className="text-xs text-gray-500 block">{row.studentName}</span>}
      </div>
    )},
    { key: 'academicYear', label: 'Academic Year', render: (row) => <span className="font-semibold">{row.academicYear || '—'}</span> },
    { key: 'totalAmount', label: 'Total Fee', render: (row) => `₹${row.totalAmount}` },
    { key: 'paidAmount', label: 'Paid Amount', render: (row) => <span className="text-green-700 font-medium">₹${row.paidAmount || 0}</span> },
    { key: 'remainingAmount', label: 'Remaining', render: (row) => <span className="text-red-600 font-medium">₹${row.remainingAmount || 0}</span> },
    {
      key: 'percentage',
      label: 'Progress',
      render: (row) => (
        <div className="w-36">
          <div className="flex justify-between text-xs text-gray-500 mb-1">
            <span>{row.paidPercentage?.toFixed(0) || 0}%</span>
            <span>{row.passEligible ? 'Pass Eligible' : '< 50%'}</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div
              className={`h-2 rounded-full ${row.paidPercentage >= 50 ? 'bg-green-600' : 'bg-blue-600'}`}
              style={{ width: `${Math.min(100, row.paidPercentage || 0)}%` }}
            ></div>
          </div>
        </div>
      )
    },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> }
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-gray-800">Fee Collection & Audit</h2>
        <p className="text-gray-500 text-sm">Monitor student transportation fees, payment percentages, and pass eligibility</p>
      </div>

      <div className="card">
        <DataTable columns={columns} data={fees} loading={loading} />
        <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
      </div>
    </div>
  );
};

export default FeeManagementPage;
