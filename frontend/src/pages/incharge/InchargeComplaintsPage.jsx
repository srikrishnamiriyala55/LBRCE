import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import StatusBadge from '../../components/common/StatusBadge';
import Modal from '../../components/common/Modal';
import { useToast } from '../../components/common/Toast';

const InchargeComplaintsPage = () => {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [modalOpen, setModalOpen] = useState(false);
  const [currentComplaint, setCurrentComplaint] = useState(null);
  const [status, setStatus] = useState('');
  const [response, setResponse] = useState('');
  const { addToast } = useToast();

  useEffect(() => {
    fetchComplaints();
  }, [page]);

  const fetchComplaints = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/incharge/complaints?page=${page}&size=10`);
      setComplaints(res.data.content || res.data);
      setTotalPages(res.data.totalPages || 1);
    } catch (err) {
      addToast('Failed to load complaints', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    try {
      await api.put(`/incharge/complaints/${currentComplaint.id}/status`, { status, response });
      addToast('Complaint updated successfully', 'success');
      setModalOpen(false);
      fetchComplaints();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to update complaint', 'error');
    }
  };

  const columns = [
    { key: 'studentName', label: 'Student', render: (row) => <div><div className="font-medium">{row.studentName || '—'}</div><div className="text-xs text-gray-500">{row.rollNumber || ''}</div></div> },
    { key: 'subject', label: 'Subject' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> },
    { key: 'actions', label: 'Actions', render: (row) => (
      row.status !== 'CLOSED' && <button onClick={() => { setCurrentComplaint(row); setStatus(row.status); setResponse(row.response || ''); setModalOpen(true); }} className="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded">
        Update
      </button>
    )}
  ];

  return (
    <div className="card">
      <h2 className="text-xl font-bold mb-6">Student Complaints</h2>
      <DataTable columns={columns} data={complaints} loading={loading} />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />

      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title="Update Complaint">
        <form onSubmit={handleUpdate} className="space-y-4">
          <div>
            <p className="text-sm font-bold text-gray-700">Subject: {currentComplaint?.subject}</p>
            <p className="text-sm text-gray-600 mt-1">{currentComplaint?.description}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
            <select className="input-field" value={status} onChange={(e) => setStatus(e.target.value)}>
              {currentComplaint?.status === 'OPEN' && <option value="OPEN">Open</option>}
              {['OPEN','IN_PROGRESS'].includes(currentComplaint?.status) && <option value="IN_PROGRESS">In Progress</option>}
              {['OPEN','IN_PROGRESS','RESOLVED'].includes(currentComplaint?.status) && <option value="RESOLVED">Resolved</option>}
              <option value="CLOSED">Closed</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Response</label>
            <textarea className="input-field" rows="3" maxLength="1000" required={status==='RESOLVED'||status==='CLOSED'} value={response} onChange={(e) => setResponse(e.target.value)}></textarea>
          </div>
          <button type="submit" className="btn-primary w-full">Update Status</button>
        </form>
      </Modal>
    </div>
  );
};

export default InchargeComplaintsPage;
