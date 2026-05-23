'use client';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/apiClient';
import { useAuthStore } from '@/stores/authStore';
import type { Wishlist, WishlistItem } from '@/types';

interface AddWishlistItemRequest {
  productId: string;
  targetPrice?: number;
}

const WISHLIST_QUERY_KEY = ['wishlist', 'me'] as const;

export function useWishlist() {
  const { isAuthenticated } = useAuthStore();

  return useQuery<Wishlist>({
    queryKey: WISHLIST_QUERY_KEY,
    queryFn: () => apiClient.get<Wishlist>('/api/v1/users/me/wishlist'),
    enabled: isAuthenticated,
    staleTime: 1000 * 60 * 2,
  });
}

export function useAddWishlistItem() {
  const queryClient = useQueryClient();

  return useMutation<WishlistItem, Error, AddWishlistItemRequest>({
    mutationFn: (data) => apiClient.post<WishlistItem>('/api/v1/users/me/wishlist', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: WISHLIST_QUERY_KEY });
    },
  });
}

export function useRemoveWishlistItem() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, string>({
    mutationFn: (itemId) => apiClient.delete<void>(`/api/v1/users/me/wishlist/${itemId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: WISHLIST_QUERY_KEY });
    },
  });
}
