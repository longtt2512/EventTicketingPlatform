import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AppShellComponent } from '@event-ticketing/ui';
@Component({
    selector: 'app-root', imports: [AppShellComponent], template: '<etp-app-shell />', changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent {}
