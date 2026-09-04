/**
 * Money is an integer number of halalas, exactly as the backend stores it
 * (price_halalas). No floats anywhere, so no rounding drift between the app
 * and the server.
 */
export function formatSar(halalas: number): string {
  const riyals = Math.round(halalas / 100);
  // Arabic apps show western digits for prices in practice; group by thousands.
  return `${riyals.toLocaleString("en-US")} ﷼`;
}

/** "قبل ٣ ساعات" style relative time from an ISO string or epoch ms. */
export function timeAgo(when: string | number): string {
  const then = typeof when === "number" ? when : Date.parse(when);
  const secs = Math.max(1, Math.floor((Date.now() - then) / 1000));
  const table: [number, string, string][] = [
    [60, "ثانية", "ثوانٍ"],
    [3600, "دقيقة", "دقائق"],
    [86400, "ساعة", "ساعات"],
    [2592000, "يوم", "أيام"],
    [31536000, "شهر", "أشهر"],
  ];
  let unitSecs = 1;
  let singular = "سنة";
  let plural = "سنوات";
  for (let i = 0; i < table.length; i++) {
    const [limit, s, p] = table[i];
    if (secs < limit) {
      singular = s;
      plural = p;
      break;
    }
    unitSecs = limit;
  }
  const n = Math.floor(secs / unitSecs);
  if (n <= 1) return `قبل ${singular}`;
  if (n === 2) return `قبل ${singular}ين`;
  if (n <= 10) return `قبل ${n} ${plural}`;
  return `قبل ${n} ${singular}`;
}
