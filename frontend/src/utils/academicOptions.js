export const DEPARTMENTS = ['CSE', 'AI&DS', 'IT', 'CSM', 'ECE', 'EEE', 'MECH', 'CIVIL', 'ASE', 'MBA'];

export const yearsForDepartment = (department) =>
  department === 'MBA' ? [1, 2] : [1, 2, 3, 4];

export const semestersForYear = (year) => {
  const numericYear = Number(year);
  return numericYear ? [numericYear * 2 - 1, numericYear * 2] : [];
};

export const normalizePhone = (value) => value.replace(/\D/g, '').slice(0, 10);

export const PASSWORD_PATTERN = '^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s]).{6,15}$';
export const PASSWORD_HELP = '6–15 characters with uppercase, lowercase, number and special character.';
