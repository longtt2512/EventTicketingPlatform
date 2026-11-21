async page => {
 const f=__FIXTURE__,checks=[],errors=[];
 const check=(ok,name)=>{if(!ok)throw new Error(name);checks.push(name);};
 const browser=page.context().browser(),contexts=new Set(browser.contexts());
 await page.addInitScript(()=>{window.confirm=()=>true;});
 page.on('pageerror',e=>errors.push(e.message));page.setDefaultTimeout(15000);
 const ready=async p=>{await p.waitForLoadState('networkidle');await p.evaluate(()=>document.fonts.ready);};
 const capture=async(p,name)=>{await ready(p);for(const width of [1440,768,390,320]){await p.setViewportSize({width,height:1000});await p.screenshot({path:`output/playwright/mockup-${name}-${width}.png`,fullPage:true});check(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),name+' fits '+width);}await p.setViewportSize({width:1440,height:1000});};
 const login=async(p,base,email,password)=>{await p.goto(base+'/login');await p.getByLabel('Email',{exact:true}).fill(email);await p.getByLabel('Mật khẩu',{exact:true}).fill(password);await p.getByRole('button',{name:'Đăng nhập',exact:true}).click();await p.waitForURL('**/home');await ready(p);};
 try{
  await page.goto('http://localhost:14200/home');await capture(page,'home');
  check(await page.locator('.home-hero-inner').evaluate(e=>{const [copy,art]=e.children;return art.getBoundingClientRect().left>=copy.getBoundingClientRect().right&&Math.abs(art.getBoundingClientRect().top-copy.getBoundingClientRect().top)<100;}),'Desktop home keeps the event beside its introduction');
  check(await page.locator('.home-hero-art a').evaluate(e=>getComputedStyle(e).backgroundImage.includes('/assets/etp/backgrounds/event-plain/')),'Home uses supplied plain artwork with live event metadata');
  await page.getByRole('link',{name:'Âm nhạc',exact:true}).click();await page.waitForURL(u=>u.pathname==='/events'&&u.searchParams.get('category')==='Music & Concert');await ready(page);check(new URL(page.url()).searchParams.get('category')==='Music & Concert','Category link applies a real discovery filter');
  await page.goto('http://localhost:14200/events');await ready(page);await page.setViewportSize({width:390,height:1000});
  const filters=page.getByRole('button',{name:'Bộ lọc sự kiện',exact:true});check(await filters.isVisible(),'Mobile exposes filter control');check(!await page.locator('.discovery-filters').isVisible(),'Mobile results are visible before optional filters');await filters.click();check(await page.getByLabel('Thành phố',{exact:true}).isVisible(),'Mobile opens filter fields');await page.getByLabel('Thành phố',{exact:true}).fill('Hà Nội');await page.getByRole('button',{name:'Áp dụng bộ lọc',exact:true}).click();await ready(page);check(new URL(page.url()).searchParams.get('city')==='Hà Nội','Mobile filters reach query and backend');check(!await page.locator('.discovery-filters').isVisible(),'Applied filters close to reveal results');await capture(page,'discovery');
  for(const path of ['/login','/register','/forgot-password','/verify-email']){await page.goto('http://localhost:14200'+path);await capture(page,path.slice(1));}
  await login(page,'http://localhost:14200',f.owner,f.password);check(await page.locator('.account-sidebar').count()===0,'Customer discovery has no account sidebar');
  await page.goto('http://localhost:14200/profile');await ready(page);check(await page.locator('.account-sidebar').isVisible(),'Account pages show account navigation');
  const name=page.getByLabel('Họ tên',{exact:true});const original=await name.inputValue();await name.fill(original+' bản nháp');await page.getByRole('button',{name:'Bảo mật',exact:true}).click();check(await page.getByLabel('Mật khẩu hiện tại',{exact:true}).isVisible(),'Security tab exposes password form');await page.getByRole('button',{name:'Thông tin cá nhân',exact:true}).click();check(await name.inputValue()===original+' bản nháp','Profile tabs preserve the unsaved form');await name.fill(original);await capture(page,'profile');
  await page.locator('.customer-navigation a[href="/account/tickets"]').click();await page.waitForURL('**/account/tickets');await capture(page,'account-tickets');
  for(const [workspace,port,email,password] of [['organizer',14201,f.owner,f.password],['admin',14202,f.adminEmail,f.adminPassword]]){
   const c=await browser.newContext();const p=await c.newPage();p.setDefaultTimeout(15000);p.on('pageerror',e=>errors.push(e.message));await login(p,'http://localhost:14200'+({14200:'',14201:'/org',14202:'/admin',14203:'/staff'}[port]),email,password);await capture(p,workspace+'-dashboard');
   await p.setViewportSize({width:390,height:1000});const menu=p.getByRole('button',{name:'Mở điều hướng',exact:true});await menu.click();check(await menu.getAttribute('aria-expanded')==='true',workspace+' mobile navigation opens');await p.locator('.portal-sidebar nav a').filter({hasText:workspace==='admin'?'Tài khoản người dùng':'Tổ chức của tôi'}).click();await p.waitForURL(workspace==='admin'?'**/users':'**/organizations');await p.locator('button[aria-controls="portal-navigation"][aria-expanded="false"]').waitFor();await ready(p);check(await menu.getAttribute('aria-expanded')==='false',workspace+' navigation closes after selecting a page');await capture(p,workspace+'-list');
  }
  check(errors.length===0,'No JavaScript exceptions in new layouts');return {passed:checks.length,checks,errors};
 }catch(e){await page.screenshot({path:'output/playwright/mockup-failure.png',fullPage:true});return {passed:checks.length,checks,errors,failure:String(e)};}
 finally{for(const c of browser.contexts())if(!contexts.has(c))await c.close();}
}
