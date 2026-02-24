import { useQuery } from '@tanstack/react-query';
import {
  Container,
  Box,
  Typography,
  Paper,
  Button,
  CircularProgress,
  Alert
} from '@mui/material';
import { getCurrentUser, logout } from '../api/auth';

export default function DashboardPage() {
  const { data: user, isLoading, error } = useQuery({
    queryKey: ['currentUser'],
    queryFn: getCurrentUser
  });

  if (isLoading) {
    return (
      <Container>
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
          <CircularProgress />
        </Box>
      </Container>
    );
  }

  if (error) {
    return (
      <Container>
        <Box sx={{ mt: 8 }}>
          <Alert severity="error">Failed to load user information</Alert>
        </Box>
      </Container>
    );
  }

  return (
    <Container maxWidth="md">
      <Box sx={{ mt: 8 }}>
        <Paper elevation={3} sx={{ p: 4 }}>
          <Typography component="h1" variant="h4" gutterBottom>
            Welcome to Live Resume
          </Typography>
          <Typography variant="body1" sx={{ mt: 2 }}>
            Email: {user?.email}
          </Typography>
          <Typography variant="body1" sx={{ mt: 1 }}>
            Tenant ID: {user?.tenantId}
          </Typography>
          <Typography variant="body1" sx={{ mt: 1 }}>
            User ID: {user?.id}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Account created: {user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}
          </Typography>

          <Box sx={{ mt: 4 }}>
            <Button variant="outlined" onClick={logout}>
              Logout
            </Button>
          </Box>

          <Box sx={{ mt: 4 }}>
            <Typography variant="body2" color="text.secondary">
              This is a placeholder dashboard. Additional features will be added in future iterations.
            </Typography>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
}
