# Implementation Plan: Исправление получения и разбора словаря PuzzleMovies

**Branch**: `007-fix-cards` | **Date**: 2026-11-04 | **Spec**: [spec.md](spec.md)

**Input**: `specs/007-fix-dictionary-parsing/spec.md`, ответы B/B и согласованная проверка пустоты на обезличенной реальной оболочке с искусственным нулевым набором.

**Status**: Design and tasks generated; live HTTP baseline executed 2026-11-09 and content defects reproduced. Feature implementation has not started. Remaining acceptance/test-harness issues are recorded below.

## Summary

Исправить получение и типизированный разбор словаря в существующем web-потоке. Использовать подтверждённый `/dictionary?item=word|phrase&noredirect=&page=N`, проверять раздел ответа и структурную пагинацию. Слова и сохранённые фразы имеют разные роли ячеек: words visible original/translation в первой, phrases metadata в первой и English/Russian во второй. Исходные поля извлекаются до нормализации; независимый эталон WORD содержит все 36 строк.

Разделить failed fetch/page recognition и partial parse отдельных записей. Только второй случай допускает скачивание и создание карточек из успешно разобранных данных с предупреждением и точным rejected count. Сохранить current schema/status enums, добавив completeness projection и versioned partial metadata в существующий errorMessage. Общие projection/eligibility используют `completeness`, `unparsedRecordCount` и `warningMessage` в SSR, polling, download и review creation; ни одна из этих границ не показывает raw metadata или не обходит её проверку.

Исследование: [research.md](research.md). Модель: [data-model.md](data-model.md). Интерфейсы: [contracts/export-and-review.md](contracts/export-and-review.md). Проверка: [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Java 21.

**Primary Dependencies**: Spring Boot 3.2.5, Spring MVC, Thymeleaf, Spring Data JPA, PostgreSQL JDBC, Java HttpClient, Jsoup 1.17.2, org.drugov:lingua-core 0.1.0. Новые runtime dependencies не нужны; существующую библиотеку не менять.

**Storage**: PostgreSQL для текущих сущностей; TSV на filesystem. Persisted enums и схема ExportJob остаются прежними. Partial metadata (`DICT_META_V1:` + компактный JSON с completeness/count) хранится в существующем errorMessage и проецируется codec в typed warning. Данные parsed source/review остаются process-local. Миграций, sidecars и ремонта исторических данных нет.

**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers PostgreSQL. Реальная обезличенная разметка и независимо проверенные expectations; отдельные client/inspector/parser tests, затем export/MVC/SSR/polling/download/import. Пустота — явно согласованный synthetic zero-data case на реальной оболочке. Реально пустой аккаунт не проверен.

**Target Platform**: Текущее серверное web-приложение Spring Boot, Maven/JAR, PostgreSQL; Thymeleaf + static JS/CSS.

**Project Type**: Единый существующий web-проект, без нового CLI, сервиса или frontend framework.

**Performance Goals**: Новые численные latency/throughput цели не вводятся. Сохранить progress phases и итоговый rowCount. Защитный предел страниц не может означать молчаливый полный успех.

**Constraints**: Двухколоночный TSV; сохранить original `to`/артикли/пунктуацию; отсутствие translation/example/movie не удаляет запись; исходный порядок; технические повторы исключаются без новой смысловой дедупликации. Не менять normalization/matcher/content keys/scheduler/history/manual override rules. Секреты и raw HTTP bodies не логируются и не включаются в fixtures.

**Scale/Scope**: Наблюдались 36 строк на первой странице, WORD last=42/13 rows, PHRASE last=22/10 rows и totals=1489/766. Это факты конкретного аккаунта на дату исследования, не лимиты приложения и не результат полной живой выгрузки. Count source rows и final TSV rowCount различаются.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Принцип / constraint | Before research | After design |
| --- | --- | --- |
| I. Не выдумывать требования/поведение источника | PASS: неизвестные выделены, материалы запрошены | PASS: реальные layouts/routes/last pages проверены; empty exception согласовано; неподтверждённый inline shape не выдумывается |
| II. Ограниченное исследование и движение по задаче | PASS | PASS: решения и пределы наблюдений записаны; реализация не начата |
| III. Границы задачи и классов | PASS | PASS: fetch/parser/export presentation и допуска partial входят в spec; card algorithm/schema/auth behavior не перерабатываются |
| IV. Текущая архитектура | PASS | PASS: существующие packages, PostgreSQL metadata, filesystem TSV; без второго export pipeline |
| V. Защита данных и полнота | PASS | PASS: секретов нет; partial видим до действий; failed fetch не выдаётся как полный или usable partial |
| Schema, data migrations, library, TSV | PASS | PASS: текущие columns/enums/TSV; no data migration/library repair |
| Phase 0 → Phase 1 | Исследование запущено | PASS: продуктовые и технические решения определены; ограничения QA не скрыты |

Исключений из конституции не требуется. Approved synthetic empty validation изменяет только способ проверки, не ожидаемое поведение пустого словаря. Post-design проверка не является утверждением о пройденных application tests.

## Project Structure

### Documentation (this feature)

```text
specs/007-fix-dictionary-parsing/
├── spec.md
├── checklists/requirements.md
├── plan.md
├── research.md
├── data-model.md
├── contracts/export-and-review.md
└── quickstart.md
```

`tasks.md` создаётся последующей командой `$speckit-tasks`, не этим workflow. Реальные raw `words.html`/`phrases.html` остаются пользовательскими входными файлами и не становятся автоматически коммитимыми fixtures.

### Source Code (repository root)

```text
src/main/java/com/puzzlemovies/export/
├── puzzlemovies/
│   ├── PuzzleMoviesDictionaryClient.java
│   ├── DictionarySection.java                # new: source set identity
│   ├── DictionaryPage.java                   # new: transient page + inspection
│   └── DictionaryPageInspector.java          # new: auth/shell/section/count/pagination
├── export/
│   ├── DictionaryParser.java
│   ├── RawDictionaryEntry.java               # new: raw fields/provenance/context
│   ├── SourcePosition.java                   # new: pre-dedupe source position
│   ├── SourceExample.java                    # new: source-only optional context
│   ├── ParseIssue.java                       # new: safe rejected position/reason
│   └── DictionaryParseResult.java            # new: records + diagnostics/accounting
├── service/
│   ├── ExportService.java
│   ├── ExportCompletionInfo.java             # new: completion projection
│   ├── ExportCompletionMetadataCodec.java    # new: existing-column persistence
│   └── GeneratedExportData.java              # new: process-local structured payload
└── web/
    ├── ExportController.java
    ├── ExportDownloadController.java
    └── ReviewController.java
src/main/resources/
├── templates/progress.html
└── static/js/progress.js
src/test/java/com/puzzlemovies/export/
├── puzzlemovies/{PuzzleMoviesDictionaryClientTest,DictionaryPageInspectorTest}.java
├── export/DictionaryParserTest.java
├── service/{ExportServiceTest,ExportCompletionMetadataCodecTest}.java
└── web/{ExportCompletionWebTest,ReviewControllerTest}.java
src/test/resources/puzzlemovies/                # new: sanitized real + labeled edge fixtures
```

**Structure Decision**: Сохранить существующие package responsibilities. Не создавать top-level legacy packages. Новые transient records компактны и имеют отдельную ответственность; не вводить новые persistent entities. GeneratedExportData нужен для согласованного immutable payload вместе с raw context; это внутреннее изменение cache representation, публичные owner-checked accessors сохраняются.

## Phase 0: Final Decisions

1. Явные item/page запросы и проверка response section; неправильный `/dictionary/phrases` набор подтверждён. Headers/cookies остаются внутри существующего клиента и не попадают в results.
2. DictionaryPageInspector распознаёт shell/auth, selected count, current page и структурную пагинацию отдельно от извлечения vocabulary fields. Фиксированный предел 50 убрать; declared source total не используется как bound, поскольку счётчик источника расходится с HTML. Сохранять visited identities и последовательные номера; сравнивать counter после структурного окончания, выдавая отдельный warning по FR-L07. Проверить набор длиннее 50 страниц без усечения.
3. Field parser использует тип набора и фактические элементы. Конкатенация текстовых узлов сохраняет пунктуацию. Неизвестные inline context subfields не выдумываются и не превращаются в новые matcher associations.
4. SourcePosition и accounting сохраняют каждую исходную строку до нормализации/dedupe и не смешивают rejected count с TSV rowCount.
5. Долговременный partial marker кодируется в существующий errorMessage, наружу выдаются completeness/warning/count. Любое неизвестное/некорректное семейство `DICT_META_` fails closed; legacy без marker остаётся без переписывания.
6. Общая completion projection и eligibility для всех user-facing путей. Per-job/per-session WarningPresentation обеспечивает показ до прямого download/POST, после чего обычная кнопка выполняет действие. Нового approval dialog нет.
7. Финальная публикация job status, phase, output path, rowCount и metadata выполняется одной согласованной persistence operation после записи файла. До неё нет доступного completed artifact. При неудаче публикации cache/output не выдаются; cleanup затрагивает только новый artifact этого job.

## Phase 1: Design and Contracts

- [data-model.md](data-model.md) определяет source/page/raw/parse-result, диагностику, completion metadata, process-local payload и состояния.
- [contracts/export-and-review.md](contracts/export-and-review.md) фиксирует retrieval/parser boundary, DTO fields, SSR/polling, warning presentation, download/import eligibility и ошибки.
- [quickstart.md](quickstart.md) даёт команды и ожидаемые результаты отдельных и end-to-end проверок. Full test suites/code bodies и migrations в документ не включены.

## Implementation Decomposition for Next Workflow

Это границы будущих independently testable tasks, не созданный tasks.md и не начало реализации. В каждом блоке сначала закрепить независимые expectations и failing checks, затем минимальный код и целевой повтор проверки.

| Блок | Точные границы / интерфейсы | Проверка результата |
| --- | --- | --- |
| Source fixtures and raw extraction | `DictionaryParser.parseSourceEntries(DictionaryPage)` → ordered RawDictionaryEntry; raw WORD/PHRASE field expectations | Исходные 36 WORD и 36 PHRASE, первые пары, пунктуация, source/movie, пустые и missing поля; unrelated tables не участвуют |
| Client and inspection | `fetchWordPages`/`fetchPhrasePages` → List<DictionaryPage>; `DictionaryPageInspector.inspect(section,page,html)` → inspection | Набор соответствует запросу; несколько страниц без потерь/повтора; structural end/count; 503/network/auth/blank/cycle/cap/changed total fail |
| Parse accounting | `parseWordPages`/`parsePhrasePages` → DictionaryParseResult<T> | accepted+rejected=source total; missing optional поля не rejected; same row once, identical independent rows preserved |
| Durable completion | ExportCompletionInfo/codec; ExportService publication/cache lifecycle | FULL/PARTIAL/FAILED разделены; exact rejected count; marker family malformed fails closed; restart сохраняет warning; artifact publication согласована |
| Web actions and review source | Общая projection, per-job presentation state, existing endpoints | SSR и polling warning до действий; direct URLs доходят до warning и затем завершаются; owner isolation; missing file/cache; existing review 409 |
| Compatibility | Existing matcher/dedupe/draftFactory/importer/formatter unchanged rules | 3 export modes, two-column TSV, original text preserved; no duplicates on re-import; manual overrides/history/schedule unchanged |

## Review Focus

- Phrase first cell contains movie metadata despite word/phrase cell classes: parser tests pin exact originals and translations independently.
- Inline spans around punctuation must not create `Man ,` or `you ?`: real phrase expectations pin exact source text.
- A changed total, page cycle or empty out-of-range page must not be mistaken for a valid empty/full dictionary: client/inspector tests pin failure.
- Internal partial metadata must not leak via SSR/errorMessage or be bypassed by direct actions: codec/MVC tests pin one projection and warning flow.
- Persisted partial file survives process cache loss: download remains available with warning, review retains 409; imported card overrides/history/scheduling remain unchanged.

## Validation Strategy

Traceability: FR-P01–06 → parser/raw/accounting; FR-L01–06 → client/inspector; FR-V01–02 → real fixtures + approved labeled empty edge; FR-V03 → compatibility; FR-V04 and SC-008/009 → projection/web/review. All user scenarios remain separately checkable before integrated tests.

Run targeted Maven tests in quickstart, then `mvn test`; manual `mvn spring-boot:run` for UI. No application tests were run by this planning command; only documents and evidence checks were verified. Full live traversal and genuinely empty account validation remain explicitly unperformed; there is no promise that a sampled live count is immutable.

## Live Baseline and Remaining Verification Work — 2026-11-09

По разрешению пользователя выполнен HTTP-прогон текущего приложения: sign-in → COMBINED export → TSV download → creation of review cards → review queue. Использована отдельная временная PostgreSQL БД, legacy SQL initialization отключена; после прогона приложение остановлено и временная БД удалена. Пользовательская библиотека не изменялась.

Результат: 1984 TSV rows, 1505 cards created; транспортный поток прошёл, **проверка содержимого не прошла**. Первый original остаётся `to mince to mince смягчать`, контрольные сохранённые phrases отсутствуют. Это baseline до исправления, не пройденная приёмка 007.

Отчёт: [live-smoke-2026-11-09.json](live-smoke-2026-11-09.json). Повторяемый script: [tools/live_smoke.py](tools/live_smoke.py); команды и prerequisites — в [quickstart.md](quickstart.md). Script читает локальный ignored secrets.txt только в памяти и возвращает неуспешный результат при неправильном содержимом; учётные данные в отчёт/fixtures не копируются.

- T027 исправлена: confirmed sourceRecordCount=0 → FULL empty export; sourceRecordCount>0 и все rejected → FAILED; mixed accepted/rejected после complete fetch → PARTIAL.
- Реальные сохранённые PHRASE examples наблюдались; populated inline WORD cell не наблюдалась. Это разграничение сохраняется в fixtures/provenance; живой вход сам по себе не закрывает неоднозначную формулировку US1.4/SC-002 о реальном непустом примере.
- Live runner проверяет реальный transport/content и пригоден для повторения после исправления, но не генерирует partial source. Для T033 остаётся подготовить управляемый test-only источник с повреждённой записью и точный способ запуска приложения с ним; не выдавать успешный live flow за проверку warning/count.
- Следующий implementation increment — T001–T014: независимые реальные fixture expectations и parser regression. Затем client/pagination, partial handling и review compatibility по tasks.md. Перед закрытием всей приёмки согласовать смысл US1.4/SC-002 и выполнить controlled partial browser scenario.

## Complexity Tracking

No constitution violations. The existing errorMessage column intentionally carries versioned partial metadata for completed partial jobs to avoid schema/data migration; a single codec/projection and corruption tests bound that tradeoff. No new persisted state enum, sidecar, parallel pipeline or speculative external parser structure is introduced.

## Approved implementation amendment — 2026-11-09

Полный обход обнаружил stable PHRASE counter=766 при 756 HTML rows на 22 страницах; WORD=1489 совпадает на 42 страницах. По ответу пользователя разрешены доступные строки с отдельным warning. Добавить SourceCountDiscrepancy(section,declaredTotal,retrievedTotal) и optional массив в существующий V1 metadata envelope; PARTIAL допускает rejected=0 при counter warning. Сохранить обе причины независимо. HTTP/auth/shape/changed-total/пагинационные ошибки не становятся partial. Проверить codec/PostgreSQL/status/SSR/gates/cache loss и повторить live traversal. Это уточнение заменяет прежнее требование exact counter equality и count-based page bound; остальные границы неизменны.
