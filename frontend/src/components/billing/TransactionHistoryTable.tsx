import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  TablePagination,
  Typography,
} from '@mui/material';
import { TransactionTypeBadge } from './TransactionTypeBadge';
import type { TransactionResponse, TransactionType } from '../../types/billing';

interface TransactionHistoryTableProps {
  transactions: TransactionResponse[];
  page: number;
  totalElements: number;
  rowsPerPage: number;
  onPageChange: (page: number) => void;
  onRowsPerPageChange: (size: number) => void;
}

export const TransactionHistoryTable = ({
  transactions,
  page,
  totalElements,
  rowsPerPage,
  onPageChange,
  onRowsPerPageChange,
}: TransactionHistoryTableProps) => {
  return (
    <TableContainer component={Paper} variant="outlined">
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Date</TableCell>
            <TableCell>Type</TableCell>
            <TableCell>Description</TableCell>
            <TableCell align="right">Amount</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {transactions.length === 0 ? (
            <TableRow>
              <TableCell colSpan={4} align="center">
                <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
                  No transactions yet
                </Typography>
              </TableCell>
            </TableRow>
          ) : (
            transactions.map((tx) => (
              <TableRow key={tx.id}>
                <TableCell>
                  {new Date(tx.createdAt).toLocaleDateString()}
                </TableCell>
                <TableCell>
                  <TransactionTypeBadge type={tx.type as TransactionType} />
                </TableCell>
                <TableCell>{tx.description || '-'}</TableCell>
                <TableCell
                  align="right"
                  sx={{
                    color: tx.amount > 0 ? 'success.main' : 'error.main',
                    fontWeight: 'bold',
                  }}
                >
                  {tx.amount > 0 ? '+' : ''}{tx.amount}
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
      {totalElements > 0 && (
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          rowsPerPage={rowsPerPage}
          onPageChange={(_, newPage) => onPageChange(newPage)}
          onRowsPerPageChange={(e) => onRowsPerPageChange(parseInt(e.target.value, 10))}
          rowsPerPageOptions={[10, 20, 50]}
        />
      )}
    </TableContainer>
  );
};
