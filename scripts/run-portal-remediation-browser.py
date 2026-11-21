#!/usr/bin/env python3
"""Exercise portal context, password controls and category dialogs on synthetic accounts."""
import json,secrets,subprocess
from pathlib import Path
from datetime import datetime
ROOT=Path(__file__).resolve().parent.parent
runtime=ROOT/'.runtime/portal-remediation';runtime.mkdir(parents=True,exist_ok=True)
artifacts=ROOT/'output/playwright/portal-remediation';artifacts.mkdir(parents=True,exist_ok=True)
fixture=json.loads((ROOT/'.runtime/phase1/credentials.json').read_text())
fixture={k:fixture[k] for k in ['adminEmail','adminPassword']}
fixture.update(stamp=datetime.now().strftime('%Y%m%d%H%M%S'),password='Portal-'+secrets.token_urlsafe(18))
for name in ['fixture.json','browser.js']:
 p=runtime/name;p.write_text(json.dumps(fixture) if name.endswith('.json') else (ROOT/'scripts/portal-remediation-browser-workflow.js').read_text().replace('__FIXTURE__',json.dumps(fixture)));p.chmod(0o600)
cli=Path.home()/'.codex/skills/playwright/scripts/playwright_cli.sh'
for args in [['open','http://localhost:14200/staff/register','--headed'],['snapshot']]:
 subprocess.run([str(cli),'-s=portal-remediation',*args],cwd=ROOT,capture_output=True,check=True)
r=subprocess.run([str(cli),'-s=portal-remediation','run-code','--filename',str(runtime/'browser.js')],cwd=ROOT,capture_output=True,text=True)
p=runtime/'browser.log';p.write_text(r.stdout+r.stderr);p.chmod(0o600)
if '### Result' not in r.stdout:raise SystemExit('Missing report; inspect protected browser.log')
report=json.loads(r.stdout.split('### Result',1)[1].split('### Ran',1)[0].strip());(artifacts/'result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({k:v for k,v in report.items() if k in ['passed','failure','errors','serverErrors']},ensure_ascii=False))
raise SystemExit(r.returncode or bool(report.get('failure') or report.get('errors') or report.get('serverErrors')))
