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
import { LinkedInInboxPage } from './pages/LinkedInInboxPage';
import { LinkedInImportPage } from './pages/LinkedInImportPage';
import { SettingsPage } from './pages/SettingsPage';
import { VisitorsPage } from './pages/VisitorsPage';
import { AdminPage } from './pages/AdminPage';
import PublicProfilePage from './pages/PublicProfilePage';
import AboutPage from './pages/AboutPage';
import { CompanyRegisterPage } from './pages/CompanyRegisterPage';
import { CompanyDashboardPage } from './pages/CompanyDashboardPage';
import { CompanyProfilePage } from './pages/CompanyProfilePage';
import { JobPostingListPage } from './pages/JobPostingListPage';
import { JobPostingFormPage } from './pages/JobPostingFormPage';
import { NotificationsPage } from './pages/NotificationsPage';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './components/layout/AppLayout';

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
        <BrowserRouter basename="/interviewme-app">
          <Routes>
            {/* Public routes (no auth) */}
            <Route path="/p/:slug" element={<PublicProfilePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/about" element={<AboutPage />} />
            <Route path="/company/register" element={<CompanyRegisterPage />} />

            {/* Authenticated routes with app layout */}
            <Route element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/profile" element={<ProfileEditorPage />} />
              <Route path="/skills" element={<SkillsPage />} />
              <Route path="/billing" element={<BillingPage />} />
              <Route path="/exports" element={<ExportsPage />} />
              <Route path="/linkedin-analyzer" element={<LinkedInAnalyzerPage />} />
              <Route path="/linkedin-inbox" element={<LinkedInInboxPage />} />
              <Route path="/linkedin-import" element={<LinkedInImportPage />} />
              <Route path="/visitors" element={<VisitorsPage />} />
              <Route path="/admin" element={<AdminPage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
              <Route path="/company/dashboard" element={<CompanyDashboardPage />} />
              <Route path="/company/profile" element={<CompanyProfilePage />} />
              <Route path="/company/jobs" element={<JobPostingListPage />} />
              <Route path="/company/jobs/new" element={<JobPostingFormPage />} />
              <Route path="/company/jobs/:id/edit" element={<JobPostingFormPage />} />
            </Route>

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
