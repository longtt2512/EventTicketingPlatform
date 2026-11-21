import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/** Decorative icons from the supplied ETP asset pack. Label the owning control. */
@Component({
  selector: 'etp-icon', standalone: true, changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<img [src]="'/assets/etp/icons/' + tone + '/' + name + '.svg'" alt="" width="20" height="20" aria-hidden="true">`,
  styles: [':host{display:inline-flex;flex-shrink:0;width:1.25rem;height:1.25rem;align-items:center;justify-content:center}img{display:block;width:100%;height:100%}'],
})
export class IconComponent {
  @Input() name = 'ticket';
  @Input() tone: 'indigo' | 'slate' | 'white' = 'slate';
}
