'use client';
import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Loader2, ImageOff } from 'lucide-react';
import { useSearchStore } from '@/stores/searchStore';
import { useUrlScrape } from '@/hooks/useSearch';
import { useToast } from '@/components/ui/toast';
import { cn } from '@/lib/utils';

const TABS = [
  { key: 'URL', label: 'Dán URL sản phẩm' },
  { key: 'TEXT', label: 'Tìm kiếm theo tên' },
  { key: 'IMAGE', label: 'Tìm theo ảnh' },
] as const;

const JOB_STATUS_LABELS: Record<string, string> = {
  PENDING: 'Đang chờ xử lý...',
  IN_PROGRESS: 'Đang scrape sản phẩm...',
  COMPLETED: 'Hoàn thành!',
  FAILED: 'Thất bại',
};

export function SearchHero() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { mode, setMode } = useSearchStore();
  const [inputValue, setInputValue] = useState('');
  const { toast } = useToast();

  // Pre-fill from ?url= query param (set by Header quick search)
  useEffect(() => {
    const urlParam = searchParams.get('url');
    if (urlParam) {
      setInputValue(urlParam);
      setMode('URL');
    }
  }, [searchParams, setMode]);

  const { scrape, isPending, isPolling, jobStatus, error, reset } = useUrlScrape({
    onCompleted: (productSlug) => {
      toast({ title: 'Scrape hoàn thành!', description: 'Đang chuyển hướng...', variant: 'success' });
      router.push(`/products/${productSlug}`);
    },
    onFailed: () => {
      toast({
        title: 'Scrape thất bại',
        description: 'Không thể lấy thông tin sản phẩm. Vui lòng thử URL khác.',
        variant: 'destructive',
      });
    },
  });

  const isLoading = isPending || isPolling;

  const handleSearch = () => {
    const trimmed = inputValue.trim();
    if (!trimmed) return;

    if (mode === 'TEXT') {
      router.push(`/search?q=${encodeURIComponent(trimmed)}`);
      return;
    }

    if (mode === 'IMAGE') return; // handled by UI message

    if (mode === 'URL') {
      if (!/^https?:\/\//.test(trimmed)) {
        toast({
          title: 'URL không hợp lệ',
          description: 'Vui lòng nhập một URL bắt đầu bằng http:// hoặc https://',
          variant: 'destructive',
        });
        return;
      }
      reset();
      scrape({ url: trimmed });
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSearch();
  };

  const statusLabel = jobStatus ? JOB_STATUS_LABELS[jobStatus] : null;

  return (
    <section className="flex flex-col items-center justify-center px-4 pt-20 pb-16">
      <h1 className="mb-2 text-4xl font-bold text-gray-900 text-center">
        PriceHawk AI
      </h1>
      <p className="mb-8 text-lg text-gray-500 text-center">
        So sánh giá &amp; phân tích review thông minh từ mọi nguồn
      </p>

      {/* Mode Tabs */}
      <div className="mb-4 flex gap-2 flex-wrap justify-center">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setMode(tab.key)}
            disabled={isLoading}
            className={cn(
              'rounded-full px-4 py-1.5 text-sm font-medium transition-colors disabled:opacity-50',
              mode === tab.key
                ? 'bg-blue-600 text-white'
                : 'bg-white text-gray-600 hover:bg-gray-100 border border-gray-200'
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* IMAGE mode message */}
      {mode === 'IMAGE' && (
        <div className="mb-4 flex w-full max-w-2xl flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed border-gray-300 bg-gray-50 p-10 text-center">
          <ImageOff className="h-10 w-10 text-gray-400" />
          <p className="text-sm font-medium text-gray-600">Tính năng tìm kiếm bằng ảnh</p>
          <p className="text-xs text-gray-400">Đang phát triển — sẽ ra mắt trong thời gian sớm</p>
        </div>
      )}

      {/* URL / TEXT input */}
      {mode !== 'IMAGE' && (
        <>
          <div className="flex w-full max-w-2xl gap-2">
            <input
              type={mode === 'URL' ? 'url' : 'text'}
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={isLoading}
              placeholder={
                mode === 'URL'
                  ? 'https://shopee.vn/product/... hoặc bất kỳ URL nào'
                  : 'iPhone 16 Pro Max 256GB...'
              }
              className={cn(
                'flex-1 rounded-xl border px-4 py-3 text-sm shadow-sm outline-none transition-colors',
                'focus:border-blue-500 focus:ring-2 focus:ring-blue-100',
                'disabled:cursor-not-allowed disabled:opacity-60',
                isLoading ? 'border-blue-300 bg-blue-50' : 'border-gray-200 bg-white'
              )}
            />
            <button
              onClick={handleSearch}
              disabled={isLoading || !inputValue.trim()}
              className={cn(
                'flex items-center gap-2 rounded-xl px-6 py-3 text-sm font-semibold text-white transition-colors',
                'disabled:cursor-not-allowed disabled:opacity-60',
                isLoading ? 'bg-blue-400' : 'bg-blue-600 hover:bg-blue-700'
              )}
            >
              {isLoading && <Loader2 className="h-4 w-4 animate-spin" />}
              {isLoading ? (statusLabel ?? 'Đang xử lý...') : 'Tìm kiếm'}
            </button>
          </div>

          {/* Scrape progress indicator */}
          {isLoading && (
            <div className="mt-3 flex items-center gap-2 text-sm text-blue-600">
              <Loader2 className="h-4 w-4 animate-spin" />
              <span>{statusLabel ?? 'Đang khởi động...'}</span>
            </div>
          )}

          {error && (
            <p className="mt-3 text-sm text-red-600">
              Lỗi: {error.message}
            </p>
          )}
        </>
      )}

      <p className="mt-4 text-xs text-gray-400 text-center max-w-lg">
        Hỗ trợ: Shopee, Lazada, Tiki, Thế Giới Di Động, FPT Shop, CellphoneS, Phong Vũ, GearVN
        và mọi website bán hàng
      </p>
    </section>
  );
}
