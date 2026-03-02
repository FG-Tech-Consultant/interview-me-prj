import { useTranslation } from 'react-i18next';
import {
  Box,
  ToggleButtonGroup,
  ToggleButton,
  Typography,
} from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import FolderZipIcon from '@mui/icons-material/FolderZip';
import PersonIcon from '@mui/icons-material/Person';
import type { AnalysisSourceType } from '../../types/linkedinAnalysis';

interface SourceSelectorProps {
  value: AnalysisSourceType;
  onChange: (value: AnalysisSourceType) => void;
  disabled?: boolean;
}

export default function SourceSelector({ value, onChange, disabled }: SourceSelectorProps) {
  const { t } = useTranslation('linkedin');

  const handleChange = (_: React.MouseEvent<HTMLElement>, newValue: AnalysisSourceType | null) => {
    if (newValue !== null) {
      onChange(newValue);
    }
  };

  return (
    <Box sx={{ mb: 3 }}>
      <Typography variant="subtitle1" sx={{ mb: 1.5, fontWeight: 500 }}>
        {t('sourceSelector.title')}
      </Typography>
      <ToggleButtonGroup
        value={value}
        exclusive
        onChange={handleChange}
        aria-label={t('sourceSelector.title')}
        disabled={disabled}
        sx={{
          width: '100%',
          '& .MuiToggleButton-root': {
            flex: 1,
            py: 1.5,
            textTransform: 'none',
            flexDirection: 'column',
            gap: 0.5,
          },
        }}
      >
        <ToggleButton value="PDF">
          <PictureAsPdfIcon />
          <Typography variant="body2">{t('sourceSelector.pdf')}</Typography>
        </ToggleButton>
        <ToggleButton value="ZIP">
          <FolderZipIcon />
          <Typography variant="body2">{t('sourceSelector.zip')}</Typography>
        </ToggleButton>
        <ToggleButton value="PROFILE">
          <PersonIcon />
          <Typography variant="body2">{t('sourceSelector.profile')}</Typography>
        </ToggleButton>
      </ToggleButtonGroup>
      <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
        {value === 'PDF' && t('sourceSelector.pdfDescription')}
        {value === 'ZIP' && t('sourceSelector.zipDescription')}
        {value === 'PROFILE' && t('sourceSelector.profileDescription')}
      </Typography>
    </Box>
  );
}
