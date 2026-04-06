import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AuthProvider, useAuth } from './AuthContext.jsx';

vi.mock('../api/auth.js');

import { getSession } from '../api/auth.js';

function TestConsumer() {
  const { user, isLoading } = useAuth();
  if (isLoading) return <div>loading</div>;
  return <div>{user ? `user:${user.username}` : 'no user'}</div>;
}

describe('AuthContext', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('provides user when getSession resolves', async () => {
    getSession.mockResolvedValue({ username: 'alice' });

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );

    expect(screen.getByText('loading')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('user:alice')).toBeInTheDocument();
    });
  });

  it('provides null user when getSession rejects', async () => {
    getSession.mockRejectedValue(new Error('401'));

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText('no user')).toBeInTheDocument();
    });
  });

  it('shows loading while getSession is pending', async () => {
    let resolve;
    getSession.mockReturnValue(
      new Promise((res) => {
        resolve = res;
      }),
    );

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );

    expect(screen.getByText('loading')).toBeInTheDocument();

    resolve({ username: 'bob' });

    await waitFor(() => {
      expect(screen.queryByText('loading')).not.toBeInTheDocument();
    });
  });
});
