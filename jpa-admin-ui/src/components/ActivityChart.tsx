import { useEffect, useLayoutEffect, useRef, useState, type KeyboardEvent } from 'react';
import { BarChart3, Table2 } from 'lucide-react';
import { fetchTimeline, type Timeline } from '@/api/client';
import { useAdmin } from '@/context/AdminContext';
import { formatCount } from '@/lib/format';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';

const RANGES = [7, 30, 90] as const;
const HEIGHT = 200;
const PAD = { top: 12, right: 8, bottom: 24, left: 36 };
const dayFormat = new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' });
const longDayFormat = new Intl.DateTimeFormat(undefined, { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' });
const parseDay = (day: string) => { const [y, m, d] = day.split('-').map(Number); return new Date(y, m - 1, d); };

/** Clean axis ticks: 0 and up to three round steps covering the max. */
function ticks(max: number): number[] {
  if (max <= 0) return [0, 1];
  const rough = max / 3;
  const magnitude = 10 ** Math.floor(Math.log10(rough));
  const step = [1, 2, 5, 10].map(m => m * magnitude).find(s => s >= rough) ?? rough;
  const out = [];
  for (let v = 0; v <= max + step * 0.001; v += step) out.push(v);
  if (out[out.length - 1] < max) out.push(out[out.length - 1] + step);
  return out;
}

/** Column path with a 4px rounded data-end and a square baseline. */
function columnPath(x: number, y: number, width: number, height: number) {
  const r = Math.min(4, width / 2, height);
  return `M${x},${y + height} L${x},${y + r} Q${x},${y} ${x + r},${y} L${x + width - r},${y} Q${x + width},${y} ${x + width},${y + r} L${x + width},${y + height} Z`;
}

export function ActivityChart() {
  const { metadata } = useAdmin();
  const [days, setDays] = useState<(typeof RANGES)[number]>(30);
  const [data, setData] = useState<Timeline | null>(null);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [showTable, setShowTable] = useState(false);
  const [active, setActive] = useState<number | null>(null);
  const [width, setWidth] = useState(600);
  const host = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let current = true;
    setLoading(true);
    fetchTimeline(days)
      .then(t => { if (current) { setData(t); setFailed(false); } })
      .catch(() => { if (current) setFailed(true); })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [days]);

  useLayoutEffect(() => {
    if (!host.current) return;
    const observer = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width));
    observer.observe(host.current);
    return () => observer.disconnect();
  }, [showTable, data === null]);

  const names = data ? Object.keys(data.series) : [];
  if (!loading && (failed || (data && names.length === 0))) return null;

  const label = (name: string) => { const e = metadata.find(m => m.entityName === name); return e?.pluralName || e?.displayName || name; };
  const totals = data ? data.days.map((_, i) => names.reduce((sum, n) => sum + (data.series[n][i] ?? 0), 0)) : [];
  const grand = totals.reduce((a, b) => a + b, 0);
  const yTicks = ticks(Math.max(0, ...totals));
  const yMax = yTicks[yTicks.length - 1];
  const plotW = Math.max(0, width - PAD.left - PAD.right);
  const plotH = HEIGHT - PAD.top - PAD.bottom;
  const band = totals.length ? plotW / totals.length : 0;
  const barW = Math.max(2, Math.min(24, band - 2));
  const y = (v: number) => PAD.top + plotH - (v / yMax) * plotH;
  const labelEvery = Math.ceil(totals.length / Math.max(1, Math.floor(plotW / 64)));

  const onKey = (event: KeyboardEvent) => {
    if (!totals.length) return;
    const step = event.key === 'ArrowRight' ? 1 : event.key === 'ArrowLeft' ? -1 : 0;
    if (event.key === 'Home') setActive(0);
    else if (event.key === 'End') setActive(totals.length - 1);
    else if (step) setActive(i => Math.min(totals.length - 1, Math.max(0, (i ?? totals.length - 1) + step)));
    else return;
    event.preventDefault();
  };

  const tip = active != null && data ? {
    day: data.days[active], total: totals[active],
    parts: names.map(n => [label(n), data.series[n][active] ?? 0] as const).filter(([, v]) => v > 0),
    left: PAD.left + band * active + band / 2,
  } : null;

  return <Card className="gap-0 py-0">
    <CardHeader className="border-b px-5 py-4 [.border-b]:pb-4">
      <CardTitle className="text-sm font-semibold">Records created</CardTitle>
      <CardDescription>{data ? `${formatCount(grand)} new ${grand === 1 ? 'record' : 'records'} in the last ${days} days` : 'Loading activity…'}</CardDescription>
      <CardAction className="flex items-center gap-2">
        <Tabs value={String(days)} onValueChange={v => { setActive(null); setDays(Number(v) as typeof days); }}>
          <TabsList className="h-8">{RANGES.map(r => <TabsTrigger key={r} value={String(r)} className="px-2.5 text-xs">{r}d</TabsTrigger>)}</TabsList>
        </Tabs>
        <Button variant="ghost" size="icon-sm" aria-label={showTable ? 'Show chart' : 'Show as table'} aria-pressed={showTable} onClick={() => setShowTable(!showTable)}>
          {showTable ? <BarChart3 /> : <Table2 />}
        </Button>
      </CardAction>
    </CardHeader>
    <CardContent className="px-5 py-4">
      {!data ? <Skeleton className="h-[200px] w-full" />
        : showTable ? <div className="max-h-[200px] overflow-y-auto">
            <table className="w-full text-sm">
              <thead className="sticky top-0 bg-card text-xs text-muted-foreground"><tr>
                <th className="py-1.5 text-left font-medium">Day</th>
                {names.map(n => <th key={n} className="py-1.5 text-right font-medium">{label(n)}</th>)}
                <th className="py-1.5 text-right font-medium">Total</th>
              </tr></thead>
              <tbody className="tabular">{[...data.days].reverse().map((day, r) => {
                const i = data.days.length - 1 - r;
                return <tr key={day} className="border-t">
                  <td className="py-1.5">{dayFormat.format(parseDay(day))}</td>
                  {names.map(n => <td key={n} className="py-1.5 text-right text-muted-foreground">{formatCount(data.series[n][i] ?? 0)}</td>)}
                  <td className="py-1.5 text-right font-medium">{formatCount(totals[i])}</td>
                </tr>;
              })}</tbody>
            </table>
          </div>
        : <div ref={host} className={cn('relative transition-opacity', loading && 'opacity-60')}>
            <svg width={width} height={HEIGHT} role="group" tabIndex={0} onKeyDown={onKey} onBlur={() => setActive(null)}
              aria-label={`Records created per day over the last ${days} days. Use the arrow keys to read each day, or switch to the table view.`}
              className="block overflow-visible rounded-sm outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50">
              {yTicks.map(t => <g key={t}>
                <line x1={PAD.left} x2={width - PAD.right} y1={y(t)} y2={y(t)} className={t === 0 ? 'stroke-border' : 'stroke-border/60'} strokeDasharray={t === 0 ? undefined : '2 4'} />
                <text x={PAD.left - 8} y={y(t)} dy="0.32em" textAnchor="end" className="fill-muted-foreground text-[11px] tabular">{formatCount(t)}</text>
              </g>)}
              {totals.map((total, i) => {
                const x = PAD.left + band * i + (band - barW) / 2;
                const h = total > 0 ? Math.max(2, (total / yMax) * plotH) : 0;
                return <g key={data.days[i]} onPointerEnter={() => setActive(i)} onPointerLeave={() => setActive(null)}>
                  <rect x={PAD.left + band * i} y={PAD.top} width={band} height={plotH} className="fill-transparent" />
                  {h > 0 && <path d={columnPath(x, y(0) - h, barW, h)} className={cn('fill-chart-1 transition-opacity', active != null && active !== i && 'opacity-40')} />}
                  {i % labelEvery === 0 && <text x={PAD.left + band * i + band / 2} y={HEIGHT - 6} textAnchor="middle" className="fill-muted-foreground text-[11px]">
                    {dayFormat.format(parseDay(data.days[i]))}
                  </text>}
                </g>;
              })}
            </svg>
            {tip && <div role="status" style={{ left: Math.min(Math.max(tip.left, 90), width - 90) }}
              className="pointer-events-none absolute top-0 z-10 w-44 -translate-x-1/2 rounded-md border bg-popover px-3 py-2 text-popover-foreground shadow-md">
              <p className="text-lg font-semibold leading-tight">{formatCount(tip.total)}</p>
              <p className="text-xs text-muted-foreground">{longDayFormat.format(parseDay(tip.day))}</p>
              {tip.parts.length > 0 && <ul className="mt-1.5 space-y-0.5 border-t pt-1.5 text-xs">
                {tip.parts.map(([name, value]) => <li key={name} className="flex justify-between gap-2"><span className="truncate text-muted-foreground">{name}</span><span className="font-medium tabular">{formatCount(value)}</span></li>)}
              </ul>}
            </div>}
            {grand === 0 && !loading && <p className="absolute inset-0 flex items-center justify-center text-sm text-muted-foreground">No records were created in this period.</p>}
          </div>}
    </CardContent>
  </Card>;
}
