import { HttpErrorResponse } from '@angular/common/http';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { EventApi } from './event-api';
import { EventEditorComponent } from './event-editor.component';
import { EventDetail } from './event-models';
const draft:EventDetail={id:'event',organizationId:'org',name:'Test',status:'DRAFT',version:0,category:null,description:'',venueName:'',venueAddress:'',city:'',timezone:'Asia/Ho_Chi_Minh',startAt:null,endAt:null,capacity:null,terms:'',bannerMediaId:null,galleryMediaIds:[],featured:false,publishedAt:null,reviewReason:'',organizationName:'',organizationDescription:'',updatedAt:'2026-11-21T00:00:00Z',ticketTypes:[]};
describe('Authoring recovery and feedback',()=>{
 let api:jasmine.SpyObj<EventApi>;
 beforeEach(()=>{api=jasmine.createSpyObj('EventApi',['categories','detail','history','update','command']);api.categories.and.returnValue(of(['Workshop']));api.detail.and.returnValue(of(draft));api.history.and.returnValue(of([]));TestBed.configureTestingModule({providers:[provideRouter([]),{provide:EventApi,useValue:api},{provide:ActivatedRoute,useValue:{snapshot:{paramMap:convertToParamMap({org:'org',event:'event'})},queryParamMap:of(convertToParamMap({step:'1'}))}}]});});
 it('shows review requirements without preventing an incomplete draft save',fakeAsync(()=>{
  const f=TestBed.createComponent(EventEditorComponent);f.detectChanges();tick();f.detectChanges();const c=f.componentInstance;api.update.and.returnValue(of({...draft,version:1}));
  expect(c.missing().some(i=>i.field==='event-terms'&&i.step===2)).toBeTrue();expect(f.nativeElement.querySelector('label[for="event-terms"] .required-marker')).not.toBeNull();
  c.save();expect(api.update).toHaveBeenCalled();expect(c.message()).toBe('Đã lưu bản nháp.');c.action('submit');expect(api.command).not.toHaveBeenCalled();expect(c.message()).toBe('');expect(c.error()).toContain('Bổ sung');f.destroy();tick();
 }));
 it('replaces a saved message with the next failure, preserving entered content',fakeAsync(()=>{
  const f=TestBed.createComponent(EventEditorComponent);f.detectChanges();tick();f.detectChanges();const c=f.componentInstance;
  c.message.set('Đã lưu bản nháp.');c.input.description='Work to preserve';api.update.and.returnValue(throwError(()=>new HttpErrorResponse({status:422,error:{code:'EVENT_VALIDATION',detail:'Không lưu được. Thử lại.'}})));c.save();f.detectChanges();expect(c.message()).toBe('');expect(c.error()).toContain('Không lưu được');expect(c.input.description).toBe('Work to preserve');expect(f.nativeElement.querySelector('.notice.success')).toBeNull();f.destroy();tick();
 }));
 it('blocks past dates before a network update and clears old success',fakeAsync(()=>{
  const f=TestBed.createComponent(EventEditorComponent);f.detectChanges();tick();f.detectChanges();const c=f.componentInstance;
  c.start='2000-01-01T12:00';c.end='2000-01-01T13:00';c.message.set('Đã lưu bản nháp.');c.save();expect(api.update).not.toHaveBeenCalled();expect(c.error()).toContain('tương lai');expect(c.message()).toBe('');f.destroy();tick();
 }));
});
