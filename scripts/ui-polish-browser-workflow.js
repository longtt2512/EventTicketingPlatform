async page => {
 const f=__FIXTURE__,base='http://localhost:14200',checks=[],errors=[],serverErrors=[],surfaces=[],contexts=[],browser=page.context().browser(),tokens=new Map();
 let active=page;
 const check=(ok,name)=>{if(!ok)throw new Error(name);checks.push(name);};
 const ready=async p=>{await p.waitForLoadState('networkidle');await p.evaluate(()=>document.fonts.ready);};
 const fresh=async()=>{const c=await browser.newContext({viewport:{width:1440,height:1000}});contexts.push(c);const p=await c.newPage();p.setDefaultTimeout(15000);p.on('dialog',d=>d.accept());p.on('pageerror',e=>errors.push(e.message));p.on('response',r=>{if(r.status()>=500)serverErrors.push({path:new URL(r.url()).pathname,status:r.status()});});return p;};
 const login=async(p,prefix,email,password)=>{await p.goto(base+prefix+'/login');await p.getByLabel('Email',{exact:true}).fill(email);await p.getByLabel('Mật khẩu',{exact:true}).fill(password);const response=p.waitForResponse(r=>r.url().endsWith('/auth/login')&&r.request().method()==='POST');await p.getByRole('button',{name:'Đăng nhập',exact:true}).click();tokens.set(p,(await(await response).json()).accessToken);await p.waitForURL('**'+prefix+'/home');await ready(p);};
 const api=async(p,path)=>{const r=await p.request.get(base+'/api/v1'+path,{headers:{Authorization:'Bearer '+tokens.get(p)}});if(!r.ok())throw new Error('Read '+path+': '+r.status());return r.json();};
 const go=async(p,path)=>{active=p;await p.goto(base+path);await ready(p);};
 const capture=async(p,name)=>{
  active=p;await ready(p);surfaces.push(name);
  check(await p.getByRole('button',{name:/^(Tải lại|Làm mới|Cập nhật trạng thái)/}).count()===0,name+' has no ordinary reload control');
  const nav=p.getByRole('navigation',{name:'Quản lý tổ chức'});if(await nav.count())check(await nav.locator('[aria-current=page]').count()===1,name+' has exactly one active organization section');
  const body=await p.locator('main').innerText();
  check(!/Ghi nhận bằng chứng mô phỏng local|Không chuyển tiền ngân hàng|môi trường local|Đây là giao dịch mô phỏng/i.test(body),name+' has task-focused copy');
  const codes=body.match(/[A-Z]+(?:_[A-Z]+)+/g)??[];
  check(codes.length===0,name+' has readable enums: '+codes.join(','));
  for(const width of [1440,768,390,320]){
   await p.setViewportSize({width,height:1000});await p.waitForTimeout(200);await ready(p);
   check(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),name+' fits '+width);
   const aligned=await p.locator('.operation-details dl').evaluateAll(lists=>lists.every(dl=>[...dl.querySelectorAll('dt')].every(dt=>{const dd=dt.nextElementSibling;if(!dd)return true;const a=dt.getBoundingClientRect(),b=dd.getBoundingClientRect();return b.left>=a.right?Math.abs(a.top-b.top)<5:b.top>=a.bottom+4;})));
   check(aligned,name+' financial labels align with values '+width);
   if(width===1440||width===390)await p.screenshot({path:'output/playwright/ui-polish/'+name+'-'+width+'.png',fullPage:true});
  }
  await p.setViewportSize({width:1440,height:1000});
 };
 const eyes=async(p,name)=>{
  await p.locator('etp-password-field').first().waitFor();await ready(p);
  for(const width of [1440,768,390,320]){
   await p.setViewportSize({width,height:1000});
   for(const size of ['16px','32px']){
    await p.evaluate(size=>document.documentElement.style.fontSize=size,size);
    const positions=await p.locator('etp-password-field').evaluateAll(hosts=>hosts.filter(h=>h.getClientRects().length).map(h=>{
     const input=h.querySelector('input'),button=h.querySelector('button'),i=input.getBoundingClientRect(),b=button.getBoundingClientRect(),error=h.querySelector('.field-error');
     return {delta:Math.abs(b.top+b.height/2-i.top-i.height/2),inside:b.top>=i.top-1&&b.bottom<=i.bottom+1&&b.left>=i.left&&b.right<=i.right+1,target:b.width>=44&&b.height>=44,errorBelow:!error||error.hidden||error.getBoundingClientRect().top>=i.bottom};
    }));
    check(positions.length>0&&positions.every(x=>x.delta<=1&&x.inside&&x.target&&x.errorBelow),name+' eyes centered inside inputs '+width+'/'+size);
   }
  }
  await p.evaluate(()=>document.documentElement.style.removeProperty('font-size'));await p.setViewportSize({width:1440,height:1000});
  const input=p.locator('etp-password-field input:visible').first(),button=p.locator('etp-password-field button:visible').first();
  await input.fill('QA-visibility-only');await button.click();check(await input.getAttribute('type')==='text'&&await input.inputValue()==='QA-visibility-only',name+' eye preserves input');
  await button.focus();await p.keyboard.press('Enter');check(await input.getAttribute('type')==='password',name+' keyboard hides password');await input.fill('');
 };
 try{
  for(const prefix of ['','/org','/admin','/staff']){
   const p=await fresh(),key=prefix.slice(1)||'customer';
   for(const mode of ['login','register','reset-password#token=invalid-layout-test']){
    await go(p,prefix+'/'+mode);const name=key+'-'+mode.split('#')[0];
    await eyes(p,name+'-default');
    const fields=p.locator('etp-password-field input');for(let i=0;i<await fields.count();i++){await fields.nth(i).fill('');await fields.nth(i).focus();await fields.nth(i).blur();}
    await eyes(p,name+'-error');await capture(p,name+'-error');
   }
   await login(p,prefix,prefix==='/admin'?f.adminEmail:f.owner,prefix==='/admin'?f.adminPassword:f.password);
   await go(p,prefix+'/profile');await p.getByRole('button',{name:'Bảo mật',exact:true}).click();await eyes(p,key+'-profile-default');
   const fields=p.locator('etp-password-field input');for(let i=0;i<await fields.count();i++){await fields.nth(i).focus();await fields.nth(i).blur();}
   await eyes(p,key+'-profile-error');await capture(p,key+'-profile-error');
  }
  const organizer=await fresh();await login(organizer,'/org',f.owner,f.password);
  const org=(await api(organizer,'/organizations')).find(o=>o.name.startsWith(f.orgName));check(!!org,'Synthetic organization available');
  await go(organizer,'/org/organizations/'+org.id);
  const name=organizer.getByLabel('Tên tổ chức',{exact:true}),original=await name.inputValue();await name.fill(original+' chưa lưu');
  let reads=0;const count=r=>{if(r.url().endsWith('/organizations/'+org.id)&&r.method()==='GET')reads++;};organizer.on('request',count);
  await organizer.evaluate(()=>window.dispatchEvent(new Event('focus')));await organizer.waitForTimeout(250);
  check(reads===0&&await name.inputValue()===original+' chưa lưu','Returning preserves unsaved organization fields');
  await name.fill(original);const response=organizer.waitForResponse(r=>r.url().endsWith('/organizations/'+org.id)&&r.request().method()==='GET');
  await organizer.evaluate(()=>window.dispatchEvent(new Event('focus')));await response;await ready(organizer);check(reads===1,'Clean organization refreshes automatically on return');organizer.off('request',count);
  const nav=organizer.getByRole('navigation',{name:'Quản lý tổ chức'});
  await nav.getByRole('link',{name:'Báo cáo doanh thu',exact:true}).click();await organizer.waitForURL('**/analytics');await ready(organizer);
  check(await nav.locator('[aria-current=page]').innerText()==='Báo cáo doanh thu','Revenue keeps organization navigation and active section');check(await nav.locator('a,button').count()===7,'Revenue keeps all seven owner destinations');await capture(organizer,'organizer-revenue');
  await nav.getByRole('link',{name:'Đối soát',exact:true}).click();await organizer.waitForURL('**/settlements');await ready(organizer);
  check(await nav.locator('[aria-current=page]').innerText()==='Đối soát','Settlement keeps navigation and active section');await capture(organizer,'organizer-settlements');
  await organizer.reload();await ready(organizer);check(await nav.locator('a,button').count()===7,'Settlement deep reload retains organization context');
  await nav.getByRole('link',{name:'Thành viên',exact:true}).click();await organizer.waitForURL('**section=members');await capture(organizer,'organizer-members');
  await nav.getByRole('button',{name:'Lịch sử thao tác',exact:true}).click();await ready(organizer);
  check(await organizer.getByText('Thêm thành viên',{exact:true}).count()>0,'Organization history translates member addition');
  check(!/\d{4}-\d\d-\d\dT\d/.test(await organizer.locator('main').innerText()),'Organization timestamps use local readable dates');await capture(organizer,'organizer-history');
  for(const section of ['general','payouts','events']){await go(organizer,'/org/organizations/'+org.id+'?section='+section);await capture(organizer,'organizer-'+section);}
  const admin=await fresh();await login(admin,'/admin',f.adminEmail,f.adminPassword);
  for(const path of ['/home','/users','/applications','/organizations','/events','/categories','/orders','/payments','/payment-exceptions','/analytics','/settlements','/commission','/settings','/audit-logs']){
   await go(admin,'/admin'+path);await capture(admin,'admin-'+path.slice(1));
  }
  await admin.getByLabel('Hành động',{exact:true}).selectOption('MEMBER_ADDED');await admin.getByRole('button',{name:'Lọc nhật ký',exact:true}).click();await ready(admin);
  await admin.getByRole('button',{name:'Xem bản ghi',exact:true}).first().click();await capture(admin,'admin-history-detail');
  check(await admin.locator('.activity-details').count()===1&&await admin.locator('pre').count()===0,'Audit detail is labelled evidence rather than raw JSON');
  check((await admin.locator('.activity-details').innerText()).includes('Soát vé')&&!(await admin.locator('.activity-details').innerText()).includes('CHECKIN'),'Admin activity reason translates the stored membership permission');
  const errorPage=await fresh();await login(errorPage,'/org',f.owner,f.password);
  await errorPage.route('**/api/v1/organizations',route=>route.abort('failed'));await go(errorPage,'/org/organizations');
  await errorPage.getByRole('alert').waitFor();check(await errorPage.getByRole('button',{name:'Thử lại',exact:true}).isVisible(),'Read failure offers contextual retry');await capture(errorPage,'organizer-load-error');
  await errorPage.unroute('**/api/v1/organizations');await errorPage.getByRole('button',{name:'Thử lại',exact:true}).click();await errorPage.getByRole('link',{name:org.name,exact:true}).waitFor();check(await errorPage.getByRole('button',{name:'Thử lại',exact:true}).count()===0,'Retry disappears after recovery');
  const customer=await fresh();await login(customer,'',f.owner,f.password);
  for(const path of ['/home','/events','/account/orders','/account/tickets']){await go(customer,path);await capture(customer,'customer-'+path.slice(1).replaceAll('/','-'));}
  const staff=await fresh();await login(staff,'/staff',f.owner,f.password);await go(staff,'/staff/check-in/events');await capture(staff,'staff-events');
  const assigned=await api(staff,'/check-in/events');if(assigned.length){for(const mode of ['scan','lookup','history','stats']){await go(staff,'/staff/check-in/events/'+assigned[0].event.id+'/'+mode);await capture(staff,'staff-'+mode);}}
  check(errors.length===0,'No JavaScript exceptions');check(serverErrors.length===0,'No unexpected server 5xx');
  return {passed:checks.length,checks,surfaces,errors,serverErrors};
 }catch(e){await active.screenshot({path:'output/playwright/ui-polish/failure.png',fullPage:true});return {failure:String(e),passed:checks.length,checks,surfaces,errors,serverErrors};}
 finally{for(const c of contexts)await c.close();}
}
