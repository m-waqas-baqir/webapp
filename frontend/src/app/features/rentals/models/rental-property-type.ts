/** Mirrors backend {@code RentalPropertyType}. */
export enum RentalPropertyType {
  RESIDENTIAL = 'RESIDENTIAL',
  COMMERCIAL = 'COMMERCIAL',
}

export const RENTAL_TYPE_OPTIONS: readonly RentalPropertyType[] = [
  RentalPropertyType.RESIDENTIAL,
  RentalPropertyType.COMMERCIAL,
] as const;
