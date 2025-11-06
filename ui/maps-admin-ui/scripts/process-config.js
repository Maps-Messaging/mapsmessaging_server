import fs from 'fs'
import path from 'path'

const configPath = path.join(process.cwd(), 'public', 'config.json')

const config = {
  title: process.env.APP_TITLE || 'Maps Messaging Admin',
  version: process.env.APP_VERSION || '1.0.0',
  buildTime: new Date().toISOString(),
  apiEndpoint: process.env.API_ENDPOINT || '/api/v1',
  environment: process.env.NODE_ENV || 'development',
}

fs.writeFileSync(configPath, JSON.stringify(config, null, 2))
console.log('Config written to', configPath)
