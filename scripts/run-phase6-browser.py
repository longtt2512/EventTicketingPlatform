#!/usr/bin/env python3
"""Local Phase 6 acceptance: real booking race and four-app UI regression."""
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RUNTIME = ROOT / '.runtime/phase6'
CLI = Path.home() / '.codex/skills/playwright/scripts/playwright_cli.sh'

# Consumer-owned fixture creates an isolated synthetic event through real APIs.
subprocess.run(['python3', 'scripts/prepare-phase3-fixture.py', '--phase6'], cwd=ROOT, check=True)
fixture = json.loads((ROOT / '.runtime/phase1/phase6-fixtures.json').read_text())
script = RUNTIME / 'browser-workflow.js'
script.write_text((ROOT / 'scripts/phase6-browser-workflow.js').read_text().replace('__FIXTURE__', json.dumps(fixture, ensure_ascii=False)))
script.chmod(0o600)
for args in [['open', 'http://localhost:14200/home', '--headed'], ['snapshot']]:
    subprocess.run([str(CLI), '-s=phase6-proof', *args], cwd=ROOT, capture_output=True, check=True)
result = subprocess.run([str(CLI), '-s=phase6-proof', 'run-code', '--filename', str(script)], cwd=ROOT, capture_output=True, text=True)
output = result.stdout + result.stderr
log = RUNTIME / 'browser-result.log'
log.write_text(output)
log.chmod(0o600)
if '### Result' not in output:
    raise SystemExit('Missing Phase 6 report; inspect protected .runtime/phase6/browser-result.log')
report = json.loads(output.split('### Result', 1)[1].split('### Ran', 1)[0].strip())
(ROOT / 'output/playwright/phase6-result.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({k: report[k] for k in ['passed', 'failure', 'errors', 'serverErrors', 'accessibilityFindings'] if k in report}, ensure_ascii=False))
raise SystemExit(result.returncode or int(bool(report.get('failure') or report.get('errors') or report.get('serverErrors'))))
