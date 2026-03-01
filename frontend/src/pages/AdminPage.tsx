import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import {
  Container,
  Typography,
  Box,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  CircularProgress,
  Alert,
  Collapse,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Divider,
  TablePagination,
  Card,
  CardContent,
  Grid,
  Skeleton,
  Link,
  Accordion,
  AccordionSummary,
  AccordionDetails,
} from '@mui/material';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import PeopleIcon from '@mui/icons-material/People';
import VisibilityIcon from '@mui/icons-material/Visibility';
import PersonIcon from '@mui/icons-material/Person';
import ChatIcon from '@mui/icons-material/Chat';
import { visitorApi } from '../api/visitorApi';
import type { VisitorResponse, VisitorSessionResponse, VisitorChatLogResponse, AccountResponse } from '../api/visitorApi';

interface StatCardProps {
  icon: React.ReactNode;
  value: number | string;
  label: string;
  loading: boolean;
}

function StatCard({ icon, value, label, loading }: StatCardProps) {
  return (
    <Card elevation={2} sx={{ height: '100%' }}>
      <CardContent sx={{ textAlign: 'center', py: 3 }}>
        {icon}
        {loading ? (
          <Skeleton
            variant="text"
            width={60}
            sx={{ mx: 'auto', fontSize: '2.5rem' }}
          />
        ) : (
          <Typography variant="h3" sx={{ mt: 1 }}>
            {value}
          </Typography>
        )}
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          {label}
        </Typography>
      </CardContent>
    </Card>
  );
}

function AdminVisitorRow({ visitor }: { visitor: VisitorResponse }) {
  const { t } = useTranslation('visitors');
  const [open, setOpen] = useState(false);
  const [sessions, setSessions] = useState<VisitorSessionResponse[]>([]);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [chatLog, setChatLog] = useState<Record<number, VisitorChatLogResponse[]>>({});
  const [chatLogOpen, setChatLogOpen] = useState<number | null>(null);

  const handleExpand = async () => {
    if (!open && sessions.length === 0) {
      setSessionsLoading(true);
      try {
        const data = await visitorApi.adminGetVisitorSessions(visitor.id);
        setSessions(data);
      } finally {
        setSessionsLoading(false);
      }
    }
    setOpen(!open);
  };

  const handleViewChat = async (sessionId: number) => {
    if (chatLogOpen === sessionId) {
      setChatLogOpen(null);
      return;
    }
    if (!chatLog[sessionId]) {
      const data = await visitorApi.adminGetSessionMessages(sessionId);
      setChatLog((prev) => ({ ...prev, [sessionId]: data }));
    }
    setChatLogOpen(sessionId);
  };

  return (
    <>
      <TableRow hover>
        <TableCell>
          <IconButton size="small" onClick={handleExpand}>
            {open ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
          </IconButton>
        </TableCell>
        <TableCell>{visitor.name}</TableCell>
        <TableCell>{visitor.company || '-'}</TableCell>
        <TableCell>{visitor.jobRole || '-'}</TableCell>
        <TableCell>{new Date(visitor.createdAt).toLocaleDateString()}</TableCell>
        <TableCell align="center">{visitor.sessionCount}</TableCell>
        <TableCell align="center">{visitor.totalMessages}</TableCell>
      </TableRow>
      <TableRow>
        <TableCell colSpan={7} sx={{ py: 0 }}>
          <Collapse in={open} timeout="auto" unmountOnExit>
            <Box sx={{ p: 2 }}>
              <Typography variant="subtitle2" gutterBottom>{t('session.title')}</Typography>
              {sessionsLoading ? (
                <CircularProgress size={20} />
              ) : (
                <List dense>
                  {sessions.map((session) => (
                    <Box key={session.id}>
                      <ListItemButton onClick={() => handleViewChat(session.id)}>
                        <ListItemText
                          primary={`${t('session.startedAt')}: ${new Date(session.startedAt).toLocaleString()}`}
                          secondary={`${t('session.messageCount')}: ${session.messageCount}`}
                        />
                      </ListItemButton>
                      <Collapse in={chatLogOpen === session.id} timeout="auto" unmountOnExit>
                        <Box sx={{ pl: 4, pr: 2, pb: 2, display: 'flex', flexDirection: 'column', gap: 1 }}>
                          {chatLog[session.id]?.map((msg) => (
                            <Paper
                              key={msg.id}
                              elevation={0}
                              sx={{
                                p: 1,
                                bgcolor: msg.role === 'user' ? 'primary.50' : 'grey.100',
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
                                        bgcolor: 'grey.200',
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
                      </Collapse>
                      <Divider />
                    </Box>
                  ))}
                </List>
              )}
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>
    </>
  );
}

function AccountsTable({ accounts, loading }: { accounts: AccountResponse[] | undefined; loading: boolean }) {
  const { t } = useTranslation('admin');

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!accounts?.length) {
    return <Alert severity="info">{t('accounts.noAccounts')}</Alert>;
  }

  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>{t('accounts.name')}</TableCell>
            <TableCell>{t('accounts.email')}</TableCell>
            <TableCell>{t('accounts.plan')}</TableCell>
            <TableCell>{t('accounts.publicProfile')}</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {accounts.map((account) => (
            <TableRow key={account.id} hover>
              <TableCell>{account.fullName}</TableCell>
              <TableCell>{account.email}</TableCell>
              <TableCell>{account.role}</TableCell>
              <TableCell>
                {account.publicProfileUrl ? (
                  <Link href={account.publicProfileUrl} target="_blank" rel="noopener noreferrer">
                    {account.publicProfileUrl}
                  </Link>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    {t('accounts.noProfile')}
                  </Typography>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

export const AdminPage = () => {
  const { t } = useTranslation('admin');
  const { t: tv } = useTranslation('visitors');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);

  const { data: globalStats, isLoading: statsLoading } = useQuery({
    queryKey: ['adminGlobalStats'],
    queryFn: visitorApi.adminGetGlobalStats,
  });

  const { data: accounts, isLoading: accountsLoading } = useQuery({
    queryKey: ['adminAccounts'],
    queryFn: visitorApi.adminGetAccounts,
  });

  const { data, isLoading, error } = useQuery({
    queryKey: ['adminVisitors', page, rowsPerPage],
    queryFn: () => visitorApi.adminGetVisitors(page, rowsPerPage),
  });

  const stats = [
    {
      icon: <PeopleIcon sx={{ fontSize: 40, color: '#1976d2' }} />,
      value: globalStats?.totalAccounts ?? 0,
      label: t('stats.totalAccounts'),
      loading: statsLoading,
    },
    {
      icon: <VisibilityIcon sx={{ fontSize: 40, color: '#2e7d32' }} />,
      value: globalStats?.totalProfileViews ?? 0,
      label: t('stats.totalProfileViews'),
      loading: statsLoading,
    },
    {
      icon: <PersonIcon sx={{ fontSize: 40, color: '#e65100' }} />,
      value: globalStats?.totalVisitors ?? 0,
      label: t('stats.totalVisitors'),
      loading: statsLoading,
    },
    {
      icon: <ChatIcon sx={{ fontSize: 40, color: '#9c27b0' }} />,
      value: globalStats?.totalInterviews ?? 0,
      label: t('stats.totalInterviews'),
      loading: statsLoading,
    },
  ];

  if (error) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <Alert severity="error">{String(error)}</Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Typography variant="h4" gutterBottom>{t('title')}</Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>{t('subtitle')}</Typography>

      {/* Global Stats Cards */}
      <Grid container spacing={2} sx={{ mb: 4 }}>
        {stats.map((stat) => (
          <Grid item xs={6} sm={3} key={stat.label}>
            <StatCard
              icon={stat.icon}
              value={stat.value}
              label={stat.label}
              loading={stat.loading}
            />
          </Grid>
        ))}
      </Grid>

      {/* Accounts List */}
      <Typography variant="h6" gutterBottom>{t('accounts.title')}</Typography>
      <Box sx={{ mb: 4 }}>
        <AccountsTable accounts={accounts} loading={accountsLoading} />
      </Box>

      {/* Visitors Section */}
      <Typography variant="h6" gutterBottom>{t('visitors.title')}</Typography>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      ) : !data?.content?.length ? (
        <Alert severity="info">{t('visitors.noData')}</Alert>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell width={50} />
                <TableCell>{tv('table.name')}</TableCell>
                <TableCell>{tv('table.company')}</TableCell>
                <TableCell>{tv('table.role')}</TableCell>
                <TableCell>{tv('table.date')}</TableCell>
                <TableCell align="center">{tv('table.sessions')}</TableCell>
                <TableCell align="center">{tv('table.messages')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {data.content.map((visitor) => (
                <AdminVisitorRow key={visitor.id} visitor={visitor} />
              ))}
            </TableBody>
          </Table>
          <TablePagination
            component="div"
            count={data.totalElements}
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
    </Container>
  );
};
