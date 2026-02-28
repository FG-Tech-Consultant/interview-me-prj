import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Container,
  Typography,
  Box,
  Grid,
  Paper,
  CircularProgress,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { useWallet, useTransactions, useQuotaStatus } from '../hooks/useBilling';
import { TransactionHistoryTable } from '../components/billing/TransactionHistoryTable';
import { QuotaStatusCard } from '../components/billing/QuotaStatusCard';
import type { TransactionType } from '../types/billing';

export const BillingPage = () => {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [typeFilter, setTypeFilter] = useState<TransactionType | ''>('');
  const { t } = useTranslation('billing');

  const { data: wallet, isLoading: walletLoading, error: walletError } = useWallet();
  const { data: transactions, isLoading: txLoading } = useTransactions({
    page,
    size: rowsPerPage,
    type: typeFilter || undefined,
  });
  const { data: chatQuota } = useQuotaStatus('CHAT_MESSAGE');
  const { data: linkedinQuota } = useQuotaStatus('LINKEDIN_DRAFT');

  if (walletLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (walletError) {
    return (
      <Container maxWidth="md" sx={{ mt: 4 }}>
        <Alert severity="error">{t('failedToLoad')}</Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Typography variant="h4" gutterBottom>
        {t('title')}
      </Typography>

      {/* Balance */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="subtitle2" color="text.secondary">
          {t('currentBalance')}
        </Typography>
        <Typography variant="h3">{t('coinsAmount', { amount: wallet?.balance ?? 0 })}</Typography>
      </Paper>

      {/* Free Tier Quotas */}
      <Typography variant="h6" gutterBottom>
        {t('freeTierQuotas')}
      </Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {chatQuota && (
          <Grid item xs={12} sm={6}>
            <QuotaStatusCard
              featureType={chatQuota.featureType}
              used={chatQuota.used}
              limit={chatQuota.limit}
            />
          </Grid>
        )}
        {linkedinQuota && (
          <Grid item xs={12} sm={6}>
            <QuotaStatusCard
              featureType={linkedinQuota.featureType}
              used={linkedinQuota.used}
              limit={linkedinQuota.limit}
            />
          </Grid>
        )}
      </Grid>

      {/* Transaction History */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">{t('transactionHistory')}</Typography>
        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>{t('filterType')}</InputLabel>
          <Select
            value={typeFilter}
            label={t('filterType')}
            onChange={(e) => {
              setTypeFilter(e.target.value as TransactionType | '');
              setPage(0);
            }}
          >
            <MenuItem value="">{t('filterAll')}</MenuItem>
            <MenuItem value="EARN">{t('filterEarn')}</MenuItem>
            <MenuItem value="SPEND">{t('filterSpend')}</MenuItem>
            <MenuItem value="REFUND">{t('filterRefund')}</MenuItem>
            <MenuItem value="PURCHASE">{t('filterPurchase')}</MenuItem>
          </Select>
        </FormControl>
      </Box>

      {txLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress size={24} />
        </Box>
      ) : (
        <TransactionHistoryTable
          transactions={transactions?.content ?? []}
          page={page}
          totalElements={transactions?.totalElements ?? 0}
          rowsPerPage={rowsPerPage}
          onPageChange={setPage}
          onRowsPerPageChange={(size) => {
            setRowsPerPage(size);
            setPage(0);
          }}
        />
      )}
    </Container>
  );
};
