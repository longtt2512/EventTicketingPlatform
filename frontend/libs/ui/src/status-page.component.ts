import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
@Component({
    selector: 'etp-status-page', imports: [RouterLink], changeDetection: ChangeDetectionStrategy.OnPush,
    template: `<section class="empty-state"><h1>{{ forbidden ? 'Không có quyền truy cập' : 'Không tìm thấy trang' }}</h1><p>{{ forbidden ? 'Tài khoản của bạn chưa được cấp quyền sử dụng khu vực này.' : 'Liên kết có thể đã thay đổi. Bạn có thể quay lại trang tổng quan.' }}</p><a class="button-link" routerLink="/home">Về tổng quan</a></section>`
})
export class StatusPageComponent { readonly forbidden = inject(ActivatedRoute).snapshot.data['forbidden'] === true; }
