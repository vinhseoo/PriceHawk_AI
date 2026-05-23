'use client';
import { useState } from 'react';
import * as Tabs from '@radix-ui/react-tabs';
import { AlertCircle } from 'lucide-react';
import {
  useProductDetail,
  useSellerListings,
  useProductSpecs,
  useProductReviews,
} from '@/hooks/useProductDetail';
import { useProductWebSocket } from '@/hooks/useWebSocket';
import { ProductHeader } from './ProductHeader';
import { SellerTable } from './SellerTable';
import { PriceHistoryChart } from './PriceHistoryChart';
import { ReviewAnalysis } from './ReviewAnalysis';
import { SpecsTable } from './SpecsTable';
import { Skeleton } from '@/components/ui/skeleton';

interface ProductDetailClientProps {
  slug: string;
}

function LoadingSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-6 md:flex-row">
        <Skeleton className="h-80 w-full rounded-xl md:w-80 shrink-0" />
        <div className="flex-1 space-y-3">
          <Skeleton className="h-5 w-24" />
          <Skeleton className="h-8 w-3/4" />
          <Skeleton className="h-8 w-1/2" />
          <Skeleton className="h-10 w-48" />
        </div>
      </div>
      <Skeleton className="h-10 w-full rounded-xl" />
      <Skeleton className="h-64 w-full rounded-xl" />
    </div>
  );
}

export function ProductDetailClient({ slug }: ProductDetailClientProps) {
  const [selectedListingId, setSelectedListingId] = useState<string | null>(null);

  const productQuery = useProductDetail(slug);

  const productId = productQuery.data?.id ?? '';

  const listingsQuery = useSellerListings(productId);
  const specsQuery = useProductSpecs(productId);
  const reviewsQuery = useProductReviews(productId);

  // Subscribe to real-time updates for this product
  useProductWebSocket(productId);

  if (productQuery.isLoading) return <LoadingSkeleton />;

  if (productQuery.isError || !productQuery.data) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-center text-gray-500">
        <AlertCircle className="mb-4 h-12 w-12 text-red-400" />
        <p className="text-lg font-medium">Không tìm thấy sản phẩm</p>
        <p className="mt-1 text-sm">
          {productQuery.error instanceof Error
            ? productQuery.error.message
            : 'Sản phẩm không tồn tại hoặc đã bị xóa.'}
        </p>
      </div>
    );
  }

  const product = productQuery.data;
  const listings = listingsQuery.data ?? [];
  const specs = specsQuery.data;
  const reviews = reviewsQuery.data ?? [];

  return (
    <div className="space-y-8">
      {/* Header */}
      <ProductHeader product={product} />

      {/* Tabs */}
      <Tabs.Root defaultValue="sellers">
        <Tabs.List className="flex border-b border-gray-200 gap-1">
          {[
            { value: 'sellers', label: `Người bán (${listings.length})` },
            { value: 'price-history', label: 'Lịch sử giá' },
            { value: 'reviews', label: `Đánh giá (${reviews.length})` },
            { value: 'specs', label: 'Thông số kỹ thuật' },
          ].map((tab) => (
            <Tabs.Trigger
              key={tab.value}
              value={tab.value}
              className="px-4 py-2.5 text-sm font-medium text-gray-600 transition-colors
                         hover:text-gray-900
                         data-[state=active]:border-b-2 data-[state=active]:border-blue-600 data-[state=active]:text-blue-600
                         focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              {tab.label}
            </Tabs.Trigger>
          ))}
        </Tabs.List>

        <div className="mt-6">
          {/* Sellers */}
          <Tabs.Content value="sellers">
            {listingsQuery.isLoading ? (
              <div className="space-y-2">
                {Array.from({ length: 4 }).map((_, i) => (
                  <Skeleton key={i} className="h-14 w-full rounded-lg" />
                ))}
              </div>
            ) : (
              <SellerTable
                listings={listings}
                onSelectListing={setSelectedListingId}
                selectedListingId={selectedListingId}
              />
            )}
          </Tabs.Content>

          {/* Price history */}
          <Tabs.Content value="price-history">
            <PriceHistoryChart
              listingId={selectedListingId ?? listings[0]?.id ?? null}
            />
          </Tabs.Content>

          {/* Reviews */}
          <Tabs.Content value="reviews">
            {reviewsQuery.isLoading ? (
              <div className="space-y-3">
                {Array.from({ length: 3 }).map((_, i) => (
                  <Skeleton key={i} className="h-24 w-full rounded-lg" />
                ))}
              </div>
            ) : (
              <ReviewAnalysis product={product} reviews={reviews} />
            )}
          </Tabs.Content>

          {/* Specs */}
          <Tabs.Content value="specs">
            {specsQuery.isLoading ? (
              <div className="space-y-2">
                {Array.from({ length: 6 }).map((_, i) => (
                  <Skeleton key={i} className="h-10 w-full rounded-lg" />
                ))}
              </div>
            ) : specs ? (
              <SpecsTable specs={specs} />
            ) : (
              <p className="py-6 text-center text-sm text-gray-400">
                Chưa có thông số kỹ thuật.
              </p>
            )}
          </Tabs.Content>
        </div>
      </Tabs.Root>
    </div>
  );
}
