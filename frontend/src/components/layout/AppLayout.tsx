import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Box, useTheme, useMediaQuery, Toolbar } from '@mui/material';
import Sidebar, { DRAWER_WIDTH_OPEN, DRAWER_WIDTH_COLLAPSED } from './Sidebar';
import TopBar from './TopBar';

export default function AppLayout() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const [sidebarOpen, setSidebarOpen] = useState(!isMobile);
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleSidebarToggle = () => setSidebarOpen((prev) => !prev);
  const handleMobileClose = () => setMobileOpen(false);
  const handleMenuClick = () => setMobileOpen(true);

  const drawerWidth = isMobile ? 0 : sidebarOpen ? DRAWER_WIDTH_OPEN : DRAWER_WIDTH_COLLAPSED;

  return (
    <Box sx={{ display: 'flex' }}>
      <TopBar onMenuClick={handleMenuClick} sidebarOpen={sidebarOpen} />
      <Sidebar
        open={sidebarOpen}
        onToggle={handleSidebarToggle}
        mobileOpen={mobileOpen}
        onMobileClose={handleMobileClose}
      />
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: `calc(100% - ${drawerWidth}px)`,
          ml: isMobile ? 0 : undefined,
          transition: theme.transitions.create(['width', 'margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.enteringScreen,
          }),
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}
