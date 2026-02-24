import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import { HelmetProvider } from 'react-helmet-async';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import { ProfileEditorPage } from './pages/ProfileEditorPage';
import { SkillsPage } from './pages/SkillsPage';
import { BillingPage } from './pages/BillingPage';
import { ExportsPage } from './pages/ExportsPage';
import { LinkedInAnalyzerPage } from './pages/LinkedInAnalyzerPage';
import PublicProfilePage from './pages/PublicProfilePage';
import ProtectedRoute from './components/ProtectedRoute';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false
    }
  }
});

const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2'
    },
    secondary: {
      main: '#dc004e'
    }
  }
});

function App() {
  const token = localStorage.getItem('token');

  return (
    <HelmetProvider>
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <BrowserRouter>
          <Routes>
            {/* Public routes (no auth) */}
            <Route path="/p/:slug" element={<PublicProfilePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <DashboardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/profile"
              element={
                <ProtectedRoute>
                  <ProfileEditorPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/skills"
              element={
                <ProtectedRoute>
                  <SkillsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/billing"
              element={
                <ProtectedRoute>
                  <BillingPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/exports"
              element={
                <ProtectedRoute>
                  <ExportsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/linkedin-analyzer"
              element={
                <ProtectedRoute>
                  <LinkedInAnalyzerPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/"
              element={<Navigate to={token ? '/dashboard' : '/login'} replace />}
            />
          </Routes>
        </BrowserRouter>
      </ThemeProvider>
    </QueryClientProvider>
    </HelmetProvider>
  );
}

export default App;
