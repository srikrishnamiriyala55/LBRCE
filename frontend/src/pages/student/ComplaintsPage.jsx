import React, { useState, useEffect } from 'react';
import api from '../../utils/axios';
import { useToast } from '../../components/common/Toast';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import StatusBadge from '../../components/common/StatusBadge';

const ComplaintsPage = () => {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [formData, setFormData] = useState({ category: 'BUS_CONDITION', subject: '', description: '' });
  const [eligibility, setEligibility] = useState(null);
  const { addToast } = useToast();

  useEffect(() => {
    fetchComplaints();
  }, [page]);

  useEffect(() => {
    api.get('/student/dashboard').then(r => setEligibility(r.data.data || r.data))
      .catch(() => setEligibility({ canComplain: false }));
  }, []);

  const fetchComplaints = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/student/complaints?page=${page}&size=10`);
      setComplaints(res.data.content || res.data);
      setTotalPages(res.data.totalPages || 1);
    } catch (err) {
      addToast('Failed to load complaints', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.post('/student/complaints', formData);
      addToast('Complaint submitted successfully', 'success');
      setFormData({ category: 'BUS_CONDITION', subject: '', description: '' });
      fetchComplaints();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to submit complaint', 'error');
    }
  };

  const columns = [
    { key: 'date', label: 'Date', render: (row) => new Date(row.createdAt).toLocaleDateString() },
    { key: 'category', label: 'Category', render: (row) => row.category.replace(/_/g, ' ') },
    { key: 'subject', label: 'Subject' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge status={row.status} /> }
  ];

  return (
    <div className="space-y-6">
      <div className="card">
        <h2 className="text-xl font-bold mb-4">Register a Complaint</h2>
        {!eligibility?.canComplain ? <p className="text-sm text-amber-700">You can submit a complaint only after an in-charge approves your application and a bus is allocated to you.</p> : <form onSubmit={handleSubmit} className="space-y-4 max-w-2xl">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
            <select className="input-field" value={formData.category} onChange={(e) => setFormData({...formData, category: e.target.value})}>
              <option value="BUS_CONDITION">Bus Condition</option>
              <option value="DRIVER_BEHAVIOR">Driver Behavior</option>
              <option value="TIMING_ISSUE">Timing Issue</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Subject</label>
            <input required type="text" maxLength="100" className="input-field" value={formData.subject} onChange={(e) => setFormData({...formData, subject: e.target.value})} />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <textarea required rows="4" maxLength="1000" className="input-field" value={formData.description} onChange={(e) => setFormData({...formData, description: e.target.value})}></textarea>
          </div>
          <button type="submit" className="btn-primary">Submit Complaint</button>
        </form>}
      </div>

      <div className="card">
        <h3 className="text-lg font-bold mb-4">My Complaints</h3>
        <DataTable columns={columns} data={complaints} loading={loading} />
        <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
      </div>
    </div>
  );
};

export default ComplaintsPage;
