import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { PasswordFieldComponent } from './password-field.component';
import { FORM_FEEDBACK } from './form-feedback.directive';
@Component({imports:[FormsModule,PasswordFieldComponent,...FORM_FEEDBACK],template:`<form (ngSubmit)="submitted=true"><label for="one">Mật khẩu</label><etp-password-field><input etpField id="one" type="password" name="one" [(ngModel)]="one" required></etp-password-field><label for="two">Nhập lại</label><etp-password-field label="mật khẩu nhập lại"><input etpField id="two" type="password" name="two" [(ngModel)]="two"></etp-password-field></form>`})
class Host {one='Sample-password-123';two='Sample-password-123';submitted=false;}
describe('Password visibility',()=>{
 it('toggles each field independently without submitting or replacing its value',async()=>{
  const f=TestBed.createComponent(Host);f.detectChanges();await f.whenStable();f.detectChanges();
  const fields=f.nativeElement.querySelectorAll('input') as NodeListOf<HTMLInputElement>;
  const buttons=f.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
  expect(fields[0].type).toBe('password');expect(fields[1].type).toBe('password');
  fields[0].setSelectionRange(2,5);buttons[0].click();f.detectChanges();
  expect(fields[0].type).toBe('text');expect(fields[0].value).toBe('Sample-password-123');expect(fields[0].selectionStart).toBe(2);
  expect(fields[1].type).toBe('password');expect(buttons[0].getAttribute('aria-label')).toBe('Ẩn mật khẩu');expect(buttons[0].getAttribute('aria-pressed')).toBe('true');expect(f.componentInstance.submitted).toBeFalse();
  buttons[0].click();f.detectChanges();expect(fields[0].type).toBe('password');
  buttons[1].click();f.detectChanges();expect(fields[1].type).toBe('text');expect(fields[0].type).toBe('password');f.destroy();
 });
});
