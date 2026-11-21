import { TestBed } from '@angular/core/testing';
import { of, firstValueFrom } from 'rxjs';
import { REFRESH_COORDINATOR } from './refresh-lock';
describe('Cross-tab refresh coordination',()=>{
 it('holds an origin-wide browser lock until the refresh completes',async()=>{
  const lock=spyOn(navigator.locks,'request').and.callFake(((_name:string,callback:()=>Promise<unknown>)=>callback()) as typeof navigator.locks.request);
  const value=await firstValueFrom(TestBed.inject(REFRESH_COORDINATOR).run(()=>of('refreshed')));
  expect(value).toBe('refreshed');expect(lock.calls.first().args[0]).toBe('etp-session-refresh');
 });
});
