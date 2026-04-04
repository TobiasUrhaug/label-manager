import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { vi } from 'vitest';
import App from './App';

vi.mock('./context/AuthContext', () => ({
  useAuth: vi.fn(),
  AuthProvider: ({ children }) => children,
}));

vi.mock('./api/auth', () => ({
  getSession: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
}));

import { useAuth } from './context/AuthContext';

function renderApp(initialEntry = '/', authValue = { user: null, isLoading: false }) {
  useAuth.mockReturnValue(authValue);
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('App routing', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('redirects unauthenticated user from / to /login', () => {
    renderApp('/', { user: null, isLoading: false });
    expect(screen.getByRole('button', { name: 'Log in' })).toBeInTheDocument();
  });

  it('unauthenticated user navigating to /login sees the login form', () => {
    renderApp('/login', { user: null, isLoading: false });
    expect(screen.getByRole('button', { name: 'Log in' })).toBeInTheDocument();
  });

  it('authenticated user navigating to /login is redirected to /', () => {
    renderApp('/login', { user: { username: 'alice' }, isLoading: false });
    expect(screen.queryByRole('button', { name: 'Log in' })).not.toBeInTheDocument();
  });
});
