import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api.js';

export default function Home() {
  const navigate = useNavigate();
  const token = localStorage.getItem('token');

  const [profile, setProfile] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!token) return;

    setLoading(true);
    api.get('/api/users/me')
      .then((res) => setProfile(res.data))
      .catch(() => setError('Failed to load profile. Your session may have expired.'))
      .finally(() => setLoading(false));
  }, [token]);

  function handleLogout() {
    localStorage.removeItem('token');
    navigate('/login');
  }

  // ── Guest view ──────────────────────────────────────────────────────────────
  if (!token) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center gap-6">
        <div className="bg-white shadow rounded-2xl p-10 text-center max-w-md w-full">
          <h1 className="text-3xl font-bold text-gray-800 mb-2">Evidence Pilot</h1>
          <p className="text-gray-500 mb-8">
            Manage and track your academic evidence in one place.
          </p>
          <div className="flex gap-4 justify-center">
            <Link
              to="/login"
              className="px-6 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition"
            >
              Log In
            </Link>
            <Link
              to="/register"
              className="px-6 py-2 border border-blue-600 text-blue-600 rounded-lg font-medium hover:bg-blue-50 transition"
            >
              Register
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // ── Authenticated view ───────────────────────────────────────────────────────
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center">
      <div className="bg-white shadow rounded-2xl p-10 max-w-md w-full">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Dashboard</h1>

        {loading && <p className="text-gray-400">Loading profile…</p>}

        {error && (
          <p className="text-red-500 text-sm mb-4">{error}</p>
        )}

        {profile && (
          <div className="mb-6 space-y-2 text-sm text-gray-700">
            <div className="flex justify-between border-b pb-2">
              <span className="font-medium">Email</span>
              <span>{profile.email}</span>
            </div>
            <div className="flex justify-between border-b pb-2">
              <span className="font-medium">Role</span>
              <span>{profile.role}</span>
            </div>
            {profile.name && (
              <div className="flex justify-between border-b pb-2">
                <span className="font-medium">Name</span>
                <span>{profile.name}</span>
              </div>
            )}
          </div>
        )}

        <button
          onClick={handleLogout}
          className="w-full px-4 py-2 bg-red-500 text-white rounded-lg font-medium hover:bg-red-600 transition"
        >
          Logout
        </button>
      </div>
    </div>
  );
}
