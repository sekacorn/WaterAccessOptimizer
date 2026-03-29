const DEMO_USER = {
  email: 'demo@wateraccessoptimizer.local',
  fullName: 'Demo Analyst',
  role: 'ADMIN',
}

export function initializeDemoSession() {
  if (!import.meta.env.VITE_ENABLE_MOCK_API || !import.meta.env.VITE_AUTO_LOGIN_DEMO) {
    return
  }

  if (!localStorage.getItem('authToken')) {
    localStorage.setItem('authToken', 'demo-auth-token')
  }

  if (!localStorage.getItem('user')) {
    localStorage.setItem('user', JSON.stringify(DEMO_USER))
  }

  const persistedState = {
    state: {
      token: 'demo-auth-token',
      user: DEMO_USER,
      isAuthenticated: true,
      sidebarOpen: true,
      mapCenter: [-1.2921, 36.8219],
      mapZoom: 12,
    },
    version: 0,
  }

  localStorage.setItem('water-optimizer-storage', JSON.stringify(persistedState))
}
