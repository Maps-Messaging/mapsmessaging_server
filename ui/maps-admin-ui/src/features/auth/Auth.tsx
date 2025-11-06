import { Box, Typography, Paper } from '@mui/material'

export function Auth() {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Authentication & Authorization
      </Typography>
      <Paper sx={{ p: 3 }}>
        <Typography>Authentication and authorization management interface</Typography>
      </Paper>
    </Box>
  )
}
