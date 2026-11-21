#!/usr/bin/env python3
"""Review long-content application spacing and organization navigation on fresh QA data.

Run run-portal-remediation-browser.py first to create a new verified organizer.
"""
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
runtime = ROOT / '.runtime/portal-remediation'
artifacts = ROOT / 'output/playwright/portal-remediation'
fixture = json.loads((runtime / 'fixture.json').read_text())
script = runtime / 'review.js'
script.write_text((ROOT / 'scripts/portal-review-browser-workflow.js').read_text().replace('__FIXTURE__', json.dumps(fixture)))
script.chmod(0o600)
cli = Path.home() / '.codex/skills/playwright/scripts/playwright_cli.sh'
subprocess.run([str(cli), '-s=portal-review', 'close'], cwd=ROOT, capture_output=True)
for args in [['open', 'http://localhost:14200/org/home', '--headed'], ['snapshot']]:
    subprocess.run([str(cli), '-s=portal-review', *args], cwd=ROOT, capture_output=True, check=True)
result = subprocess.run([str(cli), '-s=portal-review', 'run-code', '--filename', str(script)], cwd=ROOT, capture_output=True, text=True)
log = runtime / 'review.log'
log.write_text(result.stdout + result.stderr)
log.chmod(0o600)
if '### Result' not in result.stdout:
    raise SystemExit('Missing report; inspect protected review.log')
report = json.loads(result.stdout.split('### Result', 1)[1].split('### Ran', 1)[0].strip())
(artifacts / 'review-result.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({key: value for key, value in report.items() if key in ['passed', 'failure', 'errors']}, ensure_ascii=False))
raise SystemExit(result.returncode or bool(report.get('failure') or report.get('errors')))
