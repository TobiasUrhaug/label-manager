import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { vi } from 'vitest';
import AppLayout from './AppLayout';

vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../api/auth', () => ({
  logout: vi.fn(),
}));

import { useAuth } from '../context/AuthContext';
import { logout } from '../api/auth';

function renderAppLayout(
  authValue = {
    user: { username: 'alice' },
    setUser: vi.fn(),
    isLoading: false,
  },
) {
  useAuth.mockReturnValue(authValue);
  const queryClient = new QueryClient({
    defaultOptions: { mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/" element={<div>Home page</div>} />
          </Route>
          <Route path="/login" element={<div>Login page</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('AppLayout', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('renders a Log out button when user is authenticated', () => {
    renderAppLayout();
    expect(screen.getByRole('button', { name: 'Log out' })).toBeInTheDocument();
  });

  it('calls logout() when Log out button is clicked', async () => {
    logout.mockResolvedValue();
    const user = userEvent.setup();
    renderAppLayout();

    await user.click(screen.getByRole('button', { name: 'Log out' }));

    expect(logout).toHaveBeenCalledOnce();
  });

  it('navigates to /login after successful logout', async () => {
    logout.mockResolvedValue();
    const setUser = vi.fn();
    const user = userEvent.setup();
    renderAppLayout({ user: { username: 'alice' }, setUser, isLoading: false });

    await user.click(screen.getByRole('button', { name: 'Log out' }));

    expect(await screen.findByText('Login page')).toBeInTheDocument();
  });

  it('calls setUser(null) after successful logout', async () => {
    logout.mockResolvedValue();
    const setUser = vi.fn();
    const user = userEvent.setup();
    renderAppLayout({ user: { username: 'alice' }, setUser, isLoading: false });

    await user.click(screen.getByRole('button', { name: 'Log out' }));

    await screen.findByText('Login page');
    expect(setUser).toHaveBeenCalledWith(null);
  });
});
