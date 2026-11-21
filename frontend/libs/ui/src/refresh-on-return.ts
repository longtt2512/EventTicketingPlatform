import { DOCUMENT } from '@angular/common';
import { DestroyRef, ElementRef, NgZone, inject } from '@angular/core';
import { PendingEdits } from './unsaved-changes';

/** Revisit/reconnect updates data without overwriting forms or hidden tab state. */
export function refreshOnReturn(refresh: () => void, blocked: () => boolean = () => false): void {
  const document = inject(DOCUMENT), window = document.defaultView;
  const destroy = inject(DestroyRef), zone = inject(NgZone), edits = inject(PendingEdits);
  const host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  if (!window) return;
  let timer: ReturnType<typeof setTimeout> | undefined;
  const schedule = () => {
    clearTimeout(timer);
    timer = setTimeout(() => {
      if (document.visibilityState !== 'visible' || !window.navigator.onLine ||
          host.closest('[hidden]') || edits.hasChanges() || blocked()) return;
      zone.run(refresh);
    }, 150);
  };
  zone.runOutsideAngular(() => {
    window.addEventListener('focus', schedule);
    window.addEventListener('online', schedule);
    document.addEventListener('visibilitychange', schedule);
  });
  destroy.onDestroy(() => {
    clearTimeout(timer);
    window.removeEventListener('focus', schedule);
    window.removeEventListener('online', schedule);
    document.removeEventListener('visibilitychange', schedule);
  });
}
