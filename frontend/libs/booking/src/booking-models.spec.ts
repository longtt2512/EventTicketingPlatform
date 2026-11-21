import { countdown,currencyAmount,parseSelection,remainingSeconds } from './booking-models';
import { safeReturnUrl } from '@event-ticketing/session';
describe('Phase 3 booking state',()=>{
 it('represents a zero balance as money and retains the transaction currency',()=>{expect(currencyAmount(0,'VND')).toContain('0');expect(currencyAmount(0,'VND')).not.toContain('Miễn phí');expect(currencyAmount(100,'USD')).toContain('US$');expect(currencyAmount(100,'USD')).not.toContain('₫');});
 it('uses server time and monotonic elapsed time without extending the hold',()=>{expect(remainingSeconds('2026-11-20T10:10:00Z','2026-11-20T10:05:00Z',120000)).toBe(180);expect(remainingSeconds('2026-11-20T10:10:00Z','2026-11-20T10:10:00Z',0)).toBe(0);expect(remainingSeconds('2026-11-20T10:10:00Z','2026-11-20T10:11:00Z',5000)).toBe(0);});
 it('formats countdown including last minute',()=>{expect(countdown(600)).toBe('10:00');expect(countdown(59)).toBe('00:59');expect(countdown(0)).toBe('00:00');});
 it('retains safe guest selection but rejects malformed and unbounded quantities',()=>{const id='12345678-1234-1234-1234-123456789012';expect(parseSelection(`${id}:2`)[id]).toBe(2);for(const n of ['-1','1.5','10001','NaN','Infinity'])expect(parseSelection(`${id}:${n}`)).toEqual({});expect(parseSelection('bad:2')).toEqual({});});
 it('preserves exact purchase routes through login without external return redirects',()=>{for(const path of ['/booking/abc?selection=xyz%3A2','/checkout/abc','/payment/result/abc','/account/orders/abc','/payment-exceptions'])expect(safeReturnUrl(path)).toBe(path);expect(safeReturnUrl('//evil.example/checkout')).toBe('/home');expect(safeReturnUrl('/payment\\evil')).toBe('/home');});
});
