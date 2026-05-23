'use client';
import { Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { useQueries } from '@tanstack/react-query';
import Link from 'next/link';
import Image from 'next/image';
import { AlertCircle, ExternalLink, ShieldCheck } from 'lucide-react';
import { apiClient } from '@/lib/apiClient';
import type { Product, SellerListing } from '@/types';
import { formatVND, cn } from '@/lib/utils';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';

const MAX_COMPARE = 4;

// ─── Utility ─────────────────────────────────────────────────────────────────

function getKeySpecs(
  product: Product,
  listings: SellerListing[]
): Array<{ label: string; value: string }> {
  const lowestListing = listings.sort(
    (a, b) => (a.currentPrice ?? Infinity) - (b.currentPrice ?? Infinity)
  )[0];

  return [
    { label: 'Thương hiệu', value: product.brand ?? '—' },
    {
      label: 'Giá thấp nhất',
      value: formatVND(product.lowestPrice),
    },
    {
      label: 'Nơi bán tốt nhất',
      value: lowestListing?.domain ?? product.lowestPriceSeller ?? '—',
    },
    {
      label: 'Điểm tin cậy',
      value: product.sentimentScore != null
        ? `${Math.round(product.sentimentScore * 100)}%`
        : 'N/A',
    },
    {
      label: 'Tổng đánh giá',
      value: String(product.totalReviews),
    },
    {
      label: 'Review thật',
      value: product.realReviewRatio != null
        ? `${Math.round(product.realReviewRatio * 100)}%`
        : 'N/A',
    },
  ];
}

// ─── Single product column ────────────────────────────────────────────────────

interface ProductColumnProps {
  productId: string;
  index: number;
}

function ProductColumn({ productId, index }: ProductColumnProps) {
  const [productQuery, listingsQuery] = useQueries({
    queries: [
      {
        queryKey: ['product-by-id', productId] as const,
        queryFn: (): Promise<Product> => apiClient.get<Product>(`/api/v1/products/${productId}`),
        staleTime: 1000 * 60 * 5,
      },
      {
        queryKey: ['seller-listings', productId] as const,
        queryFn: (): Promise<SellerListing[]> =>
          apiClient.get<SellerListing[]>(`/api/v1/products/${productId}/listings`),
        staleTime: 1000 * 60 * 5,
      },
    ],
  });

  const highlightColors = [
    'border-blue-400',
    'border-green-400',
    'border-purple-400',
    'border-amber-400',
  ];

  if (productQuery.isLoading || listingsQuery.isLoading) {
    return (
      <div className="flex flex-col gap-3 rounded-xl border-2 border-gray-200 bg-white p-4">
        <Skeleton className="aspect-square w-full rounded-lg" />
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-6 w-1/2" />
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-8 w-full" />
        ))}
      </div>
    );
  }

  if (productQuery.isError || !productQuery.data) {
    return (
      <div className="flex flex-col items-center justify-center rounded-xl border-2 border-red-200 bg-red-50 p-8 text-center text-sm text-red-600 gap-2">
        <AlertCircle className="h-8 w-8" />
        <p>Không tìm thấy sản phẩm</p>
        <p className="text-xs text-gray-400">ID: {productId}</p>
      </div>
    );
  }

  const product = productQuery.data;
  const listings = listingsQuery.data ?? [];
  const specs = getKeySpecs(product, listings);
  const lowestListing = listings.sort(
    (a, b) => (a.currentPrice ?? Infinity) - (b.currentPrice ?? Infinity)
  )[0];

  return (
    <div
      className={cn(
        'flex flex-col gap-4 rounded-xl border-2 bg-white p-4',
        highlightColors[index] ?? 'border-gray-200'
      )}
    >
      {/* Thumbnail */}
      <Link href={`/products/${product.slug}`} className="group">
        <div className="relative aspect-square w-full overflow-hidden rounded-lg bg-gray-100">
          {product.thumbnailUrl ? (
            <Image
              src={product.thumbnailUrl}
              alt={product.name}
              fill
              sizes="(max-width: 768px) 100vw, 25vw"
              className="object-contain group-hover:scale-105 transition-transform"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-5xl text-gray-300">
              📦
            </div>
          )}
        </div>
      </Link>

      {/* Name & brand */}
      <div>
        {product.brand && (
          <Badge variant="secondary" className="mb-1">
            {product.brand}
          </Badge>
        )}
        <h3 className="text-sm font-semibold text-gray-900 line-clamp-2 leading-snug">
          {product.name}
        </h3>
      </div>

      {/* Price highlight */}
      <div className="rounded-lg bg-green-50 px-3 py-2 text-center">
        <p className="text-xs text-green-600 font-medium">Giá thấp nhất</p>
        <p className="text-xl font-bold text-green-700">{formatVND(product.lowestPrice)}</p>
      </div>

      {/* Trust score */}
      {product.sentimentScore != null && (
        <div className="flex items-center gap-2 rounded-lg bg-blue-50 px-3 py-2">
          <ShieldCheck className="h-4 w-4 text-blue-600 shrink-0" />
          <div className="flex-1">
            <p className="text-xs text-blue-600 font-medium">Điểm tin cậy</p>
            <p className="text-sm font-bold text-blue-800">
              {Math.round(product.sentimentScore * 100)}%
            </p>
          </div>
        </div>
      )}

      {/* Specs comparison */}
      <div className="space-y-2">
        {specs.map(({ label, value }) => (
          <div key={label} className="flex items-center justify-between gap-2 border-b border-gray-100 pb-2 last:border-0">
            <span className="text-xs text-gray-500">{label}</span>
            <span className="text-xs font-semibold text-gray-800 text-right">{value}</span>
          </div>
        ))}
      </div>

      {/* Best deal button */}
      {lowestListing && (
        <button
          onClick={() =>
            window.open(lowestListing.externalUrl, '_blank', 'noopener,noreferrer')
          }
          className="flex w-full items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 transition-colors"
        >
          <ExternalLink className="h-4 w-4" />
          Mua với giá tốt nhất
        </button>
      )}
    </div>
  );
}

// ─── Compare Content ──────────────────────────────────────────────────────────

function CompareContent() {
  const searchParams = useSearchParams();
  const idsParam = searchParams.get('ids');

  const ids = (idsParam ?? '')
    .split(',')
    .map((id) => id.trim())
    .filter(Boolean)
    .slice(0, MAX_COMPARE);

  if (ids.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-center text-gray-500">
        <AlertCircle className="mb-4 h-12 w-12 text-gray-300" />
        <p className="text-lg font-medium">Chưa chọn sản phẩm để so sánh</p>
        <p className="mt-1 text-sm">
          Thêm tham số <code className="rounded bg-gray-100 px-1 py-0.5 text-xs">?ids=id1,id2</code> vào URL
        </p>
        <Link
          href="/"
          className="mt-4 text-sm text-blue-600 hover:underline"
        >
          Về trang tìm kiếm
        </Link>
      </div>
    );
  }

  return (
    <div
      className={cn(
        'grid gap-4',
        ids.length === 2 && 'sm:grid-cols-2',
        ids.length === 3 && 'sm:grid-cols-3',
        ids.length === 4 && 'sm:grid-cols-2 lg:grid-cols-4'
      )}
    >
      {ids.map((id, index) => (
        <ProductColumn key={id} productId={id} index={index} />
      ))}
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function ComparePage() {
  return (
    <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">So sánh sản phẩm</h1>
      <Suspense>
        <CompareContent />
      </Suspense>
    </main>
  );
}
