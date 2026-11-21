#!/usr/bin/env python3
"""Run the real Phase 1 UI workflow with isolated synthetic accounts via Playwright CLI."""
import json
from pathlib import Path
import secrets
import subprocess
from datetime import datetime
ROOT=Path(__file__).resolve().parent.parent
runtime=ROOT/'.runtime/phase1'
credentials=json.loads((runtime/'credentials.json').read_text())
stamp=datetime.now().strftime('%Y%m%d%H%M%S')
fixture={'owner':f'owner-{stamp}@example.test','staff':f'staff-{stamp}@example.test','outsider':f'outsider-{stamp}@example.test',
 'orgName':f'Tổ chức thử nghiệm {stamp}','eventName':f'Sự kiện thử nghiệm {stamp}',
 'password':'Phase1-'+secrets.token_urlsafe(16),'changedPassword':'Changed-'+secrets.token_urlsafe(16),
 'adminEmail':credentials['adminEmail'],'adminPassword':credentials['adminPassword']}
code=(ROOT/'scripts/phase1-browser-workflow.js').read_text().replace('__FIXTURE__',json.dumps(fixture,ensure_ascii=False)).replace('__P1_NAVIGATION__',(ROOT/'scripts/p1-navigation-checks.js').read_text())
script=runtime/'browser-workflow.js';script.write_text(code);script.chmod(0o600)
(runtime/'browser-fixtures.json').write_text(json.dumps(fixture,ensure_ascii=False,indent=2));(runtime/'browser-fixtures.json').chmod(0o600)
cli=Path.home()/'.codex/skills/playwright/scripts/playwright_cli.sh'
subprocess.run([str(cli),'-s=phase1-complete','open','http://localhost:14200/register','--headed'],cwd=ROOT,capture_output=True,check=True)
subprocess.run([str(cli),'-s=phase1-complete','snapshot'],cwd=ROOT,capture_output=True,check=True)
result=subprocess.run([str(cli),'-s=phase1-complete','run-code','--filename',str(script)],cwd=ROOT,capture_output=True,text=True)
output=result.stdout+result.stderr
failed=True
(runtime/'browser-result.log').write_text(output);(runtime/'browser-result.log').chmod(0o600)
if '### Result' in output:
 text=output.split('### Result',1)[1].split('### Ran',1)[0].strip()
 print(text)
 try:
  report=json.loads(text)
  failed=bool(report.get('failure'))
  (ROOT/'output/playwright/phase1-result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
 except ValueError: pass
elif '### Error' in output:
 print(output.split('### Error',1)[1].split('### Ran',1)[0][:5000])
else:
 print('Playwright result saved to .runtime/phase1/browser-result.log; inspect for failure.')
raise SystemExit(result.returncode or (1 if failed else 0))
