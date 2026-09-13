import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../utils/axios';
import { useToast } from '../../components/common/Toast';
import LoadingSpinner from '../../components/common/LoadingSpinner';

const BusApplicationPage = () => {
  const [buses, setBuses] = useState([]);
  const [boardingPoints, setBoardingPoints] = useState([]);
  const [selectedBus, setSelectedBus] = useState('');
  const [selectedPoint, setSelectedPoint] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [eligibility, setEligibility] = useState(null);
  const navigate = useNavigate();
  const { addToast } = useToast();

  useEffect(() => {
    Promise.all([api.get('/student/dashboard'), api.get('/student/buses')])
      .then(([dashboardResponse, busesResponse]) => {
        const dashboard = dashboardResponse.data.data || dashboardResponse.data;
        setEligibility(dashboard);
        setBuses(dashboard.canApply ? busesResponse.data : []);
      })
      .catch((err) => addToast(err.response?.data?.message || 'Failed to check application eligibility', 'error'))
      .finally(() => setLoading(false));
  }, []);

  const fetchBoardingPoints = async (busId) => {
    try {
      const res = await api.get(`/student/buses/${busId}/boarding-points`);
      setBoardingPoints(res.data);
    } catch (err) {
      addToast('Failed to load boarding points', 'error');
    }
  };

  const handleBusChange = (e) => {
    const busId = e.target.value;
    setSelectedBus(busId);
    setSelectedPoint('');
    if (busId) {
      fetchBoardingPoints(busId);
    } else {
      setBoardingPoints([]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedBus || !selectedPoint) return;
    
    setSubmitting(true);
    try {
      await api.post('/student/applications', {
        busId: selectedBus,
        boardingPointId: selectedPoint
      });
      addToast('Application submitted successfully', 'success');
      navigate('/student/applications');
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to submit application', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  if (!eligibility?.canApply) return (
    <div className="card max-w-2xl mx-auto">
      <h2 className="text-xl font-bold text-gray-800 mb-3">Bus Application Unavailable</h2>
      <p className="text-gray-600">{eligibility?.transportationStatus === 'ALLOCATED'
        ? 'A bus is already allocated to you. Use the Transfers page if you need to change your bus.'
        : eligibility?.hasActiveApplication
          ? 'Your application is already pending or approved. Track it from the Applications page.'
          : 'Bus applications are not currently available. Please contact the transport office.'}</p>
      <button type="button" onClick={() => navigate('/student/applications')} className="btn-secondary mt-5">View Applications</button>
    </div>
  );

  return (
    <div className="card max-w-2xl mx-auto">
      <h2 className="text-xl font-bold text-gray-800 mb-6">Apply for Bus Service</h2>
      
      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Select Bus & Route</label>
          <select 
            className="input-field"
            value={selectedBus}
            onChange={handleBusChange}
            required
          >
            <option value="">-- Choose a Bus --</option>
            {buses.map(bus => (
              <option key={bus.id} value={bus.id}>
                {bus.busNumber} - {bus.routeName} ({bus.availableSeats} seats available)
              </option>
            ))}
          </select>
        </div>

        {selectedBus && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Select Boarding Point</label>
            <select 
              className="input-field"
              value={selectedPoint}
              onChange={(e) => setSelectedPoint(e.target.value)}
              required
            >
              <option value="">-- Choose a Boarding Point --</option>
              {boardingPoints.map(point => (
                <option key={point.id} value={point.id}>
                  {point.stationName} (Fee: ₹{point.feeAmount})
                </option>
              ))}
            </select>
          </div>
        )}

        <button 
          type="submit" 
          disabled={!selectedBus || !selectedPoint || submitting}
          className="btn-primary w-full disabled:opacity-50"
        >
          {submitting ? 'Submitting...' : 'Submit Application'}
        </button>
      </form>
    </div>
  );
};

export default BusApplicationPage;
