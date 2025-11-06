#!/usr/bin/env node

import fs from 'fs'
import path from 'path'

// Read the template config file
const configPath = path.join(process.cwd(), 'public', 'config.json')
const outputPath = path.join(process.cwd(), 'public', 'config.json')

try {
  let configContent = fs.readFileSync(configPath, 'utf8')
  
  // Replace placeholders with environment variables or defaults
  const replacements = {
    '${API_BASE_URL:http://localhost:8080}': process.env.API_BASE_URL || process.env.VITE_API_BASE_URL || 'http://localhost:8080',
    '${PROJECT_VERSION:4.1.1}': process.env.PROJECT_VERSION || process.env.VITE_APP_VERSION || '4.1.1',
    '${BUILD_TIME:unknown}': new Date().toISOString()
  }
  
  // Apply replacements
  for (const [placeholder, value] of Object.entries(replacements)) {
    configContent = configContent.replace(placeholder, value)
  }
  
  // Write the processed config
  fs.writeFileSync(outputPath, configContent)
  
  console.log('Configuration processed successfully')
  console.log('API Base URL:', replacements['${API_BASE_URL:http://localhost:8080}'])
  console.log('Version:', replacements['${PROJECT_VERSION:4.1.1}'])
  
} catch (error) {
  console.error('Error processing configuration:', error)
  process.exit(1)
}