/** The supplied text-free artwork is a fallback; uploaded event media takes priority. */
export const CATEGORY_ART: Record<string, string> = {
  'Music & Concert': 'aurora-festival', Sports: 'city-night-run', Conference: 'future-tech-summit',
  Workshop: 'product-design-lab', Exhibition: 'art-and-beyond', Festival: 'aurora-festival', Entertainment: 'jazz-after-dark',
};
export const categoryArt = (category: string | null | undefined) => '/assets/etp/backgrounds/event-plain/' + (CATEGORY_ART[category ?? ''] ?? 'aurora-festival') + '.png';
export const CATEGORY_NAMES: Record<string,string> = { 'Music & Concert': 'Âm nhạc', Sports: 'Thể thao', Conference: 'Hội nghị', Workshop: 'Workshop', Exhibition: 'Triển lãm', Festival: 'Lễ hội', Entertainment: 'Giải trí', Others: 'Khác' };
export const categoryName = (category: string | null) => CATEGORY_NAMES[category ?? ''] ?? category ?? 'Sự kiện';
export const eventDay = (value: string | null, timeZone = 'Asia/Ho_Chi_Minh') => value ? new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone }).format(new Date(value)) : 'Sắp công bố';
