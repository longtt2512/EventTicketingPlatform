import { Component } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { refreshOnReturn } from './refresh-on-return';
import { PendingEdits } from './unsaved-changes';

@Component({template:''})
class RefreshHost {
  calls=0;blocked=false;
  constructor(){refreshOnReturn(()=>this.calls++,()=>this.blocked);}
}
describe('Refresh on return',()=>{
  it('coalesces focus/reconnect events and removes listeners on destruction',fakeAsync(()=>{
    const f=TestBed.createComponent(RefreshHost);f.detectChanges();
    spyOnProperty(document,'visibilityState','get').and.returnValue('visible');
    window.dispatchEvent(new Event('focus'));window.dispatchEvent(new Event('online'));tick(160);
    expect(f.componentInstance.calls).toBe(1);
    f.destroy();window.dispatchEvent(new Event('focus'));tick(160);
    expect(f.componentInstance.calls).toBe(1);
  }));
  it('preserves dirty forms, pending work and hidden tabs',fakeAsync(()=>{
    const f=TestBed.createComponent(RefreshHost);f.detectChanges();
    spyOnProperty(document,'visibilityState','get').and.returnValue('visible');
    const edits=TestBed.inject(PendingEdits),dirty=()=>true;edits.register(dirty);
    window.dispatchEvent(new Event('focus'));tick(160);expect(f.componentInstance.calls).toBe(0);
    edits.unregister(dirty);f.componentInstance.blocked=true;
    window.dispatchEvent(new Event('focus'));tick(160);expect(f.componentInstance.calls).toBe(0);
    f.componentInstance.blocked=false;f.nativeElement.hidden=true;
    window.dispatchEvent(new Event('focus'));tick(160);expect(f.componentInstance.calls).toBe(0);
    f.nativeElement.hidden=false;window.dispatchEvent(new Event('focus'));tick(160);
    expect(f.componentInstance.calls).toBe(1);f.destroy();
  }));
});
