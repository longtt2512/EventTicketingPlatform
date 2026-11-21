import { refreshOnReturn } from '@event-ticketing/ui';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule, NgForm } from '@angular/forms';
import { finalize } from 'rxjs';
import { ConfirmDialogComponent,ConfirmationState,FORM_FEEDBACK,UnsavedChangesDirective } from '@event-ticketing/ui';
import { OperationsApi,operationError } from './operations-api';
import { Category } from './operation-models';
@Component({
    selector: 'etp-categories', imports: [FormsModule, ...FORM_FEEDBACK, ConfirmDialogComponent, UnsavedChangesDirective], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './categories.component.html', styleUrl: './operations.scss'
})
export class CategoriesComponent {
 readonly api=inject(OperationsApi);private readonly destroy=inject(DestroyRef);readonly rows=signal<Category[]>([]);readonly next=signal<string|null>(null);readonly busy=signal(false);readonly error=signal('');readonly notice=signal('');readonly confirmation=new ConfirmationState();name='';reason='';
 constructor(){refreshOnReturn(()=>void this.load(),()=>this.busy()||this.hasUnsavedChanges()||!!this.confirmation.current());this.load();}hasUnsavedChanges(){return !!this.name;}
 load(more=false){this.busy.set(true);this.error.set('');this.api.categories(more?this.next()??undefined:undefined).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:p=>{this.rows.set(more?[...this.rows(),...p.data]:p.data);this.next.set(p.nextCursor);},error:e=>this.error.set(operationError(e))});}
 create(form?:NgForm){if(this.busy()||!this.name.trim())return;this.busy.set(true);this.error.set('');this.api.createCategory(this.name.trim()).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:()=>{this.name='';form?.form.markAsPristine();form?.form.markAsUntouched();this.notice.set('Đã tạo danh mục.');this.load();},error:e=>this.error.set(operationError(e))});}
 toggle(row:Category){if(this.busy())return;this.reason='';this.confirmation.ask({title:row.active?'Ngừng cho chọn danh mục?':'Mở lại danh mục?',subject:row.name,description:'Sự kiện đã dùng danh mục vẫn giữ nguyên lịch sử.',reasonRequired:true,danger:row.active,confirmLabel:row.active?'Ngừng sử dụng':'Mở lại'},()=>{const reason=this.reason.trim();if(!reason)return;this.busy.set(true);this.error.set('');this.api.category(row,reason).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy.set(false))).subscribe({next:r=>{this.rows.update(rows=>rows.map(old=>old.id===r.id?r:old));this.reason='';this.notice.set('Đã lưu trạng thái danh mục.');},error:e=>this.error.set(operationError(e))});});}
}
