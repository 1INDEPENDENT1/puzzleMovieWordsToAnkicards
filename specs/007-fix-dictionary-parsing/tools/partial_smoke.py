"""Offline partial-export acceptance app, backed only by a disposable PostgreSQL cluster.

Prepare: mvn test-compile dependency:build-classpath -Dmdep.outputFile=target/partial-smoke-classpath.txt
Run: python specs/007-fix-dictionary-parsing/tools/partial_smoke.py --keep-running
Stop gracefully: Ctrl+C, or create target/partial-smoke.stop in another terminal.
"""
from pathlib import Path
import argparse
import http.cookiejar
import json
import os
import shutil
import socket
import subprocess
import tempfile
import time
from urllib.request import build_opener, HTTPCookieProcessor, HTTPRedirectHandler, Request
from urllib.parse import urlencode, urlparse
from urllib.error import HTTPError, URLError

ROOT = next(p for p in Path(__file__).resolve().parents if (p / 'pom.xml').exists())
BASE = 'http://127.0.0.1:18081'
FLAGS = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0


def command(args, **kwargs):
    result = subprocess.run([str(x) for x in args], cwd=ROOT, creationflags=FLAGS, **kwargs)
    if result.returncode:
        raise RuntimeError('Setup/check command failed: ' + Path(str(args[0])).name)


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--keep-running', action='store_true')
    parser.add_argument('--serve-only', action='store_true', help='Leave browser/API interactions to caller')
    parser.add_argument('--duration', type=int, default=1800, help='Maximum browser acceptance time in seconds')
    parser.add_argument('--durability-test', action='store_true', help='Run PostgreSQL repository test before app startup')
    parser.add_argument('--restart-check', action='store_true', help='Verify download and review eligibility after process cache loss')
    parser.add_argument('--counter-mismatch', action='store_true', help='Use a stable stale source counter with zero parser rejections')
    parser.add_argument('--maven', default=shutil.which('mvn.cmd') or shutil.which('mvn'), help='Maven executable, if not on PATH')
    parser.add_argument('--pg-bin', type=Path, default=Path('C:/Program Files/PostgreSQL/17/bin'))
    args = parser.parse_args()
    rejected = 0 if args.counter_mismatch else 1
    # Existing semantic dedupe merges the two article-prefixed word identities in this fixture.
    combined_rows = 4
    classpath_file = ROOT / 'target/partial-smoke-classpath.txt'
    if not classpath_file.exists():
        raise RuntimeError('First run the Maven prepare command in this script docstring')
    for port in (18081, 55438):
        with socket.socket() as sock:
            sock.bind(('127.0.0.1', port))
    tempbase = Path(tempfile.gettempdir()).resolve()
    owned = Path(tempfile.mkdtemp(prefix='puzzlemovies-partial-')).resolve()
    stopfile = ROOT / 'target/partial-smoke.stop'
    if stopfile.exists():
        stopfile.unlink()
    app = None
    pg_started = False
    log = None
    report = {'scenario': 'synthetic partial on observed sanitized source shell', 'network': 'fake HttpClient only',
              'database': 'own temporary PostgreSQL 17, loopback port 55438', 'browserValidated': False,
              'counterMismatchScenario': args.counter_mismatch}
    pg = args.pg_bin
    quiet = {'stdout': subprocess.DEVNULL, 'stderr': subprocess.DEVNULL, 'timeout': 90}
    jar = http.cookiejar.CookieJar()
    opener = build_opener(HTTPCookieProcessor(jar))
    raw = build_opener(HTTPCookieProcessor(jar), NoRedirect())

    def request(path, form=None, redirects=True):
        data = urlencode(form).encode() if form is not None else None
        try:
            response = (opener if redirects else raw).open(Request(BASE + path, data=data), timeout=30)
        except HTTPError as error:
            response = error
        with response:
            return response.status, response.geturl(), response.read(), response.headers

    try:
        command([pg / 'initdb.exe', '-D', owned / 'db', '-U', 'partial_smoke', '-A', 'trust', '--encoding=UTF8', '--locale=C'], **quiet)
        command([pg / 'pg_ctl.exe', '-D', owned / 'db', '-l', owned / 'postgres.log', '-o', '-h 127.0.0.1 -p 55438', '-w', 'start'], **quiet)
        pg_started = True
        command([pg / 'createdb.exe', '-h', '127.0.0.1', '-p', '55438', '-U', 'partial_smoke', 'partial_export'], **quiet)
        if args.durability_test:
            if not args.maven:
                raise RuntimeError('Specify --maven because Maven is absent from PATH')
            command([args.maven, '-Dtest=ExportJobCompletionPersistenceTest',
                     '-Dreview.test.jdbc-url=jdbc:postgresql://127.0.0.1:55438/partial_export',
                     '-Dreview.test.username=partial_smoke', '-Dreview.test.password=', 'test'], timeout=180)
            report['durabilityTest'] = 'PASS (actual PostgreSQL, not skipped)'
        java = Path(os.environ['JAVA_HOME']) / 'bin/java.exe'
        cp = os.pathsep.join([str(ROOT / 'target/test-classes'), str(ROOT / 'target/classes'), classpath_file.read_text().strip()])
        log = (ROOT / 'target/partial-smoke-app.log').open('wb')
        app_args = [str(java), '--enable-preview', '-cp', cp,
            'com.puzzlemovies.export.support.PartialExportSmokeApplication',
            '--spring.profiles.active=controlled-partial', '--server.address=127.0.0.1', '--server.port=18081',
            '--spring.datasource.url=jdbc:postgresql://127.0.0.1:55438/partial_export',
            '--spring.datasource.username=partial_smoke', '--spring.datasource.password=',
            '--spring.jpa.hibernate.ddl-auto=create', '--spring.sql.init.mode=never',
            '--spring.jpa.show-sql=false', '--logging.level.root=WARN',
            '--export.puzzle-movies.base-url=https://controlled.invalid', '--export.output-dir=' + str(owned / 'exports'),
            '--controlled.counter-mismatch=' + str(args.counter_mismatch).lower()]
        app = subprocess.Popen(app_args, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, creationflags=FLAGS)
        deadline = time.monotonic() + 100
        while True:
            if app.poll() is not None:
                raise RuntimeError('Controlled app exited; inspect target/partial-smoke-app.log')
            try:
                if request('/login')[0] == 200:
                    break
            except (URLError, OSError, TimeoutError):
                pass
            if time.monotonic() > deadline:
                raise RuntimeError('Controlled application startup timeout')
            time.sleep(1)
        print('READY ' + BASE + '/login (any fake email/password; no real account used)', flush=True)
        if not args.serve_only:
            status, url, _, _ = request('/login', {'email': 'controlled@example.invalid', 'password': 'FAKE_TEST_ONLY'})
            assert '/exports/menu' in url, 'Fake login failed'
            _, url, body, _ = request('/exports', {'type': 'COMBINED'})
            job_path = urlparse(url).path
            assert job_path.startswith('/exports/'), 'No export progress URL'
            initial = json.loads(request(job_path + '/status')[2])
            assert initial['status'] in ('PENDING', 'RUNNING'), 'Delayed RUNNING state unavailable'
            assert b'Partial export:' not in body, 'Initial page already contains warning'
            deadline = time.monotonic() + 90
            while True:
                state = json.loads(request(job_path + '/status')[2])
                if state['status'] in ('COMPLETED', 'FAILED'):
                    break
                assert time.monotonic() < deadline, 'Export timeout'
                time.sleep(0.5)
            assert state['status'] == 'COMPLETED', 'Controlled partial job failed'
            assert state['completeness'] == 'PARTIAL' and state['unparsedRecordCount'] == rejected, state
            if args.counter_mismatch:
                assert state['sourceCountDiscrepancies'] == [{'section': 'PHRASE', 'declaredTotal': 3, 'retrievedTotal': 2}]
            status, _, body, headers = request(job_path + '/download', redirects=False)
            assert status == 303 and headers['Location'].endswith(job_path), 'Direct download bypassed warning'
            status, _, _, headers = request('/reviews/cards', {'sourceExportId': job_path.rsplit('/', 1)[1]}, redirects=False)
            assert status == 303 and headers['Location'].endswith(job_path), 'Review import bypassed warning'
            status, _, body, _ = request(job_path)
            assert status == 200 and b'Partial export:' in body
            assert (b'Source counter mismatch' if args.counter_mismatch else b'1 source record') in body
            assert b'DICT_META_' not in body, 'Storage envelope leaked'
            for _ in range(2):
                assert request(job_path)[0] == 200, 'Progress refresh loops'
            status, _, data, _ = request(job_path + '/download')
            rows = data.decode('utf-8-sig').splitlines()
            assert status == 200 and len(rows) == combined_rows and all(len(row.split('\t')) == 2 for row in rows)
            status, url, _, _ = request('/reviews/cards', {'sourceExportId': job_path.rsplit('/', 1)[1]})
            assert status == 200 and '/reviews' in url, 'Accepted review import failed'
            assert json.loads(request('/reviews/next')[2]).get('card'), 'No accepted card imported'
            report['apiChecks'] = {'passed': True, 'rejectedSourceRecords': rejected, 'tsvRows': len(rows),
                                   'directActions303': True, 'ssrWarningBeforeActions': True,
                                   'refreshNoLoop': True, 'downloadAndReviewAfterWarning': True}
            print('API PASS: RUNNING -> PARTIAL, rejected=' + str(rejected) + ', both direct actions 303, SSR warning, TSV rows=' + str(combined_rows) + ', review imported', flush=True)
            modes = {}
            for mode, expected_rows, completeness in [('WORDS', 2, 'PARTIAL'), ('PHRASES', 2, 'PARTIAL' if args.counter_mismatch else 'FULL')]:
                _, url, _, _ = request('/exports', {'type': mode})
                mode_path = urlparse(url).path
                deadline = time.monotonic() + 90
                while True:
                    state = json.loads(request(mode_path + '/status')[2])
                    if state['status'] in ('COMPLETED', 'FAILED'):
                        break
                    assert time.monotonic() < deadline, 'Mode export timeout'
                    time.sleep(0.5)
                assert state['status'] == 'COMPLETED' and state['completeness'] == completeness
                request(mode_path)  # Present warning before either action for partial WORDS.
                status, _, content, _ = request(mode_path + '/download')
                mode_rows = content.decode('utf-8-sig').splitlines()
                assert status == 200 and len(mode_rows) == expected_rows
                assert all(len(row.split('\t')) == 2 for row in mode_rows)
                modes[mode] = {'rows': len(mode_rows), 'completeness': completeness}
            modes['COMBINED'] = {'rows': combined_rows, 'completeness': 'PARTIAL'}
            report['modeChecks'] = modes
            print('Mode checks PASS: ' + json.dumps(modes), flush=True)
            if args.restart_check:
                app.terminate()
                app.wait(timeout=30)
                # Preserve only this disposable database/artifact set; rebuild no schema on restart.
                restart_args = [a.replace('--spring.jpa.hibernate.ddl-auto=create', '--spring.jpa.hibernate.ddl-auto=validate') for a in app_args]
                app = subprocess.Popen(restart_args, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, creationflags=FLAGS)
                jar.clear()
                deadline = time.monotonic() + 100
                while True:
                    assert app.poll() is None, 'Restarted app exited'
                    try:
                        if request('/login')[0] == 200:
                            break
                    except (URLError, OSError, TimeoutError):
                        pass
                    assert time.monotonic() < deadline, 'Restart readiness timeout'
                    time.sleep(1)
                request('/login', {'email': 'controlled@example.invalid', 'password': 'FAKE_TEST_ONLY'})
                state = json.loads(request(job_path + '/status')[2])
                assert state['completeness'] == 'PARTIAL' and state['unparsedRecordCount'] == rejected
                assert state['downloadAvailable'] and not state['reviewCreationAvailable']
                assert request(job_path + '/download', redirects=False)[0] == 303
                _, _, body, _ = request(job_path)
                assert b'Partial export:' in body and b'Generated review records are no longer available' in body
                assert request(job_path + '/download')[2] == data
                assert request('/reviews/cards', {'sourceExportId': job_path.rsplit('/', 1)[1]}, redirects=False)[0] == 409
                report['restartChecks'] = {'passed': True, 'durablePartialWarning': True,
                                           'sameTsvDownload': True, 'reviewWithoutCache409': True}
                print('Restart PASS: durable PARTIAL, rejected=' + str(rejected) + ', identical TSV, lost review cache -> 409', flush=True)
        if args.keep_running or args.serve_only:
            print('Browser app remains RUNNING. Stop via Ctrl+C or target/partial-smoke.stop', flush=True)
            deadline = time.monotonic() + args.duration
            while time.monotonic() < deadline and not stopfile.exists():
                if app.poll() is not None:
                    raise RuntimeError('Controlled app stopped unexpectedly')
                time.sleep(1)
    finally:
        if app is not None and app.poll() is None:
            app.terminate()
            try:
                app.wait(timeout=20)
            except subprocess.TimeoutExpired:
                app.kill()
                app.wait(timeout=10)
        if log:
            log.close()
        stopped = not pg_started
        if pg_started:
            try:
                command([pg / 'pg_ctl.exe', '-D', owned / 'db', '-m', 'fast', '-w', 'stop'], **quiet)
                stopped = True
            finally:
                report['temporaryDatabaseStopped'] = stopped
        if stopped:
            assert owned.parent == tempbase and owned.name.startswith('puzzlemovies-partial-')
            shutil.rmtree(owned)
            report['temporaryDirectoryRemoved'] = True
        (ROOT / 'target/partial-smoke-report.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
        print('Safe report: target/partial-smoke-report.json', flush=True)


if __name__ == '__main__':
    main()
