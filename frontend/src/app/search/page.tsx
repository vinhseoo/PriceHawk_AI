'use client';
import { Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { ChevronLeft, ChevronRight, SearchX } from 'lucide-react';
import { useTextSearchPaged } from '@/hooks/useSearch';
import { ProductCard } from '@/components/product/ProductCard';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';

function SearchResults() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const query = searchParams.get('q') ?? '';
  const page = Number(searchParams.get('page') ?? '0');

  const { data, isLoading, isError } = useTextSearchPaged(
    { query, page, size: 20 },
    !!query
  );

  const setPage = (newPage: number) => {
    const params = new URLSearchParams(searchParams.toString());
    params.set('page', String(newPage));
    router.push(`/search?${params.toString()}`);
  };

  if (!query) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-center text-gray-500">
        <SearchX className="mb-4 h-12 w-12 text-gray-300" />
        <p className="text-lg font-medium">Nhập từ khóa để tìm kiếm sản phẩm</p>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="overflow-hidden rounded-xl border border-gray-200 bg-white">
            <Skeleton className="aspect-square w-full" />
            <div className="p-3 space-y-2">
              <Skeleton className="h-3 w-16" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-6 w-24" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <div className="py-16 text-center text-red-600">
        Có lỗi xảy ra khi tìm kiếm. Vui lòng thử lại.
      </div>
    );
  }

  if (!data || data.content.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-center text-gray-500">
        <SearchX className="mb-4 h-12 w-12 text-gray-300" />
        <p className="text-lg font-medium">Không tìm thấy sản phẩm nào</p>
        <p className="mt-1 text-sm">Thử tìm kiếm với từ khóa khác</p>
      </div>
    );
  }

  return (
    <>
      <p className="mb-4 text-sm text-gray-500">
        Tìm thấy <span className="font-semibold text-gray-800">{data.totalElements}</span> sản phẩm
        cho &ldquo;{query}&rdquo;
      </p>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        {data.content.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>

      {/* Pagination */}
      {data.totalPages > 1 && (
        <div className="mt-8 flex items-center justify-center gap-3">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage(page - 1)}
            disabled={page === 0}
          >
            <ChevronLeft className="h-4 w-4" />
            Trước
          </Button>

          <span className="text-sm text-gray-600">
            Trang {page + 1} / {data.totalPages}
          </span>

          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage(page + 1)}
            disabled={data.last}
          >
            Tiếp
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      )}
    </>
  );
}

export default function SearchPage() {
  return (
    <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Kết quả tìm kiếm</h1>
      <Suspense>
        <SearchResults />
      </Suspense>
    </main>
  );
}
