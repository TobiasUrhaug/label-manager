import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { vi } from 'vitest';
import LoginPage from './LoginPage';

vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from '../context/AuthContext';

function renderLoginPage(initialEntry = '/login', authValue = { user: null, isLoading: false }) {
  useAuth.mockReturnValue(authValue);
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<div>Home page</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  describe('default state (unauthenticated)', () => {
    it('renders the username field', () => {
      renderLoginPage();
      expect(screen.getByLabelText('Username')).toBeInTheDocument();
    });

    it('renders the password field', () => {
      renderLoginPage();
      expect(screen.getByLabelText('Password')).toBeInTheDocument();
    });

    it('renders the Log in button', () => {
      renderLoginPage();
      expect(screen.getByRole('button', { name: 'Log in' })).toBeInTheDocument();
    });

    it('username field has autofocus', () => {
      renderLoginPage();
      expect(screen.getByLabelText('Username')).toHaveFocus();
    });
  });

  describe('already-authenticated redirect', () => {
    it('redirects to / when user is already authenticated', () => {
      renderLoginPage('/login', { user: { username: 'alice' }, isLoading: false });
      expect(screen.getByText('Home page')).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Log in' })).not.toBeInTheDocument();
    });
  });
});
