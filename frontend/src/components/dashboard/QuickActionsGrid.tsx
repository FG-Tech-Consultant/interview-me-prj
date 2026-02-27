import {
  Grid,
  Card,
  CardActionArea,
  Avatar,
  Typography,
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import PsychologyIcon from '@mui/icons-material/Psychology';
import DescriptionIcon from '@mui/icons-material/Description';
import AssessmentIcon from '@mui/icons-material/Assessment';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import { useNavigate } from 'react-router-dom';

interface QuickAction {
  label: string;
  description: string;
  icon: React.ReactNode;
  color: string;
  path: string;
}

const ACTIONS: QuickAction[] = [
  {
    label: 'Edit Profile',
    description: 'Manage your personal and professional info',
    icon: <PersonIcon />,
    color: '#1976d2',
    path: '/profile',
  },
  {
    label: 'Manage Skills',
    description: 'Add and track your technical skills',
    icon: <PsychologyIcon />,
    color: '#7b1fa2',
    path: '/skills',
  },
  {
    label: 'Export Resume',
    description: 'Generate PDF or other export formats',
    icon: <DescriptionIcon />,
    color: '#00897b',
    path: '/exports',
  },
  {
    label: 'LinkedIn Analyzer',
    description: 'Get AI feedback on your LinkedIn profile',
    icon: <AssessmentIcon />,
    color: '#e65100',
    path: '/linkedin-analyzer',
  },
  {
    label: 'View Billing',
    description: 'Check your coin balance and usage',
    icon: <AccountBalanceWalletIcon />,
    color: '#2e7d32',
    path: '/billing',
  },
];

const QuickActionsGrid = () => {
  const navigate = useNavigate();

  return (
    <Grid container spacing={2}>
      {ACTIONS.map((action) => (
        <Grid item xs={12} sm={6} md={4} key={action.path}>
          <Card
            variant="outlined"
            sx={{
              transition: 'box-shadow 0.2s',
              '&:hover': { boxShadow: 4 },
            }}
          >
            <CardActionArea
              onClick={() => navigate(action.path)}
              sx={{ p: 2, display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}
            >
              <Avatar sx={{ bgcolor: action.color, width: 48, height: 48, mb: 1.5 }}>
                {action.icon}
              </Avatar>
              <Typography variant="h6" sx={{ fontSize: '1rem' }}>
                {action.label}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {action.description}
              </Typography>
            </CardActionArea>
          </Card>
        </Grid>
      ))}
    </Grid>
  );
};

export default QuickActionsGrid;
