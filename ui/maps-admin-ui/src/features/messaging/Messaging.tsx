import { Box, Typography, Paper } from '@mui/material'

export function Messaging() {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Messaging
      </Typography>
      <Paper sx={{ p: 3 }}>
        <Typography>Messaging configuration and management interface</Typography>
      </Paper>
    </Box>
  )
}
