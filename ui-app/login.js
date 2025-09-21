(function(){
  const gatewayDefault = 'http://localhost:18081';
  const url = new URL(window.location.href);
  const gateway = url.searchParams.get('gateway') || gatewayDefault;
  document.getElementById('gatewayUrl').textContent = gateway;

  const usernameEl = document.getElementById('username');
  const passwordEl = document.getElementById('password');
  const statusEl = document.getElementById('status');

  document.getElementById('loginBtn').addEventListener('click', async () => {
    statusEl.textContent = 'Logging in...';
    try {
      const res = await fetch(gateway + '/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include', // receive HttpOnly refresh cookie
        body: JSON.stringify({
          username: usernameEl.value || 'admin',
          password: passwordEl.value || 'admin',
        }),
      });
      if (!res.ok) {
        const t = await res.text();
        throw new Error('Login failed: ' + t);
      }
      const data = await res.json();
      if (!data.accessToken) throw new Error('No accessToken returned');
      localStorage.setItem('chatbot_jwt', data.accessToken);
      statusEl.textContent = 'Login successful. Redirecting...';
      const target = '/index.html' + (gateway ? ('?gateway=' + encodeURIComponent(gateway)) : '');
      window.location.href = target;
    } catch (e) {
      statusEl.textContent = 'Error: ' + e.message;
    }
  });
})();
