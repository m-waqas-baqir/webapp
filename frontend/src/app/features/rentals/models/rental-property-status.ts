/** Mirrors backend {@code RentalPropertyStatus}. */
export enum RentalPropertyStatus {
  AVAILABLE = 'AVAILABLE',
  RENTED = 'RENTED',
  PENDING = 'PENDING',
  OFF_MARKET = 'OFF_MARKET',
}

export const RENTAL_STATUS_OPTIONS: readonly RentalPropertyStatus[] = [
  RentalPropertyStatus.AVAILABLE,
  RentalPropertyStatus.RENTED,
  RentalPropertyStatus.PENDING,
  RentalPropertyStatus.OFF_MARKET,
] as const;
