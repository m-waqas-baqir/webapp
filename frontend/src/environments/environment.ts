/**
 * Local development (`ng serve`, default port 4200).
 * Override by changing this file or adding a new environment + `fileReplacements` in `angular.json`.
 */
export const environment = {
  production: false,
  /** Shown on marketing footer and dashboard. */
  appVersion: '0.1.0',
  /**
   * Base URL for the Spring Boot API. Used to prefix requests that start with `/api`.
   * Dev: backend on port 8080. Prod build: see `environment.prod.ts` (typically same-origin behind Nginx).
   */
  apiBaseUrl: 'http://localhost:8080',
};
