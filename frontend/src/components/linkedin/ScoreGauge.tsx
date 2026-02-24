import { Box, CircularProgress, Typography } from '@mui/material';

interface ScoreGaugeProps {
  score: number;
  size?: 'small' | 'large';
}

function getScoreColor(score: number): string {
  if (score < 40) return '#f44336'; // red
  if (score < 60) return '#ff9800'; // orange
  if (score < 80) return '#2196f3'; // blue
  return '#4caf50'; // green
}

export default function ScoreGauge({ score, size = 'large' }: ScoreGaugeProps) {
  const dimensions = size === 'large' ? 120 : 60;
  const fontSize = size === 'large' ? 'h4' : 'body1';
  const color = getScoreColor(score);

  return (
    <Box sx={{ position: 'relative', display: 'inline-flex' }}>
      {/* Background circle */}
      <CircularProgress
        variant="determinate"
        value={100}
        size={dimensions}
        thickness={4}
        sx={{ color: 'grey.200', position: 'absolute' }}
      />
      {/* Score circle */}
      <CircularProgress
        variant="determinate"
        value={score}
        size={dimensions}
        thickness={4}
        sx={{ color }}
      />
      <Box
        sx={{
          top: 0,
          left: 0,
          bottom: 0,
          right: 0,
          position: 'absolute',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Typography variant={fontSize} component="span" fontWeight="bold" sx={{ color }}>
          {score}
        </Typography>
      </Box>
    </Box>
  );
}
