import { InjectionToken } from '@angular/core';
export type Workspace = 'customer' | 'organizer' | 'admin' | 'checkin';
export const WORKSPACE = new InjectionToken<Workspace>('workspace', { factory: () => 'customer' });
export const WORKSPACE_NAMES: Record<Workspace, string> = { customer: 'Tài khoản', organizer: 'Nhà tổ chức', admin: 'Quản trị', checkin: 'Nhân viên soát vé' };
