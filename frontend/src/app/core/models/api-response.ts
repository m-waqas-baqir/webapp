/** Matches backend {@code ApiResponse<T>}. */
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}
