import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom'
import { Box, Container, AppBar, Toolbar, Typography, Button } from '@mui/material'
import { Toaster } from 'sonner'
import { Dashboard } from '@features/dashboard'
import { Monitoring } from '@features/monitoring'
import { Messaging } from '@features/messaging'
import { Resources } from '@features/resources'
import { Schemas } from '@features/schemas'
import { Auth } from '@features/auth'
import { Settings } from '@features/settings'
import './App.css'

function App() {
  return (
    <Router>
      <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
        <AppBar position="static">
          <Toolbar>
            <Typography variant="h6" sx={{ flexGrow: 1 }}>
              MAPS Admin UI
            </Typography>
            <nav style={{ display: 'flex', gap: '10px' }}>
              <Button color="inherit" component={Link} to="/">
                Dashboard
              </Button>
              <Button color="inherit" component={Link} to="/monitoring">
                Monitoring
              </Button>
              <Button color="inherit" component={Link} to="/messaging">
                Messaging
              </Button>
              <Button color="inherit" component={Link} to="/resources">
                Resources
              </Button>
              <Button color="inherit" component={Link} to="/schemas">
                Schemas
              </Button>
              <Button color="inherit" component={Link} to="/auth">
                Auth
              </Button>
              <Button color="inherit" component={Link} to="/settings">
                Settings
              </Button>
            </nav>
          </Toolbar>
        </AppBar>

        <Container component="main" sx={{ flex: 1, py: 4 }}>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/monitoring" element={<Monitoring />} />
            <Route path="/messaging" element={<Messaging />} />
            <Route path="/resources" element={<Resources />} />
            <Route path="/schemas" element={<Schemas />} />
            <Route path="/auth" element={<Auth />} />
            <Route path="/settings" element={<Settings />} />
          </Routes>
        </Container>
      </Box>
      <Toaster position="top-right" />
    </Router>
  )
}

export default App
