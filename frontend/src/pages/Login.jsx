import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import api from '../utils/axios';
import { ArrowLeft, Eye, EyeOff, Loader2, UserPlus } from 'lucide-react';
import { DEPARTMENTS, PASSWORD_HELP, PASSWORD_PATTERN, normalizePhone, semestersForYear, yearsForDepartment } from '../utils/academicOptions';

const Login = () => {
  const [rollNumber, setRollNumber] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isRegistering, setIsRegistering] = useState(false);
  const [success, setSuccess] = useState('');
  const [rollNumberStatus, setRollNumberStatus] = useState('idle');
  const [photo, setPhoto] = useState(null);
  const [registrationOtp, setRegistrationOtp] = useState('');
  const [registrationOtpSent, setRegistrationOtpSent] = useState(false);
  const [registrationToken, setRegistrationToken] = useState('');
  const [loginChallenge, setLoginChallenge] = useState(null);
  const [loginOtp, setLoginOtp] = useState('');
  const [registration, setRegistration] = useState({
    rollNumber: '', name: '', email: '', phoneNumber: '', branch: '',
    year: '', semester: '', dob: '', gender: '', address: '', bloodGroup: '',
    parentName: '', parentPhoneNumber: '', password: '', confirmPassword: ''
  });
  
  const { login, completeOtpLogin } = useAuth();
  const navigate = useNavigate();

  const navigateForRole = (user) => {
    // After a 401, axios.js saves the current URL so we can redirect back after login
    const savedRedirect = sessionStorage.getItem('redirectAfterLogin');
    if (savedRedirect) {
      sessionStorage.removeItem('redirectAfterLogin');
      navigate(savedRedirect);
      return;
    }
    if (user.role === 'STUDENT') navigate('/student/dashboard');
    else if (user.role === 'INCHARGE') navigate('/incharge/dashboard');
    else if (user.role === 'ADMIN') navigate('/admin/dashboard');
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    try {
      const result = await login(rollNumber, password);
      if (result.otpRequired) { setLoginChallenge(result); setSuccess('A 6-digit verification code was sent to your registered email.'); }
      else navigateForRole(result);
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Please check your credentials.');
    } finally {
      setIsLoading(false);
    }
  };

  const verifyLoginOtp = async (event) => { event.preventDefault(); setError(''); setIsLoading(true); try { const user=await completeOtpLogin(loginChallenge.otpChallengeToken, loginOtp); navigateForRole(user); } catch(err) { setError(err.response?.data?.message || 'Invalid or expired OTP.'); } finally { setIsLoading(false); } };
  const resendLoginOtp = async () => { setError(''); setIsLoading(true); try { await api.post('/auth/login/resend-otp', { challengeToken: loginChallenge.otpChallengeToken }); setLoginOtp(''); setSuccess('A new OTP was sent. It is valid for 5 minutes.'); } catch(err) { setError(err.response?.data?.message || 'Unable to resend OTP.'); } finally { setIsLoading(false); } };

  const sendRegistrationOtp = async () => { setError(''); if (!registration.email) { setError('Enter your email before requesting OTP.'); return; } setIsLoading(true); try { await api.post('/auth/registration-otp/send', { email: registration.email }); setRegistrationOtp(''); setRegistrationOtpSent(true); setSuccess('OTP sent to your email. It is valid for 5 minutes.'); } catch(err) { setError(err.response?.data?.message || 'Unable to send OTP.'); } finally { setIsLoading(false); } };
  const verifyRegistrationOtp = async () => { setError(''); setIsLoading(true); try { const result=await api.post('/auth/registration-otp/verify', { email: registration.email, otp: registrationOtp }); setRegistrationToken(result.data.verificationToken); setSuccess('Email verified. You can create your account now.'); } catch(err) { setError(err.response?.data?.message || 'Invalid or expired OTP.'); } finally { setIsLoading(false); } };

  const updateRegistration = (event) => {
    const { name, value } = event.target;
    const nextValue = ['phoneNumber', 'parentPhoneNumber'].includes(name) ? normalizePhone(value) : name === 'rollNumber' ? value.toUpperCase() : value;
    if (name === 'email') { setRegistrationOtp(''); setRegistrationOtpSent(false); setRegistrationToken(''); }
    setRegistration((current) => ({ ...current, [name]: nextValue,
      ...(name === 'year' ? { semester: '' } : {}),
      ...(name === 'branch' ? { year: '', semester: '' } : {}) }));
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
    if (!registrationToken || !photo) { setError('Verify your email OTP and upload a student photo before creating the account.'); return; }
    setIsLoading(true);
    try {
      setRollNumberStatus('checking');
      const candidate = registration.rollNumber.trim().toUpperCase();
      const availability = await api.get('/auth/register/student/roll-number-exists', { params: { rollNumber: candidate } });
      if (availability.data.exists) {
        setRollNumberStatus('exists');
        setError(`User with roll number ${candidate} already exists.`);
        return;
      }
      setRollNumberStatus('available');
      const { confirmPassword, ...payload } = registration;
      payload.year = Number(payload.year);
      payload.semester = Number(payload.semester);
      payload.emailVerificationToken = registrationToken;
      const formData = new FormData(); formData.append('student', new Blob([JSON.stringify(payload)], { type: 'application/json' })); formData.append('photo', photo);
      await api.post('/auth/register/student', formData);
      const studentId = registration.rollNumber.trim().toUpperCase();
      setRollNumber(studentId);
      setPassword('');
      setRegistration({ rollNumber: '', name: '', email: '', phoneNumber: '', branch: '', year: '', semester: '', dob: '', gender: '', address: '', bloodGroup: '', parentName: '', parentPhoneNumber: '', password: '', confirmPassword: '' }); setPhoto(null); setRegistrationOtp(''); setRegistrationOtpSent(false); setRegistrationToken('');
      setIsRegistering(false);
      setSuccess('Account created successfully. You can sign in now.');
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to create the account. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <div className={`${isRegistering ? 'max-w-4xl' : 'max-w-md'} w-full bg-white rounded-xl shadow-lg border border-gray-100 p-4 sm:p-8`}>
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

        {loginChallenge ? <form onSubmit={verifyLoginOtp} className="space-y-5"><p className="text-sm text-gray-600">Verify the code sent to <strong>{loginChallenge.user?.email}</strong>. It is valid for 5 minutes.</p><input required inputMode="numeric" pattern="[0-9]{6}" maxLength="6" className="input-field" placeholder="Enter 6-digit OTP" value={loginOtp} onChange={e=>setLoginOtp(e.target.value.replace(/\D/g,''))}/><button type="submit" disabled={isLoading} className="w-full btn-primary py-3">{isLoading?'Verifying...':'Verify and Sign In'}</button><button type="button" disabled={isLoading} onClick={resendLoginOtp} className="w-full text-sm text-blue-700 hover:text-blue-900">Resend OTP</button><button type="button" className="w-full text-sm text-blue-700" onClick={()=>{setLoginChallenge(null);setLoginOtp('');}}>Back to sign in</button></form> : !isRegistering ? <form onSubmit={handleLogin} className="space-y-5">
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
            <div><label htmlFor="registerStudentId" className="block text-sm font-medium text-gray-700 mb-1">Student ID *</label><input id="registerStudentId" name="rollNumber" value={registration.rollNumber} onChange={updateRegistration} className={`input-field uppercase ${rollNumberStatus === 'exists' ? 'border-red-500 focus:ring-red-500' : ''}`} placeholder="e.g. 21761A0501" maxLength="30" aria-describedby="rollNumberStatus" required />{rollNumberStatus === 'exists' && <span id="rollNumberStatus" className="mt-1 block text-xs text-red-600">User with roll number {registration.rollNumber.trim().toUpperCase()} already exists.</span>}</div>
            <div><label htmlFor="registerName" className="block text-sm font-medium text-gray-700 mb-1">Full Name *</label><input id="registerName" name="name" value={registration.name} onChange={updateRegistration} className="input-field" placeholder="Student full name" maxLength="100" required /></div>
            <div><label htmlFor="registerEmail" className="block text-sm font-medium text-gray-700 mb-1">Email *</label><div className="relative"><input id="registerEmail" type="email" name="email" value={registration.email} onChange={updateRegistration} className="input-field pr-20" placeholder="student@lbrce.ac.in" maxLength="120" required disabled={Boolean(registrationToken)} /><button type="button" onClick={sendRegistrationOtp} disabled={isLoading || Boolean(registrationToken)} className="absolute inset-y-1 right-1 rounded-md bg-blue-700 px-3 text-xs font-medium text-white disabled:opacity-50">Verify</button></div>{!registrationToken && registrationOtpSent && <div className="mt-2 space-y-2"><input inputMode="numeric" maxLength="6" value={registrationOtp} onChange={e=>setRegistrationOtp(e.target.value.replace(/\D/g,''))} className="input-field" placeholder="Enter 6-digit OTP" /><div className="flex items-center gap-3 text-xs"><button type="button" onClick={verifyRegistrationOtp} disabled={isLoading || registrationOtp.length!==6} className="text-blue-700 font-medium disabled:opacity-50">Verify OTP</button><button type="button" onClick={sendRegistrationOtp} disabled={isLoading} className="text-blue-700 underline disabled:opacity-50">Resend OTP</button><span className="text-gray-500">Valid for 5 minutes</span></div></div>}{registrationToken&&<p className="mt-1 text-xs text-green-700">Email verified.</p>}</div>
            <div><label htmlFor="registerPhone" className="block text-sm font-medium text-gray-700 mb-1">Phone Number *</label><input id="registerPhone" type="tel" inputMode="numeric" name="phoneNumber" value={registration.phoneNumber} onChange={updateRegistration} className="input-field" placeholder="Exactly 10 digits" pattern="[0-9]{10}" minLength="10" maxLength="10" required /></div>
            <div><label htmlFor="registerDob" className="block text-sm font-medium text-gray-700 mb-1">Date of Birth *</label><input id="registerDob" type="date" name="dob" value={registration.dob} max={new Date().toISOString().split('T')[0]} onChange={updateRegistration} className="input-field" required /></div>
            <div><label htmlFor="registerGender" className="block text-sm font-medium text-gray-700 mb-1">Gender *</label><select id="registerGender" name="gender" value={registration.gender} onChange={updateRegistration} className="input-field" required><option value="">Select</option><option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option></select></div>
            <div><label htmlFor="registerBranch" className="block text-sm font-medium text-gray-700 mb-1">Department *</label><select id="registerBranch" name="branch" value={registration.branch} onChange={updateRegistration} className="input-field" required><option value="">Select department</option>{DEPARTMENTS.map(value=><option key={value} value={value}>{value}</option>)}</select></div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div><label htmlFor="registerYear" className="block text-sm font-medium text-gray-700 mb-1">Year *</label><select id="registerYear" name="year" value={registration.year} onChange={updateRegistration} className="input-field" disabled={!registration.branch} required><option value="">Select</option>{yearsForDepartment(registration.branch).map(value => <option key={value} value={value}>{value}</option>)}</select></div>
              <div><label htmlFor="registerSemester" className="block text-sm font-medium text-gray-700 mb-1">Semester *</label><select id="registerSemester" name="semester" value={registration.semester} onChange={updateRegistration} className="input-field" disabled={!registration.year} required><option value="">Select</option>{semestersForYear(registration.year).map(value => <option key={value} value={value}>{value}</option>)}</select></div>
            </div>
            <div><label htmlFor="registerBloodGroup" className="block text-sm font-medium text-gray-700 mb-1">Blood Group *</label><select id="registerBloodGroup" name="bloodGroup" value={registration.bloodGroup} onChange={updateRegistration} className="input-field" required><option value="">Select</option>{['A+','A-','B+','B-','AB+','AB-','O+','O-'].map(value => <option key={value}>{value}</option>)}</select></div>
            <div><label htmlFor="registerParentName" className="block text-sm font-medium text-gray-700 mb-1">Parent / Guardian Name *</label><input id="registerParentName" name="parentName" value={registration.parentName} onChange={updateRegistration} className="input-field" maxLength="100" required /></div>
            <div><label htmlFor="registerParentPhone" className="block text-sm font-medium text-gray-700 mb-1">Parent Phone Number *</label><input id="registerParentPhone" type="tel" inputMode="numeric" name="parentPhoneNumber" value={registration.parentPhoneNumber} onChange={updateRegistration} className="input-field" pattern="[0-9]{10}" minLength="10" maxLength="10" placeholder="Exactly 10 digits" required /></div>
            <div className="sm:col-span-2"><label htmlFor="registerAddress" className="block text-sm font-medium text-gray-700 mb-1">Residential Address *</label><textarea id="registerAddress" name="address" value={registration.address} onChange={updateRegistration} className="input-field min-h-24" maxLength="500" required /></div>
            <div className="sm:col-span-2"><label className="block text-sm font-medium text-gray-700 mb-1">Student Photo *</label><input required type="file" accept="image/jpeg,image/png" className="input-field" onChange={e=>setPhoto(e.target.files?.[0] || null)} /><p className="mt-1 text-xs text-gray-500">JPEG or PNG, maximum 2 MB. This photo will be used on the profile and bus pass.</p></div>
            <div><label htmlFor="registerPassword" className="block text-sm font-medium text-gray-700 mb-1">Password *</label><input id="registerPassword" type={showPassword ? 'text' : 'password'} name="password" value={registration.password} onChange={updateRegistration} className="input-field" minLength="6" maxLength="15" pattern={PASSWORD_PATTERN} title={PASSWORD_HELP} autoComplete="new-password" required /><span className="mt-1 block text-xs text-gray-500">{PASSWORD_HELP}</span></div>
            <div><label htmlFor="registerConfirmPassword" className="block text-sm font-medium text-gray-700 mb-1">Confirm Password *</label><input id="registerConfirmPassword" type={showPassword ? 'text' : 'password'} name="confirmPassword" value={registration.confirmPassword} onChange={updateRegistration} className="input-field" minLength="6" maxLength="15" pattern={PASSWORD_PATTERN} title={PASSWORD_HELP} autoComplete="new-password" required /></div>
          </div>
          <label className="flex items-center gap-2 text-sm text-gray-600"><input type="checkbox" checked={showPassword} onChange={() => setShowPassword(!showPassword)} /> Show passwords</label>
          <button type="submit" disabled={isLoading || !registrationToken} className="w-full btn-primary py-3 flex justify-center items-center gap-2 disabled:cursor-not-allowed disabled:opacity-60">{isLoading ? <Loader2 className="animate-spin" size={20} /> : <><UserPlus size={18} /> Create Account</>}</button>
          <button type="button" disabled={isLoading} onClick={() => { setIsRegistering(false); setError(''); }} className="w-full text-sm text-blue-700 hover:text-blue-900 flex justify-center items-center gap-2"><ArrowLeft size={16} /> Back to sign in</button>
        </form>}
      </div>
    </div>
  );
};

export default Login;
