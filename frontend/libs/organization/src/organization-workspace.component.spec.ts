import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { SessionStore } from '@event-ticketing/session';
import { OrganizationWorkspaceComponent } from './organization-workspace.component';
import { OrganizationApi } from './organization-api';

describe('Organization workspace context',()=>{
  beforeEach(()=>TestBed.configureTestingModule({providers:[provideRouter([]),
    {provide:SessionStore,useValue:{user:()=>({roles:['CUSTOMER','ORGANIZER']})}},
    {provide:OrganizationApi,useValue:{organization:()=>of({id:'org',name:'Tổ chức kiểm thử',status:'ACTIVE',owner:true})}},
  ]}));
  it('retains every owner destination and marks the report route as selected',()=>{
    const f=TestBed.createComponent(OrganizationWorkspaceComponent);
    f.componentRef.setInput('organizationId','org');f.componentRef.setInput('active','analytics');f.detectChanges();
    expect(f.nativeElement.querySelectorAll('nav a').length).toBe(7);
    expect(f.nativeElement.querySelector('[aria-current="page"]').textContent).toBe('Báo cáo doanh thu');
    expect(f.nativeElement.querySelector('[aria-current="page"]').getAttribute('href')).toBe('/organizations/org/analytics');
    expect(f.nativeElement.querySelector('h1').textContent).toBe('Tổ chức kiểm thử');
  });
  it('uses current organization permissions for navigation visibility',()=>{
    const f=TestBed.createComponent(OrganizationWorkspaceComponent);
    f.componentRef.setInput('organization',{id:'org',name:'Tổ chức',status:'ACTIVE',owner:false,permission:'FINANCE'});f.detectChanges();
    const names=f.componentInstance.sections().map(s=>s.id);
    expect(names).toEqual(['general','analytics','settlements','payouts']);
    f.componentRef.setInput('organization',{id:'org',name:'Tổ chức',status:'ACTIVE',owner:false,permission:'CHECKIN'});f.detectChanges();
    expect(f.componentInstance.sections().map(s=>s.id)).toEqual(['general']);
  });
});
