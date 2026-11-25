import { Component, ViewChild } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule, NgForm } from '@angular/forms';
import { FORM_FEEDBACK } from './form-feedback.directive';
import { parseVnd } from './vnd-input.directive';
@Component({imports:[FormsModule,...FORM_FEEDBACK],template:`<form #form="ngForm" etpForm><label for="money">Giá vé (VNĐ)</label><input id="money" etpField etpVnd name="price" [(ngModel)]="price" min="0" [max]="max" required><label for="optional">Mô tả</label><input id="optional" etpField [etpRequiredForReview]="true" name="description" ngModel></form>`})
class Host {price:number|null=1500000;max=2000000;@ViewChild('form') form!:NgForm;}
describe('VND money input and required markers',()=>{
 it('accepts dong grouping without rounding decimals or unsafe integers',()=>{
  expect(parseVnd('1.500.000 ₫')).toBe(1500000);expect(parseVnd('1,500,000 VND')).toBe(1500000);expect(parseVnd('0')).toBe(0);expect(parseVnd('')).toBeNull();
  for(const v of ['1.5','1,5','-500','1e5','9007199254740992','abc'])expect(Number.isNaN(parseVnd(v)!)).toBeTrue();
 });
 it('formats typed digits while preserving numeric model, cursor, required state and range validation',async()=>{
  const f=TestBed.createComponent(Host);f.detectChanges();await f.whenStable();f.detectChanges();const input=f.nativeElement.querySelector('#money') as HTMLInputElement;
  expect(input.value).toBe('1.500.000');expect(input.type).toBe('text');expect(input.inputMode).toBe('numeric');
  expect(f.nativeElement.querySelectorAll('.required-marker').length).toBe(2);expect(input.getAttribute('aria-required')).toBe('true');expect(f.componentInstance.form.valid).toBeTrue();
  input.value='150000';input.setSelectionRange(6,6);input.dispatchEvent(new InputEvent('input',{bubbles:true,inputType:'insertText',data:'0'}));f.detectChanges();expect(input.value).toBe('150.000');expect(f.componentInstance.price).toBe(150000);expect(input.selectionStart).toBe(7);
  input.value='2.500.000';input.dispatchEvent(new InputEvent('input',{bubbles:true,inputType:'insertFromPaste'}));input.dispatchEvent(new Event('blur'));f.detectChanges();expect(f.componentInstance.form.invalid).toBeTrue();expect(f.nativeElement.querySelector('#money-error').textContent).toContain('2.000.000');
  input.value='1.5';input.dispatchEvent(new InputEvent('input',{bubbles:true,inputType:'insertFromPaste'}));f.detectChanges();expect(f.componentInstance.form.invalid).toBeTrue();expect(input.value).toBe('1.5');expect(f.nativeElement.querySelector('#money-error').textContent).toContain('VNĐ nguyên');
  input.value='';input.dispatchEvent(new InputEvent('input',{bubbles:true,inputType:'deleteContentBackward'}));f.detectChanges();expect(f.componentInstance.price).toBeNull();expect(f.componentInstance.form.controls['price'].hasError('required')).toBeTrue();
  input.value='0';input.dispatchEvent(new InputEvent('input',{bubbles:true,inputType:'insertText',data:'0'}));f.detectChanges();expect(f.componentInstance.form.valid).toBeTrue();expect(f.componentInstance.price).toBe(0);f.destroy();
 });
});
