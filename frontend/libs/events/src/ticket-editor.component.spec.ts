import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TicketEditorComponent } from './ticket-editor.component';
import { EventDetail } from './event-models';
const event:EventDetail={id:'event',organizationId:'org',name:'Workshop',status:'PUBLISHED',version:4,category:'Workshop',description:'',venueName:'',venueAddress:'',city:'',timezone:'Asia/Ho_Chi_Minh',startAt:'2027-03-02T03:00:00Z',endAt:'2027-03-02T05:00:00Z',capacity:10,terms:'',bannerMediaId:null,galleryMediaIds:[],featured:false,publishedAt:'2027-01-01T00:00:00Z',reviewReason:'',organizationName:'',organizationDescription:'',updatedAt:'2027-01-01T00:00:00Z',ticketTypes:[{id:'ticket',name:'Vé thường',description:'',price:100000,quota:10,reserved:2,sold:1,available:7,saleStart:'2027-01-01T00:00:00Z',saleEnd:'2027-03-02T03:00:00Z',maxPerOrder:2,paused:false,version:0,saleState:'ON_SALE'}]};
describe('Published ticket changes',()=>{
 beforeEach(()=>TestBed.configureTestingModule({imports:[TicketEditorComponent],providers:[provideHttpClient(),provideHttpClientTesting()]}));
 afterEach(()=>TestBed.inject(HttpTestingController).verify());
 it('does not pause until confirmation; cancellation makes no request',()=>{
  const f=TestBed.createComponent(TicketEditorComponent);f.componentRef.setInput('event',event);f.detectChanges();const component=f.componentInstance,http=TestBed.inject(HttpTestingController);
  component.pause(event.ticketTypes[0]);http.expectNone('/api/v1/organizations/org/events/event/ticket-types/ticket/pause');component.confirmation.resolve(false);http.expectNone('/api/v1/organizations/org/events/event/ticket-types/ticket/pause');
  component.pause(event.ticketTypes[0]);component.confirmation.resolve(true);const request=http.expectOne('/api/v1/organizations/org/events/event/ticket-types/ticket/pause');expect(request.request.headers.get('If-Match')).toBe('0');request.flush({...event,version:5});expect(component.message()).toContain('tạm dừng');
 });
 it('keeps ticket edits after a failed save',async()=>{
  const f=TestBed.createComponent(TicketEditorComponent);f.componentRef.setInput('event',{...event,status:'DRAFT'});f.detectChanges();f.componentInstance.edit(event.ticketTypes[0]);f.detectChanges();await f.whenStable();
  f.componentInstance.save();TestBed.inject(HttpTestingController).expectOne('/api/v1/organizations/org/events/event/ticket-types/ticket').flush({code:'EVENT_VALIDATION',detail:'Hạn mức vượt sức chứa.'},{status:422,statusText:'Invalid'});
  expect(f.componentInstance.editing()).toBeTrue();expect(f.componentInstance.name).toBe('Vé thường');expect(f.componentInstance.error()).toContain('sức chứa');expect(f.componentInstance.busy()).toBeFalse();
 });
});
