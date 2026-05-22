import { Suspense } from 'react';
import { SearchHero } from '@/components/search/SearchHero';

export default function HomePage() {
  return (
    <main className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <Suspense>
        <SearchHero />
      </Suspense>
    </main>
  );
}
