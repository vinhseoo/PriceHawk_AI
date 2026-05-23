'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { Heart, Clock, Trash2, ExternalLink, Crown } from 'lucide-react';
import { useAuthStore } from '@/stores/authStore';
import { useWishlist, useRemoveWishlistItem } from '@/hooks/useWishlist';
import { apiClient } from '@/lib/apiClient';
import type { SearchHistory } from '@/types';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { useToast } from '@/components/ui/toast';
import { formatVND } from '@/lib/utils';

// ─── Wishlist Section ─────────────────────────────────────────────────────────

function WishlistSection() {
  const { data: wishlist, isLoading } = useWishlist();
  const { mutate: removeItem, isPending } = useRemoveWishlistItem();
  const { toast } = useToast();

  const handleRemove = (itemId: string) => {
    removeItem(itemId, {
      onError: () => {
        toast({
          title: 'Lỗi',
          description: 'Không thể xóa khỏi Wishlist.',
          variant: 'destructive',
        });
      },
    });
  };

  if (isLoading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-16 w-full rounded-xl" />
        ))}
      </div>
    );
  }

  const items = wishlist?.items ?? [];

  if (items.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-gray-300 py-12 text-center text-gray-400 text-sm">
        Chưa có sản phẩm nào trong Wishlist.{' '}
        <Link href="/" className="text-blue-600 hover:underline">
          Tìm sản phẩm
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {items.map((item) => (
        <div
          key={item.id}
          className="flex items-center justify-between rounded-xl border border-gray-200 bg-white px-4 py-3 gap-4"
        >
          <div className="flex-1 min-w-0">
            <Link
              href={`/products/${item.productId}`}
              className="text-sm font-medium text-gray-800 hover:text-blue-600 truncate block"
            >
              {item.productId}
            </Link>
            {item.targetPrice != null && (
              <p className="text-xs text-gray-500 mt-0.5">
                Giá mục tiêu: <span className="font-semibold">{formatVND(item.targetPrice)}</span>
              </p>
            )}
          </div>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => handleRemove(item.id)}
            disabled={isPending}
            className="text-red-400 hover:text-red-600 hover:bg-red-50 shrink-0"
            aria-label="Xóa khỏi Wishlist"
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      ))}
    </div>
  );
}

// ─── Search History Section ───────────────────────────────────────────────────

function SearchHistorySection() {
  const { isAuthenticated } = useAuthStore();

  const { data, isLoading } = useQuery<SearchHistory[]>({
    queryKey: ['search-history'],
    queryFn: () => apiClient.get<SearchHistory[]>('/api/v1/users/me/history'),
    enabled: isAuthenticated,
    staleTime: 1000 * 60,
  });

  if (isLoading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-12 w-full rounded-xl" />
        ))}
      </div>
    );
  }

  const history = data ?? [];

  if (history.length === 0) {
    return (
      <p className="py-8 text-center text-sm text-gray-400">
        Chưa có lịch sử tìm kiếm.
      </p>
    );
  }

  return (
    <div className="space-y-2">
      {history.map((item) => (
        <Link
          key={item.id}
          href={
            item.queryType === 'URL'
              ? `/?url=${encodeURIComponent(item.query)}`
              : `/search?q=${encodeURIComponent(item.query)}`
          }
          className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-4 py-3 hover:border-blue-300 hover:bg-blue-50 transition-colors"
        >
          <Clock className="h-4 w-4 text-gray-400 shrink-0" />
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-gray-800 truncate">{item.query}</p>
            <p className="text-xs text-gray-400">
              {new Date(item.createdAt).toLocaleDateString('vi-VN', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })}
            </p>
          </div>
          <ExternalLink className="h-4 w-4 text-gray-400 shrink-0" />
        </Link>
      ))}
    </div>
  );
}

// ─── User Info Card ───────────────────────────────────────────────────────────

function UserInfoCard() {
  const { user } = useAuthStore();
  if (!user) return null;

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-5 flex items-center gap-4">
      <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-blue-600 text-xl font-bold text-white">
        {user.fullName?.charAt(0).toUpperCase() ?? user.email.charAt(0).toUpperCase()}
      </div>
      <div className="flex-1 min-w-0">
        <h2 className="text-lg font-bold text-gray-900 truncate">
          {user.fullName ?? 'Người dùng'}
        </h2>
        <p className="text-sm text-gray-500 truncate">{user.email}</p>
      </div>
      <Badge
        variant={user.subscriptionPlan === 'PREMIUM_USER' ? 'default' : 'secondary'}
        className="gap-1 shrink-0"
      >
        {user.subscriptionPlan === 'PREMIUM_USER' && <Crown className="h-3 w-3" />}
        {user.subscriptionPlan === 'PREMIUM_USER' ? 'Premium' : 'Free'}
      </Badge>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function DashboardPage() {
  const { isAuthenticated } = useAuthStore();
  const router = useRouter();

  useEffect(() => {
    if (!isAuthenticated) {
      router.replace('/auth/login');
    }
  }, [isAuthenticated, router]);

  if (!isAuthenticated) return null;

  return (
    <main className="mx-auto max-w-4xl px-4 py-8 sm:px-6">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Dashboard</h1>

      <div className="space-y-8">
        {/* User info */}
        <UserInfoCard />

        {/* Wishlist */}
        <section>
          <div className="mb-4 flex items-center gap-2">
            <Heart className="h-5 w-5 text-red-500" />
            <h2 className="text-lg font-semibold text-gray-800">Wishlist của tôi</h2>
          </div>
          <WishlistSection />
        </section>

        {/* History */}
        <section>
          <div className="mb-4 flex items-center gap-2">
            <Clock className="h-5 w-5 text-blue-500" />
            <h2 className="text-lg font-semibold text-gray-800">Lịch sử tìm kiếm</h2>
          </div>
          <SearchHistorySection />
        </section>
      </div>
    </main>
  );
}
