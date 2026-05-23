'use client';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { apiClient } from '@/lib/apiClient';
import type { Product, PageResponse, ScrapeJob, ScrapeStatus } from '@/types';

const POLL_INTERVAL_MS = 2000;
const TERMINAL_STATUSES: ScrapeStatus[] = ['COMPLETED', 'FAILED'];

interface TextSearchParams {
  query: string;
  page?: number;
  size?: number;
}

interface UrlScrapeRequest {
  url: string;
  userId?: string;
}

interface UrlScrapeResponse {
  jobId: string;
}

export function useTextSearch(query: string, enabled = true) {
  return useQuery<PageResponse<Product>>({
    queryKey: ['search', 'text', query],
    queryFn: () =>
      apiClient.get<PageResponse<Product>>('/api/v1/search', {
        query,
        page: 0,
        size: 20,
      }),
    enabled: enabled && query.trim().length > 0,
    staleTime: 1000 * 60,
  });
}

export function useTextSearchPaged(params: TextSearchParams, enabled = true) {
  const { query, page = 0, size = 20 } = params;
  return useQuery<PageResponse<Product>>({
    queryKey: ['search', 'text', query, page, size],
    queryFn: () =>
      apiClient.get<PageResponse<Product>>('/api/v1/search', {
        query,
        page,
        size,
      }),
    enabled: enabled && query.trim().length > 0,
    staleTime: 1000 * 60,
    placeholderData: (prev) => prev,
  });
}

export function usePollScrapeJob(jobId: string | null) {
  return useQuery<ScrapeJob>({
    queryKey: ['scrape-job', jobId],
    queryFn: () => apiClient.get<ScrapeJob>(`/api/v1/scrape/jobs/${jobId}`),
    enabled: !!jobId,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status && TERMINAL_STATUSES.includes(status)) return false;
      return POLL_INTERVAL_MS;
    },
    staleTime: 0,
  });
}

interface UseUrlScrapeOptions {
  onCompleted?: (productSlug: string) => void;
  onFailed?: (jobId: string) => void;
}

export function useUrlScrape(options?: UseUrlScrapeOptions) {
  const queryClient = useQueryClient();

  // Stable refs so callbacks never cause effect re-runs
  const onCompletedRef = useRef(options?.onCompleted);
  const onFailedRef = useRef(options?.onFailed);
  onCompletedRef.current = options?.onCompleted;
  onFailedRef.current = options?.onFailed;

  const mutation = useMutation<UrlScrapeResponse, Error, UrlScrapeRequest>({
    mutationFn: (data) => apiClient.post<UrlScrapeResponse>('/api/v1/search/url', data),
  });

  const jobQuery = usePollScrapeJob(mutation.data?.jobId ?? null);

  useEffect(() => {
    const job = jobQuery.data;
    if (!job) return;

    if (job.status === 'COMPLETED' && job.productSlug) {
      onCompletedRef.current?.(job.productSlug);
      queryClient.removeQueries({ queryKey: ['scrape-job', job.jobId] });
    } else if (job.status === 'FAILED') {
      onFailedRef.current?.(job.jobId);
      queryClient.removeQueries({ queryKey: ['scrape-job', job.jobId] });
    }
  }, [jobQuery.data, queryClient]);

  return {
    scrape: mutation.mutate,
    scrapeAsync: mutation.mutateAsync,
    isPending: mutation.isPending,
    isPolling: !!mutation.data?.jobId && jobQuery.isFetching,
    jobStatus: jobQuery.data?.status ?? null,
    jobId: mutation.data?.jobId ?? null,
    error: mutation.error,
    reset: mutation.reset,
  };
}
