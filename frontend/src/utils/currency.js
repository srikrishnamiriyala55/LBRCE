const inrFormatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 0,
  maximumFractionDigits: 0,
});

export const formatINR = (value) => {
  const numericValue = typeof value === 'number'
    ? value
    : Number(String(value ?? 0).replace(/[^0-9.-]/g, ''));
  return inrFormatter.format(Number.isFinite(numericValue) ? numericValue : 0);
};
