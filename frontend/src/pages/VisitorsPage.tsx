import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Container,
  Typography,
  Box,
  Grid,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  IconButton,
  Chip,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Drawer,
  List,
  ListItemButton,
  ListItemText,
  Divider,
  Link,
  TablePagination,
  Accordion,
  AccordionSummary,
  AccordionDetails,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import PeopleIcon from '@mui/icons-material/People';
import ChatIcon from '@mui/icons-material/Chat';
import LockIcon from '@mui/icons-material/Lock';
import EmailIcon from '@mui/icons-material/Email';
import LinkedInIcon from '@mui/icons-material/LinkedIn';
import WhatsAppIcon from '@mui/icons-material/WhatsApp';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { visitorApi } from '../api/visitorApi';
import type { VisitorResponse, VisitorSessionResponse, VisitorChatLogResponse } from '../api/visitorApi';

export const VisitorsPage = () => {
  const { t } = useTranslation('visitors');
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [confirmRevealId, setConfirmRevealId] = useState<number | null>(null);
  const [selectedVisitor, setSelectedVisitor] = useState<VisitorResponse | null>(null);
  const [sessions, setSessions] = useState<VisitorSessionResponse[]>([]);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [chatLog, setChatLog] = useState<VisitorChatLogResponse[] | null>(null);
  const [chatLoading, setChatLoading] = useState(false);

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['visitorStats'],
    queryFn: visitorApi.getVisitorStats,
  });

  const { data: visitorsData, isLoading: visitorsLoading, error: visitorsError } = useQuery({
    queryKey: ['visitors', page, rowsPerPage],
    queryFn: () => visitorApi.getMyVisitors(page, rowsPerPage),
  });

  const [revealError, setRevealError] = useState<string | null>(null);

  const revealMutation = useMutation({
    mutationFn: (visitorId: number) => visitorApi.revealContact(visitorId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['visitors'] });
      setConfirmRevealId(null);
      setRevealError(null);
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || error?.message || 'Failed to reveal contact';
      setRevealError(msg);
    },
  });

  const handleOpenSessions = async (visitor: VisitorResponse) => {
    setSelectedVisitor(visitor);
    setChatLog(null);
    setSessionsLoading(true);
    try {
      const data = await visitorApi.getVisitorSessions(visitor.id);
      setSessions(data);
    } finally {
      setSessionsLoading(false);
    }
  };

  const handleViewChat = async (sessionId: number) => {
    setChatLoading(true);
    try {
      const data = await visitorApi.getSessionMessages(sessionId);
      setChatLog(data);
    } finally {
      setChatLoading(false);
    }
  };

  const statCards = [
    { icon: <VisibilityIcon sx={{ fontSize: 40, color: '#1976d2' }} />, value: stats?.profileViews ?? 0, label: t('stats.profileViews') },
    { icon: <PeopleIcon sx={{ fontSize: 40, color: '#9c27b0' }} />, value: stats?.totalVisitors ?? 0, label: t('stats.totalVisitors') },
    { icon: <ChatIcon sx={{ fontSize: 40, color: '#2e7d32' }} />, value: stats?.chatVisitors ?? 0, label: t('stats.chatVisitors') },
  ];

  if (visitorsError) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <Alert severity="error">{String(visitorsError)}</Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Typography variant="h4" gutterBottom>{t('title')}</Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>{t('subtitle')}</Typography>

      {/* Stat Cards */}
      <Grid container spacing={2} sx={{ mb: 4 }}>
        {statCards.map((stat) => (
          <Grid item xs={12} sm={4} key={stat.label}>
            <Card elevation={2}>
              <CardContent sx={{ textAlign: 'center', py: 3 }}>
                {stat.icon}
                {statsLoading ? (
                  <CircularProgress size={24} sx={{ mt: 1 }} />
                ) : (
                  <Typography variant="h3" sx={{ mt: 1 }}>{stat.value}</Typography>
                )}
                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>{stat.label}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Visitors Table */}
      {visitorsLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      ) : !visitorsData?.content?.length ? (
        <Alert severity="info">{t('empty')}</Alert>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>{t('table.name')}</TableCell>
                <TableCell>{t('table.company')}</TableCell>
                <TableCell>{t('table.role')}</TableCell>
                <TableCell>{t('table.date')}</TableCell>
                <TableCell align="center">{t('table.sessions')}</TableCell>
                <TableCell align="center">{t('table.messages')}</TableCell>
                <TableCell>{t('table.contact')}</TableCell>
                <TableCell>{t('table.actions')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {visitorsData.content.map((visitor) => (
                <TableRow key={visitor.id} hover>
                  <TableCell>
                    <Link
                      component="button"
                      variant="body2"
                      onClick={() => handleOpenSessions(visitor)}
                      sx={{ textDecoration: 'none', fontWeight: 500 }}
                    >
                      {visitor.name}
                    </Link>
                  </TableCell>
                  <TableCell>{visitor.company || '-'}</TableCell>
                  <TableCell>{visitor.jobRole || '-'}</TableCell>
                  <TableCell>{new Date(visitor.createdAt).toLocaleDateString()}</TableCell>
                  <TableCell align="center">{visitor.sessionCount}</TableCell>
                  <TableCell align="center">{visitor.totalMessages}</TableCell>
                  <TableCell>
                    {visitor.isRevealed ? (
                      <Box sx={{ display: 'flex', gap: 0.5 }}>
                        {visitor.contactEmail && (
                          <IconButton size="small" href={`mailto:${visitor.contactEmail}`}>
                            <EmailIcon fontSize="small" />
                          </IconButton>
                        )}
                        {visitor.linkedinUrl && (
                          <IconButton size="small" href={visitor.linkedinUrl} target="_blank">
                            <LinkedInIcon fontSize="small" />
                          </IconButton>
                        )}
                        {visitor.contactWhatsapp && (
                          <IconButton size="small" href={`https://wa.me/${visitor.contactWhatsapp}`} target="_blank">
                            <WhatsAppIcon fontSize="small" />
                          </IconButton>
                        )}
                      </Box>
                    ) : (
                      <Chip icon={<LockIcon />} label={t('hidden')} size="small" variant="outlined" />
                    )}
                  </TableCell>
                  <TableCell>
                    {!visitor.isRevealed && (
                      <Button
                        size="small"
                        variant="outlined"
                        onClick={() => setConfirmRevealId(visitor.id)}
                      >
                        {t('reveal.button')}
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <TablePagination
            component="div"
            count={visitorsData.totalElements}
            page={page}
            onPageChange={(_, newPage) => setPage(newPage)}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={(e) => {
              setRowsPerPage(parseInt(e.target.value, 10));
              setPage(0);
            }}
          />
        </TableContainer>
      )}

      {/* Reveal Confirm Dialog */}
      <Dialog open={confirmRevealId !== null} onClose={() => { setConfirmRevealId(null); setRevealError(null); }}>
        <DialogTitle>{t('reveal.confirmTitle')}</DialogTitle>
        <DialogContent>
          <DialogContentText>{t('reveal.confirmMessage', { cost: 5 })}</DialogContentText>
          {revealError && (
            <Alert severity="error" sx={{ mt: 2 }}>{revealError}</Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setConfirmRevealId(null); setRevealError(null); }}>{t('common:buttons.cancel')}</Button>
          <Button
            variant="contained"
            onClick={() => confirmRevealId && revealMutation.mutate(confirmRevealId)}
            disabled={revealMutation.isPending}
          >
            {revealMutation.isPending ? <CircularProgress size={20} /> : t('reveal.button')}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Sessions Drawer */}
      <Drawer
        anchor="right"
        open={selectedVisitor !== null}
        onClose={() => { setSelectedVisitor(null); setChatLog(null); }}
        sx={{ '& .MuiDrawer-paper': { width: { xs: '100%', sm: 500 } } }}
      >
        {selectedVisitor && (
          <Box sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              {selectedVisitor.name} - {t('session.title')}
            </Typography>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              {selectedVisitor.company} {selectedVisitor.jobRole ? `/ ${selectedVisitor.jobRole}` : ''}
            </Typography>
            <Divider sx={{ my: 2 }} />

            {sessionsLoading ? (
              <CircularProgress />
            ) : (
              <List>
                {sessions.map((session) => (
                  <ListItemButton key={session.id} onClick={() => handleViewChat(session.id)}>
                    <ListItemText
                      primary={`${t('session.startedAt')}: ${new Date(session.startedAt).toLocaleString()}`}
                      secondary={`${t('session.messageCount')}: ${session.messageCount}`}
                    />
                  </ListItemButton>
                ))}
              </List>
            )}

            {/* Chat Transcript */}
            {chatLog !== null && (
              <>
                <Divider sx={{ my: 2 }} />
                <Typography variant="subtitle1" gutterBottom>{t('chatLog.title')}</Typography>
                {chatLoading ? (
                  <CircularProgress />
                ) : (
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    {chatLog.map((msg) => (
                      <Paper
                        key={msg.id}
                        elevation={1}
                        sx={{
                          p: 1.5,
                          bgcolor: msg.role === 'user' ? 'primary.50' : 'grey.50',
                          borderLeft: `3px solid ${msg.role === 'user' ? '#1976d2' : '#9c27b0'}`,
                        }}
                      >
                        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
                          {msg.role === 'user' ? t('chatLog.visitor') : t('chatLog.assistant')}
                        </Typography>
                        <Typography variant="body2" sx={{ mt: 0.5, whiteSpace: 'pre-wrap' }}>
                          {msg.content}
                        </Typography>
                        {msg.llmRequest && (
                          <Accordion
                            elevation={0}
                            sx={{ mt: 1, bgcolor: 'transparent', '&:before': { display: 'none' } }}
                          >
                            <AccordionSummary
                              expandIcon={<ExpandMoreIcon />}
                              sx={{ minHeight: 'auto', px: 0, '& .MuiAccordionSummary-content': { my: 0.5 } }}
                            >
                              <Typography variant="caption" color="text.secondary">
                                {t('chatLog.llmRequest')}
                              </Typography>
                            </AccordionSummary>
                            <AccordionDetails sx={{ px: 0, pt: 0 }}>
                              <Box
                                component="pre"
                                sx={{
                                  fontSize: '0.75rem',
                                  bgcolor: 'grey.100',
                                  p: 1,
                                  borderRadius: 1,
                                  overflow: 'auto',
                                  maxHeight: 300,
                                  whiteSpace: 'pre-wrap',
                                  wordBreak: 'break-word',
                                }}
                              >
                                {(() => {
                                  try {
                                    return JSON.stringify(JSON.parse(msg.llmRequest), null, 2);
                                  } catch {
                                    return msg.llmRequest;
                                  }
                                })()}
                              </Box>
                            </AccordionDetails>
                          </Accordion>
                        )}
                      </Paper>
                    ))}
                  </Box>
                )}
              </>
            )}
          </Box>
        )}
      </Drawer>
    </Container>
  );
};
