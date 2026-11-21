/** Only known, internal task destinations can survive an authentication redirect. */
export function safeReturnUrl(value: string | null | undefined): string {
  if (!value || value.length > 2048 || /[\\\u0000-\u001f\u007f]/.test(value)) return '/home';
  return /^\/(?:home|booking|checkout|payment|account|payments|payment-exceptions|orders|events|profile|apply|organizations|assignments|applications|users|check-in|analytics|settlements|categories|commission|settings|audit-logs)(?:[/?#]|$)/.test(value) ? value : '/home';
}
