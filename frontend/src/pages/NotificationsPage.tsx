import React from 'react';
import {
  Box, Typography, Paper, List, ListItem, ListItemText, ListItemIcon,
  Chip, Button, CircularProgress, Alert, Divider,
} from '@mui/material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import StarIcon from '@mui/icons-material/Star';
import UpdateIcon from '@mui/icons-material/Update';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import { notificationApi } from '../api/notificationApi';
import type { NotificationItem } from '../types/notification';

const typeIcons: Record<string, React.ReactNode> = {
  NEW_APPLICATION: <PersonAddIcon color="primary" />,
  HIGH_FIT_CANDIDATE: <StarIcon sx={{ color: '#f59e0b' }} />,
  APPLICATION_STATUS_CHANGED: <UpdateIcon color="info" />,
  APPLICATION_REVIEWED: <DoneAllIcon color="success" />,
  PROFILE_VIEWED: <NotificationsActiveIcon color="action" />,
};

export const NotificationsPage: React.FC = () => {
  const queryClient = useQueryClient();

  const { data: notifications, isLoading, error } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => notificationApi.list(),
  });

  const markReadMutation = useMutation({
    mutationFn: (id: number) => notificationApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notification-count'] });
    },
  });

  const markAllMutation = useMutation({
    mutationFn: () => notificationApi.markAllAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notification-count'] });
    },
  });

  if (isLoading) {
    return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}><CircularProgress /></Box>;
  }

  if (error) {
    return <Alert severity="error">Failed to load notifications</Alert>;
  }

  const unreadCount = notifications?.filter((n: NotificationItem) => !n.read).length ?? 0;

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">
          Notifications
          {unreadCount > 0 && (
            <Chip label={unreadCount} color="error" size="small" sx={{ ml: 1 }} />
          )}
        </Typography>
        {unreadCount > 0 && (
          <Button
            variant="outlined"
            startIcon={<DoneAllIcon />}
            onClick={() => markAllMutation.mutate()}
            disabled={markAllMutation.isPending}
          >
            Mark All Read
          </Button>
        )}
      </Box>

      {!notifications?.length ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">No notifications yet</Typography>
        </Paper>
      ) : (
        <Paper>
          <List>
            {notifications.map((n: NotificationItem, index: number) => (
              <React.Fragment key={n.id}>
                {index > 0 && <Divider />}
                <ListItem
                  sx={{
                    bgcolor: n.read ? 'transparent' : 'action.hover',
                    cursor: n.read ? 'default' : 'pointer',
                  }}
                  onClick={() => {
                    if (!n.read) markReadMutation.mutate(n.id);
                  }}
                >
                  <ListItemIcon>
                    {typeIcons[n.type] || <NotificationsActiveIcon />}
                  </ListItemIcon>
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Typography fontWeight={n.read ? 400 : 600}>
                          {n.title}
                        </Typography>
                        {!n.read && <Chip label="New" size="small" color="primary" />}
                      </Box>
                    }
                    secondary={
                      <>
                        <Typography variant="body2" color="text.secondary">
                          {n.message}
                        </Typography>
                        <Typography variant="caption" color="text.disabled">
                          {new Date(n.createdAt).toLocaleString()}
                        </Typography>
                      </>
                    }
                  />
                </ListItem>
              </React.Fragment>
            ))}
          </List>
        </Paper>
      )}
    </Box>
  );
};
