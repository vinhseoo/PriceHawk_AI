import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/apiClient';
import type { Product, SellerListing, ProductSpecs, Review } from '@/types';

export function useProductDetail(slug: string) {
  return useQuery({
    queryKey: ['product', slug],
    queryFn: () => apiClient.get<Product>(`/api/v1/products/slug/${slug}`),
    enabled: !!slug,
    staleTime: 1000 * 60 * 5,
  });
}

export function useSellerListings(productId: string) {
  return useQuery({
    queryKey: ['seller-listings', productId],
    queryFn: () => apiClient.get<SellerListing[]>(`/api/v1/products/${productId}/listings`),
    enabled: !!productId,
  });
}

export function useProductSpecs(productId: string) {
  return useQuery({
    queryKey: ['product-specs', productId],
    queryFn: () => apiClient.get<ProductSpecs>(`/api/v1/products/${productId}/specs`),
    enabled: !!productId,
  });
}

export function useProductReviews(productId: string) {
  return useQuery({
    queryKey: ['product-reviews', productId],
    queryFn: () => apiClient.get<Review[]>(`/api/v1/products/${productId}/reviews`),
    enabled: !!productId,
  });
}
