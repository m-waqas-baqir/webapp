/**
 * Approximate Phase 8 locality footprint for visual UX only (not cadastral survey data).
 * GeoJSON used locally — no API or database.
 */
export const DHA_PHASE8_BOUNDARY_FEATURE: GeoJSON.Feature<GeoJSON.Polygon> = {
  type: 'Feature',
  properties: { label: 'DHA Phase 8 (approx.)' },
  geometry: {
    type: 'Polygon',
    coordinates: [
      [
        [67.048, 24.792],
        [67.098, 24.792],
        [67.104, 24.822],
        [67.092, 24.842],
        [67.062, 24.848],
        [67.038, 24.824],
        [67.038, 24.804],
        [67.048, 24.792],
      ],
    ],
  },
};
