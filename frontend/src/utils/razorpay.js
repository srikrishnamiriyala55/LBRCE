export const loadRazorpayCheckout = () => new Promise((resolve, reject) => {
  if (window.Razorpay) {
    resolve();
    return;
  }
  const existing = document.querySelector('script[data-razorpay-checkout]');
  if (existing) {
    existing.addEventListener('load', resolve, { once: true });
    existing.addEventListener('error', () => reject(new Error('Unable to load secure payment checkout')), { once: true });
    return;
  }
  const script = document.createElement('script');
  script.src = 'https://checkout.razorpay.com/v1/checkout.js';
  script.async = true;
  script.dataset.razorpayCheckout = 'true';
  script.onload = resolve;
  script.onerror = () => reject(new Error('Unable to load secure payment checkout'));
  document.body.appendChild(script);
});

export const apiMessage = (error, fallback) => error?.response?.data?.message || error?.message || fallback;

export const openRazorpayCheckout = async ({ order, onSuccess, onFailure, onDismiss }) => {
  await loadRazorpayCheckout();
  const checkout = new window.Razorpay({
    key: order.keyId,
    order_id: order.orderId,
    amount: order.amountPaise,
    currency: order.currency,
    name: order.name,
    description: order.description,
    prefill: {
      name: order.prefillName || '',
      email: order.prefillEmail || '',
      contact: order.prefillContact || ''
    },
    theme: { color: '#1d4ed8' },
    modal: { ondismiss: onDismiss },
    handler: onSuccess
  });
  checkout.on('payment.failed', response => onFailure(response?.error?.description || 'Payment failed. No fee balance was changed.'));
  checkout.open();
};
