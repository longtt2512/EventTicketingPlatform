#!/usr/bin/env python3
"""Real Phase 2 workflow on owned local services, using existing synthetic Phase 1 owner."""
import base64,json,secrets,subprocess,zlib,struct
from datetime import datetime,timedelta
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
runtime=ROOT/'.runtime/phase1'
fixture=json.loads((runtime/'browser-fixtures.json').read_text())
fixture['eventName']='Workshop kiểm chứng '+datetime.now().strftime('%Y%m%d%H%M%S')
fixture['start']=(datetime.now()+timedelta(days=20)).strftime('%Y-%m-%dT09:00')
fixture['end']=(datetime.now()+timedelta(days=20)).strftime('%Y-%m-%dT17:00')
fixture['saleStart']=(datetime.now()-timedelta(days=1)).strftime('%Y-%m-%dT09:00')
fixture['saleEnd']=(datetime.now()+timedelta(days=19)).strftime('%Y-%m-%dT23:00')
# A genuine PNG fixture, visibly synthetic; no stock/production asset is invented.
w,h=640,360
raw=b''.join(b'\x00'+b''.join(bytes((67+(x//20)%35,56+(y//10)%55,160+(x+y)%70)) for x in range(w)) for y in range(h))
def chunk(kind,data):return struct.pack('!I',len(data))+kind+data+struct.pack('!I',zlib.crc32(kind+data)&0xffffffff)
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',w,h,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b'')
fixture['banner']=base64.b64encode(png).decode()
script=runtime/'phase2-browser-workflow.js';script.write_text((ROOT/'scripts/phase2-browser-workflow.js').read_text().replace('__FIXTURE__',json.dumps(fixture,ensure_ascii=False)));script.chmod(0o600)
cli=Path.home()/'.codex/skills/playwright/scripts/playwright_cli.sh'
subprocess.run([str(cli),'-s=phase2-complete','open','http://localhost:14200/org/login','--headed'],cwd=ROOT,capture_output=True,check=True)
subprocess.run([str(cli),'-s=phase2-complete','snapshot'],cwd=ROOT,capture_output=True,check=True)
result=subprocess.run([str(cli),'-s=phase2-complete','run-code','--filename',str(script)],cwd=ROOT,capture_output=True,text=True)
output=result.stdout+result.stderr;(runtime/'phase2-browser-result.log').write_text(output);(runtime/'phase2-browser-result.log').chmod(0o600)
failed=True
if '### Result' in output:
 text=output.split('### Result',1)[1].split('### Ran',1)[0].strip()
 try:
  report=json.loads(text);failed=bool(report.get('failure'));(ROOT/'output/playwright/phase2-result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False))
 except ValueError:print(text[:1500])
else:print(output.split('### Ran')[0][:2500])
raise SystemExit(result.returncode or int(failed))
