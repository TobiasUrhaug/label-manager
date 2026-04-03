import { useRef, useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { login } from '../api/auth';

export default function LoginPage() {
  const { user, setUser } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const usernameRef = useRef(null);

  const [loginError, setLoginError] = useState(null);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const mutation = useMutation({
    mutationFn: ({ username, password }) => login(username, password),
    onSuccess: () => {
      setUser({ username });
      navigate(location.state?.from ?? '/');
    },
    onError: () => {
      setLoginError('Invalid username or password.');
      setUsername('');
      setPassword('');
      usernameRef.current?.focus();
    },
  });

  if (user) {
    return <Navigate to="/" replace />;
  }

  function handleSubmit(e) {
    e.preventDefault();
    mutation.mutate({ username, password });
  }

  return (
    <div>
      <form onSubmit={handleSubmit}>
        {loginError && <p role="alert">{loginError}</p>}
        <label htmlFor="username">Username</label>
        <input
          id="username"
          type="text"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
          autoFocus
          ref={usernameRef}
        />
        <label htmlFor="password">Password</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <button type="submit" disabled={mutation.isPending}>
          Log in
        </button>
      </form>
    </div>
  );
}
