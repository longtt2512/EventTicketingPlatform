async (browser, f, orgId, check, watch) => {
  const organizer='http://localhost:14200/org', adminBase='http://localhost:14200/admin';
  const context=await browser.newContext({viewport:{width:1280,height:900}});
  const page=await context.newPage();watch(page);
  const target='/organizations/'+orgId+'?section=payouts';
  const signIn=async (p,email,password)=>{
    await p.getByLabel('Email',{exact:true}).fill(email);await p.getByLabel('Mật khẩu',{exact:true}).fill(password);
    await p.getByRole('button',{name:'Đăng nhập',exact:true}).click();
  };
  try {
    await page.goto(organizer+target);await page.getByRole('heading',{name:'Đăng nhập',exact:true}).waitFor();
    check(new URL(page.url()).searchParams.get('returnUrl')===target,'Anonymous login preserves organization and section');
    await page.getByRole('link',{name:'Quên mật khẩu?'}).click();await page.getByRole('link',{name:'Quay lại đăng nhập'}).click();
    check(new URL(page.url()).searchParams.get('returnUrl')===target,'Password recovery navigation keeps the intended destination');
    await signIn(page,f.owner,f.password);await page.waitForURL(organizer+target);await page.getByLabel('Tên ngân hàng',{exact:true}).waitFor();
    check(true,'Login returns to the exact organization section');
    await page.getByLabel('Tên ngân hàng',{exact:true}).fill('Bản nháp chưa gửi');await page.getByLabel('Số tài khoản ngân hàng',{exact:true}).fill('123456789');
    await page.getByRole('button',{name:'Thành viên',exact:true}).click();await page.waitForURL('**section=members');await page.goBack();await page.waitForURL('**section=payouts');
    check(await page.getByLabel('Tên ngân hàng',{exact:true}).inputValue()==='Bản nháp chưa gửi','Back navigation retains unsaved section input');
    check(await page.getByRole('button',{name:'Tài khoản nhận tiền',exact:true}).getAttribute('aria-current')==='page','Current organization section is visibly identified');
    page.once('dialog',dialog=>dialog.dismiss());await page.getByRole('link',{name:'Hồ sơ cá nhân',exact:true}).click();
    check(new URL(page.url()).searchParams.get('section')==='payouts','Cancelling departure keeps the current page');
    check(await page.getByLabel('Số tài khoản ngân hàng',{exact:true}).inputValue()==='123456789','Cancelling departure retains the sensitive draft in memory');
    check(await page.evaluate(()=>localStorage.length===0&&sessionStorage.length===0),'Unsaved bank input is never written to web storage');
    check(await page.evaluate(()=>{const e=new Event('beforeunload',{cancelable:true});window.dispatchEvent(e);return e.defaultPrevented;}),'Dirty form protects reload and window close');
    page.once('dialog',dialog=>dialog.accept());await page.getByRole('link',{name:'Hồ sơ cá nhân',exact:true}).click();await page.waitForURL('**/profile');
    check(true,'Confirmed departure opens the selected page');
    await page.goto(organizer+target);await page.getByLabel('Tên ngân hàng',{exact:true}).waitFor();
    check(await page.getByLabel('Tên ngân hàng',{exact:true}).inputValue()==='','Discarded bank input is not restored from storage');
    await page.request.post(new URL(organizer).origin+'/api/v1/auth/logout',{headers:{Origin:new URL(organizer).origin},data:{}});
    await page.evaluate(()=>window.dispatchEvent(new Event('focus')));await page.getByRole('heading',{name:'Đăng nhập',exact:true}).waitFor();
    check(new URL(page.url()).searchParams.get('returnUrl')===target,'Expired session preserves the section for reauthentication');
    await signIn(page,f.owner,f.password);await page.waitForURL(organizer+target);check(true,'Reauthentication restores the intended organization section');

    const admin=await browser.newContext();const p=await admin.newPage();watch(p);
    try {
      await p.goto(adminBase+'/login');await signIn(p,f.adminEmail,f.adminPassword);await p.waitForURL('**/home');
      await p.goto(adminBase+'/organizations/'+orgId);await p.getByLabel('Lý do thay đổi trạng thái').fill('Bản xem trước kiểm thử, không thực hiện đình chỉ');
      await p.getByRole('button',{name:'Đình chỉ tổ chức',exact:true}).click();await p.getByRole('dialog').waitFor();
      for(const [width,height] of [[1440,900],[768,1024],[390,844]]){
        await p.setViewportSize({width,height});await p.screenshot({path:'output/playwright/p1-confirmation-'+width+'.png',fullPage:false});
        check(await p.getByRole('dialog').evaluate(el=>el.getBoundingClientRect().width<=innerWidth),'Confirmation fits viewport '+width);
      }
      await p.keyboard.press('Escape');check(await p.getByRole('dialog').count()===0,'Escape cancels the consequential action');
      check(await p.getByRole('button',{name:'Đình chỉ tổ chức',exact:true}).evaluate(el=>el===document.activeElement),'Confirmation restores focus to the originating action');
    } finally {await admin.close();}
  } finally {await context.close();}
}
