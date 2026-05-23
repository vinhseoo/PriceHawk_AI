'use client';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { AlertTriangle, Star, ShieldAlert } from 'lucide-react';
import type { Product, Review } from '@/types';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

interface ReviewAnalysisProps {
  product: Product;
  reviews: Review[];
}

// ─── Sentiment Donut ──────────────────────────────────────────────────────────

function SentimentDonut({ score }: { score: number | null }) {
  if (score == null) return null;

  const positive = Math.round(score * 100);
  const negative = Math.round((1 - score) * 0.6 * 100);
  const neutral = 100 - positive - negative;

  const pieData = [
    { name: 'Tích cực', value: positive, color: '#16a34a' },
    { name: 'Tiêu cực', value: Math.max(negative, 0), color: '#dc2626' },
    { name: 'Trung lập', value: Math.max(neutral, 0), color: '#9ca3af' },
  ].filter((d) => d.value > 0);

  return (
    <div className="flex flex-col items-center gap-2">
      <ResponsiveContainer width={140} height={140}>
        <PieChart>
          <Pie
            data={pieData}
            cx="50%"
            cy="50%"
            innerRadius={42}
            outerRadius={60}
            dataKey="value"
            strokeWidth={2}
          >
            {pieData.map((entry, index) => (
              <Cell key={index} fill={entry.color} />
            ))}
          </Pie>
          <Tooltip
            formatter={(value: number, name: string) => [`${value}%`, name]}
          />
        </PieChart>
      </ResponsiveContainer>
      <div className="flex flex-wrap justify-center gap-2">
        {pieData.map((d) => (
          <div key={d.name} className="flex items-center gap-1 text-xs text-gray-600">
            <span
              className="h-2.5 w-2.5 rounded-full"
              style={{ backgroundColor: d.color }}
            />
            {d.name}: {d.value}%
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Real Review Bar ──────────────────────────────────────────────────────────

function RealReviewBar({ ratio }: { ratio: number | null }) {
  if (ratio == null) return null;
  const pct = Math.round(ratio * 100);
  const colorClass = ratio >= 0.7 ? 'bg-green-500' : ratio >= 0.5 ? 'bg-amber-400' : 'bg-red-500';

  return (
    <div>
      <div className="mb-1 flex items-center justify-between text-xs">
        <span className="font-medium text-gray-700">Tỉ lệ review thật</span>
        <span className={cn('font-bold', ratio >= 0.7 ? 'text-green-700' : ratio >= 0.5 ? 'text-amber-700' : 'text-red-700')}>
          {pct}%
        </span>
      </div>
      <div className="h-2 w-full overflow-hidden rounded-full bg-gray-200">
        <div
          className={cn('h-full rounded-full transition-all', colorClass)}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}

// ─── Review Item ──────────────────────────────────────────────────────────────

function StarRating({ rating }: { rating: number | null }) {
  if (rating == null) return null;
  return (
    <div className="flex items-center gap-0.5">
      {Array.from({ length: 5 }).map((_, i) => (
        <Star
          key={i}
          className={cn(
            'h-3.5 w-3.5',
            i < Math.round(rating) ? 'fill-amber-400 text-amber-400' : 'text-gray-300'
          )}
        />
      ))}
    </div>
  );
}

function ReviewItem({ review }: { review: Review }) {
  return (
    <div
      className={cn(
        'rounded-lg border p-3 text-sm',
        review.isLikelyFake
          ? 'border-red-200 bg-red-50'
          : 'border-gray-200 bg-white'
      )}
    >
      <div className="mb-1.5 flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="font-medium text-gray-800">
            {review.reviewerName ?? 'Ẩn danh'}
          </span>
          <StarRating rating={review.rating} />
        </div>
        <div className="flex items-center gap-1.5">
          {review.isLikelyFake && (
            <Badge variant="destructive" className="gap-1 text-xs">
              <ShieldAlert className="h-3 w-3" />
              Giả
            </Badge>
          )}
          {review.reviewDate && (
            <span className="text-xs text-gray-400">
              {new Date(review.reviewDate).toLocaleDateString('vi-VN')}
            </span>
          )}
        </div>
      </div>
      {review.content && (
        <p className="text-gray-600 line-clamp-3 leading-relaxed">{review.content}</p>
      )}
      {review.isLikelyFake && review.fakeReason && (
        <p className="mt-1 text-xs text-red-600">
          Lý do: {review.fakeReason}
        </p>
      )}
    </div>
  );
}

// ─── Main Component ───────────────────────────────────────────────────────────

export function ReviewAnalysis({ product, reviews }: ReviewAnalysisProps) {
  const top10 = reviews.slice(0, 10);
  const fakeCount = reviews.filter((r) => r.isLikelyFake).length;
  const showFakeWarning =
    product.realReviewRatio != null && product.realReviewRatio < 0.7;

  return (
    <div className="space-y-6">
      {/* Summary row */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
        <div className="flex flex-col items-center rounded-xl border border-gray-200 bg-white p-4 sm:w-48 shrink-0">
          <SentimentDonut score={product.sentimentScore} />
        </div>

        <div className="flex-1 space-y-4 rounded-xl border border-gray-200 bg-white p-4">
          <RealReviewBar ratio={product.realReviewRatio} />

          <div className="flex items-center justify-between text-sm">
            <span className="text-gray-600">Tổng đánh giá</span>
            <span className="font-semibold text-gray-900">{product.totalReviews}</span>
          </div>

          {fakeCount > 0 && (
            <div className="flex items-center justify-between text-sm">
              <span className="text-gray-600">Review nghi ngờ giả</span>
              <span className="font-semibold text-red-600">{fakeCount}</span>
            </div>
          )}

          {product.aiSummary && (
            <div className="border-t border-gray-100 pt-3">
              <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-blue-600">
                Tóm tắt AI
              </p>
              <p className="text-sm text-gray-700 leading-relaxed">{product.aiSummary}</p>
            </div>
          )}
        </div>
      </div>

      {/* Fake warning banner */}
      {showFakeWarning && (
        <div className="flex items-start gap-3 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
          <p>
            <span className="font-semibold">Cảnh báo review giả:</span> Chỉ{' '}
            {Math.round((product.realReviewRatio ?? 0) * 100)}% đánh giá được xác nhận là thật.
            Hãy đọc kỹ trước khi mua.
          </p>
        </div>
      )}

      {/* Review list */}
      {top10.length > 0 && (
        <div>
          <h4 className="mb-3 font-semibold text-gray-800">
            Đánh giá nổi bật ({Math.min(reviews.length, 10)}/{reviews.length})
          </h4>
          <div className="space-y-2">
            {top10.map((review) => (
              <ReviewItem key={review.id} review={review} />
            ))}
          </div>
        </div>
      )}

      {reviews.length === 0 && (
        <p className="py-6 text-center text-sm text-gray-400">
          Chưa có đánh giá nào được thu thập.
        </p>
      )}
    </div>
  );
}
