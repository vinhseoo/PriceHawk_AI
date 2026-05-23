'use client';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { subDays, parseISO, isAfter } from 'date-fns';
import { apiClient } from '@/lib/apiClient';
import type { PriceHistory } from '@/types';
import { formatVND } from '@/lib/utils';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';

type Range = '30d' | '90d' | 'all';

const RANGE_LABELS: Record<Range, string> = {
  '30d': '30 ngày',
  '90d': '90 ngày',
  all: 'Tất cả',
};

interface PriceHistoryChartProps {
  listingId: string | null;
}

function filterByRange(history: PriceHistory[], range: Range): PriceHistory[] {
  if (range === 'all') return history;
  const cutoff = subDays(new Date(), range === '30d' ? 30 : 90);
  return history.filter((h) => isAfter(parseISO(h.recordedAt), cutoff));
}

interface TooltipPayload {
  value: number;
}

function CustomTooltip({
  active,
  payload,
  label,
}: {
  active?: boolean;
  payload?: TooltipPayload[];
  label?: string;
}) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-lg border border-gray-200 bg-white px-3 py-2 shadow-md text-sm">
      <p className="text-gray-500 text-xs">{label}</p>
      <p className="font-bold text-green-700">{formatVND(payload[0].value)}</p>
    </div>
  );
}

export function PriceHistoryChart({ listingId }: PriceHistoryChartProps) {
  const [range, setRange] = useState<Range>('30d');

  const { data, isLoading } = useQuery<PriceHistory[]>({
    queryKey: ['price-history', listingId],
    queryFn: () =>
      apiClient.get<PriceHistory[]>(
        `/api/v1/products/listings/${listingId}/price-history`
      ),
    enabled: !!listingId,
    staleTime: 1000 * 60 * 5,
  });

  if (!listingId) {
    return (
      <div className="flex items-center justify-center rounded-xl border border-dashed border-gray-300 py-16 text-sm text-gray-400">
        Chọn một người bán để xem lịch sử giá
      </div>
    );
  }

  if (isLoading) {
    return <Skeleton className="h-56 w-full rounded-xl" />;
  }

  const filtered = filterByRange(data ?? [], range);

  const chartData = filtered.map((h) => ({
    date: new Date(h.recordedAt).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
    }),
    price: h.price,
  }));

  const prices = chartData.map((d) => d.price);
  const minPrice = prices.length ? Math.min(...prices) : 0;
  const maxPrice = prices.length ? Math.max(...prices) : 0;

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4">
      <div className="mb-4 flex items-center justify-between gap-2">
        <h3 className="font-semibold text-gray-800">Lịch sử giá</h3>
        <div className="flex gap-1">
          {(Object.keys(RANGE_LABELS) as Range[]).map((r) => (
            <button
              key={r}
              onClick={() => setRange(r)}
              className={cn(
                'rounded-full px-3 py-1 text-xs font-medium transition-colors',
                range === r
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              )}
            >
              {RANGE_LABELS[r]}
            </button>
          ))}
        </div>
      </div>

      {chartData.length === 0 ? (
        <div className="flex items-center justify-center py-12 text-sm text-gray-400">
          Chưa có dữ liệu giá trong khoảng thời gian này
        </div>
      ) : (
        <>
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={chartData} margin={{ top: 5, right: 10, left: 10, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis
                dataKey="date"
                tick={{ fontSize: 11, fill: '#9ca3af' }}
                tickLine={false}
                axisLine={false}
              />
              <YAxis
                tick={{ fontSize: 11, fill: '#9ca3af' }}
                tickLine={false}
                axisLine={false}
                tickFormatter={(v) =>
                  new Intl.NumberFormat('vi-VN', {
                    notation: 'compact',
                    maximumFractionDigits: 1,
                  }).format(v)
                }
                domain={[minPrice * 0.97, maxPrice * 1.03]}
              />
              <Tooltip content={<CustomTooltip />} />
              <Line
                type="monotone"
                dataKey="price"
                stroke="#2563eb"
                strokeWidth={2}
                dot={false}
                activeDot={{ r: 4, fill: '#2563eb' }}
              />
            </LineChart>
          </ResponsiveContainer>

          <div className="mt-3 flex justify-between text-xs text-gray-500">
            <span>
              Thấp nhất:{' '}
              <span className="font-semibold text-green-700">{formatVND(minPrice)}</span>
            </span>
            <span>
              Cao nhất:{' '}
              <span className="font-semibold text-red-600">{formatVND(maxPrice)}</span>
            </span>
          </div>
        </>
      )}
    </div>
  );
}
