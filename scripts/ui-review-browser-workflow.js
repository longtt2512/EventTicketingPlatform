async page => {
 const f=__FIXTURE__,checks=[],errors=[],smallText=[],contexts=[],tokens=new Map(),browser=page.context().browser();
 let active=page;
 const check=(ok,name)=>{if(!ok)throw new Error(name);checks.push(name);};
 const ready=async p=>{await p.waitForLoadState('networkidle');await p.evaluate(()=>document.fonts.ready);};
 const api=async(p,path)=>{const r=await p.request.get(new URL('/api/v1'+path,p.url()).href,{headers:{Authorization:'Bearer '+tokens.get(p)}});if(!r.ok())throw new Error('Read request failed: '+r.status());return r.json();};
 const visit=async(p,path)=>{await p.goto(new URL((new URL(p.url()).pathname.match(/^\/(org|admin|staff)(?:\/|$)/)?.[0].replace(/\/$/,'')??'')+path,p.url()).href);await ready(p);};
 const capture=async(p,name)=>{
  active=p;await ready(p);
  for(const width of [1440,768,390,320]){
   await p.setViewportSize({width,height:1000});
   check(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),name+' fits '+width);
   if(width===1440||width===390)await p.screenshot({path:`output/playwright/ui-review-${name}-${width}.png`,fullPage:true});
  }
  const small=await p.evaluate(()=>[...document.querySelectorAll('main :is(p,small,button,a,label,th,td,dt,dd), nav a')].filter(e=>e.getClientRects().length&&getComputedStyle(e).visibility!=='hidden'&&parseFloat(getComputedStyle(e).fontSize)<11).map(e=>({tag:e.tagName,cls:e.className,size:getComputedStyle(e).fontSize})).slice(0,12));
  if(small.length)smallText.push({page:name,elements:small});
  await p.setViewportSize({width:1440,height:1000});
 };
 const workspace=async(port,email,password)=>{
  const c=await browser.newContext();contexts.push(c);const p=await c.newPage();active=p;p.setDefaultTimeout(15000);p.on('pageerror',e=>errors.push(e.message));
  await p.goto('http://localhost:14200'+({14200:'',14201:'/org',14202:'/admin',14203:'/staff'}[port])+'/login');await p.getByLabel('Email',{exact:true}).fill(email);await p.getByLabel('Mật khẩu',{exact:true}).fill(password);const response=p.waitForResponse(r=>r.url().endsWith('/auth/login')&&r.request().method()==='POST');await p.getByRole('button',{name:'Đăng nhập',exact:true}).click();tokens.set(p,(await(await response).json()).accessToken);await p.waitForURL('**/home');await ready(p);return p;
 };
 try{
  const customer=await workspace(14200,f.owner,f.password);
  const discovery=await api(customer,'/discover/events');check(discovery.data.length>0,'Live public event available');const event=discovery.data[0];
  await visit(customer,'/events/'+event.id);await capture(customer,'event-detail');
  await visit(customer,'/booking/'+event.id);await capture(customer,'ticket-selection');
  const orders=await api(customer,'/orders?status=CONFIRMED');check(orders.data.length>0,'Existing confirmed fixture order available');
  const order=orders.data[0];await visit(customer,'/account/orders/'+order.id);await customer.locator('etp-order-tickets .button-link').waitFor();await capture(customer,'confirmed-order');
  check(await customer.getByRole('link',{name:'Xem chi tiết đơn',exact:true}).count()===0,'Order detail has no redundant self-link');
  const backControl=customer.locator('main > etp-order > a.back-link');check(await backControl.evaluate(e=>e.tagName==='A'&&getComputedStyle(e).textDecorationLine==='none'&&e.getBoundingClientRect().height>=44),'Back control is a labelled anchor with a consistent touch target');
  await backControl.focus();check(await backControl.evaluate(e=>e===document.activeElement&&getComputedStyle(e).outlineStyle!=='none'),'Keyboard focus is visible on return navigation');
  const refreshed=customer.waitForResponse(r=>r.url().endsWith('/orders/'+order.id+'/tickets')&&r.request().method()==='GET');await customer.evaluate(()=>window.dispatchEvent(new Event('focus')));await refreshed;check(true,'Returning to the order updates issuance status automatically');
  await customer.getByRole('link',{name:'Xem vé của đơn hàng',exact:true}).click();await customer.waitForURL(u=>u.pathname==='/account/tickets');await ready(customer);check(new URL(customer.url()).searchParams.get('orderId')===order.id,'Ticket CTA preserves order scope');await capture(customer,'order-tickets');
  await customer.getByRole('link',{name:'Xem vé',exact:true}).first().click();await customer.waitForURL('**/account/tickets/*');await capture(customer,'ticket-detail');
  await customer.getByRole('link',{name:'Xem đơn hàng',exact:true}).click();await customer.waitForURL('**/account/orders/'+order.id);check(true,'Ticket return opens its originating order');
  await visit(customer,'/payment/result/'+order.id);await capture(customer,'payment-result');check(await customer.getByRole('link',{name:'Xem chi tiết đơn',exact:true}).evaluate(e=>e.classList.contains('button-link')),'Result page keeps a secondary order-details control');
  await visit(customer,'/account/orders');await capture(customer,'orders');await visit(customer,'/apply');await capture(customer,'customer-application');
  const organizer=await workspace(14201,f.owner,f.password);const orgs=await api(organizer,'/organizations');check(orgs.length>0,'Existing organizer fixture available');const org=orgs[0];
  for(const tab of ['general','members','payouts','events','audit']){await visit(organizer,'/organizations/'+org.id+'?section='+tab);await capture(organizer,'organization-'+tab);}
  const events=await api(organizer,'/organizations/'+org.id+'/events');check(events.length>0,'Existing authoring fixture available');const authored=events.find(e=>e.id===event.id)||events[0];
  await visit(organizer,'/organizations/'+org.id+'/events/'+authored.id);await capture(organizer,'event-editor');
  const steps=organizer.locator('.event-steps button');for(let i=1;i<await steps.count();i++){await steps.nth(i).click();await ready(organizer);await capture(organizer,'event-editor-step-'+i);}
  await visit(organizer,'/organizations/'+org.id+'/events/'+authored.id+'/orders');await capture(organizer,'organizer-orders');
  const admin=await workspace(14202,f.adminEmail,f.adminPassword);
  for(const path of ['/home','/applications','/events','/payments','/payment-exceptions']){await visit(admin,path);await capture(admin,'admin-'+path.slice(1));}
  const checkin=await workspace(14203,f.owner,f.password);await visit(checkin,'/check-in/events');await capture(checkin,'checkin-events');const access=await api(checkin,'/check-in/events');
  if(access.length){for(const tab of ['scan','lookup','history','stats']){await visit(checkin,'/check-in/events/'+access[0].event.id+'/'+tab);await capture(checkin,'checkin-'+tab);}}
  await customer.emulateMedia({reducedMotion:'reduce'});await visit(customer,'/home');check(await customer.locator('.hero-event-ticket').evaluate(e=>getComputedStyle(e).animationName==='none'),'Reduced motion disables the ticket entrance');
  await customer.setViewportSize({width:390,height:1000});check(await customer.locator('.mobile-navigation a').evaluateAll(es=>es.every(e=>parseFloat(getComputedStyle(e).fontSize)>=12)),'Mobile navigation text is at least 12px');
  await customer.locator('.hero-event-ticket').focus();await customer.keyboard.press('Enter');await customer.waitForURL('**/events/*');check(true,'Featured event works with the keyboard');
  check(smallText.length===0,'No reviewed functional text below 11px');check(errors.length===0,'No JavaScript exceptions across reviewed screens');return {passed:checks.length,checks,errors,smallText};
 }catch(e){await active.screenshot({path:'output/playwright/ui-review-failure.png',fullPage:true});return {passed:checks.length,checks,errors,smallText,failure:String(e)};}
 finally{for(const c of contexts)await c.close();}
}
