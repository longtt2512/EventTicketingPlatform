async page => {
 const f=__FIXTURE__,checks=[],errors=[],serverErrors=[],accessibilityFindings=[],surfaces=[],contexts=[],tokens=new Map();
 const browser=page.context().browser();let active=page;
 const check=(ok,name)=>{if(!ok)throw new Error(name);checks.push(name);};
 const ready=async p=>{await p.waitForLoadState('networkidle');await p.evaluate(()=>document.fonts.ready);};
 const visit=async(p,path)=>{active=p;await p.goto(new URL((new URL(p.url()).pathname.match(/^\/(org|admin|staff)(?:\/|$)/)?.[0].replace(/\/$/,'')??'')+path,p.url()).href);await ready(p);};
 const api=async(p,path,options={})=>{const r=await p.request.fetch(new URL('/api/v1'+path,p.url()).href,{...options,headers:{Authorization:'Bearer '+tokens.get(p),...options.headers}});if(!r.ok())throw new Error('Fixture API '+path+': '+r.status());return r.json();};
 const workspace=async(port,email,password)=>{
  const c=await browser.newContext();contexts.push(c);const p=await c.newPage();active=p;p.setDefaultTimeout(15000);
  p.on('pageerror',e=>errors.push(e.message));p.on('response',r=>{if(r.status()>=500)serverErrors.push({url:r.url(),status:r.status()});});
  await p.goto('http://localhost:14200'+({14200:'',14201:'/org',14202:'/admin',14203:'/staff'}[port])+'/login');await p.getByLabel('Email',{exact:true}).fill(email);await p.getByLabel('Mật khẩu',{exact:true}).fill(password);
  const response=p.waitForResponse(r=>r.url().endsWith('/auth/login')&&r.request().method()==='POST');await p.getByRole('button',{name:'Đăng nhập',exact:true}).click();
  tokens.set(p,(await(await response).json()).accessToken);await p.waitForURL('**/home');await ready(p);return p;
 };
 const inspect=async(p,name)=>{
  active=p;await ready(p);
  for(const width of [1440,768,390,320]){
   await p.setViewportSize({width,height:1000});
   check(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),name+' reflows at '+width+'px');
   if(width===390||width===1440)await p.screenshot({path:'output/playwright/phase6-'+name+'-'+width+'.png',fullPage:true});
  }
  await p.setViewportSize({width:1440,height:1000});
  await p.mouse.move(0,0);
  await p.evaluate(async()=>{await Promise.all(document.getAnimations().filter(a=>a.effect?.getTiming().iterations!==Infinity).map(a=>a.finished.catch(()=>{})));});
  const audit=await p.evaluate(()=>{
   const visible=e=>e.getClientRects().length&&getComputedStyle(e).visibility!=='hidden'&&!e.closest('[aria-hidden=true]');
   const named=e=>e.getAttribute('aria-label')||e.getAttribute('title')||(e.getAttribute('aria-labelledby')||'').split(' ').map(id=>document.getElementById(id)?.textContent||'').join('').trim()||[...(e.labels||[])].map(l=>l.textContent).join('').trim()||e.textContent?.trim();
   const inputs=[...document.querySelectorAll('input:not([type=hidden]),select,textarea')].filter(visible);
   const buttons=[...document.querySelectorAll('button')].filter(visible);
   const ids=[...document.querySelectorAll('[id]')].map(e=>e.id);
   const findings=[];
   const rgb=s=>{const a=s.match(/[\d.]+/g)?.map(Number);return a?.length>=3?a:null;};
   const luminance=a=>a.slice(0,3).map(x=>x/255).map(x=>x<=.04045?x/12.92:((x+.055)/1.055)**2.4).reduce((s,x,i)=>s+x*[.2126,.7152,.0722][i],0);
   let sampled=0,complexBackgrounds=0;
   for(const e of document.querySelectorAll('main p,main label,main button,main a,main dt,main dd,main th,main td,nav a')){
    if(!visible(e)||e.matches(':disabled')||!e.textContent.trim()||e.getAnimations().some(a=>a.playState==='running'))continue;
    const style=getComputedStyle(e),fg=rgb(style.color);let parent=e,bg=null,complex=false;
    while(parent){const s=getComputedStyle(parent);if(s.backgroundImage!=='none'||Number(s.opacity)<1){complex=true;break;}const color=rgb(s.backgroundColor);if(color&&(color.length===3||color[3]===1)){bg=color;break;}if(color&&color[3]>0){complex=true;break;}parent=parent.parentElement;}
    if(complex||!fg||fg[3]<1){complexBackgrounds++;continue;}bg=bg||[255,255,255];sampled++;
    const a=luminance(fg),b=luminance(bg),ratio=(Math.max(a,b)+.05)/(Math.min(a,b)+.05);
    const large=parseFloat(style.fontSize)>=24||(parseFloat(style.fontSize)>=18.66&&Number(style.fontWeight)>=700),minimum=large?3:4.5;
    if(ratio+.01<minimum)findings.push({kind:'contrast',tag:e.tagName,className:e.className,text:e.textContent.trim().slice(0,70),ratio:+ratio.toFixed(2),minimum});
   }
   return {main:document.querySelectorAll('main').length,h1:document.querySelectorAll('main h1').length,unnamedInputs:inputs.filter(e=>!named(e)).map(e=>e.id||e.name),unnamedButtons:buttons.filter(e=>!named(e)).map(e=>e.outerHTML.slice(0,120)),missingAlt:[...document.querySelectorAll('img')].filter(visible).filter(e=>!e.hasAttribute('alt')).length,duplicateIds:ids.filter((id,i)=>ids.indexOf(id)!==i),sampled,complexBackgrounds,findings};
  });
  check(audit.main===1&&audit.h1===1,name+' has one main landmark and page heading');
  check(!audit.unnamedInputs.length&&!audit.unnamedButtons.length,name+' exposes names for visible form controls');
  check(!audit.missingAlt&&!audit.duplicateIds.length,name+' has image alternatives and unique element IDs');
  accessibilityFindings.push(...audit.findings.map(x=>({surface:name,...x})));surfaces.push({name,path:new URL(p.url()).pathname,...audit});
 };
 const textScale=async(p,name)=>{
  await p.setViewportSize({width:1280,height:1000});await p.evaluate(()=>document.documentElement.style.fontSize=(parseFloat(getComputedStyle(document.documentElement).fontSize)*2)+'px');
  check(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),name+' reflows at 200% root text size');
  await p.screenshot({path:'output/playwright/phase6-'+name+'-text-200.png',fullPage:true});
  await p.evaluate(()=>document.documentElement.style.fontSize='');
  const beforeSkip=p.url();await p.locator('.skip-link').focus();await p.keyboard.press('Enter');
  check(await p.locator('main').evaluate(e=>e===document.activeElement)&&p.url()===beforeSkip,name+' skip link focuses main and preserves the current route');
  await p.setViewportSize({width:1440,height:1000});
 };
 const failedQuery=async(p,path,pattern,retry,name)=>{
  await p.route(pattern,r=>r.abort('failed'));await visit(p,path);await p.getByRole('alert').first().waitFor();
  check(true,name+' explains a failed query');await p.unroute(pattern);
  const response=p.waitForResponse(r=>r.request().method()==='GET'&&r.ok()&&new RegExp(pattern.replaceAll('*','.*')).test(r.url()));
  await p.getByRole('button',{name:retry,exact:true}).click();await response;await ready(p);
  check(await p.getByRole('alert').count()===0,name+' retry loads real data and clears the error');
 };
 try {
  const customer=await workspace(14200,f.owner,f.password),organizer=await workspace(14201,f.owner,f.password),admin=await workspace(14202,f.adminEmail,f.adminPassword),staff=await workspace(14203,f.owner,f.password);
  // A fresh synthetic event: two paid units, one consumed by the mixed order and one for the race.
  const path='/organizations/'+f.phase3Org+'/events/'+f.phase3Event;
  let event=await api(organizer,path),paid=event.ticketTypes.find(t=>t.id===f.paidTicket);
  check(paid.quota===2&&paid.sold===0&&paid.reserved===0,'Fresh two-seat fixture has no prior allocations');
  await visit(customer,'/booking/'+f.phase3Event);await customer.getByLabel('Số lượng Vé tiêu chuẩn',{exact:true}).fill('1');await customer.getByLabel('Số lượng Vé cộng đồng',{exact:true}).fill('2');await customer.getByRole('checkbox',{name:'Tôi đồng ý điều khoản tham dự'}).check();
  const held=customer.waitForResponse(r=>r.url().endsWith('/orders/reservations')&&r.request().method()==='POST');await customer.getByRole('button',{name:'Tiếp tục thanh toán',exact:true}).click();const mixed=await(await held).json();
  check(mixed.totalVnd===100000&&mixed.items.reduce((s,i)=>s+i.quantity,0)===3,'BRS-T17 mixed checkout prices only paid units');
  await customer.waitForURL('**/checkout/'+mixed.id);await customer.getByRole('checkbox',{name:'Tôi đã kiểm tra đơn và đồng ý điều khoản'}).check();await customer.getByRole('button',{name:/^Thanh toán [0-9]/}).click();await customer.waitForURL('**/payment/checkout/*');
  await inspect(customer,'payment-methods');await customer.getByRole('button',{name:'Hiển thị mã QR',exact:true}).click();await customer.waitForURL('**/payment/result/*');
  let tickets=[];for(let i=0;i<80;i++){const r=await api(customer,'/tickets?orderId='+mixed.id);tickets=r.data;if(tickets.length===3)break;await new Promise(r=>setTimeout(r,250));}
  check(tickets.length===3&&new Set(tickets.map(t=>t.id)).size===3,'BRS-T17 mixed payment issues exactly three stable ticket identities');
  await customer.reload();await ready(customer);check((await api(customer,'/tickets?orderId='+mixed.id)).data.map(t=>t.id).sort().join()===tickets.map(t=>t.id).sort().join(),'UX-06 refreshing paid result does not issue extra tickets');
  const other=await workspace(14200,f.staff,f.password);
  for(const p of [customer,other]){await visit(p,'/booking/'+f.phase3Event);await p.getByLabel('Số lượng Vé tiêu chuẩn',{exact:true}).fill('1');await p.getByRole('checkbox',{name:'Tôi đồng ý điều khoản tham dự'}).check();}
  // Hold both browser submissions until both buyers reach the request boundary.
  let arrivals=0,release;const barrier=new Promise(r=>release=r);const racePattern='**/api/v1/orders/reservations';
  for(const p of [customer,other])await p.route(racePattern,async r=>{if(++arrivals===2)release();await barrier;await r.continue();});
  const responses=[customer,other].map(p=>p.waitForResponse(r=>r.url().endsWith('/orders/reservations')&&r.request().method()==='POST'));
  await Promise.all([customer,other].map(p=>p.getByRole('button',{name:'Tiếp tục thanh toán',exact:true}).click()));
  const results=await Promise.all(responses);check(results.map(r=>r.status()).sort().join()==='200,409','UX-02 simultaneous browsers compete for one last hold');
  for(const p of [customer,other])await p.unroute(racePattern);
  const loser=[customer,other][results.findIndex(r=>r.status()===409)],winner=[customer,other][results.findIndex(r=>r.status()===200)];const last=await results.find(r=>r.status()===200).json();
  await loser.getByRole('alert').waitFor();await loser.getByRole('button',{name:'Xem hạng vé hiện tại',exact:true}).click();await ready(loser);
  await loser.waitForFunction(id=>document.getElementById('qty-'+id)?.max==='0',f.paidTicket);
  const displayedStock=await loser.getByLabel('Số lượng Vé tiêu chuẩn',{exact:true}).getAttribute('max');
  check(displayedStock==='0'&&await loser.getByRole('button',{name:'Tăng số lượng Vé tiêu chuẩn',exact:true}).isDisabled(),'UX-02 losing buyer reloads authoritative zero available inventory; max='+displayedStock);
  await loser.getByRole('button',{name:'Giảm số lượng Vé tiêu chuẩn',exact:true}).click();
  check(await loser.getByLabel('Số lượng Vé tiêu chuẩn',{exact:true}).inputValue()==='0','UX-02 losing buyer can remove the unavailable line and choose another type');
  event=await api(organizer,path);paid=event.ticketTypes.find(t=>t.id===f.paidTicket);check(paid.sold===1&&paid.reserved===1&&paid.available===0,'BRS-T09 persisted inventory conserves the final two paid units');
  await api(winner,'/orders/'+last.id+'/cancel',{method:'POST',headers:{'Idempotency-Key':'phase6-cancel-'+last.id},data:{}});
  // Four workspaces, current data, desktop/tablet/mobile layouts and semantic checks.
  for(const [path,name] of [['/home','customer-home'],['/events','discovery'],['/booking/'+f.phase3Event,'booking'],['/account/orders','orders'],['/account/orders/'+mixed.id,'order'],['/account/tickets','tickets'],['/account/tickets/'+tickets[0].id,'ticket'],['/profile','profile']]){await visit(customer,path);await inspect(customer,name);}
  await textScale(customer,'customer');
  for(const [path,name] of [['/organizations','organizations'],['/organizations/'+f.phase3Org,'organization'],['/organizations/'+f.phase3Org+'/events/'+f.phase3Event,'event-editor'],['/organizations/'+f.phase3Org+'/analytics','organizer-analytics'],['/organizations/'+f.phase3Org+'/settlements','organizer-settlements']]){await visit(organizer,path);await inspect(organizer,name);}
  await textScale(organizer,'organizer');
  for(const path of ['/home','/users','/applications','/events','/payments','/payment-exceptions','/orders','/commission','/settlements','/categories','/audit-logs','/settings']){await visit(admin,path);await inspect(admin,'admin-'+path.slice(1));}
  await textScale(admin,'admin');
  await visit(staff,'/check-in/events');await inspect(staff,'checkin-events');
  for(const tab of ['scan','lookup','history','stats']){await visit(staff,'/check-in/events/'+f.phase3Event+'/'+tab);await inspect(staff,'checkin-'+tab);}
  await textScale(staff,'checkin');
  for(const p of [organizer,admin]){await p.setViewportSize({width:320,height:900});const menu=p.getByRole('button',{name:'Mở điều hướng',exact:true});await menu.focus();await p.keyboard.press('Enter');check(await menu.getAttribute('aria-expanded')==='true','Portal navigation opens by keyboard at 320px');await p.keyboard.press('Enter');check(await menu.getAttribute('aria-expanded')==='false','Portal navigation closes by keyboard at 320px');await p.setViewportSize({width:1440,height:1000});}
  await failedQuery(customer,'/events','**/api/v1/discover/events*','Thử tìm lại','Customer discovery');
  await failedQuery(organizer,'/organizations','**/api/v1/organizations','Thử lại','Organizer list');
  await failedQuery(admin,'/settings','**/api/v1/admin/settings','Thử lại','Admin settings');
  await failedQuery(staff,'/check-in/events','**/api/v1/check-in/events','Thử lại','Check-in events');
  await visit(customer,'/events?q=phase6-no-event-'+Date.now());await customer.getByRole('heading',{name:'Chưa tìm thấy sự kiện phù hợp'}).waitFor();check(true,'An empty search explains how to change filters');await customer.getByRole('button',{name:'Xóa bộ lọc',exact:true}).last().click();await customer.locator('etp-event-card').first().waitFor();check(await customer.locator('etp-event-card').count()>0,'Clearing an empty search restores real events');
  await visit(admin,'/settings');const form=admin.locator('form').first();await form.getByLabel('Lý do thay đổi',{exact:true}).fill('Phase 6 keyboard confirmation, cancelled without saving');const trigger=form.getByRole('button',{name:'Lưu thiết lập',exact:true});await trigger.focus();await admin.keyboard.press('Enter');const dialog=admin.getByRole('dialog');await dialog.waitFor();
  check(await dialog.getByRole('button',{name:'Hủy',exact:true}).evaluate(e=>e===document.activeElement),'Confirmation focuses the safe cancel action');
  for(let i=0;i<4;i++){await admin.keyboard.press('Tab');check(await dialog.evaluate(e=>e.contains(document.activeElement)),'Confirmation contains keyboard focus '+i);}
  await admin.keyboard.press('Shift+Tab');check(await dialog.evaluate(e=>e.contains(document.activeElement)),'Confirmation wraps reverse keyboard traversal');
  await admin.keyboard.press('Escape');check(await dialog.count()===0&&await trigger.evaluate(e=>e===document.activeElement),'Escape cancels confirmation and restores trigger focus');
  admin.once('dialog',d=>d.accept());await visit(admin,'/home');
  await customer.emulateMedia({reducedMotion:'reduce'});await visit(customer,'/home');check(await customer.locator('.hero-event-ticket').evaluate(e=>getComputedStyle(e).animationName==='none'),'Reduced motion keeps the featured event static and usable');
  await visit(customer,'/account/orders');await customer.getByRole('button',{name:'Đăng xuất',exact:true}).click();await customer.waitForURL('**/login');await visit(customer,'/account/orders');await customer.waitForURL('**/login?*');check(await customer.locator('etp-orders').count()===0,'UX-21 logout removes private orders and requires login again');
  check(accessibilityFindings.length===0,'No contrast failures in sampled stable text on simple opaque backgrounds');
  check(errors.length===0,'No JavaScript exceptions');check(serverErrors.length===0,'No unexpected server errors');
  return {passed:checks.length,checks,errors,serverErrors,accessibilityFindings,surfaces,eventId:f.phase3Event,mixedOrderId:mixed.id,scope:'local; no disaster recovery, backup, security/dependency audit or performance measurement'};
 }catch(e){await active.screenshot({path:'output/playwright/phase6-failure.png',fullPage:true});return {passed:checks.length,checks,errors,serverErrors,accessibilityFindings,surfaces,failure:String(e)};}
 finally{for(const c of contexts)await c.close();}
}
