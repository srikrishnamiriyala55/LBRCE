import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import { useToast } from '../../components/common/Toast';

const AuditLogsPage = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  const { addToast } = useToast();

  useEffect(() => {
    fetchLogs();
  }, []);

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/audit-logs?unpaged=true');
      setLogs(Array.isArray(res.data) ? res.data : (res.data?.content || []));
    } catch (err) {
      addToast('Failed to load audit logs', 'error');
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    { key: 'timestamp', label: 'Timestamp', render: (row) => new Date(row.timestamp).toLocaleString() },
    { key: 'userId', label: 'User / Actor', render: (row) => <span className="font-semibold text-gray-800">{row.userId || 'SYSTEM'}</span> },
    {
      key: 'userRole',
      label: 'Role',
      render: (row) => (
        <span className="text-xs bg-blue-100 text-blue-800 px-2 py-0.5 rounded font-medium">
          {row.userRole || 'ADMIN'}
        </span>
      )
    },
    { key: 'action', label: 'Action Perfomed', render: (row) => <span className="font-medium text-gray-900">{row.action}</span> },
    { key: 'entityType', label: 'Target Entity', render: (row) => `${row.entityType || '—'} #${row.entityId || ''}` },
    { key: 'ipAddress', label: 'IP Address', render: (row) => row.ipAddress || '—' }
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-gray-800">System Audit Trail</h2>
        <p className="text-gray-500 text-sm">Security and administrative activity logs for compliance and monitoring</p>
      </div>

      <div className="card">
        <DataTable columns={columns} data={logs} loading={loading} searchable={false} />
      </div>
    </div>
  );
};

export default AuditLogsPage;
