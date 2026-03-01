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
} from '@mui/material';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import { visitorApi } from '../api/visitorApi';
import type { VisitorResponse, VisitorSessionResponse, VisitorChatLogResponse } from '../api/visitorApi';

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

export const AdminPage = () => {
  const { t } = useTranslation('admin');
  const { t: tv } = useTranslation('visitors');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);

  const { data, isLoading, error } = useQuery({
    queryKey: ['adminVisitors', page, rowsPerPage],
    queryFn: () => visitorApi.adminGetVisitors(page, rowsPerPage),
  });

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
