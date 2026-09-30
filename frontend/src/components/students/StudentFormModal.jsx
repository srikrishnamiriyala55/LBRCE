import React, { useEffect, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import Modal from '../common/Modal';
import { DEPARTMENTS, PASSWORD_HELP, PASSWORD_PATTERN, normalizePhone, semestersForYear, yearsForDepartment } from '../../utils/academicOptions';

export const emptyStudentForm = { rollNumber:'',name:'',email:'',phoneNumber:'',password:'',branch:'CSE',year:1,semester:1,dob:'',gender:'MALE',bloodGroup:'O+',address:'',parentName:'',parentPhoneNumber:'' };
export const studentFormFromProfile = student => ({...emptyStudentForm,...student,password:'',dob:student?.dob||'',gender:(student?.gender||'MALE').toUpperCase()});

export default function StudentFormModal({isOpen,onClose,title,initial=emptyStudentForm,mode='create',onSubmit}){
  const [form,setForm]=useState(emptyStudentForm),[photo,setPhoto]=useState(null),[saving,setSaving]=useState(false),[showPassword,setShowPassword]=useState(false);
  useEffect(()=>{if(isOpen){setForm(studentFormFromProfile(initial));setPhoto(null);}},[isOpen,initial]);
  const set=(name,value)=>setForm(current=>{
    const nextValue=['phoneNumber','parentPhoneNumber'].includes(name)?normalizePhone(value):value;
    if(name==='branch') return {...current,branch:nextValue,year:1,semester:1};
    return {...current,[name]:nextValue,...(name==='year'?{semester:Number(nextValue)*2-1}:{})};
  });
  const submit=async event=>{event.preventDefault();setSaving(true);try{await onSubmit(form,photo);}finally{setSaving(false);}};
  const editing=mode!=='create',photoAllowed=true;
  return <Modal isOpen={isOpen} onClose={onClose} title={title} size="xl"><form onSubmit={submit} className="space-y-4">
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <label className="text-sm font-medium">Roll Number *<input required readOnly={mode==='inchargeEdit'} className={`input-field mt-1 uppercase ${mode==='inchargeEdit'?'bg-gray-100':''}`} value={form.rollNumber} onChange={e=>set('rollNumber',e.target.value.toUpperCase())} /><span className="mt-1 block text-xs text-gray-500">Only a unique roll number can be saved.</span></label>
      <label className="text-sm font-medium">Full Name *<input required maxLength="100" className="input-field mt-1" value={form.name} onChange={e=>set('name',e.target.value)} /></label>
      <label className="text-sm font-medium">Email *<input required type="email" maxLength="120" className="input-field mt-1" value={form.email} onChange={e=>set('email',e.target.value)} /></label>
      <label className="text-sm font-medium">Phone Number *<input required type="tel" inputMode="numeric" pattern="[0-9]{10}" minLength="10" maxLength="10" className="input-field mt-1" value={form.phoneNumber} onChange={e=>set('phoneNumber',e.target.value)} /></label>
      <label className="text-sm font-medium">Date of Birth *<input required type="date" max={new Date().toISOString().split('T')[0]} className="input-field mt-1" value={form.dob} onChange={e=>set('dob',e.target.value)} /></label>
      <label className="text-sm font-medium">Gender *<select required className="input-field mt-1" value={form.gender} onChange={e=>set('gender',e.target.value)}><option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option></select></label>
      <label className="text-sm font-medium">Department *<select required className="input-field mt-1" value={form.branch} onChange={e=>set('branch',e.target.value)}>{DEPARTMENTS.map(v=><option key={v} value={v}>{v}</option>)}</select></label>
      <div className="grid grid-cols-2 gap-3"><label className="text-sm font-medium">Year *<select required className="input-field mt-1" value={form.year} onChange={e=>set('year',Number(e.target.value))}>{yearsForDepartment(form.branch).map(v=><option key={v} value={v}>{v}</option>)}</select></label><label className="text-sm font-medium">Semester *<select required className="input-field mt-1" value={form.semester} onChange={e=>set('semester',Number(e.target.value))}>{semestersForYear(form.year).map(v=><option key={v} value={v}>{v}</option>)}</select></label></div>
      <label className="text-sm font-medium">Blood Group *<select required className="input-field mt-1" value={form.bloodGroup} onChange={e=>set('bloodGroup',e.target.value)}>{['A+','A-','B+','B-','AB+','AB-','O+','O-'].map(v=><option key={v}>{v}</option>)}</select></label>
      <label className="text-sm font-medium">Parent / Guardian Name *<input required maxLength="100" className="input-field mt-1" value={form.parentName} onChange={e=>set('parentName',e.target.value)} /></label>
      <label className="text-sm font-medium">Parent Phone Number *<input required type="tel" inputMode="numeric" pattern="[0-9]{10}" minLength="10" maxLength="10" className="input-field mt-1" value={form.parentPhoneNumber} onChange={e=>set('parentPhoneNumber',e.target.value)} /></label>
      <label className="sm:col-span-2 text-sm font-medium">Residential Address *<textarea required maxLength="500" rows="3" className="input-field mt-1" value={form.address} onChange={e=>set('address',e.target.value)} /></label>
      {mode==='create'&&<label className="text-sm font-medium">Initial Password *<div className="relative"><input required type={showPassword?'text':'password'} minLength="6" maxLength="15" pattern={PASSWORD_PATTERN} title={PASSWORD_HELP} className="input-field mt-1" value={form.password} onChange={e=>set('password',e.target.value)} /><button type="button" onClick={()=>setShowPassword(value=>!value)} aria-label={showPassword?'Hide password':'Show password'} className="absolute right-3 top-1/2 translate-y-0 text-gray-500">{showPassword?<EyeOff size={18}/>:<Eye size={18}/>}</button></div><span className="mt-1 block text-xs text-gray-500">{PASSWORD_HELP}</span></label>}
      {photoAllowed&&<label className="text-sm font-medium">Student Photo {mode==='create'?'*':'(optional replacement)'}<input required={mode==='create'} type="file" accept="image/jpeg,image/png" className="input-field mt-1" onChange={e=>setPhoto(e.target.files?.[0]||null)} /><span className="mt-1 block text-xs text-gray-500">JPEG or PNG, maximum 2 MB.</span></label>}
    </div>
    <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-3"><button type="button" onClick={onClose} className="btn-secondary">Cancel</button><button disabled={saving} className="btn-primary disabled:opacity-60">{saving?'Saving...':editing?'Save Changes':'Register Student'}</button></div>
  </form></Modal>;
}
