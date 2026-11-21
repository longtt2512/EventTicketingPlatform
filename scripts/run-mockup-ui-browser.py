#!/usr/bin/env python3
"""Read-only UI layout/navigation proof on the isolated validation stack."""
import json,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
runtime=ROOT/'.runtime/phase1'
fixture=json.loads((runtime/'browser-fixtures.json').read_text())
script=runtime/'mockup-ui-browser-workflow.js'
script.write_text((ROOT/'scripts/mockup-ui-browser-workflow.js').read_text().replace('__FIXTURE__',json.dumps(fixture,ensure_ascii=False)));script.chmod(0o600)
cli=Path.home()/'.codex/skills/playwright/scripts/playwright_cli.sh'
subprocess.run([str(cli),'-s=mockup-verification','close'],cwd=ROOT,capture_output=True)
for args in [['open','http://localhost:14200/home','--headed'],['snapshot']]:
 subprocess.run([str(cli),'-s=mockup-verification',*args],cwd=ROOT,capture_output=True,check=True)
r=subprocess.run([str(cli),'-s=mockup-verification','run-code','--filename',str(script)],cwd=ROOT,capture_output=True,text=True)
output=r.stdout+r.stderr
log=runtime/'mockup-ui-browser-result.log';log.write_text(output);log.chmod(0o600)
if '### Result' not in output:
 print('UI proof did not return a report; inspect protected runtime log.');raise SystemExit(1)
report=json.loads(output.split('### Result',1)[1].split('### Ran',1)[0].strip())
(ROOT/'output/playwright/mockup-ui-result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(report,ensure_ascii=False))
raise SystemExit(r.returncode or int(bool(report.get('failure'))))
