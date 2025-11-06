import { Box, Typography, Paper } from '@mui/material'

export function Settings() {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Settings
      </Typography>
      <Paper sx={{ p: 3 }}>
        <Typography>System settings and configuration interface</Typography>
      </Paper>
    </Box>
  )
}
