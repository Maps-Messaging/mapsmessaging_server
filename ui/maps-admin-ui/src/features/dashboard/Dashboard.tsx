import { Box, Typography, Paper, Grid } from '@mui/material'

export function Dashboard() {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>
      <Grid container spacing={3}>
        <Grid item xs={12} sm={6} md={3}>
          <Paper sx={{ p: 2 }}>
            <Typography color="textSecondary" gutterBottom>
              Active Connections
            </Typography>
            <Typography variant="h5">0</Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Paper sx={{ p: 2 }}>
            <Typography color="textSecondary" gutterBottom>
              Messages Processed
            </Typography>
            <Typography variant="h5">0</Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Paper sx={{ p: 2 }}>
            <Typography color="textSecondary" gutterBottom>
              System Uptime
            </Typography>
            <Typography variant="h5">0s</Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Paper sx={{ p: 2 }}>
            <Typography color="textSecondary" gutterBottom>
              Error Rate
            </Typography>
            <Typography variant="h5">0%</Typography>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  )
}
