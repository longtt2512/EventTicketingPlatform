#!/usr/bin/env python3
"""Create clearly synthetic Phase 5 event through real isolated application APIs."""
import json,urllib.request,hashlib,struct,zlib
from pathlib import Path
from datetime import datetime,timedelta,timezone
ROOT=Path(__file__).resolve().parent.parent
runtime=ROOT/'.runtime/phase1';f=json.loads((runtime/'browser-fixtures.json').read_text());base='http://localhost:18080/api/v1'
def call(method,path,body=None,token=None,extra=None):
 headers={'Content-Type':'application/json'}
 if token:headers['Authorization']='Bearer '+token
 headers.update(extra or {})
 req=urllib.request.Request(base+path,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method)
 with urllib.request.urlopen(req,timeout=15) as r:
  data=r.read();return json.loads(data) if data else None
token=call('POST','/auth/login',{'email':f['owner'],'password':f['password']})['accessToken']
admin=call('POST','/auth/login',{'email':f['adminEmail'],'password':f['adminPassword']})['accessToken']
org=next(o for o in call('GET','/organizations',token=token) if o['name'].startswith(f['orgName']))
name='Đối soát local '+datetime.now().strftime('%Y%m%d%H%M%S')
e=call('POST',f"/organizations/{org['id']}/events",{'name':name},token)
path=f"/organizations/{org['id']}/events/{e['id']}"
w,h=640,360
raw=b''.join(b'\x00'+b''.join(bytes((67+(x//20)%35,56+(y//10)%55,160+(x+y)%70)) for x in range(w)) for y in range(h))
def chunk(kind,data):return struct.pack('!I',len(data))+kind+data+struct.pack('!I',zlib.crc32(kind+data)&0xffffffff)
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',w,h,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b'')
intent=call('POST','/media/uploads',{'organizationId':org['id'],'eventId':e['id'],'kind':'BANNER','contentType':'image/png','byteSize':len(png),'sha256':hashlib.sha256(png).hexdigest()},token)
with urllib.request.urlopen(urllib.request.Request(intent['uploadUrl'],data=png,headers={'Content-Type':'image/png'},method='PUT')) as r:assert r.status==200
media=call('POST',f"/media/uploads/{intent['id']}/complete",{},token)
now=datetime.now(timezone.utc);start=now+timedelta(hours=1)
body={'name':name,'category':'Workshop','description':'Sự kiện tổng hợp để kiểm chứng chọn vé, đơn hàng và thanh toán mô phỏng. Dữ liệu local, không thu tiền thật.','venueName':'Không gian thử nghiệm','venueAddress':'123 Phố Thử Nghiệm','city':'Hà Nội','timezone':'Asia/Ho_Chi_Minh','startAt':start.isoformat(),'endAt':(start+timedelta(hours=3)).isoformat(),'capacity':100,'terms':'Đọc kỹ thông tin đơn. Thanh toán chỉ được mô phỏng ở local.','bannerMediaId':media['mediaId'],'galleryMediaIds':[]}
e=call('PATCH',path,body,token,{'If-Match':'0'})
for title,price,quota in [('Vé tiêu chuẩn',100000,70),('Vé cộng đồng',0,30)]:
 e=call('POST',path+'/ticket-types',{'name':title,'description':'Hạng vé kiểm chứng local','price':price,'quota':quota,'saleStart':(now-timedelta(days=1)).isoformat(),'saleEnd':(start-timedelta(minutes=30)).isoformat(),'maxPerOrder':4},token,{'If-Match':str(e['version'])})
e=call('POST',path+'/submit',{},token,{'If-Match':str(e['version'])})
e=call('POST',f"/admin/events/{e['id']}/approve",{'reason':'Sự kiện kiểm chứng Phase 5 local'},admin,{'If-Match':str(e['version'])})
e=call('POST',path+'/publish',{},token,{'If-Match':str(e['version'])})
f.update({'phase5Event':e['id'],'phase5Org':org['id'],'phase5Name':name,'paidTicket':next(t['id'] for t in e['ticketTypes'] if t['price']>0),'freeTicket':next(t['id'] for t in e['ticketTypes'] if t['price']==0)})
members=call('GET',f"/organizations/{org['id']}/members",token=token)
staff=next((m for m in members if m['email']==f['staff']),None)
if not staff or staff['status']!='ACTIVE':
 call('POST',f"/organizations/{org['id']}/members",{'email':f['staff'],'permission':'CHECKIN'},token)
 members=call('GET',f"/organizations/{org['id']}/members",token=token)
 staff=next(m for m in members if m['email']==f['staff'])
call('PUT',path+'/staff/'+staff['userId'],{},token)
f['phase5StaffId']=staff['userId']
p=runtime/'phase5-fixtures.json';p.write_text(json.dumps(f,ensure_ascii=False,indent=2));p.chmod(0o600)
print('Created synthetic published event for Phase 5 local testing. Credentials remain in protected fixture file.')
