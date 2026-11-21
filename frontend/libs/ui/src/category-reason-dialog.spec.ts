import { TestBed } from '@angular/core/testing';
import { ConfirmDialogComponent } from './confirm-dialog.component';
describe('Category reason dialog',()=>{
 it('keeps focus in the dialog and rejects empty or whitespace reasons',()=>{
  const f=TestBed.createComponent(ConfirmDialogComponent);f.componentRef.setInput('confirmation',{title:'Ngừng danh mục?',subject:'Workshop',description:'Giữ nguyên lịch sử.',confirmLabel:'Ngừng sử dụng',reasonRequired:true});f.detectChanges();
  const submit=spyOn(f.componentInstance.resolved,'emit');const reason=spyOn(f.componentInstance.reasonSubmitted,'emit');
  const buttons=f.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;const field=f.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
  buttons[1].click();f.detectChanges();expect(submit).not.toHaveBeenCalled();expect(f.nativeElement.textContent).toContain('Vui lòng nhập lý do');
  field.value='   ';field.dispatchEvent(new Event('input'));buttons[1].click();expect(submit).not.toHaveBeenCalled();
  field.value='  Ngừng lựa chọn mới  ';field.dispatchEvent(new Event('input'));buttons[1].click();expect(reason).toHaveBeenCalledWith('Ngừng lựa chọn mới');expect(submit).toHaveBeenCalledWith(true);f.destroy();
 });
});
