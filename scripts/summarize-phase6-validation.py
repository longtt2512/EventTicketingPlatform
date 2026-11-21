#!/usr/bin/env python3
"""Collect existing local Phase 6 evidence; never execute or fabricate a check."""
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ARTIFACTS = ROOT / 'output/playwright'
RUNTIME = ROOT / '.runtime/phase6'

def identity(path):
    return {'path': str(path.relative_to(ROOT)), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
            'modifiedAt': datetime.fromtimestamp(path.stat().st_mtime, timezone.utc).isoformat()}

def require_clean(report, name):
    for key in ['failure', 'errors', 'pageErrors', 'serverErrors', 'consoleErrors', 'accessibilityFindings']:
        if report.get(key):
            raise SystemExit(f'{name}: unresolved {key}; inspect the source report')
    for stage in report.get('reports', []):
        require_clean(stage, name + '/' + stage.get('stage', 'stage'))

backend = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0, 'modules': {}}
xml_paths = sorted((ROOT / 'backend').glob('**/build/test-results/test/TEST-*.xml'))
if not xml_paths:
    raise SystemExit('Missing backend XML evidence')
for path in xml_paths:
    suite = ET.parse(path).getroot()
    for key in ['tests', 'failures', 'errors', 'skipped']:
        backend[key] += int(suite.get(key, 0))
    module = path.relative_to(ROOT).parts[2]
    backend['modules'][module] = backend['modules'].get(module, 0) + int(suite.get('tests', 0))
if any(backend[key] for key in ['failures', 'errors', 'skipped']):
    raise SystemExit('Backend has failed, errored or skipped cases')
if 'BUILD SUCCESSFUL' not in (RUNTIME / 'backend-final.log').read_text():
    raise SystemExit('Missing successful backend build log')

unit_log = (RUNTIME / 'frontend-tests.log').read_text()
unit_log = re.sub(r'\x1b\[[0-9;]*[A-Za-z]', '', unit_log)
unit_counts = re.findall(r'TOTAL: (\d+) SUCCESS', unit_log)
if not unit_counts:
    raise SystemExit('Missing successful frontend tests')
build_log = (RUNTIME / 'frontend-build.log').read_text()
if build_log.count('Output location:') != 4 or '[ERROR]' in build_log:
    raise SystemExit('All four frontend builds must finish successfully')
coverage_path = ROOT / 'frontend/coverage/phase1/coverage-summary.json'
coverage = json.loads(coverage_path.read_text())['total']

browser = {}
report_paths = []
for name in ['phase1', 'phase2', 'phase3', 'phase3-edge', 'phase4', 'payment-methods', 'phase5', 'phase5-unmapped', 'phase6']:
    path = ARTIFACTS / (name + '-result.json')
    report = json.loads(path.read_text())
    require_clean(report, name)
    if not report.get('passed'):
        raise SystemExit(f'{name}: no passing assertions')
    browser[name] = {'passed': report['passed'], 'evidence': identity(path)}
    report_paths.append(path)
phase6 = json.loads(report_paths[-1].read_text())
summary = {
    'generatedAt': datetime.now(timezone.utc).isoformat(),
    'scope': 'Local functional acceptance, concurrency and four-app UX',
    'excludedByOwner': ['Disaster recovery', 'Backup/restore', 'Security audit', 'Dependency remediation', 'Performance measurement'],
    'backend': backend,
    'frontend': {'tests': int(unit_counts[-1]), 'builds': 4, 'coveragePercent': {k: v['pct'] for k, v in coverage.items() if isinstance(v, dict) and 'pct' in v},
                 'warnings': ['Unused RouterLink import in Organizer application', 'SCSS warning thresholds exceeded: operations and payment checkout; budgets unchanged']},
    'browser': browser,
    'browserChecks': sum(r['passed'] for r in browser.values()),
    'ui': {'surfaces': len(phase6['surfaces']), 'viewports': [320, 390, 768, 1440], 'rootTextScalePercent': 200,
           'sampledText': sum(s['sampled'] for s in phase6['surfaces']),
           'complexBackgroundSamplesNotMeasured': sum(s['complexBackgrounds'] for s in phase6['surfaces']),
           'contrastFindings': len(phase6['accessibilityFindings']), 'physicalDeviceOrScreenReaderCertification': False},
    'nativeEvidence': [identity(p) for p in xml_paths] + [identity(RUNTIME / n) for n in ['backend-final.log', 'frontend-tests.log', 'frontend-build.log']] + [identity(coverage_path)],
    'changedProductSources': [identity(ROOT / p) for p in ['frontend/libs/booking/src/booking.component.html', 'frontend/libs/ui/src/app-shell.component.ts', 'frontend/libs/ui/src/app-shell.component.html', 'frontend/libs/ui/src/confirm-dialog.component.ts', 'frontend/libs/ui/src/_shell.scss']],
}
target = ARTIFACTS / 'phase6-validation-summary.json'
target.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({k: summary[k] for k in ['backend', 'frontend', 'browserChecks', 'ui']}, ensure_ascii=False, indent=2))
