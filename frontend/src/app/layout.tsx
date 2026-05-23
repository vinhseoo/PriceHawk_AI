import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';
import { Providers } from '@/components/Providers';
import { Header } from '@/components/layout/Header';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: {
    default: 'PriceHawk AI — So sánh giá & phân tích đánh giá thông minh',
    template: '%s | PriceHawk AI',
  },
  description:
    'So sánh giá từ Shopee, Lazada, Tiki và hàng trăm website bán hàng. AI phân tích review thật, phát hiện giá ảo.',
  keywords: ['so sánh giá', 'mua sắm thông minh', 'review thật', 'giá tốt nhất'],
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi">
      <body className={inter.className}>
        <Providers>
          <Header />
          {children}
        </Providers>
      </body>
    </html>
  );
}
