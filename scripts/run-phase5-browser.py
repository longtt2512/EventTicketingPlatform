#!/usr/bin/env python3
"""Real Phase 5 proof in the owned validation stack; no production or money movement."""
import json,subprocess,time,urllib.request,urllib.error
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent;runtime=ROOT/'.runtime/phase1';cli=Path.home()/'.codex/skills/playwright/scripts/playwright_cli.sh'
def sql(query):
 r=subprocess.run(['docker','compose','-f','infra/validation/docker-compose.yml','exec','-T','postgres','psql','-U','postgres','-d','event_ticketing','-XAt','-v','ON_ERROR_STOP=1'],input=query,text=True,capture_output=True,check=True,cwd=ROOT);return r.stdout.strip()
def api(method,path,body=None,version=None):
 headers={'Content-Type':'application/json'}
 if admin:headers['Authorization']='Bearer '+admin
 if version is not None:headers['If-Match']=str(version)
 r=urllib.request.urlopen(urllib.request.Request('http://localhost:18080/api/v1'+path,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method));data=r.read();return json.loads(data) if data else None
fixtures=[]
for i in range(3):
 subprocess.run(['python3','scripts/prepare-phase5-fixture.py'],cwd=ROOT,check=True)
 fixtures.append(json.loads((runtime/'phase5-fixtures.json').read_text()))
f=fixtures[0];f['zeroEvent']=fixtures[1]['phase5Event'];f['zeroName']=fixtures[1]['phase5Name'];f['zeroTicket']=fixtures[1]['freeTicket'];f['cancelEvent']=fixtures[2]['phase5Event'];f['cancelName']=fixtures[2]['phase5Name'];f['cancelTicket']=fixtures[2]['freeTicket']
admin='';admin=api('POST','/auth/login',{'email':f['adminEmail'],'password':f['adminPassword']})['accessToken']
original=api('GET','/admin/commission-config')['current'];settings=api('GET','/admin/settings')['settings']
reports=[]
def stage(name):
 data={**f,'stage':name};script=runtime/'phase5-browser-workflow.js';script.write_text((ROOT/'scripts/phase5-browser-workflow.js').read_text().replace('__FIXTURE__',json.dumps(data,ensure_ascii=False)));script.chmod(0o600)
 r=subprocess.run([str(cli),'-s=phase5-proof','run-code','--filename',str(script)],cwd=ROOT,capture_output=True,text=True)
 output=r.stdout+r.stderr;log=runtime/('phase5-'+name+'.log');log.write_text(output);log.chmod(0o600)
 if '### Result' not in output:raise RuntimeError('Missing browser report; inspect protected '+str(log))
 report=json.loads(output.split('### Result',1)[1].split('### Ran',1)[0].strip());report['stage']=name;reports.append(report)
 (ROOT/'output/playwright/phase5-result.json').write_text(json.dumps({'passed':sum(x['passed'] for x in reports),'reports':reports},ensure_ascii=False,indent=2)+'\n');print(name,report['passed'],'checks',report.get('failure',''),flush=True)
 if r.returncode or report.get('failure') or report.get('errors') or report.get('serverErrors'):raise RuntimeError('Phase 5 proof failed: '+name)
 f.update(report.get('state',{}))
try:
 # Known 5% starting point for new synthetic orders only; existing snapshots are untouched.
 current=api('GET','/admin/commission-config');api('POST','/admin/commission-config',{'mode':'PERCENTAGE','rateBps':500,'fixedFeeVnd':None,'effectiveAt':None,'reason':'Phase 5 synthetic baseline fee'},current['version'])
 subprocess.run([str(cli),'-s=phase5-proof','open','http://localhost:14200/admin/login','--headed'],cwd=ROOT,capture_output=True,check=True)
 subprocess.run([str(cli),'-s=phase5-proof','snapshot'],cwd=ROOT,capture_output=True,check=True)
 stage('sales')
 # Both paid orders belong to one recipient; the real scheduler must send one reminder.
 for i in range(90):
  reminder_rows=sql("select count(*) from notification.operations_deliveries where obligation_key in ('reminder:"+f['phase5Event']+"','reminder:"+f['zeroEvent']+"') and delivered_at is not null;")
  if reminder_rows=='2':break
  time.sleep(.5)
 else:raise RuntimeError('Expected exactly one delivered reminder per event/recipient')
 print('Verified two delivered reminder obligations, including two paid orders deduplicated to one reminder.',flush=True)
 # Only this run's synthetic events: advance fixture wall times, then let the real lifecycle produce COMPLETED.
 for id in [f['phase5Event'],f['zeroEvent']]:
  sql("update event_commerce.events set start_at=clock_timestamp()-interval '2 hours',end_at=clock_timestamp()-interval '1 hour' where id='"+id+"' and name like 'Đối soát local %' and status='PUBLISHED';")
 for i in range(40):
  if sql("select count(*) from event_commerce.events where id in ('"+f['phase5Event']+"','"+f['zeroEvent']+"') and status='COMPLETED';")=='2':break
  time.sleep(.5)
 else:raise RuntimeError('Lifecycle did not complete the synthetic fixtures')
 stage('settlements')
 stage('admin')
 print('Notification obligations:',sql("select payload->>'kind',count(*),count(delivered_at) from notification.operations_deliveries where payload->>'eventId' in ('"+f['phase5Event']+"','"+f['zeroEvent']+"','"+f['cancelEvent']+"') group by payload->>'kind';"),flush=True)
finally:
 try:
  current=api('GET','/admin/commission-config');api('POST','/admin/commission-config',{'mode':original['mode'],'rateBps':original['rateBps'],'fixedFeeVnd':original['fixedFeeVnd'],'effectiveAt':None,'reason':'Restore fee after Phase 5 synthetic proof'},current['version'])
  latest=api('GET','/admin/settings')['settings']
  for old in settings:
   now=next(s for s in latest if s['key']==old['key'])
   if now['value']!=old['value']:api('PATCH','/admin/settings/'+old['key'],{'value':old['value'],'reason':'Restore after Phase 5 synthetic proof'},now['version'])
  print('Restored initial commission and operational setting values; all audit and order snapshots retained.',flush=True)
 except Exception as e:print('Configuration restoration requires inspection:',str(e),flush=True)
 (runtime/'phase5-fixtures.json').write_text(json.dumps(f,ensure_ascii=False,indent=2));(runtime/'phase5-fixtures.json').chmod(0o600)
