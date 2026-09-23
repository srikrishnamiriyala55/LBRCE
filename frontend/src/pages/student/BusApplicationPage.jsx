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
  const [studentDetails, setStudentDetails] = useState({
    rollNumber: '', name: '', email: '', phoneNumber: '', dob: '', gender: '', address: '',
    branch: '', year: '', semester: '', bloodGroup: '', parentName: '', parentPhoneNumber: ''
  });
  const [photo, setPhoto] = useState(null);
  const [photoPreview, setPhotoPreview] = useState('');
  const navigate = useNavigate();
  const { addToast } = useToast();

  useEffect(() => {
    Promise.all([api.get('/student/dashboard'), api.get('/student/buses'), api.get('/student/profile')])
      .then(([dashboardResponse, busesResponse, profileResponse]) => {
        const dashboard = dashboardResponse.data.data || dashboardResponse.data;
        setEligibility(dashboard);
        setBuses(dashboard.canApply ? busesResponse.data : []);
        const profile = profileResponse.data;
        setStudentDetails({
          rollNumber: profile.rollNumber || '', name: profile.name || '', email: profile.email || '',
          phoneNumber: profile.phoneNumber || '', dob: profile.dob || '', gender: profile.gender || '',
          address: profile.address || '', branch: profile.branch || '', year: profile.year || '',
          semester: profile.semester || '', bloodGroup: profile.bloodGroup || '', parentName: profile.parentName || '',
          parentPhoneNumber: profile.parentPhoneNumber || ''
        });
      })
      .catch((err) => addToast(err.response?.data?.message || 'Failed to check application eligibility', 'error'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => () => {
    if (photoPreview) URL.revokeObjectURL(photoPreview);
  }, [photoPreview]);

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
    if (!selectedBus || !selectedPoint || !photo) return;
    
    setSubmitting(true);
    try {
      const formData = new FormData();
      const { rollNumber, ...details } = studentDetails;
      formData.append('application', new Blob([JSON.stringify({
        busId: Number(selectedBus),
        boardingPointId: Number(selectedPoint),
        ...details,
        year: Number(studentDetails.year),
        semester: Number(studentDetails.semester)
      })], { type: 'application/json' }));
      formData.append('photo', photo);
      await api.post('/student/applications', formData);
      addToast('Application submitted successfully', 'success');
      navigate('/student/applications');
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to submit application', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePhotoChange = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      addToast('Select a JPEG or PNG photo', 'error');
      event.target.value = '';
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      addToast('Student photo must not exceed 2 MB', 'error');
      event.target.value = '';
      return;
    }
    if (photoPreview) URL.revokeObjectURL(photoPreview);
    setPhoto(file);
    setPhotoPreview(URL.createObjectURL(file));
  };

  const updateStudentDetails = (event) => {
    const { name, value } = event.target;
    setStudentDetails((current) => ({ ...current, [name]: value, ...(name === 'year' ? { semester: '' } : {}) }));
  };

  if (loading) return <LoadingSpinner />;

  if (!eligibility?.canApply) return (
    <div className="card max-w-4xl mx-auto">
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
        <section>
          <h3 className="text-base font-semibold text-gray-800 mb-3">Confirm Student Details</h3>
          <p className="text-sm text-gray-500 mb-4">Review these details carefully. They will update your student record and be used for transport administration.</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Student ID</label><input value={studentDetails.rollNumber} className="input-field bg-gray-100" readOnly /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Full Name *</label><input name="name" value={studentDetails.name} onChange={updateStudentDetails} className="input-field" maxLength="100" required /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Email *</label><input type="email" name="email" value={studentDetails.email} onChange={updateStudentDetails} className="input-field" maxLength="120" required /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Phone Number *</label><input type="tel" name="phoneNumber" value={studentDetails.phoneNumber} onChange={updateStudentDetails} className="input-field" pattern="[0-9]{10,15}" required /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Date of Birth *</label><input type="date" name="dob" value={studentDetails.dob} max={new Date().toISOString().split('T')[0]} onChange={updateStudentDetails} className="input-field" required /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Gender *</label><select name="gender" value={studentDetails.gender} onChange={updateStudentDetails} className="input-field" required><option value="">Select</option><option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option></select></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Branch *</label><input name="branch" value={studentDetails.branch} onChange={updateStudentDetails} className="input-field uppercase" maxLength="50" required /></div>
            <div className="grid grid-cols-2 gap-3">
              <div><label className="block text-sm font-medium text-gray-700 mb-1">Year *</label><select name="year" value={studentDetails.year} onChange={updateStudentDetails} className="input-field" required><option value="">Select</option>{[1,2,3,4].map(value => <option key={value} value={value}>{value}</option>)}</select></div>
              <div><label className="block text-sm font-medium text-gray-700 mb-1">Semester *</label><select name="semester" value={studentDetails.semester} onChange={updateStudentDetails} className="input-field" disabled={!studentDetails.year} required><option value="">Select</option>{studentDetails.year && [Number(studentDetails.year) * 2 - 1, Number(studentDetails.year) * 2].map(value => <option key={value} value={value}>{value}</option>)}</select></div>
            </div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Blood Group *</label><select name="bloodGroup" value={studentDetails.bloodGroup} onChange={updateStudentDetails} className="input-field" required><option value="">Select</option>{['A+','A-','B+','B-','AB+','AB-','O+','O-'].map(value => <option key={value}>{value}</option>)}</select></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Parent / Guardian Name *</label><input name="parentName" value={studentDetails.parentName} onChange={updateStudentDetails} className="input-field" maxLength="100" required /></div>
            <div><label className="block text-sm font-medium text-gray-700 mb-1">Parent Phone Number *</label><input type="tel" name="parentPhoneNumber" value={studentDetails.parentPhoneNumber} onChange={updateStudentDetails} className="input-field" pattern="[0-9]{10,15}" required /></div>
            <div className="sm:col-span-2"><label className="block text-sm font-medium text-gray-700 mb-1">Residential Address *</label><textarea name="address" value={studentDetails.address} onChange={updateStudentDetails} className="input-field min-h-24" maxLength="500" required /></div>
          </div>
        </section>

        <section className="border-t pt-6">
          <h3 className="text-base font-semibold text-gray-800 mb-3">Transport Selection</h3>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Select Bus</label>
          <select 
            className="input-field"
            value={selectedBus}
            onChange={handleBusChange}
            required
          >
            <option value="">-- Choose a Bus --</option>
            {buses.map(bus => (
              <option key={bus.id} value={bus.id}>
                {bus.busNumber} — {bus.startingPoint} to {bus.endingPoint} ({bus.availableSeats} seats available)
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
        </section>

        <div className="border-t pt-6">
          <label htmlFor="studentPhoto" className="block text-sm font-medium text-gray-700 mb-1">Recent Student Photo *</label>
          <input id="studentPhoto" type="file" accept="image/jpeg,image/png" onChange={handlePhotoChange} className="input-field" required />
          <p className="mt-1 text-xs text-gray-500">JPEG or PNG only, maximum 2 MB. This photo will appear on your digital bus pass.</p>
          {photoPreview && <img src={photoPreview} alt="Student preview" className="mt-3 h-32 w-28 rounded-lg border object-cover" />}
        </div>

        <button 
          type="submit" 
          disabled={!selectedBus || !selectedPoint || !photo || submitting}
          className="btn-primary w-full disabled:opacity-50"
        >
          {submitting ? 'Submitting...' : 'Submit Application'}
        </button>
      </form>
    </div>
  );
};

export default BusApplicationPage;
