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
    onSuccess: (_, variables) => {
      setUser({ username: variables.username });
      navigate(location.state?.from ?? '/');
    },
    onError: () => {
      setLoginError('Invalid credentials.');
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
    <div className="min-h-screen bg-neutral-100 flex items-center justify-center">
      <div className="bg-white rounded-lg shadow-sm border border-neutral-200 w-full max-w-sm mx-auto p-8">
        <h1 className="text-xl font-semibold text-gray-900 text-center mb-6">Label Manager</h1>

        {loginError && (
          <p role="alert" className="text-red-600 text-sm mb-4">
            {loginError}
          </p>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1">
            <label htmlFor="username" className="text-sm font-medium text-gray-700">
              Username
            </label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              autoFocus
              ref={usernameRef}
              className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label htmlFor="password" className="text-sm font-medium text-gray-700">
              Password
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <button
            type="submit"
            disabled={mutation.isPending}
            className="w-full bg-blue-600 text-white rounded px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Log in
          </button>
        </form>
      </div>
    </div>
  );
}
