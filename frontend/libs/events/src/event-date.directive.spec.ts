import { eventDateError } from './event-date.directive';
describe('Event schedule validation',()=>{
 const now=Date.parse('2026-11-21T10:00:00Z'),zone='Asia/Ho_Chi_Minh';
 it('permits missing draft dates and rejects past or current start and end',()=>{
  for(const part of ['start','end'] as const){expect(eventDateError('',part,'',zone,now)).toBe('');expect(eventDateError('2026-11-20T17:00',part,'',zone,now)).toContain('tương lai');expect(eventDateError('2026-11-21T17:00',part,'',zone,now)).toContain('tương lai');}
 });
 it('compares in event timezone and requires end after start',()=>{
  expect(eventDateError('2026-11-21T17:01','start','',zone,now)).toBe('');
  expect(eventDateError('2026-11-22T17:00','end','2026-11-22T17:00',zone,now)).toContain('sau');
  expect(eventDateError('2026-11-22T16:59','end','2026-11-22T17:00',zone,now)).toContain('sau');
  expect(eventDateError('2026-11-22T18:00','end','2026-11-22T17:00',zone,now)).toBe('');
  expect(eventDateError('bad','start','',zone,now)).toContain('không hợp lệ');
 });
});
