import { Typography, TypographyProps } from '@mui/material';
import { useAppInfo } from '../../hooks/useAppInfo';

interface AppVersionProps {
  prefix?: string;
  sx?: TypographyProps['sx'];
}

export default function AppVersion({ prefix = '', sx }: AppVersionProps) {
  const { data: appInfo } = useAppInfo();
  const version = appInfo?.build?.version;

  if (!version) return null;

  return (
    <Typography variant="caption" color="text.secondary" sx={sx}>
      {prefix}v{version}
    </Typography>
  );
}
