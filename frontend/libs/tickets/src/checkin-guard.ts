import {CanDeactivateFn} from '@angular/router';
import {CheckinWorkspaceComponent} from './checkin-workspace.component';
export const checkinPendingGuard:CanDeactivateFn<CheckinWorkspaceComponent>=(component)=>!component.operation.current||window.confirm('Kết quả check-in chưa rõ. Nếu rời trang, bạn cần tra cứu mã vé để đối chiếu trước khi cho khách vào. Rời trang?');
