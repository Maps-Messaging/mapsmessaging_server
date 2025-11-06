import { Box, Typography, Paper } from '@mui/material'

export function Monitoring() {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Monitoring
      </Typography>
      <Paper sx={{ p: 3 }}>
        <Typography>Monitoring dashboard - charts and metrics will appear here</Typography>
      </Paper>
    </Box>
  )
}
