import { Box, Typography, Link, Divider } from '@mui/material';
import { useAppInfo } from '../../hooks/useAppInfo';

const GITHUB_URL = 'https://github.com/fhgomes';

export default function PoweredByFooter() {
  const { data: appInfo } = useAppInfo();
  const version = appInfo?.build?.version;

  return (
    <>
      <Divider sx={{ mt: 4, mb: 2 }} />
      <Box sx={{ textAlign: 'center', py: 2 }}>
        <Typography variant="caption" color="text.secondary">
          powered by{' '}
          <Link href={GITHUB_URL} color="primary" underline="hover" target="_blank" rel="noopener">
            fhgomes
          </Link>
          {' '}tech consultant
          {version && ` · v${version}`}
        </Typography>
      </Box>
    </>
  );
}
