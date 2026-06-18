import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api.js';

export default function Login() {
  const navigate = useNavigate();

  const [form, setForm] = useState({ email: '', passwordHash: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await api.post('/api/users/login', form);

      // Adjust the key below if your backend wraps the token differently
      // e.g. res.data.accessToken or res.data.jwt
      const token = res.data.token ?? res.data.accessToken ?? res.data.jwt;

      if (!token) throw new Error('Token not found in response');

      localStorage.setItem('token', token);
      navigate('/');
    } catch (err) {
      const msg = err.response?.data?.message
        ?? err.response?.data?.error
        ?? err.message
        ?? 'Login failed. Please check your credentials.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="bg-white shadow rounded-2xl p-10 max-w-md w-full">
        <h1 className="text-2xl font-bold text-gray-800 mb-1">Welcome back</h1>
        <p className="text-gray-500 text-sm mb-8">Sign in to Evidence Pilot</p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Email
            </label>
            <input
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              required
              placeholder="you@example.com"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Password
            </label>
            <input
              type="password"
              name="passwordHash"
              value={form.passwordHash}
              onChange={handleChange}
              required
              placeholder="••••••••"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {error && (
            <p className="text-red-500 text-sm">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition disabled:opacity-50"
          >
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-6">
          No account?{' '}
          <Link to="/register" className="text-blue-600 hover:underline">
            Register
          </Link>
        </p>
      </div>
    </div>
  );
}
