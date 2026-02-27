import { useLocation, useNavigate } from 'react-router-dom';
import {
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  IconButton,
  Box,
  useTheme,
  useMediaQuery,
} from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PersonIcon from '@mui/icons-material/Person';
import PsychologyIcon from '@mui/icons-material/Psychology';
import DescriptionIcon from '@mui/icons-material/Description';
import AssessmentIcon from '@mui/icons-material/Assessment';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';

const DRAWER_WIDTH_OPEN = 240;
const DRAWER_WIDTH_COLLAPSED = 72;

const navItems = [
  { label: 'Dashboard', path: '/dashboard', icon: <DashboardIcon /> },
  { label: 'Profile', path: '/profile', icon: <PersonIcon /> },
  { label: 'Skills', path: '/skills', icon: <PsychologyIcon /> },
  { label: 'Exports', path: '/exports', icon: <DescriptionIcon /> },
  { label: 'LinkedIn Analyzer', path: '/linkedin-analyzer', icon: <AssessmentIcon /> },
  { label: 'Billing', path: '/billing', icon: <AccountBalanceWalletIcon /> },
];

interface SidebarProps {
  open: boolean;
  onToggle: () => void;
  mobileOpen: boolean;
  onMobileClose: () => void;
}

export default function Sidebar({ open, onToggle, mobileOpen, onMobileClose }: SidebarProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const drawerWidth = open ? DRAWER_WIDTH_OPEN : DRAWER_WIDTH_COLLAPSED;

  const handleNavClick = (path: string) => {
    navigate(path);
    if (isMobile) {
      onMobileClose();
    }
  };

  const drawerContent = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Box sx={{ flexGrow: 1, pt: 8 }}>
        <List>
          {navItems.map((item) => (
            <ListItemButton
              key={item.path}
              selected={location.pathname === item.path}
              onClick={() => handleNavClick(item.path)}
              sx={{
                minHeight: 48,
                justifyContent: open || isMobile ? 'initial' : 'center',
                px: 2.5,
              }}
            >
              <ListItemIcon
                sx={{
                  minWidth: 0,
                  mr: open || isMobile ? 2 : 'auto',
                  justifyContent: 'center',
                }}
              >
                {item.icon}
              </ListItemIcon>
              {(open || isMobile) && <ListItemText primary={item.label} />}
            </ListItemButton>
          ))}
        </List>
      </Box>
      {!isMobile && (
        <Box sx={{ p: 1, display: 'flex', justifyContent: 'center' }}>
          <IconButton onClick={onToggle}>
            {open ? <ChevronLeftIcon /> : <ChevronRightIcon />}
          </IconButton>
        </Box>
      )}
    </Box>
  );

  if (isMobile) {
    return (
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={onMobileClose}
        ModalProps={{ keepMounted: true }}
        sx={{
          '& .MuiDrawer-paper': {
            width: DRAWER_WIDTH_OPEN,
            boxSizing: 'border-box',
          },
        }}
      >
        {drawerContent}
      </Drawer>
    );
  }

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: drawerWidth,
          boxSizing: 'border-box',
          transition: theme.transitions.create('width', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.enteringScreen,
          }),
          overflowX: 'hidden',
        },
      }}
    >
      {drawerContent}
    </Drawer>
  );
}

export { DRAWER_WIDTH_OPEN, DRAWER_WIDTH_COLLAPSED };
