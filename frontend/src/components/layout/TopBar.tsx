import {
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Box,
  useTheme,
  useMediaQuery,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import LogoutIcon from '@mui/icons-material/Logout';
import { useQuery } from '@tanstack/react-query';
import { getCurrentUser, logout } from '../../api/auth';
import { DRAWER_WIDTH_OPEN, DRAWER_WIDTH_COLLAPSED } from './Sidebar';

interface TopBarProps {
  onMenuClick: () => void;
  sidebarOpen: boolean;
}

export default function TopBar({ onMenuClick, sidebarOpen }: TopBarProps) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const { data: user } = useQuery({
    queryKey: ['currentUser'],
    queryFn: getCurrentUser,
  });

  const drawerWidth = isMobile ? 0 : sidebarOpen ? DRAWER_WIDTH_OPEN : DRAWER_WIDTH_COLLAPSED;

  return (
    <AppBar
      position="fixed"
      sx={{
        width: `calc(100% - ${drawerWidth}px)`,
        ml: `${drawerWidth}px`,
        transition: theme.transitions.create(['width', 'margin'], {
          easing: theme.transitions.easing.sharp,
          duration: theme.transitions.duration.enteringScreen,
        }),
      }}
    >
      <Toolbar>
        {isMobile && (
          <IconButton
            color="inherit"
            edge="start"
            onClick={onMenuClick}
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>
        )}
        <Typography variant="h6" noWrap sx={{ flexGrow: 1 }}>
          Live Resume
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {user && (
            <Typography variant="body2" noWrap>
              {user.email}
            </Typography>
          )}
          <IconButton color="inherit" onClick={logout}>
            <LogoutIcon />
          </IconButton>
        </Box>
      </Toolbar>
    </AppBar>
  );
}
