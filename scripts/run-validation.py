#!/usr/bin/env python3
"""Manage only the Phase 1 processes owned by .runtime/phase1/pids.json."""
import argparse
import base64
import json
import os
from pathlib import Path
import secrets
import signal
import shutil
import socket
import subprocess
import time
import urllib.request

ROOT = Path(__file__).resolve().parent.parent
RUNTIME = ROOT / '.runtime' / 'phase1'
PORTS = {'identity-service': 18081, 'organization-service': 18082, 'api-gateway': 18080, 'event-commerce-service': 18083, 'discovery-service': 18084, 'payment-service': 18085, 'notification-service': 18086, 'ticket-admission-service': 18087, 'finance-service': 18088, 'admin-operations-service': 18089, 'frontends': 14200}

def process_command(pid):
    result = subprocess.run(['ps', '-p', str(pid), '-o', 'command='], capture_output=True, text=True)
    return result.stdout.strip()

def owned(name, pid):
    marker = str(ROOT / 'scripts' / 'serve-frontends.mjs') if name == 'frontends' else str(ROOT / 'backend' / 'services' / name / 'build' / 'libs' / f'{name}-0.1.0-SNAPSHOT.jar')
    command = process_command(pid)
    runtime_marker = str(RUNTIME / f'{name}.jar')
    if marker in command or (name != 'frontends' and runtime_marker in command):
        return True
    if name == 'frontends' and command == 'node scripts/serve-frontends.mjs':
        cwd = subprocess.run(['lsof', '-a', '-p', str(pid), '-d', 'cwd', '-Fn'], capture_output=True, text=True).stdout
        return 'n' + str(ROOT) in cwd.splitlines()
    return False

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['start', 'restart', 'stop', 'status'])
    parser.add_argument('--services', nargs='+', choices=PORTS, default=list(PORTS))
    args = parser.parse_args()
    RUNTIME.mkdir(parents=True, exist_ok=True)
    credentials = RUNTIME / 'credentials.json'
    saved = json.loads(credentials.read_text()) if credentials.exists() else {}
    saved.setdefault('adminEmail', 'admin-validation@example.test')
    saved.setdefault('adminPassword', secrets.token_urlsafe(24))
    saved.setdefault('serviceToken', secrets.token_urlsafe(32))
    saved.setdefault('mockPaymentSecret', secrets.token_urlsafe(48))
    saved.setdefault('dataKey', base64.b64encode(secrets.token_bytes(32)).decode())
    credentials.write_text(json.dumps(saved, indent=2) + '\n'); credentials.chmod(0o600)
    pid_file = RUNTIME / 'pids.json'
    pids = json.loads(pid_file.read_text()) if pid_file.exists() else {}
    if args.action == 'status':
        for name in args.services:
            pid = pids.get(name)
            print(f'{name}: pid={pid}, owned/running={bool(pid and owned(name, pid))}, port={PORTS[name]}')
        return
    if args.action in ['stop', 'restart']:
        for name in args.services:
            pid = pids.get(name)
            if pid and owned(name, pid):
                os.kill(pid, signal.SIGTERM)
                deadline = time.monotonic() + 10
                while owned(name, pid) and time.monotonic() < deadline:
                    time.sleep(.2)
                if owned(name, pid):
                    raise RuntimeError(f'{name} did not stop; leave it for inspection, do not kill another process.')
                print(f'Stopped owned {name} pid={pid}')
            pids.pop(name, None)
        pid_file.write_text(json.dumps(pids, indent=2) + '\n')
    if args.action == 'stop':
        return
    for name in args.services:
        pid = pids.get(name)
        if pid and owned(name, pid):
            print(f'{name} already running with owned pid={pid}'); continue
        with socket.socket() as connection:
            if connection.connect_ex(('127.0.0.1', PORTS[name])) == 0:
                raise RuntimeError(f'Port {PORTS[name]} is occupied by an unowned process; refusing to replace it.')
        env = os.environ.copy()
        env.update({
            'SERVER_PORT': str(PORTS[name]), 'DB_URL': 'jdbc:postgresql://localhost:15432/event_ticketing',
            'IDENTITY_SERVICE_TOKEN': saved['serviceToken'], 'DATA_ENCRYPTION_KEY': saved['dataKey'],
            'JWT_ISSUER': 'http://localhost:18081', 'MAIL_PORT': '11025', 'WEB_BASE_URL': 'http://localhost:14200',
            'IDENTITY_SERVICE_URL': 'http://localhost:18081', 'ORGANIZATION_SERVICE_URL': 'http://localhost:18082',
            'ADMISSION_SERVICE_URL': 'http://localhost:18087', 'PAYMENT_SERVICE_URL': 'http://localhost:18085', 'LOCAL_PAYMENT_MOCK_ENABLED': 'true', 'LOCAL_PAYMENT_MOCK_SECRET': saved['mockPaymentSecret'],
            'ADMIN_OPERATIONS_SERVICE_URL': 'http://localhost:18089', 'ORGANIZER_WEB_BASE_URL': 'http://localhost:14200/org', 'FINANCE_SERVICE_URL': 'http://localhost:18088', 'DISCOVERY_SERVICE_URL': 'http://localhost:18084', 'KAFKA_BOOTSTRAP_SERVERS': 'localhost:19092',
            'COMMERCE_SERVICE_URL': 'http://localhost:18083', 'COMMERCE_BASE_URL': 'http://localhost:18083',
            'MINIO_ENDPOINT': 'http://localhost:19000', 'MINIO_ACCESS_KEY': 'validation', 'MINIO_SECRET_KEY': 'validation_only_storage',
            'IDENTITY_BASE_URL': 'http://localhost:18081', 'IDENTITY_JWK_URI': 'http://localhost:18081/api/v1/.well-known/jwks.json',
            'CORS_ALLOWED_ORIGINS': 'http://localhost:14200',
            'BOOTSTRAP_ADMIN_EMAIL': saved['adminEmail'] if name == 'identity-service' else '',
            'BOOTSTRAP_ADMIN_PASSWORD': saved['adminPassword'] if name == 'identity-service' else '',
        })
        if name != 'frontends':
            shutil.copy2(ROOT / 'backend' / 'services' / name / 'build' / 'libs' / f'{name}-0.1.0-SNAPSHOT.jar', RUNTIME / f'{name}.jar')
        command = ['node', str(ROOT / 'scripts' / 'serve-frontends.mjs')] if name == 'frontends' else ['java', '-Xmx256m', '-jar', str(RUNTIME / f'{name}.jar')]
        with (RUNTIME / f'{name}.log').open('a') as log:
            process = subprocess.Popen(command, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
        pids[name] = process.pid
        pid_file.write_text(json.dumps(pids, indent=2) + '\n')
        print(f'Started owned {name} pid={process.pid}')
    deadline = time.monotonic() + 45
    pending = set(args.services)
    while pending and time.monotonic() < deadline:
        for name in list(pending):
            url = f'http://localhost:{PORTS[name]}' + ('/login' if name == 'frontends' else '/actuator/health')
            try:
                with urllib.request.urlopen(url, timeout=2) as response:
                    healthy = response.status == 200 and (name == 'frontends' or json.load(response).get('status') == 'UP')
                if healthy: pending.remove(name); print(f'Ready: {name}')
            except (OSError, ValueError):
                pass
        if pending: time.sleep(.5)
    if pending: raise RuntimeError(f'Readiness failed: {sorted(pending)}. Inspect .runtime/phase1/*.log.')

if __name__ == '__main__':
    main()
