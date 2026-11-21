import {localTime,utcTime,money} from './event-models';
describe('Event time and price presentation',()=>{
 it('keeps event timezone independent of browser timezone',()=>{
  expect(localTime('2027-03-02T03:00:00Z')).toBe('2027-03-02T10:00');
  expect(utcTime('2027-03-02T10:00')).toBe('2027-03-02T03:00:00.000Z');
  expect(utcTime('2027-07-01T10:00','America/New_York')).toBe('2027-07-01T14:00:00.000Z');
  expect(()=>utcTime('2027-03-14T02:30','America/New_York')).toThrow();
 });
 it('distinguishes missing price from a free ticket',()=>{expect(money(null)).toBe('Chưa có giá vé');expect(money(0)).toBe('Miễn phí');});
});
