#!/usr/bin/env python3
"""Real Phase 3 workflow on owned local services, using existing synthetic Phase 1 owner."""
import base64,json,secrets,subprocess,zlib,struct
from datetime import datetime,timedelta
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
runtime=ROOT/'.runtime/phase1'
fixture=json.loads((runtime/'phase3-fixtures.json').read_text())
script=runtime/'phase3-browser-workflow.js';script.write_text((ROOT/'scripts/phase3-browser-workflow.js').read_text().replace('__FIXTURE__',json.dumps(fixture,ensure_ascii=False)));script.chmod(0o600)
cli=Path.home()/'.codex/skills/playwright/scripts/playwright_cli.sh'
subprocess.run([str(cli),'-s=phase3-complete','open','http://localhost:14200/events','--headed'],cwd=ROOT,capture_output=True,check=True)
subprocess.run([str(cli),'-s=phase3-complete','snapshot'],cwd=ROOT,capture_output=True,check=True)
result=subprocess.run([str(cli),'-s=phase3-complete','run-code','--filename',str(script)],cwd=ROOT,capture_output=True,text=True)
output=result.stdout+result.stderr;(runtime/'phase3-browser-result.log').write_text(output);(runtime/'phase3-browser-result.log').chmod(0o600)
failed=True
if '### Result' in output:
 text=output.split('### Result',1)[1].split('### Ran',1)[0].strip()
 try:
  report=json.loads(text);failed=bool(report.get('failure'));(ROOT/'output/playwright/phase3-result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False))
 except ValueError:print(text[:1500])
else:print(output.split('### Ran')[0][:2500])
raise SystemExit(result.returncode or int(failed))
