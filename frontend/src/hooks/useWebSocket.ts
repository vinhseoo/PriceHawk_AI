'use client';
import { useEffect, useRef, useCallback } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { useNotificationStore } from '@/stores/notificationStore';
import type { WsNotification } from '@/types';

export type { WsNotification };

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8085';

interface UseWebSocketReturn {
  sendMessage: (destination: string, body: string) => void;
  connected: boolean;
}

// Module-level singleton — avoids multiple STOMP connections across re-renders
let stompClient: Client | null = null;
let connectionCount = 0;

export function useWebSocket(): UseWebSocketReturn {
  const queryClient = useQueryClient();
  const { user, accessToken } = useAuthStore();
  const setLatest = useNotificationStore((s) => s.setLatest);
  const connectedRef = useRef(false);

  const handleNotification = useCallback(
    (message: IMessage) => {
      try {
        const notification: WsNotification = JSON.parse(message.body);
        // Push to notification store for bell display
        setLatest(notification);

        // Invalidate relevant caches on data-changing events
        if (
          notification.type === 'ANALYSIS_COMPLETE' ||
          notification.type === 'PRICE_UPDATE'
        ) {
          queryClient.invalidateQueries({ queryKey: ['product', notification.productId] });
          queryClient.invalidateQueries({
            queryKey: ['seller-listings', notification.productId],
          });
        }
      } catch {
        // Malformed message — discard silently
      }
    },
    [queryClient, setLatest]
  );

  useEffect(() => {
    if (!accessToken || !user) return;

    connectionCount++;

    // Lazy-import SockJS to avoid SSR issues
    import('sockjs-client').then(({ default: SockJS }) => {
      if (stompClient?.connected) return;

      stompClient = new Client({
        webSocketFactory: () => new SockJS(`${WS_URL}/ws/notifications`),
        connectHeaders: {
          'X-User-Id': user.id,
          Authorization: `Bearer ${accessToken}`,
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          connectedRef.current = true;
          stompClient?.subscribe('/user/queue/notifications', handleNotification);
        },
        onDisconnect: () => {
          connectedRef.current = false;
        },
        onStompError: (frame) => {
          console.warn('[WS] STOMP error:', frame.headers['message']);
        },
      });

      stompClient.activate();
    });

    return () => {
      connectionCount--;
      if (connectionCount === 0 && stompClient) {
        stompClient.deactivate();
        stompClient = null;
        connectedRef.current = false;
      }
    };
  }, [accessToken, user, handleNotification]);

  const sendMessage = useCallback((destination: string, body: string) => {
    if (stompClient?.connected) {
      stompClient.publish({ destination, body });
    }
  }, []);

  return { sendMessage, connected: connectedRef.current };
}

// ─── Per-product subscription ─────────────────────────────────────────────────

export function useProductWebSocket(productId: string): WsNotification | null {
  const latestRef = useRef<WsNotification | null>(null);
  const { accessToken, user } = useAuthStore();

  useEffect(() => {
    if (!accessToken || !user || !productId) return;

    let subscription: ReturnType<Client['subscribe']> | null = null;
    let cancelled = false;
    let timerId: ReturnType<typeof setTimeout> | null = null;

    const trySubscribe = () => {
      if (cancelled) return;
      if (stompClient?.connected) {
        subscription = stompClient.subscribe(
          `/topic/product/${productId}`,
          (message: IMessage) => {
            try {
              latestRef.current = JSON.parse(message.body) as WsNotification;
            } catch {
              // Discard
            }
          }
        );
      } else {
        timerId = setTimeout(trySubscribe, 1000);
      }
    };

    trySubscribe();

    return () => {
      cancelled = true;
      if (timerId) clearTimeout(timerId);
      subscription?.unsubscribe();
    };
  }, [productId, accessToken, user]);

  return latestRef.current;
}
