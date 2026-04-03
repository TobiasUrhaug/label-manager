import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { vi } from 'vitest';
import LoginPage from './LoginPage';

vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../api/auth', () => ({
  login: vi.fn(),
}));

import { useAuth } from '../context/AuthContext';
import { login } from '../api/auth';

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

  describe('loading state', () => {
    it('disables the Log in button while login is pending', async () => {
      login.mockReturnValue(new Promise(() => {})); // never resolves
      const user = userEvent.setup();
      renderLoginPage();

      await user.type(screen.getByLabelText('Username'), 'alice');
      await user.type(screen.getByLabelText('Password'), 'secret');
      await user.click(screen.getByRole('button', { name: 'Log in' }));

      expect(screen.getByRole('button', { name: 'Log in' })).toBeDisabled();
    });
  });

  describe('error state', () => {
    it('shows inline error message when login returns 401', async () => {
      login.mockRejectedValue({ status: 401 });
      const user = userEvent.setup();
      renderLoginPage();

      await user.type(screen.getByLabelText('Username'), 'alice');
      await user.type(screen.getByLabelText('Password'), 'wrongpassword');
      await user.click(screen.getByRole('button', { name: 'Log in' }));

      expect(await screen.findByText('Invalid username or password.')).toBeInTheDocument();
    });

    it('clears both fields after a 401 error', async () => {
      login.mockRejectedValue({ status: 401 });
      const user = userEvent.setup();
      renderLoginPage();

      await user.type(screen.getByLabelText('Username'), 'alice');
      await user.type(screen.getByLabelText('Password'), 'wrongpassword');
      await user.click(screen.getByRole('button', { name: 'Log in' }));

      await screen.findByText('Invalid username or password.');

      expect(screen.getByLabelText('Username')).toHaveValue('');
      expect(screen.getByLabelText('Password')).toHaveValue('');
    });

    it('focuses the username field after a 401 error', async () => {
      login.mockRejectedValue({ status: 401 });
      const user = userEvent.setup();
      renderLoginPage();

      await user.type(screen.getByLabelText('Username'), 'alice');
      await user.type(screen.getByLabelText('Password'), 'wrongpassword');
      await user.click(screen.getByRole('button', { name: 'Log in' }));

      await screen.findByText('Invalid username or password.');

      expect(screen.getByLabelText('Username')).toHaveFocus();
    });
  });
});
