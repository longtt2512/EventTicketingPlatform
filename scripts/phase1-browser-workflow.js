async page => {
  const f = __FIXTURE__;
  const initialContexts=new Set(page.context().browser().contexts());
  const checks = [], pageErrors = [], serverErrors = [], consoleErrors = [];
  const check = (ok, text) => { if (!ok) throw new Error(text); checks.push(text); };
  const watch = p => { p.setDefaultTimeout(10000); p.on('pageerror', e => pageErrors.push(e.message)); p.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()); }); p.on('response', r => { if (r.status() >= 500) serverErrors.push({url:r.url(),status:r.status()}); }); };
  watch(page);
  // Accept native confirmations in this synthetic workflow; unit tests exercise cancel/confirm paths.
  await page.addInitScript(() => { window.confirm = () => true; });
  const customer = 'http://localhost:14200', organizer = 'http://localhost:14200/org', adminBase = 'http://localhost:14200/admin', checkin = 'http://localhost:14200/staff';
  const mailLink = async (email, path) => {
    for (let i=0;i<75;i++) {
      const response=await page.request.get('http://localhost:18025/api/v2/search',{params:{kind:'to',query:email}});
      const messages=(await response.json()).items??[];
      for (const item of messages) {
        const body=(item.Content?.Body??'').replace(/=\r?\n/g,'').replace(/=3D/g,'=');
        const match=body.match(new RegExp('http://localhost:14200/'+path+'(?:\\?[^#\\s]+)?#token=[A-Za-z0-9_-]+'));
        if(match) return match[0];
      }
      await new Promise(resolve=>setTimeout(resolve,200));
    }
    throw new Error('Email delivery timed out for '+path);
  };
  const login = async (p,base,email,password) => {
    await p.goto(base+'/login');await p.getByRole('heading',{name:'Đăng nhập',exact:true}).waitFor();
    await p.getByLabel('Email',{exact:true}).fill(email);await p.getByLabel('Mật khẩu',{exact:true}).fill(password);
    const response=p.waitForResponse(r=>r.url().endsWith('/auth/login')&&r.request().method()==='POST');
    await p.getByRole('button',{name:'Đăng nhập',exact:true}).click();const data=await (await response).json();
    check(!!data.accessToken,'Login succeeds for '+email.split('@')[0]);await p.waitForURL('**/home');return data;
  };
  const decide = async (p, trigger, action=trigger) => {
    let writes=0;const watchWrite=request=>{if(['POST','PATCH','DELETE','PUT'].includes(request.method())&&request.url().includes('/api/v1/'))writes++;};
    p.on('request',watchWrite);
    await p.getByRole('button',{name:trigger,exact:true}).click();
    const dialog=p.getByRole('dialog');await dialog.waitFor();
    check(writes===0,'Admin action waits for confirmation: '+action);
    check(await dialog.getByRole('button',{name:'Hủy',exact:true}).evaluate(el=>el===document.activeElement),'Cancel receives focus: '+action);
    await dialog.getByRole('button',{name:'Hủy',exact:true}).click();await dialog.waitFor({state:'detached'});
    check(writes===0,'Cancel sends no mutation: '+action);
    await p.getByRole('button',{name:trigger,exact:true}).click();await dialog.getByRole('button',{name:action,exact:true}).click();
    p.off('request',watchWrite);
  };
  const apiUser=async (email,name) => {
    const res=await page.request.post(new URL(customer).origin+'/api/v1/auth/register',{data:{email,password:f.password,displayName:name,locale:'vi'}});
    check(res.status()===202,'Fixture registration accepted');const link=await mailLink(email,'verify-email');
    const verify=await page.request.post(new URL(customer).origin+'/api/v1/auth/verify-email',{data:{token:link.split('token=')[1]}});check(verify.status()===204,'Fixture email verified');
  };
  try {
  await page.goto(customer+'/register');await page.getByRole('heading',{name:'Tạo tài khoản',exact:true}).waitFor();
  await page.getByLabel('Họ tên',{exact:true}).fill('Chủ tổ chức kiểm thử');await page.getByLabel('Email',{exact:true}).fill(f.owner);
  await page.getByLabel('Mật khẩu',{exact:true}).fill(f.password);await page.getByLabel('Nhập lại mật khẩu',{exact:true}).fill('Mismatch-123!');
  await page.getByRole('button',{name:'Tạo tài khoản',exact:true}).click();await page.locator('.field-error').filter({hasText:'chưa khớp'}).waitFor();
  await page.getByLabel('Nhập lại mật khẩu',{exact:true}).fill(f.password);await page.getByRole('button',{name:'Tạo tài khoản',exact:true}).click();
  await page.getByRole('heading',{name:'Kiểm tra hộp thư'}).waitFor();check(true,'Registration form validates confirmation and submits');
  const initialVerification=await mailLink(f.owner,'verify-email');
  await page.goto(customer+'/verify-email');
  await page.getByLabel('Email đã đăng ký',{exact:true}).fill(f.owner);
  await page.getByRole('button',{name:'Gửi lại email xác thực',exact:true}).click();
  await page.getByRole('status').filter({hasText:'Nếu email thuộc tài khoản cần xác thực'}).waitFor();
  check(await page.getByRole('button',{name:'Gửi lại email xác thực',exact:true}).isDisabled(),'Resend begins a visible 60 second cooldown');
  const initialMail=(await (await page.request.get('http://localhost:18025/api/v2/search',{params:{kind:'to',query:f.owner}})).json()).items;
  check(initialMail.length===1,'Initial registration also participates in resend cooldown');
  await page.setViewportSize({width:390,height:844});await page.screenshot({path:'output/playwright/resend-mobile.png',fullPage:true});
  check(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Resend fits a mobile viewport');
  await page.getByRole('button',{name:'Gửi lại email xác thực',exact:true}).waitFor({state:'visible'});
  await page.waitForFunction(()=>!Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim()==='Gửi lại email xác thực').disabled,{},{timeout:65000});
  await page.getByRole('button',{name:'Gửi lại email xác thực',exact:true}).click();
  let verificationLink=initialVerification;
  for(let i=0;i<75&&verificationLink===initialVerification;i++) {verificationLink=await mailLink(f.owner,'verify-email');if(verificationLink===initialVerification)await new Promise(resolve=>setTimeout(resolve,200));}
  check(verificationLink!==initialVerification,'Resend after cooldown delivers a new real email link');
  await page.setViewportSize({width:1280,height:900});await page.goto(verificationLink);await page.getByRole('button',{name:'Xác thực email',exact:true}).click();
  await page.getByRole('status').filter({hasText:'Email đã được xác thực'}).waitFor();check(!page.url().includes('token='),'Email token is removed from URL');
  const reused=await page.request.post(new URL(customer).origin+'/api/v1/auth/verify-email',{data:{token:verificationLink.split('token=')[1]}});check(reused.status()===422,'Verification token is single-use');
  let ownerSession=await login(page,customer,f.owner,f.password);
  await page.goto(customer+'/profile');await page.getByRole('heading',{name:'Hồ sơ cá nhân',exact:true}).waitFor();
  check(await page.evaluate(()=>localStorage.length===0&&sessionStorage.length===0),'Credentials are not stored in browser storage');
  const cookieBefore=(await page.context().cookies()).find(c=>c.name==='refresh_token');check(cookieBefore?.httpOnly&&cookieBefore?.sameSite==='Lax','Refresh cookie is HttpOnly/SameSite=Lax');
  await page.reload();await page.getByRole('heading',{name:'Hồ sơ cá nhân',exact:true}).waitFor();
  check(cookieBefore.value!==(await page.context().cookies()).find(c=>c.name==='refresh_token').value,'Reload restores session and rotates refresh token');
  await page.getByLabel('Họ tên',{exact:true}).fill('Chủ tổ chức cập nhật');
  await page.route('**/api/v1/users/me',r=>r.abort('failed'));await page.getByRole('button',{name:'Lưu hồ sơ',exact:true}).click();
  await page.getByRole('alert').filter({hasText:'kết nối'}).waitFor();check(await page.getByRole('button',{name:'Lưu hồ sơ',exact:true}).isEnabled(),'Profile recovers after network failure');
  await page.unroute('**/api/v1/users/me');await page.getByRole('button',{name:'Lưu hồ sơ',exact:true}).click();await page.getByRole('status').filter({hasText:'Đã lưu hồ sơ'}).waitFor();
  await page.getByRole('button',{name:'Bảo mật',exact:true}).click();
  await page.getByLabel('Mật khẩu hiện tại',{exact:true}).fill(f.password);await page.getByLabel('Mật khẩu mới',{exact:true}).fill(f.changedPassword);await page.getByLabel('Nhập lại mật khẩu mới',{exact:true}).fill(f.changedPassword);
  await page.getByRole('button',{name:'Đổi mật khẩu',exact:true}).click();await page.waitForURL(url=>url.pathname==='/login'&&url.searchParams.get('passwordChanged')==='1');
  check((await page.request.get(new URL(customer).origin+'/api/v1/users/me',{headers:{Authorization:'Bearer '+ownerSession.accessToken}})).status()===401,'Password change revokes earlier access');
  ownerSession=await login(page,customer,f.owner,f.changedPassword);
  await page.route('**/api/v1/auth/logout',r=>r.abort('failed'));await page.getByRole('button',{name:'Đăng xuất',exact:true}).click();await page.getByRole('alert').filter({hasText:'thu hồi'}).waitFor();
  await page.unroute('**/api/v1/auth/logout');await page.getByRole('button',{name:'Đăng xuất',exact:true}).click();await page.waitForURL('**/login');check(true,'Logout reports failure and supports retry');
  await page.getByRole('link',{name:'Quên mật khẩu?'}).click();await page.getByRole('heading',{name:'Quên mật khẩu',exact:true}).waitFor();await page.getByLabel('Email',{exact:true}).fill(f.owner);await page.getByRole('button',{name:'Gửi liên kết'}).click();
  await page.getByRole('status').filter({hasText:'Nếu email thuộc'}).waitFor();const resetLink=await mailLink(f.owner,'reset-password');await page.goto(resetLink);
  await page.getByLabel('Mật khẩu mới',{exact:true}).fill(f.password);await page.getByLabel('Nhập lại mật khẩu mới',{exact:true}).fill(f.password);await page.getByRole('button',{name:'Đặt lại mật khẩu',exact:true}).click();
  await page.getByRole('status').filter({hasText:'Đã đặt lại mật khẩu'}).waitFor();
  check((await page.request.post(new URL(customer).origin+'/api/v1/auth/reset-password',{data:{token:resetLink.split('token=')[1],newPassword:f.password}})).status()===422,'Password reset token is single-use');
  ownerSession=await login(page,organizer,f.owner,f.password);
  await apiUser(f.staff,'Nhân viên kiểm thử');await apiUser(f.outsider,'Người ngoài tổ chức');
  await page.goto(organizer+'/apply');await page.getByRole('heading',{name:'Hồ sơ nhà tổ chức',exact:true}).waitFor();
  await page.getByLabel('Tên pháp lý tổ chức',{exact:true}).fill(f.orgName);await page.getByLabel('Email liên hệ',{exact:true}).fill(f.owner);await page.getByLabel('Địa chỉ',{exact:true}).fill('123 Đường Kiểm Thử, Hà Nội');await page.getByLabel('Giới thiệu tổ chức',{exact:true}).fill('Tổ chức dữ liệu thử nghiệm Phase 1.');
  await page.getByLabel('Tài liệu xác minh',{exact:true}).setInputFiles({name:'invalid.txt',mimeType:'text/plain',buffer:Buffer.from('invalid')});
  await page.getByRole('alert').filter({hasText:'PDF, PNG hoặc JPEG'}).waitFor();
  await page.getByLabel('Tài liệu xác minh',{exact:true}).setInputFiles({name:'verification.pdf',mimeType:'application/pdf',buffer:Buffer.from('%PDF-1.4\nPhase 1 synthetic verification document\n%%EOF')});
  await page.getByRole('status').filter({hasText:'Tài liệu đã tải lên'}).waitFor();
  const applicationResponse=page.waitForResponse(r=>r.url().endsWith('/organizer-applications')&&r.request().method()==='POST');await page.getByRole('button',{name:'Gửi hồ sơ',exact:true}).click();const application=await (await applicationResponse).json();
  await page.getByRole('status').filter({hasText:'Đã gửi hồ sơ'}).waitFor();check(await page.getByLabel('Tên pháp lý tổ chức').isDisabled(),'Pending application is read-only');
  const adminContext=await page.context().browser().newContext();const adminPage=await adminContext.newPage();watch(adminPage);const adminSession=await login(adminPage,adminBase,f.adminEmail,f.adminPassword);
  await adminPage.goto(adminBase+'/applications');await adminPage.getByRole('row').filter({hasText:f.orgName}).getByRole('button',{name:'Xem hồ sơ'}).click();
  await adminPage.getByLabel('Lý do từ chối',{exact:true}).fill('Bổ sung thông tin địa chỉ');await decide(adminPage,'Từ chối hồ sơ');await adminPage.getByRole('status').filter({hasText:'Đã từ chối hồ sơ'}).waitFor();
  await page.evaluate(()=>window.dispatchEvent(new Event('focus')));await page.getByText('Lý do từ chối: Bổ sung thông tin địa chỉ').waitFor();await page.getByLabel('Địa chỉ',{exact:true}).fill('123 Đường Kiểm Thử, Phường 1, Hà Nội');
  await page.getByRole('button',{name:'Lưu và gửi lại',exact:true}).click();await page.getByRole('status').filter({hasText:'Đã gửi lại hồ sơ'}).waitFor();
  await adminPage.evaluate(()=>window.dispatchEvent(new Event('focus')));await decide(adminPage,'Phê duyệt hồ sơ');await adminPage.getByRole('status').filter({hasText:'Đã phê duyệt tổ chức'}).waitFor();
  check(true,'Organizer rejection, correction, resubmission and approval work through UI');
  await page.goto(organizer+'/organizations');await page.getByRole('link',{name:f.orgName,exact:true}).click();await page.getByRole('heading',{name:f.orgName,exact:true}).waitFor();await page.waitForURL(url=>/\/organizations\/[0-9a-f-]{36}$/.test(url.pathname));const orgId=new URL(page.url()).pathname.split('/').pop();
  await page.getByLabel('Tên tổ chức',{exact:true}).fill(f.orgName+' cập nhật');await page.getByRole('button',{name:'Lưu thông tin tổ chức'}).click();await page.getByRole('status').filter({hasText:'Đã lưu thông tin tổ chức'}).waitFor();check(true,'Organization metadata persists');
  await page.getByRole('button',{name:'Thành viên',exact:true}).click();await page.getByLabel('Email thành viên').fill(f.staff);await page.getByLabel('Quyền thành viên').selectOption('CHECKIN');await page.getByRole('button',{name:'Thêm thành viên',exact:true}).click();await page.getByRole('status').filter({hasText:'Đã thêm thành viên'}).waitFor();
  await page.getByRole('button',{name:'Sự kiện và phân công',exact:true}).click();await page.getByLabel('Tên sự kiện mới',{exact:true}).fill(f.eventName);await page.getByRole('button',{name:'Tạo và chỉnh sửa',exact:true}).click();await page.getByRole('heading',{name:f.eventName,exact:true}).waitFor();
  await page.getByRole('link',{name:'Về sự kiện của tổ chức',exact:true}).click();await page.locator('summary').filter({hasText:'Phân công nhân viên soát vé'}).click();await page.getByLabel('Sự kiện',{exact:true}).selectOption({label:f.eventName});
  await page.getByLabel('Nhân viên soát vé',{exact:true}).selectOption({label:'Nhân viên kiểm thử — '+f.staff});
  const assignmentResponse=page.waitForResponse(r=>r.request().method()==='PUT'&&r.url().includes('/staff/'));await page.getByRole('button',{name:'Phân công nhân viên',exact:true}).click();const assignmentUrl=(await assignmentResponse).url();await page.getByRole('status').filter({hasText:'Đã phân công nhân viên'}).waitFor();const eventId=assignmentUrl.split('/events/')[1].split('/')[0];
  const staffContext=await page.context().browser().newContext();const staffPage=await staffContext.newPage();watch(staffPage);const staffSession=await login(staffPage,checkin,f.staff,f.password);await staffPage.goto(checkin+'/assignments');await staffPage.getByRole('heading',{name:f.eventName,exact:true}).waitFor();await staffPage.getByRole('button',{name:'Xem phân công'}).click();await staffPage.getByRole('status').filter({hasText:'Bạn đang được phân công'}).waitFor();check(true,'Assigned staff sees the authorized draft event');
  await page.getByRole('button',{name:'Tài khoản nhận tiền',exact:true}).click();await page.getByLabel('Tên ngân hàng',{exact:true}).fill('Ngân hàng kiểm thử');await page.getByLabel('Tên chủ tài khoản',{exact:true}).fill('CHU TO CHUC');await page.getByLabel('Số tài khoản ngân hàng',{exact:true}).fill('12345678901234');await page.getByRole('button',{name:'Gửi tài khoản để xác minh'}).click();await page.getByRole('status').filter({hasText:'Đã gửi tài khoản nhận tiền'}).waitFor();
  check(!(await page.locator('body').innerText()).includes('12345678901234'),'Payout account is masked after saving');check(await page.getByLabel('Số tài khoản ngân hàng',{exact:true}).inputValue()==='','Full payout account input is cleared');
  await adminPage.goto(adminBase+'/organizations/'+orgId);await adminPage.getByRole('button',{name:'Tài khoản nhận tiền',exact:true}).click();await adminPage.getByLabel('Ghi chú xét duyệt tài khoản').fill('Đã xác minh bản thử nghiệm');await decide(adminPage,'Xác minh tài khoản');await adminPage.getByRole('status').filter({hasText:'Đã lưu quyết định xác minh'}).waitFor();
  await page.evaluate(()=>window.dispatchEvent(new Event('focus')));await page.getByRole('row').filter({hasText:'•••• 1234'}).getByRole('cell').filter({hasText:'Đang hoạt động'}).waitFor();check(true,'Payout remains pending until Admin verification');
  await page.setViewportSize({width:390,height:844});await page.screenshot({path:'output/playwright/phase1-payout-mobile.png',fullPage:true});check(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Payout view fits mobile viewport');await page.setViewportSize({width:1280,height:900});
  await adminPage.getByRole('button',{name:'Thông tin chung',exact:true}).click();await adminPage.getByLabel('Lý do thay đổi trạng thái').fill('Kiểm tra đình chỉ');await decide(adminPage,'Đình chỉ tổ chức');await adminPage.getByRole('status').filter({hasText:'Đã cập nhật trạng thái tổ chức'}).waitFor();
  await staffPage.evaluate(()=>window.dispatchEvent(new Event('focus')));await staffPage.getByRole('heading',{name:'Chưa có sự kiện được phân công'}).waitFor();
  await page.evaluate(()=>window.dispatchEvent(new Event('focus')));await page.getByText('Tổ chức đã bị đình chỉ.',{exact:false}).waitFor();check(true,'Suspension blocks staff access and organizer mutation controls');
  await adminPage.getByLabel('Lý do thay đổi trạng thái').fill('Đã kiểm tra lại');await decide(adminPage,'Khôi phục tổ chức');await adminPage.getByRole('button',{name:'Đình chỉ tổ chức',exact:true}).waitFor();await staffPage.evaluate(()=>window.dispatchEvent(new Event('focus')));await staffPage.getByRole('heading',{name:f.eventName,exact:true}).waitFor();check(true,'Restore re-enables valid existing assignments');
  await page.evaluate(()=>window.dispatchEvent(new Event('focus')));await page.getByRole('button',{name:'Sự kiện và phân công',exact:true}).click();await page.getByRole('button',{name:'Thu hồi phân công',exact:true}).click();await page.getByRole('status').filter({hasText:'Đã thu hồi phân công'}).waitFor();
  check((await staffPage.request.get(new URL(checkin).origin+'/api/v1/staff/events/'+eventId,{headers:{Authorization:'Bearer '+staffSession.accessToken}})).status()===404,'Revoked assignment rejects the earlier valid access token');
  await page.getByLabel('Sự kiện',{exact:true}).selectOption(eventId);await page.getByLabel('Nhân viên soát vé',{exact:true}).selectOption({label:'Nhân viên kiểm thử — '+f.staff});await page.getByRole('button',{name:'Phân công nhân viên',exact:true}).click();await page.getByRole('status').filter({hasText:'Đã phân công nhân viên'}).waitFor();
  await page.getByRole('button',{name:'Thành viên',exact:true}).click();await page.getByRole('button',{name:'Thu hồi thành viên',exact:true}).click();await page.getByRole('status').filter({hasText:'Đã thu hồi thành viên'}).waitFor();
  check((await staffPage.request.get(new URL(checkin).origin+'/api/v1/staff/events/'+eventId,{headers:{Authorization:'Bearer '+staffSession.accessToken}})).status()===404,'Membership revocation also revokes assignments');
  const outsiderContext=await page.context().browser().newContext();const outsiderPage=await outsiderContext.newPage();watch(outsiderPage);const outsiderSession=await login(outsiderPage,organizer,f.outsider,f.password);await outsiderPage.goto(organizer+'/organizations/'+orgId);await outsiderPage.getByRole('alert').filter({hasText:'Không tìm thấy dữ liệu'}).waitFor();check(true,'Cross-organization UI route reveals no organization data');
  for(const path of ['/organizations/'+orgId+'/members','/organizations/'+orgId+'/payout-destinations','/verification-documents/'+application.documentId]) {
    const denied=await outsiderPage.request.get(new URL(organizer).origin+'/api/v1'+path,{headers:{Authorization:'Bearer '+outsiderSession.accessToken}});check(denied.status()===404,'Cross-organization API denied: '+path.split('/')[1]);
  }
  await page.getByRole('button',{name:'Lịch sử thao tác',exact:true}).click();await page.getByText('Thu hồi quyền thành viên',{exact:true}).waitFor();await page.screenshot({path:'output/playwright/phase1-organization-desktop.png',fullPage:true});
  await adminPage.goto(adminBase+'/users');await adminPage.getByRole('heading',{name:'Tài khoản người dùng',exact:true}).waitFor();
  // The Admin UI must revoke a live token, and unlocking must not restore it.
  const findUser=async()=>{
    // Wait for the first page as well as later pages; the heading renders before data.
    await adminPage.getByRole('row').nth(1).waitFor();
    const row=adminPage.getByRole('row').filter({hasText:f.outsider});
    for(let i=0;i<20;i++) {
      await adminPage.waitForLoadState('networkidle');
      if(await row.count())return row;
      const more=adminPage.getByRole('button',{name:'Xem thêm',exact:true});
      if(!await more.count())break;
      const loaded=adminPage.waitForResponse(r=>r.url().includes('/admin/users?'));
      await more.click();await loaded;
    }
    throw new Error('Synthetic account missing after loading Admin user pages');
  };
  let targetRow=await findUser();
  await targetRow.getByRole('button',{name:'Khóa',exact:true}).click();await adminPage.getByLabel('Lý do',{exact:true}).fill('Kiểm tra thu hồi phiên');
  await decide(adminPage,'Xác nhận thay đổi','Khóa tài khoản');await adminPage.getByRole('status').filter({hasText:'Đã cập nhật trạng thái tài khoản'}).waitFor();
  check((await outsiderPage.request.get(new URL(organizer).origin+'/api/v1/users/me',{headers:{Authorization:'Bearer '+outsiderSession.accessToken}})).status()===401,'Admin lock blocks an already-issued token');
  targetRow=await findUser();
  await targetRow.getByRole('button',{name:'Mở khóa',exact:true}).click();await decide(adminPage,'Xác nhận thay đổi','Mở khóa tài khoản');await adminPage.getByRole('status').filter({hasText:'Đã cập nhật trạng thái tài khoản'}).waitFor();
  check((await outsiderPage.request.get(new URL(organizer).origin+'/api/v1/users/me',{headers:{Authorization:'Bearer '+outsiderSession.accessToken}})).status()===401,'Unlock does not resurrect revoked sessions');
  await login(outsiderPage,organizer,f.outsider,f.password);
  const oldRefresh=(await outsiderContext.cookies()).find(c=>c.name==='refresh_token').value;
  const rotated=await outsiderPage.request.post(new URL(organizer).origin+'/api/v1/auth/refresh',{headers:{Origin:new URL(organizer).origin},data:{}});check(rotated.status()===200,'Refresh rotation succeeds');const rotatedAccess=(await rotated.json()).accessToken;
  const replay=await outsiderPage.request.post(new URL(organizer).origin+'/api/v1/auth/refresh',{headers:{Origin:new URL(organizer).origin,Cookie:'refresh_token='+oldRefresh},data:{}});check(replay.status()===401&&(await replay.json()).code==='REFRESH_REUSE_DETECTED','Replayed refresh token is rejected');
  check((await outsiderPage.request.get(new URL(organizer).origin+'/api/v1/users/me',{headers:{Authorization:'Bearer '+rotatedAccess}})).status()===401,'Refresh replay revokes the replacement session too');
  await outsiderPage.goto(organizer+'/profile');await outsiderPage.getByRole('heading',{name:'Đăng nhập',exact:true}).waitFor();check(true,'Revoked session returns to login on a protected route');
  await (__P1_NAVIGATION__)(page.context().browser(),f,orgId,check,watch);
  check(pageErrors.length===0,'No browser JavaScript exceptions');check(serverErrors.length===0,'No unexpected server 5xx responses');
  await adminContext.close();await staffContext.close();await outsiderContext.close();
  return {passed:checks.length,checks,pageErrors,serverErrors,consoleErrors:consoleErrors.filter(x=>!x.includes('Failed to load resource')),organizationId:orgId,eventId};
  } catch(error) {
    const pages=[];
    for(const context of page.context().browser().contexts()) for(const p of context.pages()) {
      try {pages.push({url:p.url(),text:(await p.locator('body').innerText()).slice(0,6000)});} catch {}
    }
    return {failure:String(error),checks,pageErrors,serverErrors,pages};
  } finally {
    for(const context of page.context().browser().contexts()) if(!initialContexts.has(context)) await context.close().catch(()=>{});
  }

}
