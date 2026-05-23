import { create } from 'zustand';
import type { WsNotification } from '@/types';

interface NotificationState {
  latest: WsNotification | null;
  setLatest: (n: WsNotification) => void;
  clear: () => void;
}

export const useNotificationStore = create<NotificationState>((set) => ({
  latest: null,
  setLatest: (n) => set({ latest: n }),
  clear: () => set({ latest: null }),
}));
