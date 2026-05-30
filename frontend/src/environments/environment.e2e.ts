/**
 * Used only by `ng serve --configuration e2e` (Playwright webServer) so E2E can target a dedicated
 * mock API port without colliding with a local Spring Boot instance on :8080.
 */
export const environment = {
  production: false,
  appVersion: '0.1.0-e2e',
  apiBaseUrl: 'http://127.0.0.1:18080',
};
