import { create } from 'zustand';

type SearchMode = 'URL' | 'TEXT' | 'IMAGE';

interface SearchState {
  mode: SearchMode;
  query: string;
  pendingJobId: string | null;
  setMode: (mode: SearchMode) => void;
  setQuery: (query: string) => void;
  setPendingJob: (jobId: string | null) => void;
}

export const useSearchStore = create<SearchState>((set) => ({
  mode: 'URL',
  query: '',
  pendingJobId: null,
  setMode: (mode) => set({ mode }),
  setQuery: (query) => set({ query }),
  setPendingJob: (pendingJobId) => set({ pendingJobId }),
}));
