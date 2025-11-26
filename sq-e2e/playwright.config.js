module.exports = {
  testDir: './tests',
  use: {
    baseURL: 'http://localhost:3000',
    headless: true,
  },
  webServer: {
    command: 'echo "Frontend should be running on http://localhost:3000"',
    port: 3000,
    reuseExistingServer: true,
  },
};

