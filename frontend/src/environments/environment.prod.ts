/**
 * Production build (`ng build --configuration=production`).
 * Static files are served by Nginx; API is usually reverse-proxied on the same origin under `/api`.
 */
export const environment = {
  production: true,
  appVersion: '0.1.0',
  /** Same-origin: browser calls `/api/...` relative to the site origin. */
  apiBaseUrl: '',
};
