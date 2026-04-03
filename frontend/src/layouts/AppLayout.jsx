import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { logout } from '../api/auth';

export default function AppLayout() {
  const { setUser } = useAuth();
  const navigate = useNavigate();

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: () => {
      setUser(null);
      navigate('/login');
    },
  });

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white border-b border-gray-200">
        <div className="max-w-5xl mx-auto px-4 h-14 flex items-center gap-6">
          <span className="font-semibold text-gray-900">Label Manager</span>
          <NavLink
            to="/"
            end
            className={({ isActive }) =>
              isActive ? 'text-blue-600 font-medium' : 'text-gray-600 hover:text-gray-900'
            }
          >
            Home
          </NavLink>
          <button
            type="button"
            onClick={() => logoutMutation.mutate()}
            className="ml-auto text-sm text-gray-600 hover:text-gray-900"
          >
            Log out
          </button>
        </div>
      </nav>
      <main className="max-w-5xl mx-auto px-4 py-8">
        <Outlet />
      </main>
    </div>
  );
}
