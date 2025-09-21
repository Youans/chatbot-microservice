(function(){
  // Use gateway for all APIs; JWT access with refresh cookie
  const gatewayDefault = 'http://localhost:18081';
  const url = new URL(window.location.href);
  const gateway = url.searchParams.get('gateway') || gatewayDefault;

  document.getElementById('gatewayUrl').textContent = 'Gateway: ' + gateway;
  const swaggerLink = document.getElementById('swaggerLink');
  if (swaggerLink) swaggerLink.href = gateway + '/swagger-ui/index.html';

  const sessionIdEl = document.getElementById('sessionId');
  const chatWindow = document.getElementById('chatWindow');
  const messageInput = document.getElementById('messageInput');
  const userInfoEl = document.getElementById('userInfo');
  const healthBtn = document.getElementById('healthBtn');
  const healthStatus = document.getElementById('healthStatus');
  const apiOutput = document.getElementById('apiOutput');

  function getToken() {
    return localStorage.getItem('chatbot_jwt') || '';
  }
  function setToken(t) { localStorage.setItem('chatbot_jwt', t || ''); }

  function ensureAuth() {
    if (!getToken()) {
      const target = '/login.html' + (gateway ? ('?gateway=' + encodeURIComponent(gateway)) : '');
      window.location.href = target;
    }
  }

  function parseJwtClaims(token) {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const json = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(json);
    } catch { return null; }
  }

  function refreshUserInfo() {
    const t = getToken();
    const c = parseJwtClaims(t);
    if (c) {
      const exp = c.exp ? new Date(c.exp * 1000).toLocaleString() : '-';
      userInfoEl.textContent = `sub: ${c.sub || '-'}, exp: ${exp}`;
    } else {
      userInfoEl.textContent = 'sub: -, exp: -';
    }
  }

  function addMsg(role, content) {
    const div = document.createElement('div');
    div.className = 'msg ' + role;
    div.innerText = content;
    chatWindow.appendChild(div);
    chatWindow.scrollTop = chatWindow.scrollHeight;
  }

  async function tryRefresh() {
    try {
      const r = await fetch(gateway + '/auth/refresh', { method: 'POST', credentials: 'include' });
      if (!r.ok) return false;
      const j = await r.json();
      if (j && j.accessToken) { setToken(j.accessToken); return true; }
      return false;
    } catch { return false; }
  }

  async function api(path, method = 'GET', body) {
    const headers = { 'Content-Type': 'application/json' };
    const t = getToken();
    if (t) headers['Authorization'] = 'Bearer ' + t;
    const useCreds = path.startsWith('/auth/'); // only include cookies for auth endpoints
    let res = await fetch(gateway + path, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      credentials: useCreds ? 'include' : 'omit', // avoid credentialed CORS unless needed
    });
    if (res.status === 401) {
      const ok = await tryRefresh();
      if (ok) {
        const headers2 = { 'Content-Type': 'application/json' };
        const t2 = getToken();
        if (t2) headers2['Authorization'] = 'Bearer ' + t2;
        res = await fetch(gateway + path, {
          method,
          headers: headers2,
          body: body ? JSON.stringify(body) : undefined,
          credentials: useCreds ? 'include' : 'omit',
        });
      }
    }
    if (!res.ok) {
      const text = await res.text();
      throw new Error('HTTP ' + res.status + ': ' + text);
    }
    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')) return res.json();
    return res.text();
  }

  document.getElementById('logoutBtn').addEventListener('click', async () => {
    try { await fetch(gateway + '/auth/logout', { method: 'POST', credentials: 'include' }); } catch {}
    localStorage.removeItem('chatbot_jwt');
    const target = '/login.html' + (gateway ? ('?gateway=' + encodeURIComponent(gateway)) : '');
    window.location.href = target;
  });

  document.getElementById('createSessionBtn').addEventListener('click', async () => {
    try {
      const body = { userId: 'demo-user' }; // provide userId for session creation
      const data = await api('/api/chat/session', 'POST', body);
      sessionIdEl.value = data.sessionId;
      const sid = data.sessionId;
      const sSend = document.getElementById('apiSessionIdSend');
      const sHist = document.getElementById('apiSessionIdHistory');
      if (sSend) sSend.value = sid;
      if (sHist) sHist.value = sid;
      addMsg('system', 'Session created: ' + data.sessionId);
    } catch (e) {
      addMsg('system', 'Error: ' + e.message);
    }
  });

  document.getElementById('sendBtn').addEventListener('click', async () => {
    try {
      const sessionId = sessionIdEl.value.trim();
      const message = messageInput.value.trim();
      if (!sessionId) throw new Error('Session ID is required');
      if (!message) return;
      addMsg('user', message);
      messageInput.value = '';
      const data = await api('/api/chat/message', 'POST', { sessionId, message });
      addMsg('assistant', data.response || data.reply || '[no reply]');
    } catch (e) {
      addMsg('system', 'Error: ' + e.message);
    }
  });

  document.getElementById('refreshBtn').addEventListener('click', async () => {
    try {
      const sessionId = sessionIdEl.value.trim();
      if (!sessionId) throw new Error('Session ID is required');
      const history = await api('/api/chat/history/' + encodeURIComponent(sessionId), 'GET');
      chatWindow.innerHTML = '';
      history.forEach(m => addMsg(m.role, m.content));
    } catch (e) {
      addMsg('system', 'Error: ' + e.message);
    }
  });

  // Health button - use gateway health endpoint
  healthBtn.addEventListener('click', async () => {
    try {
      const res = await fetch(gateway + '/health');
      const text = await res.text();
      healthStatus.textContent = text;
    } catch (e) {
      healthStatus.textContent = 'Error: ' + e.message;
    }
  });

  // API Explorer
  const apiUserId = document.getElementById('apiUserId');
  const apiCreateSessionBtn = document.getElementById('apiCreateSessionBtn');
  const apiSessionIdSend = document.getElementById('apiSessionIdSend');
  const apiMessage = document.getElementById('apiMessage');
  const apiSendMessageBtn = document.getElementById('apiSendMessageBtn');
  const apiSessionIdHistory = document.getElementById('apiSessionIdHistory');
  const apiGetHistoryBtn = document.getElementById('apiGetHistoryBtn');
  const apiHealthBtn = document.getElementById('apiHealthBtn');

  function showApiOutput(obj) {
    apiOutput.textContent = typeof obj === 'string' ? obj : JSON.stringify(obj, null, 2);
  }

  apiCreateSessionBtn.addEventListener('click', async () => {
    try {
      const body = apiUserId.value ? { userId: apiUserId.value } : undefined;
      const out = await api('/api/chat/session', 'POST', body);
      showApiOutput(out);
      if (out.sessionId) {
        sessionIdEl.value = out.sessionId;
        apiSessionIdSend.value = out.sessionId;
        apiSessionIdHistory.value = out.sessionId;
      }
    } catch (e) { showApiOutput(e.message); }
  });

  apiSendMessageBtn.addEventListener('click', async () => {
    try {
      const payload = { sessionId: apiSessionIdSend.value, message: apiMessage.value };
      const out = await api('/api/chat/message', 'POST', payload);
      showApiOutput(out);
    } catch (e) { showApiOutput(e.message); }
  });

  apiGetHistoryBtn.addEventListener('click', async () => {
    try {
      const out = await api('/api/chat/history/' + encodeURIComponent(apiSessionIdHistory.value), 'GET');
      showApiOutput(out);
    } catch (e) { showApiOutput(e.message); }
  });

  apiHealthBtn.addEventListener('click', async () => {
    try {
      const res = await fetch(gateway + '/health');
      const text = await res.text();
      showApiOutput(text);
    } catch (e) { showApiOutput(e.message); }
  });

  ensureAuth();
  refreshUserInfo();
})();
