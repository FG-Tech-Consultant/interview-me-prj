import { useState } from 'react';
import {
  Box,
  Typography,
  TextField,
  Button,
  Paper,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Chip,
  IconButton,
  Tooltip,
  Alert,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Snackbar,
  Divider,
} from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import DeleteIcon from '@mui/icons-material/Delete';
import SendIcon from '@mui/icons-material/Send';
import ArchiveIcon from '@mui/icons-material/Archive';
import {
  useLinkedInDrafts,
  useCreateLinkedInDraft,
  useDeleteLinkedInDraft,
  useUpdateDraftStatus,
} from '../hooks/useLinkedInDrafts';
import type { DraftResponse, DraftCategory, DraftStatus } from '../types/linkedinDraft';
import { TONE_OPTIONS, CATEGORY_COLORS, STATUS_COLORS } from '../types/linkedinDraft';

export function LinkedInInboxPage() {
  const [message, setMessage] = useState('');
  const [tone, setTone] = useState('professional');
  const [page, setPage] = useState(0);
  const [snackbar, setSnackbar] = useState<string | null>(null);
  const [selectedDraft, setSelectedDraft] = useState<DraftResponse | null>(null);

  const { data: draftsPage, isLoading: loadingDrafts } = useLinkedInDrafts(page);
  const createDraft = useCreateLinkedInDraft();
  const deleteDraft = useDeleteLinkedInDraft();
  const updateStatus = useUpdateDraftStatus();

  const handleGenerate = () => {
    if (!message.trim()) return;
    createDraft.mutate(
      { originalMessage: message.trim(), tone },
      {
        onSuccess: (draft) => {
          setSelectedDraft(draft);
          setMessage('');
          setSnackbar('Draft generated successfully');
        },
      }
    );
  };

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    setSnackbar('Copied to clipboard');
  };

  const handleDelete = (id: number) => {
    deleteDraft.mutate(id, {
      onSuccess: () => {
        if (selectedDraft?.id === id) setSelectedDraft(null);
        setSnackbar('Draft deleted');
      },
    });
  };

  const handleStatusUpdate = (id: number, status: 'SENT' | 'ARCHIVED') => {
    updateStatus.mutate(
      { id, request: { status } },
      {
        onSuccess: (updated) => {
          if (selectedDraft?.id === id) setSelectedDraft(updated);
          setSnackbar(`Draft marked as ${status.toLowerCase()}`);
        },
      }
    );
  };

  const getCategoryChip = (category: DraftCategory) => (
    <Chip
      label={category}
      size="small"
      sx={{ bgcolor: CATEGORY_COLORS[category], color: '#fff', fontWeight: 600 }}
    />
  );

  const getStatusChip = (status: DraftStatus) => (
    <Chip label={status} size="small" color={STATUS_COLORS[status]} variant="outlined" />
  );

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <Box sx={{ p: 3, maxWidth: 1200, mx: 'auto' }}>
      <Typography variant="h4" gutterBottom>
        LinkedIn Inbox Assistant
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Paste a LinkedIn message to get a categorized draft reply. Free tier: 10 drafts/month.
      </Typography>

      {/* Compose Section */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          Generate Reply
        </Typography>
        <TextField
          fullWidth
          multiline
          rows={5}
          placeholder="Paste a LinkedIn message here..."
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          sx={{ mb: 2 }}
          inputProps={{ maxLength: 5000 }}
        />
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <FormControl size="small" sx={{ minWidth: 160 }}>
            <InputLabel>Tone</InputLabel>
            <Select value={tone} label="Tone" onChange={(e) => setTone(e.target.value)}>
              {TONE_OPTIONS.map((opt) => (
                <MenuItem key={opt.value} value={opt.value}>
                  {opt.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button
            variant="contained"
            onClick={handleGenerate}
            disabled={!message.trim() || createDraft.isPending}
            startIcon={createDraft.isPending ? <CircularProgress size={16} /> : undefined}
          >
            {createDraft.isPending ? 'Generating...' : 'Generate Reply'}
          </Button>
        </Box>
        {createDraft.isError && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {(createDraft.error as any)?.response?.data?.message || 'Failed to generate draft. Please try again.'}
          </Alert>
        )}
      </Paper>

      {/* Selected Draft Result */}
      {selectedDraft && (
        <Paper sx={{ p: 3, mb: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
              <Typography variant="h6">Generated Draft</Typography>
              {getCategoryChip(selectedDraft.category)}
              {getStatusChip(selectedDraft.status)}
            </Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Tooltip title="Copy reply">
                <IconButton onClick={() => handleCopy(selectedDraft.suggestedReply)} color="primary">
                  <ContentCopyIcon />
                </IconButton>
              </Tooltip>
              <Tooltip title="Mark as sent">
                <IconButton
                  onClick={() => handleStatusUpdate(selectedDraft.id, 'SENT')}
                  color="success"
                  disabled={selectedDraft.status !== 'DRAFT'}
                >
                  <SendIcon />
                </IconButton>
              </Tooltip>
              <Tooltip title="Archive">
                <IconButton
                  onClick={() => handleStatusUpdate(selectedDraft.id, 'ARCHIVED')}
                  color="info"
                  disabled={selectedDraft.status === 'ARCHIVED'}
                >
                  <ArchiveIcon />
                </IconButton>
              </Tooltip>
            </Box>
          </Box>
          <Typography variant="subtitle2" color="text.secondary" gutterBottom>
            Original Message:
          </Typography>
          <Typography variant="body2" sx={{ mb: 2, whiteSpace: 'pre-wrap', bgcolor: 'grey.50', p: 1.5, borderRadius: 1 }}>
            {selectedDraft.originalMessage}
          </Typography>
          <Divider sx={{ my: 2 }} />
          <Typography variant="subtitle2" color="text.secondary" gutterBottom>
            Suggested Reply ({selectedDraft.tone}):
          </Typography>
          <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap', bgcolor: 'primary.50', p: 1.5, borderRadius: 1, border: '1px solid', borderColor: 'primary.200' }}>
            {selectedDraft.suggestedReply}
          </Typography>
        </Paper>
      )}

      {/* Draft History */}
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Draft History
        </Typography>
        {loadingDrafts ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
            <CircularProgress />
          </Box>
        ) : draftsPage && draftsPage.content.length > 0 ? (
          <>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell>Category</TableCell>
                    <TableCell>Message Preview</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {draftsPage.content.map((draft) => (
                    <TableRow
                      key={draft.id}
                      hover
                      sx={{ cursor: 'pointer' }}
                      onClick={() => setSelectedDraft(draft)}
                      selected={selectedDraft?.id === draft.id}
                    >
                      <TableCell sx={{ whiteSpace: 'nowrap' }}>
                        {formatDate(draft.createdAt)}
                      </TableCell>
                      <TableCell>{getCategoryChip(draft.category)}</TableCell>
                      <TableCell sx={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {draft.originalMessage}
                      </TableCell>
                      <TableCell>{getStatusChip(draft.status)}</TableCell>
                      <TableCell align="right">
                        <Tooltip title="Copy reply">
                          <IconButton
                            size="small"
                            onClick={(e) => {
                              e.stopPropagation();
                              handleCopy(draft.suggestedReply);
                            }}
                          >
                            <ContentCopyIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton
                            size="small"
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDelete(draft.id);
                            }}
                            color="error"
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            <TablePagination
              component="div"
              count={draftsPage.totalElements}
              page={page}
              onPageChange={(_, newPage) => setPage(newPage)}
              rowsPerPage={20}
              rowsPerPageOptions={[20]}
            />
          </>
        ) : (
          <Typography color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
            No drafts yet. Paste a LinkedIn message above to get started.
          </Typography>
        )}
      </Paper>

      <Snackbar
        open={!!snackbar}
        autoHideDuration={3000}
        onClose={() => setSnackbar(null)}
        message={snackbar}
      />
    </Box>
  );
}
