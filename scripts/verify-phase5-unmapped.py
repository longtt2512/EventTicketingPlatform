#!/usr/bin/env python3
"""Exercise an unmatched local receipt, Kafka reports and its evidenced return."""
import hashlib,hmac,json,subprocess,time,urllib.request,uuid
from datetime import datetime,timezone,timedelta
from pathlib import Path
from urllib.parse import urlencode

ROOT=Path(__file__).resolve().parent.parent
runtime=ROOT/'.runtime/phase1'
credentials=json.loads((runtime/'credentials.json').read_text())
fixture=json.loads((runtime/'phase5-fixtures.json').read_text())
base='http://localhost:18080/api/v1'
def now():return datetime.now(timezone.utc).isoformat()
def api(path,method='GET',body=None,token=None,headers=None):
    data=json.dumps(body,separators=(',',':')).encode() if body is not None else None
    h={'Content-Type':'application/json',**(headers or {})}
    if token:h['Authorization']='Bearer '+token
    with urllib.request.urlopen(urllib.request.Request(base+path,data=data,headers=h,method=method),timeout=20) as response:
        raw=response.read();return json.loads(raw) if raw else None
def login(email,password):return api('/auth/login','POST',{'email':email,'password':password})['accessToken']
def sql(query):
    return subprocess.run(['docker','compose','-f','infra/validation/docker-compose.yml','exec','-T','postgres','psql','-U','postgres','-d','event_ticketing','-XAt','-v','ON_ERROR_STOP=1'],input=query,text=True,capture_output=True,check=True,cwd=ROOT).stdout.strip()
admin=login(credentials['adminEmail'],credentials['adminPassword'])
owner=login(fixture['owner'],fixture['password'])
query=urlencode({'from':now(),'to':(datetime.now(timezone.utc)+timedelta(minutes=10)).isoformat(),'timezone':'Asia/Ho_Chi_Minh','dateBasis':'CONFIRMED_AT'})
reference='MOCK-UNMATCHED-'+str(uuid.uuid4());exception=None;checks=[]
def report(org=False):return api(('/organizations/'+fixture['phase5Org'] if org else '/admin')+'/reports/summary?'+query,token=owner if org else admin)['metrics']
def wait(predicate):
    for _ in range(60):
        value=report()
        if predicate(value):return value
        time.sleep(.5)
    raise AssertionError('Financial report did not converge')
try:
    receipt={'deliveryId':str(uuid.uuid4()),'provider':'local-mock','merchant':'local-event-ticketing','attemptId':str(uuid.uuid4()),'orderId':str(uuid.uuid4()),'reference':reference,'amount':100,'currency':'VND','status':'SUCCESS','deliveredAt':now(),'paidAt':now()}
    raw=json.dumps(receipt,separators=(',',':')).encode()
    signature=hmac.new(credentials['mockPaymentSecret'].encode(),raw,hashlib.sha256).hexdigest()
    api('/webhooks/payments/local-mock','POST',receipt,headers={'X-Mock-Signature':signature})
    exception=sql("select e.id from payment.exceptions e join payment.transactions t on t.id=e.transaction_id where t.reference='"+reference+"';")
    assert exception
    received=wait(lambda m:m['cashCollectedVnd']==100 and m['grossVnd']==0)
    checks.append('Unmapped collection reaches global cash through Kafka without creating event sales')
    assert received['openExceptions']>=1
    checks.append('Unmapped collection remains an explicit open financial obligation')
    assert report(True)['cashCollectedVnd']==0
    checks.append('Unassigned cash is absent from the Organizer report')
finally:
    if exception:
        api('/admin/payment-exceptions/'+exception+'/review','POST',{'reason':'Verify unmatched synthetic Phase 5 receipt and return in full'},admin)
        api('/admin/payment-exceptions/'+exception+'/resolve','POST',{'reference':'MOCK-RETURN-'+str(uuid.uuid4()),'amount':100,'currency':'VND','evidence':'Verified full local simulated return; no real bank movement','returnedAt':now()},admin)
if exception:
    returned=wait(lambda m:m['cashCollectedVnd']==100 and m['cashReturnedVnd']==100 and m['netCashVnd']==0 and m['grossVnd']==0)
    assert sql("select status from payment.exceptions where id='"+exception+"';")=='RESOLVED'
    checks.append('Full evidenced return closes the obligation and preserves both cash facts')
result={'passed':len(checks),'checks':checks,'localOnly':True}
(ROOT/'output/playwright/phase5-unmapped-result.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result))
