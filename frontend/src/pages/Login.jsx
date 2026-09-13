import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import api from '../utils/axios';
import { ArrowLeft, Eye, EyeOff, Loader2, UserPlus } from 'lucide-react';

const Login = () => {
  const [rollNumber, setRollNumber] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isRegistering, setIsRegistering] = useState(false);
  const [success, setSuccess] = useState('');
  const [registration, setRegistration] = useState({
    rollNumber: '', name: '', email: '', phoneNumber: '', branch: '',
    year: '', semester: '', password: '', confirmPassword: ''
  });
  
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    try {
      const user = await login(rollNumber, password);
      if (user.role === 'STUDENT') navigate('/student/dashboard');
      else if (user.role === 'INCHARGE') navigate('/incharge/dashboard');
      else if (user.role === 'ADMIN') navigate('/admin/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Please check your credentials.');
    } finally {
      setIsLoading(false);
    }
  };

  const updateRegistration = (event) => {
    setRegistration((current) => ({ ...current, [event.target.name]: event.target.value }));
  };

  const openRegistration = () => {
    setError('');
    setSuccess('');
    setIsRegistering(true);
  };

  const handleRegistration = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    if (registration.password !== registration.confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    setIsLoading(true);
    try {
      const { confirmPassword, ...payload } = registration;
      payload.year = Number(payload.year);
      payload.semester = Number(payload.semester);
      await api.post('/auth/register/student', payload);
      setRollNumber(payload.rollNumber.trim().toUpperCase());
      setPassword('');
      setRegistration({ rollNumber: '', name: '', email: '', phoneNumber: '', branch: '', year: '', semester: '', password: '', confirmPassword: '' });
      setIsRegistering(false);
      setSuccess('Registration submitted successfully. An administrator must verify and activate your student account before you can sign in.');
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to create the account. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <div className={`${isRegistering ? 'max-w-2xl' : 'max-w-md'} w-full bg-white rounded-xl shadow-lg border border-gray-100 p-4 sm:p-8`}>
        <div className="flex flex-col items-center mb-8 text-center">
          <img src="/logo.jpg" alt="LBRCE Logo" className="h-20 w-20 mb-4" />
          <h1 className="text-xl font-bold text-blue-800 uppercase">
            Lakireddy Bali Reddy College of Engineering
          </h1>
          <p className="text-sm text-gray-500 mt-2 font-medium">Bus Transportation Management System</p>
          <p className="text-sm font-semibold text-gray-700 mt-3">{isRegistering ? 'Create Student Account' : 'Sign in to your account'}</p>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-50 border-l-4 border-red-500 text-red-700 text-sm">
            {error}
          </div>
        )}

        {success && <div className="mb-4 p-3 bg-green-50 border-l-4 border-green-500 text-green-700 text-sm">{success}</div>}

        {!isRegistering ? <form onSubmit={handleLogin} className="space-y-5">
          <div>
            <label htmlFor="loginId" className="block text-sm font-medium text-gray-700 mb-1">Roll Number / User ID</label>
            <input
              id="loginId"
              type="text"
              required
              className="input-field"
              placeholder="Enter your ID"
              value={rollNumber}
              onChange={(e) => setRollNumber(e.target.value)}
            />
          </div>
          
          <div>
            <div className="flex items-center justify-between mb-1">
              <label htmlFor="loginPassword" className="block text-sm font-medium text-gray-700">Password</label>
              <button type="button" className="text-xs text-blue-600 hover:text-blue-800" onClick={() => alert('Please contact administrator to reset password.')}>
                Forgot Password?
              </button>
            </div>
            <div className="relative">
              <input
                id="loginPassword"
                type={showPassword ? 'text' : 'password'}
                required
                className="input-field pr-10"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <button
                type="button"
                className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
                onClick={() => setShowPassword(!showPassword)}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full btn-primary py-3 flex justify-center items-center"
          >
            {isLoading ? <Loader2 className="animate-spin" size={20} /> : 'Sign In'}
          </button>
          <div className="relative py-1"><div className="absolute inset-0 flex items-center"><div className="w-full border-t border-gray-200" /></div><div className="relative flex justify-center"><span className="bg-white px-3 text-xs uppercase text-gray-400">New student</span></div></div>
          <button type="button" onClick={openRegistration} className="w-full btn-secondary py-3 flex justify-center items-center gap-2">
            <UserPlus size={18} /> Create Student Account
          </button>
        </form> : <form onSubmit={handleRegistration} className="space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div><label htmlFor="registerStudentId" className="block text-sm font-medium text-gray-700 mb-1">Student ID *</label><input id="registerStudentId" name="rollNumber" value={registration.rollNumber} onChange={updateRegistration} className="input-field uppercase" placeholder="e.g. 21761A0501" maxLength="30" required /></div>
            <div><label htmlFor="registerName" className="block text-sm font-medium text-gray-700 mb-1">Full Name *</label><input id="registerName" name="name" value={registration.name} onChange={updateRegistration} className="input-field" placeholder="Student full name" maxLength="100" required /></div>
            <div><label htmlFor="registerEmail" className="block text-sm font-medium text-gray-700 mb-1">Email *</label><input id="registerEmail" type="email" name="email" value={registration.email} onChange={updateRegistration} className="input-field" placeholder="student@lbrce.ac.in" maxLength="120" required /></div>
            <div><label htmlFor="registerPhone" className="block text-sm font-medium text-gray-700 mb-1">Phone Number *</label><input id="registerPhone" type="tel" name="phoneNumber" value={registration.phoneNumber} onChange={updateRegistration} className="input-field" placeholder="10-digit mobile number" minLength="10" maxLength="15" required /></div>
            <div><label htmlFor="registerBranch" className="block text-sm font-medium text-gray-700 mb-1">Branch *</label><input id="registerBranch" name="branch" value={registration.branch} onChange={updateRegistration} className="input-field uppercase" placeholder="e.g. CSE" maxLength="50" required /></div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div><label htmlFor="registerYear" className="block text-sm font-medium text-gray-700 mb-1">Year *</label><select id="registerYear" name="year" value={registration.year} onChange={updateRegistration} className="input-field" required><option value="">Select</option>{[1,2,3,4].map(value => <option key={value} value={value}>{value}</option>)}</select></div>
              <div><label htmlFor="registerSemester" className="block text-sm font-medium text-gray-700 mb-1">Semester *</label><select id="registerSemester" name="semester" value={registration.semester} onChange={updateRegistration} className="input-field" required><option value="">Select</option>{[1,2,3,4,5,6,7,8].map(value => <option key={value} value={value}>{value}</option>)}</select></div>
            </div>
            <div><label htmlFor="registerPassword" className="block text-sm font-medium text-gray-700 mb-1">Password *</label><input id="registerPassword" type={showPassword ? 'text' : 'password'} name="password" value={registration.password} onChange={updateRegistration} className="input-field" minLength="8" maxLength="72" autoComplete="new-password" required /></div>
            <div><label htmlFor="registerConfirmPassword" className="block text-sm font-medium text-gray-700 mb-1">Confirm Password *</label><input id="registerConfirmPassword" type={showPassword ? 'text' : 'password'} name="confirmPassword" value={registration.confirmPassword} onChange={updateRegistration} className="input-field" minLength="8" maxLength="72" autoComplete="new-password" required /></div>
          </div>
          <label className="flex items-center gap-2 text-sm text-gray-600"><input type="checkbox" checked={showPassword} onChange={() => setShowPassword(!showPassword)} /> Show passwords</label>
          <button type="submit" disabled={isLoading} className="w-full btn-primary py-3 flex justify-center items-center gap-2">{isLoading ? <Loader2 className="animate-spin" size={20} /> : <><UserPlus size={18} /> Create Account</>}</button>
          <button type="button" disabled={isLoading} onClick={() => { setIsRegistering(false); setError(''); }} className="w-full text-sm text-blue-700 hover:text-blue-900 flex justify-center items-center gap-2"><ArrowLeft size={16} /> Back to sign in</button>
        </form>}
      </div>
    </div>
  );
};

export default Login;
