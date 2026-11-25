import { Directive, Input, OnChanges, forwardRef } from '@angular/core';
import { AbstractControl, NG_VALIDATORS, Validator } from '@angular/forms';
import { utcTime } from './event-models';

export function eventDateError(value: string, part: 'start'|'end', start: string, timezone: string, now = Date.now()): string {
  if (!value) return '';
  try {
    const date = Date.parse(utcTime(value, timezone)!);
    if (!Number.isFinite(date)) return 'Chọn ngày và giờ hợp lệ.';
    if (date <= now) return `Thời gian ${part === 'start' ? 'bắt đầu' : 'kết thúc'} phải ở tương lai. Hãy chọn lại ngày và giờ.`;
    if (part === 'end' && start && date <= Date.parse(utcTime(start, timezone)!)) return 'Giờ kết thúc phải sau giờ bắt đầu.';
    return '';
  } catch { return 'Thời gian không hợp lệ trong múi giờ sự kiện. Hãy chọn lại.'; }
}

@Directive({selector:'[etpEventDate]', standalone:true, providers:[{provide:NG_VALIDATORS,useExisting:forwardRef(()=>EventDateDirective),multi:true}]})
export class EventDateDirective implements Validator, OnChanges {
  @Input() etpEventDate: 'start'|'end' = 'start';
  @Input() eventStart = '';
  @Input() eventTimezone = 'Asia/Ho_Chi_Minh';
  private changed = () => {};
  validate(control: AbstractControl) { const error = eventDateError(control.value, this.etpEventDate, this.eventStart, this.eventTimezone); return error ? {eventRule:error} : null; }
  registerOnValidatorChange(fn: () => void) { this.changed = fn; }
  ngOnChanges() { this.changed(); }
}
