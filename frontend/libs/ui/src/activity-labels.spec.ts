import { activityLabel,activityDetail,activityFields,activityOptions,activitySource } from './activity-labels';
describe('Readable activity history',()=>{
  it('explains organization actions and permission codes without exposing enum syntax',()=>{
    expect(activityLabel('MEMBER_ADDED')).toBe('Thêm thành viên');
    expect(activityLabel('APPLICATION_APPROVED')).toBe('Phê duyệt hồ sơ tổ chức');
    expect(activityDetail('VND')).toBe('VND');
    expect(activityFields({action:'MEMBER_ADDED',reason:'CHECKIN'}).find(r=>r.label==='Lý do')?.value).toBe('Soát vé');
    expect(activityDetail('CHECKIN')).toBe('Soát vé');
    expect(activityDetail('Ghi chú của người dùng')).toBe('Ghi chú của người dùng');
    expect(activityLabel('FUTURE_EVENT')).toBe('Hoạt động khác');
    expect(activitySource('identity')).toBe('Tài khoản');
    expect(activityOptions().find(o=>o.code==='SETTING_CHANGED')?.label).toBe('Thay đổi cấu hình vận hành');
  });
  it('formats structured evidence while retaining identifiers, amounts and free-text reasons',()=>{
    const rows=activityFields({action:'ORGANIZATION_UPDATED',reason:'APPLICATION_APPROVED',after:{permission:'CHECKIN',version:2},occurred_at:'2026-11-21T10:54:16Z',resource_id:'ID_WITH_UNDERSCORES',currency:'VND'});
    expect(rows.find(x=>x.label==='Thao tác')?.value).toBe('Cập nhật thông tin tổ chức');
    expect(rows.find(x=>x.label==='Sau thay đổi · Quyền')?.value).toBe('Soát vé');
    expect(rows.find(x=>x.label==='Thời điểm')?.value).toContain('17:54');
    expect(rows.find(x=>x.label==='Lý do')?.value).toBe('APPLICATION_APPROVED');
    expect(rows.find(x=>x.label==='Mã đối tượng')?.value).toBe('ID_WITH_UNDERSCORES');
    expect(rows.find(x=>x.label==='Tiền tệ')?.value).toBe('VND');
    expect(rows.find(x=>x.label==='Sau thay đổi · Phiên bản')?.value).toBe('2');
  });
});
