import React, { useEffect, useState } from 'react';
import Modal from '../common/Modal';

export const emptyStudentForm = { rollNumber:'',name:'',email:'',phoneNumber:'',password:'',branch:'CSE',year:1,semester:1,dob:'',gender:'MALE',bloodGroup:'O+',address:'',parentName:'',parentPhoneNumber:'' };
export const studentFormFromProfile = student => ({...emptyStudentForm,...student,password:'',dob:student?.dob||'',gender:(student?.gender||'MALE').toUpperCase()});

export default function StudentFormModal({isOpen,onClose,title,initial=emptyStudentForm,mode='create',onSubmit}){
  const [form,setForm]=useState(emptyStudentForm),[photo,setPhoto]=useState(null),[saving,setSaving]=useState(false);
  useEffect(()=>{if(isOpen){setForm(studentFormFromProfile(initial));setPhoto(null);}},[isOpen,initial]);
  const set=(name,value)=>setForm(current=>({...current,[name]:value,...(name==='year'?{semester:Number(value)*2-1}:{})}));
  const submit=async event=>{event.preventDefault();setSaving(true);try{await onSubmit(form,photo);}finally{setSaving(false);}};
  const editing=mode!=='create',photoAllowed=mode!=='inchargeEdit';
  return <Modal isOpen={isOpen} onClose={onClose} title={title} size="xl"><form onSubmit={submit} className="space-y-4">
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <label className="text-sm font-medium">Roll Number *<input required readOnly={editing} className={`input-field mt-1 uppercase ${editing?'bg-gray-100':''}`} value={form.rollNumber} onChange={e=>set('rollNumber',e.target.value)} /></label>
      <label className="text-sm font-medium">Full Name *<input required maxLength="100" className="input-field mt-1" value={form.name} onChange={e=>set('name',e.target.value)} /></label>
      <label className="text-sm font-medium">Email *<input required type="email" maxLength="120" className="input-field mt-1" value={form.email} onChange={e=>set('email',e.target.value)} /></label>
      <label className="text-sm font-medium">Phone Number *<input required type="tel" pattern="[0-9]{10,15}" className="input-field mt-1" value={form.phoneNumber} onChange={e=>set('phoneNumber',e.target.value)} /></label>
      <label className="text-sm font-medium">Date of Birth *<input required type="date" max={new Date().toISOString().split('T')[0]} className="input-field mt-1" value={form.dob} onChange={e=>set('dob',e.target.value)} /></label>
      <label className="text-sm font-medium">Gender *<select required className="input-field mt-1" value={form.gender} onChange={e=>set('gender',e.target.value)}><option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option></select></label>
      <label className="text-sm font-medium">Branch *<input required maxLength="50" className="input-field mt-1 uppercase" value={form.branch} onChange={e=>set('branch',e.target.value)} /></label>
      <div className="grid grid-cols-2 gap-3"><label className="text-sm font-medium">Year *<select required className="input-field mt-1" value={form.year} onChange={e=>set('year',Number(e.target.value))}>{[1,2,3,4].map(v=><option key={v} value={v}>{v}</option>)}</select></label><label className="text-sm font-medium">Semester *<select required className="input-field mt-1" value={form.semester} onChange={e=>set('semester',Number(e.target.value))}>{[form.year*2-1,form.year*2].map(v=><option key={v} value={v}>{v}</option>)}</select></label></div>
      <label className="text-sm font-medium">Blood Group *<select required className="input-field mt-1" value={form.bloodGroup} onChange={e=>set('bloodGroup',e.target.value)}>{['A+','A-','B+','B-','AB+','AB-','O+','O-'].map(v=><option key={v}>{v}</option>)}</select></label>
      <label className="text-sm font-medium">Parent / Guardian Name *<input required maxLength="100" className="input-field mt-1" value={form.parentName} onChange={e=>set('parentName',e.target.value)} /></label>
      <label className="text-sm font-medium">Parent Phone Number *<input required type="tel" pattern="[0-9]{10,15}" className="input-field mt-1" value={form.parentPhoneNumber} onChange={e=>set('parentPhoneNumber',e.target.value)} /></label>
      <label className="sm:col-span-2 text-sm font-medium">Residential Address *<textarea required maxLength="500" rows="3" className="input-field mt-1" value={form.address} onChange={e=>set('address',e.target.value)} /></label>
      {mode==='create'&&<label className="text-sm font-medium">Initial Password *<input required type="password" minLength="8" maxLength="72" className="input-field mt-1" value={form.password} onChange={e=>set('password',e.target.value)} /></label>}
      {photoAllowed&&<label className="text-sm font-medium">Student Photo {mode==='create'?'*':'(optional replacement)'}<input required={mode==='create'} type="file" accept="image/jpeg,image/png" className="input-field mt-1" onChange={e=>setPhoto(e.target.files?.[0]||null)} /><span className="mt-1 block text-xs text-gray-500">JPEG or PNG, maximum 2 MB.</span></label>}
    </div>
    <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-3"><button type="button" onClick={onClose} className="btn-secondary">Cancel</button><button disabled={saving} className="btn-primary disabled:opacity-60">{saving?'Saving...':editing?'Save Changes':'Register Student'}</button></div>
  </form></Modal>;
}
