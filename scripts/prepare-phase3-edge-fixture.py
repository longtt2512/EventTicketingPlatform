#!/usr/bin/env python3
"""Accelerate only new synthetic fixture deadlines to exercise late/expired UI states."""
import json,uuid,subprocess,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent;runtime=ROOT/'.runtime/phase1';f=json.loads((runtime/'phase3-fixtures.json').read_text());base='http://localhost:18080/api/v1'
def call(path,method='GET',data=None,token=None,key=None):
 headers={'Content-Type':'application/json'}
 if token:headers['Authorization']='Bearer '+token
 if key:headers['Idempotency-Key']=key
 req=urllib.request.Request(base+path,data=json.dumps(data).encode() if data is not None else None,headers=headers,method=method)
 with urllib.request.urlopen(req,timeout=15) as r:return json.load(r)
t=call('/auth/login','POST',{'email':f['owner'],'password':f['password']})['accessToken']
def reserve(ticket):
 q=call('/events/'+f['phase3Event']+'/booking',token=t)
 return call('/orders/reservations','POST',{'eventId':f['phase3Event'],'selectionVersion':q['selectionVersion'],'lines':[{'ticketTypeId':ticket,'quantity':1}],'termsAccepted':True},t,str(uuid.uuid4()))
late=reserve(f['paidTicket']);attempt=call('/orders/'+late['id']+'/payment-attempts','POST',{},t,str(uuid.uuid4()));expired=reserve(f['freeTicket'])
# Restricted fixture setup, not application cross-service access. UUID parsing prevents SQL interpolation.
late_id=str(uuid.UUID(late['id']));expired_id=str(uuid.UUID(expired['id']));attempt_id=str(uuid.UUID(attempt['id']))
sql=f"BEGIN; UPDATE event_commerce.orders SET expires_at=clock_timestamp()-interval '5 seconds' WHERE id IN ('{late_id}','{expired_id}'); UPDATE event_commerce.inventory_allocations SET expires_at=clock_timestamp()-interval '5 seconds' WHERE id IN ('{late_id}','{expired_id}'); UPDATE payment.attempts SET expires_at=clock_timestamp()-interval '5 seconds' WHERE id='{attempt_id}'; COMMIT;"
subprocess.run(['docker','compose','-f','infra/validation/docker-compose.yml','exec','-T','postgres','psql','-U','postgres','-d','event_ticketing','-v','ON_ERROR_STOP=1'],input=sql,text=True,stdout=subprocess.DEVNULL,check=True,cwd=ROOT)
f.update({'lateOrder':late_id,'lateAttempt':attempt_id,'expiredOrder':expired_id});p=runtime/'phase3-edge-fixtures.json';p.write_text(json.dumps(f,ensure_ascii=False));p.chmod(0o600)
print('Prepared only the new synthetic late/expired orders; their deadlines were accelerated for browser boundary proof.')
