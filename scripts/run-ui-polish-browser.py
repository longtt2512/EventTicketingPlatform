#!/usr/bin/env python3
"""Review real states across all four workspaces without altering user UAT data."""
import json
import subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
runtime=ROOT/'.runtime/ui-polish';runtime.mkdir(parents=True,exist_ok=True);runtime.chmod(0o700)
artifacts=ROOT/'output/playwright/ui-polish';artifacts.mkdir(parents=True,exist_ok=True)
fixture=json.loads((ROOT/'.runtime/phase1/browser-fixtures.json').read_text())
script=runtime/'browser.js';script.write_text((ROOT/'scripts/ui-polish-browser-workflow.js').read_text().replace('__FIXTURE__',json.dumps(fixture)));script.chmod(0o600)
cli=Path.home()/'.codex/skills/playwright/scripts/playwright_cli.sh'
subprocess.run([str(cli),'-s=ui-polish-review','close'],cwd=ROOT,capture_output=True)
for args in [['open','http://localhost:14200/register','--headed'],['snapshot']]:
 subprocess.run([str(cli),'-s=ui-polish-review',*args],cwd=ROOT,capture_output=True,check=True)
r=subprocess.run([str(cli),'-s=ui-polish-review','run-code','--filename',str(script)],cwd=ROOT,capture_output=True,text=True)
log=runtime/'browser.log';log.write_text(r.stdout+r.stderr);log.chmod(0o600)
if '### Result' not in r.stdout:raise SystemExit('Missing report; inspect protected browser.log')
report=json.loads(r.stdout.split('### Result',1)[1].split('### Ran',1)[0].strip())
(artifacts/'result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({k:v for k,v in report.items() if k in ['passed','failure','errors','serverErrors']},ensure_ascii=False))
raise SystemExit(r.returncode or bool(report.get('failure') or report.get('errors') or report.get('serverErrors')))
