const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

export function fmtNumber(n: number | null | undefined, digits = 0): string {
  if (n === null || n === undefined || Number.isNaN(n)) return '--';
  return n.toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits });
}

/** "2017-07-24T20:10" -> "24 Jul 2017" */
export function fmtDate(iso: string | null | undefined): string {
  if (!iso) return '--';
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number);
  return `${d} ${MONTHS[m - 1]} ${y}`;
}

/** "2017-07-24T20:10" -> "24 Jul 2017, 20:10" */
export function fmtDateTime(iso: string | null | undefined): string {
  if (!iso) return '--';
  return `${fmtDate(iso)}, ${iso.slice(11, 16) || '00:00'}`;
}

/** "2017-07-24" -> "Jul 24" (short axis label) */
export function fmtAxisDate(iso: string, granularity: 'day' | 'week' | 'month' = 'day'): string {
  const [y, m, d] = iso.split('-').map(Number);
  if (granularity === 'month') return `${MONTHS[m - 1]} ${String(y).slice(2)}`;
  return `${MONTHS[m - 1]} ${d}`;
}

/** Energy in MWh -> "622.2 GWh" or "950 MWh". */
export function fmtEnergyMWh(mwh: number): string {
  return mwh >= 1000 ? `${fmtNumber(mwh / 1000, 1)} GWh` : `${fmtNumber(mwh, 0)} MWh`;
}

export function fmtMinutes(min: number): string {
  if (min < 60) return `${min} min`;
  const h = Math.floor(min / 60);
  if (h < 48) return `${h} h ${min % 60} min`;
  return `${(min / 1440).toFixed(1)} days`;
}

export const EVENT_LABELS: Record<string, string> = {
  OVER_VOLTAGE: 'Over-voltage',
  UNDER_VOLTAGE: 'Under-voltage',
  OVERCURRENT: 'Overcurrent',
  LOAD_SURGE: 'Load surge',
};
