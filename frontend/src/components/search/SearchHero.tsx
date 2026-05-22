'use client';
import { useState } from 'react';
import { useSearchStore } from '@/stores/searchStore';
import { cn } from '@/lib/utils';

const TABS = [
  { key: 'URL', label: 'Dán URL sản phẩm' },
  { key: 'TEXT', label: 'Tìm kiếm theo tên' },
  { key: 'IMAGE', label: 'Tìm theo ảnh' },
] as const;

export function SearchHero() {
  const { mode, query, setMode, setQuery } = useSearchStore();
  const [inputValue, setInputValue] = useState('');

  const handleSearch = () => {
    setQuery(inputValue);
    // TODO: trigger scrape or search based on mode
  };

  return (
    <section className="flex flex-col items-center justify-center px-4 pt-24 pb-16">
      <h1 className="mb-2 text-4xl font-bold text-gray-900">SmartCart AI</h1>
      <p className="mb-8 text-lg text-gray-500">
        So sánh giá & phân tích review thông minh từ mọi nguồn
      </p>

      {/* Tabs */}
      <div className="mb-4 flex gap-2">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setMode(tab.key)}
            className={cn(
              'rounded-full px-4 py-1.5 text-sm font-medium transition-colors',
              mode === tab.key
                ? 'bg-blue-600 text-white'
                : 'bg-white text-gray-600 hover:bg-gray-100 border border-gray-200'
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Search Input */}
      <div className="flex w-full max-w-2xl gap-2">
        <input
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          placeholder={
            mode === 'URL'
              ? 'https://shopee.vn/product/... hoặc bất kỳ URL nào'
              : mode === 'TEXT'
              ? 'iPhone 16 Pro Max 256GB...'
              : 'Upload ảnh sản phẩm'
          }
          className="flex-1 rounded-xl border border-gray-200 px-4 py-3 text-sm shadow-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
        />
        <button
          onClick={handleSearch}
          className="rounded-xl bg-blue-600 px-6 py-3 text-sm font-semibold text-white hover:bg-blue-700 transition-colors"
        >
          Tìm kiếm
        </button>
      </div>

      <p className="mt-4 text-xs text-gray-400">
        Hỗ trợ: Shopee, Lazada, Tiki, Thế Giới Di Động, FPT Shop, CellphoneS, Phong Vũ, GearVN và mọi website bán hàng
      </p>
    </section>
  );
}
