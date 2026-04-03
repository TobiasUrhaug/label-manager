export async function getSession() {
  const res = await fetch('/api/session');
  if (!res.ok) throw new Error('Not authenticated');
  return res.json();
}

export async function login(username, password) {
  const body = new URLSearchParams({ username, password });
  const res = await fetch('/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString(),
  });
  if (res.status === 401) throw { status: 401 };
  if (!res.ok) throw new Error('Login failed');
}

export async function logout() {
  const res = await fetch('/logout', { method: 'POST' });
  if (!res.ok) throw new Error('Logout failed');
}
