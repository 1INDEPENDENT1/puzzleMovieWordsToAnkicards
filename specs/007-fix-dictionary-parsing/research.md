# Research: Исправление получения и разбора словаря PuzzleMovies

**Date**: 2026-11-04
**Status**: Phase 0 complete — технические решения определены; ограничения наблюдений явно сохранены.
**Spec**: [spec.md](spec.md)

## Sources and Observation Limits

Прочитаны AGENTS.md, PROJECT_CONTEXT.md, конституция, specs 003/005, текущие клиент/парсер/export/review код и тесты. Два локальных исследования выполнялись независимо. Затем по разрешению пользователя выполнен вход и read-only проверка сайта в браузере. Данные словаря не редактировались; секреты в эти документы и тестовые ресурсы не переносились.

| Источник | Проверенное основание |
| --- | --- |
| Исходное вложение из spec.md | 175511 байт; SHA-256 `c1ed170b89e2d9a6572bb5f8868b0ef5fd197bd52623227368e8a4c2a63d86ef`; 36 строк слов, пустые inline-примеры |
| `words.html` в корне | 600294 байт; SHA-256 `55a24451534f21cd579d5a2e74d5717f1252f275375b4005ffa40827d5308a3c`; полная страница слов |
| `phrases.html` в корне | 531504 байт; SHA-256 `290381207a261133caaa9e9dd016d97b99d4fca3d7a486b3c47405ffe8534246`; 36 сохранённых фраз с переводом и фильмом |
| Авторизованная навигация 2026-11-04 | Оба пользовательских query-адреса; обе последние страницы; phrase page 23; оба текущих client paths |

Браузерная проверка подтверждает итоговые URL и отображённую структуру, но не записывает HTTP status/redirect chain сетевого клиента. Все промежуточные страницы не выгружались. Числа 1489/766 и 42/22 относятся к наблюдавшемуся аккаунту, а не к ограничениям приложения. Полные HTML пользователя не являются готовыми обезличенными fixtures и этой командой не изменялись.

## R-01: Requests and Section Identity — Resolved

**Decision**: Использовать общий `/dictionary` с явными параметрами `item=word|phrase`, `noredirect=` и `page=N` для каждого запроса. Не полагаться на default section или sticky cookie. Проверять активный раздел ответа независимо от выбранного адреса. Параметры строить URI encoding средствами существующего стека; `noredirect` задавать один раз.

**Evidence**: Пользовательские URLs и меню обоих HTML указывают этот способ выбора. В авторизованном браузере `item=word` показывает слова, `item=phrase` — сохранённые фразы. `/dictionary/words?page=1` тоже показывает слова. `/dictionary/phrases?page=1` показывает активную вкладку слов: 36 `.translate_line_without_popup`, 0 `.dict__video__list-table__phrase__eng`, первая запись `to mince`. Теперь неправильный набор для текущего запроса фраз подтверждён; неработоспособность пути words не утверждается.

**Rationale**: Явный параметр раздела устраняет зависимость от состояния между запросами. Проверка ответа предотвращает повтор набора слов под именем фраз.

**Alternatives considered**: Сохранить `/dictionary/phrases` — возвращает неверный набор. Выбрать исторический URL только по истории — доказательств прежде не было; теперь выбор основан на реальном интерфейсе и навигации. Прямо выполнять произвольный href — может потерять раздел; из подтверждённых ссылок брать номер страницы, запрос строить для текущего набора и разрешённого origin.

## R-02: Typed Field Extraction — Resolved for Observed Shapes

**Decision**: Различать WORD и PHRASE по requested section и подтверждённой структуре. Field extraction предшествует нормализации. Сохранять оригинальные текстовые узлы с пробельными разделителями, не вставляя пробел между каждым inline span.

| Поле | WORD | PHRASE |
| --- | --- | --- |
| Original | `.translate_line_without_popup` в строке | `.dict__video__list-table__phrase__eng` / `.dictionary-phrase__eng` |
| Translation | `.dict__video__list-table__word__translate` | `.dict__video__list-table__phrase__rus` |
| Context | Доступный текст отдельной inline-ячейки, отсутствует на наблюдённых word pages | Сама сохранённая фраза является источником контекста для существующего matcher |
| Source label | Только если присутствует в подтверждённом поле | `.puzzle-card__phrase-source` |
| Movie | Только подтверждённые данные, без подстановки | `a.dict__video__list-table__info__balloon__title`, href относительно base URL |

В phrases row первая ячейка имеет также классы `dict__video__list-table__phrase` и `dict__video__list-table__word`, но содержит film/source metadata. Поэтому общий класс ячейки и её номер не определяют роль без типа набора.

**Independent expectations**:

| № PHRASE | Original | Translation | Movie title | Movie URL |
| --- | --- | --- | --- | --- |
| 1 | Man, heck of a win! | Вот это победа! | Тачки 3 / Cars 3 | https://puzzle-movies.com/films/cars-3-2017 |
| 2 | You don't mince words around here, do you? | А вы вокруг да около не ходите, верно? | Тачки 3 / Cars 3 | https://puzzle-movies.com/films/cars-3-2017 |

WORD expectations: все 36 строк записаны в spec.md независимо от текущего парсера. Hidden original не добавляется к visible original; translation и служебные действия не попадают в original.

**Rationale**: Сохранённые фразы и их metadata подтверждены реальной разметкой. Непустой inline-пример именно в WORD row не наблюдался: его доступный текст следует сохранять отдельно, но неизвестную вложенную структуру не объявлять подтверждённой. Если такой образец появляется, зафиксировать его независимые поля перед поддержкой дополнительных подпунктов. Дополнительный синтетический случай проверяет хранение текста inline-ячейки, не выдавая его за реальное наблюдение.

**Alternatives considered**: `td[0].text()` и `td[1].text()` — смешивают поля обоих типов. Универсальные card selectors без доказательств — во вложении не нашли контейнеров. Автоматически превращать inline context в saved phrase или связь для карточки — меняет matching и выходит за границы.

## R-03: Pagination and Empty Results — Resolved with Approved Validation Exception

**Decision**: Проверять известную оболочку dictionary, выбранный раздел, его общий счётчик, текущую страницу и структурный paginator. На непустом наборе следовать страницам в возрастающем порядке и останавливаться только при отсутствии подтверждённого продолжения ; стабильное расхождение счётчика после полного обхода даёт отдельное предупреждение по уточнению 2026-11-09. Защитный предел, цикл, повтор page identity, смена раздела/total или неизвестная структура дают ошибку полноты, а не успешное усечение.

Фиксированный успешный предел 50 убрать. Ранее предложенный bound по declared source total отменён после фактического расхождения счётчика и HTML; обход использует visited page identity и последовательные номера без искусственного усечения наборов длиннее 50 страниц. Упорядоченное продолжение не пропускает номера: ближайший подтверждённый next должен быть current+1; противоречие даёт ошибку, а не прыжок через неизвестные страницы.

**Observed**:

- WORD page 1 содержит 36 строк; total=1489; последняя page 42 содержит 13 строк и ссылки только назад.
- PHRASE page 1 содержит 36 строк; total=766; последняя page 22 содержит 10 строк и ссылки только назад. Paginator сохраняет `item=phrase`.
- PHRASE page 23 содержит распознаваемую пустую `table.dict__video__list-table.append-word-to`, активный раздел всё ещё имеет total=766, paginator ссылается назад. Это out-of-range, а не EMPTY_DICTIONARY. В штатном обходе page 23 запрашивать не нужно.
- До входа dictionary возвращал предложение войти вместо персональных таблиц. Один HTTP 200 не является доказательством авторизации или корректной пустоты.

**Empty classification**: Известная авторизованная оболочка + подтверждённый requested section + его total=0 + отсутствие исходных строк и противоречащего продолжения → EMPTY. Пустое тело, неизвестная оболочка, login page или нулевые rows при положительном total → соответствующая ошибка, а не EMPTY. Число строк для completeness считается до смысловой дедупликации и включает строки, позднее отклонённые field parser.

**Validation exception approved by user**: Для total=0 используется обезличенная реальная оболочка с искусственно нулевыми данными. Реально пустой аккаунт не проверялся. Удалять реальные данные для создания образца нельзя. Это исключение согласовано и внесено в spec.md, а не замаскировано под полевую проверку.

**Rationale**: Структурные признаки и сверка source total отличают конец от ошибки. Противоречие между expected total и строками исключает ошибочное признание page 23 пустым словарём. Изменение словаря во время обхода обрабатывается как неполнота с предложением повторить экспорт, без нового механизма snapshot/retry.

**Alternatives considered**: Поиск `page=N` во всём body, остановка на любом не-200, лимит 50 как успешный конец, сравнение только текста двух страниц. Они допускают потерю данных или смешивают технические повторы с самостоятельными одинаковыми записями.

## R-04: Parse Diagnostics and Partial Outcome — Resolved

**Decision**: Клиент возвращает страницы с безопасным контекстом; parser возвращает successful typed records и диагностику source positions. Ошибки отдельных records допускают PARTIAL completeness и обе пользовательские операции. Ошибки fetch/auth/page structure/completeness дают FAILED и не публикуют файл/structured source как доступный результат.

Число rejected records — точное число уникальных исходных позиций до dedupe, не количество ошибок полей и не разница source count/TSV count. Missing translation/example/movie не являются rejection. Неизвестная структура всей страницы не маскируется partial. Случай, где ни одна исходная запись не разобрана, не считается корректно пустым словарём; usable partial требует хотя бы одной успешной записи.

**Rationale**: Это реализует ответы B/B и сохраняет требования загрузки. Существующая обработка exceptions → FAILED может остаться общей границей, но ошибки следует преобразовать в безопасные категории, не сохранять произвольный текст ответа/секреты.

**Alternatives considered**: Распространить partial на HTTP 503 — пользователь этого не разрешал. Молчаливые `continue` — скрывают потери. Менять нормализатор или карточные identities — вне задачи.

## R-05: Durable Completion Metadata without Migration — Resolved

**Decision**: Сохранить существующие persisted enum statuses и схему ExportJob. `COMPLETED` означает окончание формирования usable artifact; полнота передаётся отдельным projected `completeness=FULL|PARTIAL`. Для PARTIAL записывать в существующий `ExportJob.errorMessage` компактный versioned metadata envelope с префиксом `DICT_META_V1:` и JSON `{ "completeness": "PARTIAL", "unparsedRecordCount": N }`. Добавить единый codec, не размазывать сериализацию по controllers. На status/UI metadata превращается в trusted warning и число, а `errorMessage` для completed partial наружу равен null. FAILED хранит безопасное сообщение ошибки как прежде.

Для обычного нового полного экспорта и исторических completed jobs без marker действуют прежние правила допуска; исторические задания не переклассифицируются и не переписываются. Отсутствие marker не означает, что старые данные проверены новым парсером. Любое семейство marker `DICT_META_` с неизвестной версией, некорректным JSON, отсутствующим, дробным, нецелым или неположительным count для PARTIAL не скрывать как FULL: обе операции блокируются с безопасной ошибкой metadata. В metadata нет сырых строк словаря, cookie или паролей; размер фиксированно мал относительно существующего лимита 4000.

**Rationale**: Warning/count переживают restart вместе с job и TSV без новых DB columns, schema constraints или миграции старых данных. Raw statuses/phase enum не расширяются, поэтому не требуется менять возможные database enum CHECK constraints. Обозначение FULL/PARTIAL становится обязательной частью нового UI контракта, а не скрытым смыслом слова COMPLETED.

**Alternatives considered**: Новый persisted PARTIAL enum может требовать schema changes. Новые columns нарушают выбранную минимальную область. Sidecar metadata расходится с текущим правилом хранить export metadata в PostgreSQL. Только process-local map теряет предупреждение при доступном после рестарта TSV. Текстовое предупреждение без versioned count не обеспечивает надёжного разбора.

Финальная публикация status/phase, output path, rowCount и metadata должна быть согласованной persistence operation после записи usable artifact. Cache не должен делать результат доступным до неё; при неудаче публикации cleanup затрагивает только новый artifact этого job, и raw status не обходит metadata validation.

## R-06: Shared Eligibility and Review Compatibility — Resolved

**Decision**: Единый completion projection и eligibility используются status, SSR, download и review creation. FULL/PARTIAL completed + принадлежащий пользователю job + существующий файл → download; review дополнительно требует текущий app-owned structured source. Для partial прямой download/POST без предварительного отображения предупреждения проходит через страницу предупреждения; повторное действие с тем же job допускается без нового общего approval flow. Обычный полный экспорт сохраняет прежнее поведение.

Существующие generated records/source caches можно объединить внутри ExportService в immutable `GeneratedExportData`, сохранив публичные accessor signatures и process-local lifecycle. Контекст raw records не превращается в новые matcher inputs автоматически. После утраты cache сохраняется existing 409 для создания карточек; TSV при этом доступен с сохранённым warning. Никакого восстановления review source путём разбора TSV.

**Rationale**: Ответы B/B требуют согласованного отображения предупреждения до обеих операций, включая прямые ссылки. DraftFactory, contentKey, scheduler, attempts и manual override semantics не меняются.

**Alternatives considered**: Только JS предупреждение обходится прямым URL; менять review identity или восстанавливать source из TSV выходит за scope. Отдельная ручная approval-процедура сверх обычной кнопки не нужна.

## R-07: Dependencies and Test Approach — Resolved

**Decision**: Сохранить Java 21 / Spring Boot 3.2.5 / Jsoup 1.17.2 / lingua-core 0.1.0. Использовать текущие JUnit/Mockito/Testcontainers PostgreSQL без новых runtime dependencies и H2. Разделить проверки client/page inspection и typed field parser, затем service/MVC/SSR/JS integration. Реальные HTML обезличиваются в implementation phase с сохранением selectors/text-node boundaries; expected values утверждаются отдельно от parser output.

**Rationale**: Стек уже содержит нужные средства. Целевые contract tests покрывают реальные отличия WORD/PHRASE и устойчивость partial warning. Полный test suite запускается после реализации, а не объявляется пройденным на стадии документов.

**Alternatives considered**: Новый frontend, CLI или linguistic library fix не связаны с подтверждёнными дефектами.

## Exit Result

Технических решений, требующих неподтверждённой догадки, не осталось. Phase 1 разрешена. Наблюдательные ограничения остаются частью QA: нет реального пустого аккаунта, не наблюдался непустой inline WORD example, не выполнена полная живая выгрузка всех промежуточных страниц, не измерен HTTP redirect chain. Эти ограничения не выдаются за пройденную приёмку. HTTP failures проверяются управляемыми ответами; пустота — явно согласованным искусственным краем на реальной оболочке.

## Full traversal and approved decision — 2026-11-09

Read-only Java HttpClient traversal independently counted all table tr and dictionary-class rows: both counts agree on each page. WORD pages 1–41 each 36, page42 13: sum1489. PHRASE pages 2,4,5,6,8,15,19,20 each35; page12 34; remaining 1–21 each36; page22 10: sum756, active counter remains766. Thus the difference is already in server HTML, before field parsing. No reason for the counter discrepancy is asserted. The initial strict implementation correctly failed at PHRASE22; the user explicitly chose available data with a separate counter warning. Apply FR-L07 and amended data-model/contracts. Raw responses/credentials remain memory-only; sanitized page2/page22 fixtures already capture the variable page sizes. See live reports for post-fix content acceptance.
