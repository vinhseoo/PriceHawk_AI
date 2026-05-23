import type { ProductSpecs } from '@/types';

interface SpecsTableProps {
  specs: ProductSpecs;
}

function toSentenceCase(key: string): string {
  // Convert camelCase / snake_case keys to sentence case
  const spaced = key
    .replace(/_/g, ' ')
    .replace(/([A-Z])/g, ' $1')
    .trim();
  return spaced.charAt(0).toUpperCase() + spaced.slice(1).toLowerCase();
}

function renderValue(value: unknown): string {
  if (value === null || value === undefined) return '—';
  if (typeof value === 'boolean') return value ? 'Có' : 'Không';
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}

export function SpecsTable({ specs }: SpecsTableProps) {
  const entries = Object.entries(specs.specs ?? {});

  if (entries.length === 0) {
    return (
      <p className="py-6 text-center text-sm text-gray-400">
        Chưa có thông số kỹ thuật nào.
      </p>
    );
  }

  return (
    <div className="overflow-hidden rounded-xl border border-gray-200">
      <table className="w-full text-sm">
        <tbody className="divide-y divide-gray-100">
          {entries.map(([key, value], index) => (
            <tr
              key={key}
              className={index % 2 === 0 ? 'bg-white' : 'bg-gray-50'}
            >
              <td className="w-2/5 px-4 py-3 font-medium text-gray-700">
                {toSentenceCase(key)}
              </td>
              <td className="px-4 py-3 text-gray-600">{renderValue(value)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
