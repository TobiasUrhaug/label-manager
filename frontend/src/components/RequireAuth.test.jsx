import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import { vi } from 'vitest';
import RequireAuth from './RequireAuth';

vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from '../context/AuthContext';

function CaptureLocation({ onCapture }) {
  const location = useLocation();
  onCapture(location);
  return <div>Login page</div>;
}

function renderWithRouter(initialEntry = '/') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route element={<RequireAuth />}>
          <Route path="/" element={<div>Protected content</div>} />
        </Route>
        <Route path="/login" element={<div>Login page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('RequireAuth', () => {
  it('renders nothing while loading', () => {
    useAuth.mockReturnValue({ user: null, isLoading: true });
    const { container } = renderWithRouter('/');
    expect(container).toBeEmptyDOMElement();
  });

  it('redirects to /login when user is null', () => {
    useAuth.mockReturnValue({ user: null, isLoading: false });
    renderWithRouter('/');
    expect(screen.getByText('Login page')).toBeInTheDocument();
  });

  it('passes state.from as the current path when redirecting to /login', () => {
    useAuth.mockReturnValue({ user: null, isLoading: false });
    let capturedLocation;
    render(
      <MemoryRouter initialEntries={['/some-path']}>
        <Routes>
          <Route element={<RequireAuth />}>
            <Route path="/some-path" element={<div>Protected</div>} />
          </Route>
          <Route
            path="/login"
            element={
              <CaptureLocation onCapture={(loc) => (capturedLocation = loc)} />
            }
          />
        </Routes>
      </MemoryRouter>,
    );
    expect(capturedLocation?.state?.from).toBe('/some-path');
  });

  it('renders child content via Outlet when user is authenticated', () => {
    useAuth.mockReturnValue({ user: { username: 'alice' }, isLoading: false });
    renderWithRouter('/');
    expect(screen.getByText('Protected content')).toBeInTheDocument();
  });
});
