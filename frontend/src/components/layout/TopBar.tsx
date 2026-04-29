import {
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Box,
  Chip,
  Badge,
  useTheme,
  useMediaQuery,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import LogoutIcon from '@mui/icons-material/Logout';
import NotificationsIcon from '@mui/icons-material/Notifications';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { getCurrentUser, logout } from '../../api/auth';
import { useAppInfo } from '../../hooks/useAppInfo';
import { DRAWER_WIDTH_OPEN, DRAWER_WIDTH_COLLAPSED } from './Sidebar';
import LanguageSelector from './LanguageSelector';
import { CoinBalanceBadge } from '../billing/CoinBalanceBadge';
import { notificationApi } from '../../api/notificationApi';

interface TopBarProps {
  onMenuClick: () => void;
  sidebarOpen: boolean;
}

export default function TopBar({ onMenuClick, sidebarOpen }: TopBarProps) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { t } = useTranslation('common');
  const navigate = useNavigate();

  const { data: user } = useQuery({
    queryKey: ['currentUser'],
    queryFn: getCurrentUser,
  });

  const { data: appInfo } = useAppInfo();

  const { data: unreadCount } = useQuery({
    queryKey: ['notification-count'],
    queryFn: notificationApi.countUnread,
    refetchInterval: 30000,
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
          {t('appName')}
          {appInfo?.build?.version && (
            <Chip
              label={`v${appInfo.build.version}`}
              size="small"
              sx={{
                ml: 1,
                height: 20,
                fontSize: '0.7rem',
                backgroundColor: 'rgba(255,255,255,0.2)',
                color: 'inherit',
              }}
            />
          )}
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <IconButton color="inherit" onClick={() => navigate('/notifications')}>
            <Badge badgeContent={unreadCount ?? 0} color="error">
              <NotificationsIcon />
            </Badge>
          </IconButton>
          <CoinBalanceBadge />
          <LanguageSelector />
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
