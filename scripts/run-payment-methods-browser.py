#!/usr/bin/env python3
"""Verify checkout through real UI and SQL configuration; restore both keys in finally."""
import json, subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parent.parent
runtime=ROOT/'.runtime/phase1'
cli=Path.home()/'.codex/skills/playwright/scripts/playwright_cli.sh'
fixture=json.loads((runtime/'phase3-fixtures.json').read_text())
def sql(query):
    r=subprocess.run(['docker','compose','-f','infra/validation/docker-compose.yml','exec','-T','postgres','psql','-U','postgres','-d','event_ticketing','-XAt','-v','ON_ERROR_STOP=1'],input=query,text=True,capture_output=True,check=True,cwd=ROOT)
    return r.stdout.strip()
original=json.loads(sql("select json_agg(t) from (select key,value,updated_at from payment.mock_config where key in ('QR_PAYMENT','CARD_PAYMENT')) t;"))
backup=runtime/'checkout-original-config.json';backup.write_text(json.dumps(original));backup.chmod(0o600)
def set_config(key,value):
    literal=json.dumps(value,ensure_ascii=False).replace("'","''")
    sql(f"update payment.mock_config set value='{literal}'::jsonb,updated_at=clock_timestamp() where key='{key}';")
reports=[]
def stage(name,snapshot=None):
    data={**fixture,'stage':name,'snapshot':snapshot}
    script=runtime/'payment-methods-browser-workflow.js'
    script.write_text((ROOT/'scripts/payment-methods-browser-workflow.js').read_text().replace('__FIXTURE__',json.dumps(data,ensure_ascii=False)));script.chmod(0o600)
    r=subprocess.run([str(cli),'-s=payment-methods','run-code','--filename',str(script)],cwd=ROOT,capture_output=True,text=True)
    output=r.stdout+r.stderr
    log=runtime/('payment-methods-'+name+'.log');log.write_text(output);log.chmod(0o600)
    if '### Result' not in output: raise RuntimeError('Browser report missing; inspect protected runtime log for '+name)
    report=json.loads(output.split('### Result',1)[1].split('### Ran',1)[0].strip());report['stage']=name;reports.append(report)
    (ROOT/'output/playwright/payment-methods-result.json').write_text(json.dumps({'passed':sum(x['passed'] for x in reports),'reports':reports},ensure_ascii=False,indent=2)+'\n')
    print(name,report['passed'],'checks',report.get('failure',''),flush=True)
    if r.returncode or report.get('failure') or report.get('errors') or report.get('serverErrors'): raise RuntimeError('Checkout proof failed: '+name)
    return report.get('snapshot')
try:
    set_config('QR_PAYMENT',{'status':'SUCCESS','delaySeconds':4,'lifetimeSeconds':600})
    set_config('CARD_PAYMENT',{'status':'SUCCESS','delaySeconds':4,'lifetimeSeconds':600,'cards':[{'number':'4242424242424242','status':'SUCCESS'},{'number':'5555555555554444','status':'SUCCESS'},{'number':'4000000000000002','status':'FAILED','failure':{'code':'INSUFFICIENT_FUNDS','message':'Số dư thẻ không đủ. Vui lòng chọn thẻ khác hoặc thanh toán qua QR.'}}]})
    subprocess.run([str(cli),'-s=payment-methods','close'],cwd=ROOT,capture_output=True)
    subprocess.run([str(cli),'-s=payment-methods','open','http://localhost:14200/home','--headed'],cwd=ROOT,capture_output=True,check=True)
    subprocess.run([str(cli),'-s=payment-methods','snapshot'],cwd=ROOT,capture_output=True,check=True)
    stage('default')
    set_config('QR_PAYMENT',{'status':'SUCCESS','delaySeconds':20,'lifetimeSeconds':60})
    pending=stage('snapshot-create')
    set_config('QR_PAYMENT','failed')
    stage('snapshot-resume',pending)
    stage('failure-string')
    set_config('QR_PAYMENT',{'status':'FAILED','delaySeconds':4,'failure':{'code':'QR_NETWORK_BUSY','message':'Kết nối thanh toán đang bận. Vui lòng thử lại.'}})
    stage('failure-json')
    set_config('QR_PAYMENT',{'status':'SUCCESS','delaySeconds':4,'lifetimeSeconds':2})
    stage('expired')
    set_config('QR_PAYMENT',{'status':'UNKNOWN_CONFIG_VALUE'})
    pending=stage('invalid-config')
    set_config('QR_PAYMENT','success')
    stage('config-recovered',pending)
    print('Total:',sum(x['passed'] for x in reports),'browser checks passed.')
finally:
    for row in original:
        literal=json.dumps(row['value'],ensure_ascii=False).replace("'","''")
        sql(f"update payment.mock_config set value='{literal}'::jsonb,updated_at='{row['updated_at']}'::timestamptz where key='{row['key']}';")
    print('Restored original QR_PAYMENT and CARD_PAYMENT configuration.',flush=True)
