from pathlib import Path
from datetime import datetime,timezone,timedelta
import os,re,json,time,tempfile,subprocess,shutil,http.cookiejar,argparse
from urllib.request import build_opener,HTTPCookieProcessor,Request
from urllib.parse import urlencode,urlparse
from urllib.error import HTTPError,URLError
ROOT=next(p for p in Path(__file__).resolve().parents if (p/"pom.xml").exists())
PG=Path('C:/Program Files/PostgreSQL/17/bin')
TMPBASE=Path(tempfile.gettempdir()).resolve()
RUN=Path(tempfile.mkdtemp(prefix='puzzlemovies-smoke-')).resolve()
assert RUN.parent==TMPBASE and RUN.name.startswith('puzzlemovies-smoke-')
BASE='http://127.0.0.1:18080'
opts=argparse.ArgumentParser()
opts.add_argument('--report-label', default='current')
args=opts.parse_args()
if not re.fullmatch(r'[A-Za-z0-9_-]+',args.report_label):raise SystemExit('Invalid safe report label')
RUN_DATE=datetime.now(timezone(timedelta(hours=3))).date().isoformat()
report={'date':RUN_DATE,'database':'isolated temporary PostgreSQL','applicationCodeChanged':False,'steps':[]}
app=None;pg_started=False;applog=None
flags=subprocess.CREATE_NO_WINDOW if os.name=='nt' else 0

def run(args):
 cp=subprocess.run([str(x) for x in args],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,creationflags=flags,timeout=90)
 if cp.returncode:raise RuntimeError('Local setup command failed: '+Path(str(args[0])).name)

def note(stage,**data):
 entry={'stage':stage,**data};report['steps'].append(entry);print(json.dumps(entry,ensure_ascii=False),flush=True)

jar=http.cookiejar.CookieJar();opener=build_opener(HTTPCookieProcessor(jar))
def request(path,form=None,timeout=180):
 data=urlencode(form).encode() if form is not None else None
 req=Request(BASE+path,data=data,headers={'Content-Type':'application/x-www-form-urlencoded'} if data else {})
 with opener.open(req,timeout=timeout) as resp:return resp.status,resp.geturl(),resp.read()
try:
 run([PG/'initdb.exe','-D',RUN/'db','-U','smoke_user','-A','trust','--encoding=UTF8','--locale=C'])
 run([PG/'pg_ctl.exe','-D',RUN/'db','-l',RUN/'postgres.log','-o','-h 127.0.0.1 -p 55437','-w','start']);pg_started=True
 run([PG/'createdb.exe','-h','127.0.0.1','-p','55437','-U','smoke_user','smoke_export'])
 note('temporary_database_ready',port=55437)
 jars=[p for p in (ROOT/'target').glob('*.jar') if not p.name.endswith('.original')]
 if len(jars)!=1:raise RuntimeError('Expected one executable application jar')
 applog=(ROOT/'target/live-smoke-app.log').open('wb')
 app=subprocess.Popen([str(Path(os.environ['JAVA_HOME'])/'bin/java.exe'),'--enable-preview','-jar',str(jars[0]),'--server.address=127.0.0.1','--server.port=18080','--spring.datasource.url=jdbc:postgresql://127.0.0.1:55437/smoke_export','--spring.datasource.username=smoke_user','--spring.datasource.password=','--spring.jpa.hibernate.ddl-auto=create','--spring.sql.init.mode=never','--spring.jpa.show-sql=false','--logging.level.root=WARN','--logging.level.org.hibernate.SQL=OFF','--logging.level.org.hibernate.orm.jdbc.bind=OFF','--logging.level.org.springframework.web=ERROR','--export.output-dir='+str(RUN/'exports')],stdout=applog,stderr=subprocess.STDOUT,creationflags=flags,cwd=str(ROOT))
 deadline=time.monotonic()+100
 while True:
  if app.poll() is not None:raise RuntimeError('Isolated application exited before readiness')
  try:
   status,_,_=request('/login',timeout=2)
   if status==200:break
  except (URLError,TimeoutError,OSError):pass
  if time.monotonic()>deadline:raise RuntimeError('Isolated application readiness timeout')
  time.sleep(1)
 note('application_ready',port=18080,legacySqlInitialization='disabled')
 secret=(ROOT/'secrets.txt').read_text(encoding='utf-8-sig')
 lines=[x.strip() for x in secret.splitlines() if x.strip()]
 emails=re.findall(r'[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}',secret)
 if len(emails)!=1:raise RuntimeError('Credentials file must contain exactly one account identifier')
 email=emails[0]
 candidates=[x for x in lines if email not in x]
 if len(candidates)!=1:raise RuntimeError('Credentials file must contain one separate password line')
 password=re.sub(r'^(?:password|pass|пароль)\s*[:=]\s*','',candidates[0],flags=re.I).strip().strip('"').strip("'")
 status,url,body=request('/login',{'email':email,'password':password},timeout=90)
 del password,secret,lines,candidates
 if '/exports/menu' not in url:raise RuntimeError('Account login did not reach authenticated export menu')
 note('login',http=status,authenticated=True)
 status,url,body=request('/exports',{'type':'COMBINED'},timeout=300)
 match=re.search(r'/exports/([0-9a-f-]{36})',url)
 if not match:raise RuntimeError('Export did not return a job identifier')
 job=match.group(1);note('export_started',type='COMBINED')
 deadline=time.monotonic()+300
 while True:
  status,_,body=request('/exports/'+job+'/status',timeout=30)
  state=json.loads(body)
  if state['status'] in ('COMPLETED','FAILED'):break
  if time.monotonic()>deadline:raise RuntimeError('Export completion timeout')
  time.sleep(1)
 if state['status']!='COMPLETED':
  failure=state.get('errorMessage') or 'Export failed'
  report['exportFailure']=failure if failure.startswith('Dictionary ') else 'Export failed'
  raise RuntimeError('Application export reported FAILED')
 note('export_completed',rowCount=state.get('rowCount'),phase=state.get('phase'),completeness=state.get('completeness'),
      unparsedRecordCount=state.get('unparsedRecordCount'),sourceCountDiscrepancies=state.get('sourceCountDiscrepancies',[]))
 if state.get('completeness')=='PARTIAL':
  _,_,warning_page=request('/exports/'+job)
  if b'Partial export:' not in warning_page:raise RuntimeError('Partial export warning missing before actions')
  note('partial_warning_presented',counterDiscrepancy=bool(state.get('sourceCountDiscrepancies')),
       rejectedSourceRecords=state.get('unparsedRecordCount'))
 status,_,tsv=request('/exports/'+job+'/download')
 rows=tsv.decode('utf-8-sig').splitlines();columns=[len(x.split('\t')) for x in rows]
 note('download',http=status,rows=len(rows),twoColumns=bool(rows) and all(n==2 for n in columns))
 status,url,body=request('/reviews/cards',{'sourceExportId':job})
 if '/reviews' not in url:raise RuntimeError('Review import did not redirect to review UI')
 msg=re.search(r'Review cards ready:\s*(\d+) created,\s*(\d+) refreshed',body.decode('utf-8','replace'))
 note('review_import',http=status,created=int(msg.group(1)) if msg else None,refreshed=int(msg.group(2)) if msg else None)
 status,_,body=request('/reviews/next');queue=json.loads(body);card=queue.get('card')
 if not card:raise RuntimeError('No review card available after import')
 note('review_queue',http=status,cardAvailable=True,hasExample=bool(card.get('instanceText')))
 fronts=[r.split('\t',1)[0] for r in rows]
 report['contentChecks']={'wordOriginalClean':'to mince' in fronts,'containsKnownPhrase':any('Man, heck of a win!'==x or "You don't mince words around here, do you?"==x for x in fronts),'firstFronts':fronts[:3],'firstCardOriginal':card.get('originalText'),'firstCardHasExample':bool(card.get('instanceText'))}
 report['flowCompleted']=True
 report['contentCorrect']=report['contentChecks']['wordOriginalClean'] and report['contentChecks']['containsKnownPhrase']
 note('content_validation',correct=report['contentCorrect'],wordOriginalClean=report['contentChecks']['wordOriginalClean'],containsKnownPhrase=report['contentChecks']['containsKnownPhrase'])
except Exception as exc:
 report['flowCompleted']=False
 report['failureCategory']=type(exc).__name__
 if isinstance(exc,HTTPError):report['failureHttpStatus']=exc.code
 # Only our own fixed messages are safe to report; never dump HTTP response/headers or input values.
 if type(exc) is RuntimeError:report['failure']=str(exc)
 print(json.dumps({'stage':'failed','category':report['failureCategory'],'reason':report.get('failure','see safe diagnostic report')},ensure_ascii=False),flush=True)
finally:
 if app is not None and app.poll() is None:
  app.terminate()
  try:app.wait(timeout=20)
  except subprocess.TimeoutExpired:app.kill();app.wait(timeout=10)
 if applog:applog.close()
 if pg_started:
  try:run([PG/'pg_ctl.exe','-D',RUN/'db','-m','fast','-w','stop'])
  except Exception:report['cleanupDatabaseStopped']=False
 if not report.get('cleanupDatabaseStopped') is False:
  assert RUN.parent==TMPBASE and RUN.name.startswith('puzzlemovies-smoke-')
  shutil.rmtree(RUN)
  report['temporaryDatabaseRemoved']=True
 report_path=ROOT/('specs/007-fix-dictionary-parsing/live-smoke-'+RUN_DATE+'-'+args.report_label+'.json')
 report_path.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
 print('Safe report: '+str(report_path),flush=True)

raise SystemExit(0 if report.get("flowCompleted") and report.get("contentCorrect") else 1)
