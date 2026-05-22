'use client';
import { useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8085';

export function useWebSocket() {
  const socketRef = useRef<ReturnType<typeof import('socket.io-client').io> | null>(null);
  const queryClient = useQueryClient();
  const { accessToken } = useAuthStore();

  useEffect(() => {
    if (!accessToken) return;

    // Lazy import socket.io-client to avoid SSR issues
    import('socket.io-client').then(({ io }) => {
      socketRef.current = io(WS_URL, { auth: { token: accessToken }, transports: ['websocket'] });

      socketRef.current.on('analysis.completed', (data: { productId: string }) => {
        queryClient.invalidateQueries({ queryKey: ['product', data.productId] });
        queryClient.invalidateQueries({ queryKey: ['seller-listings', data.productId] });
      });

      socketRef.current.on('price.updated', (data: { productId: string }) => {
        queryClient.invalidateQueries({ queryKey: ['seller-listings', data.productId] });
      });
    });

    return () => {
      socketRef.current?.disconnect();
    };
  }, [accessToken, queryClient]);
}
