---
description: "Задачи реализации связанных карточек и всех примеров"
---

# Tasks: Все примеры в связанных карточках слов

**Input**: `specs/008-linked-card-examples/spec.md`, `plan.md`, `research.md`, `data-model.md`, `fresh-start.md`, `contracts/http-and-ui.md`, `quickstart.md`.

**Tests**: обязательны по конституции проекта и плану для изменений поведения, схемы, интерфейсов и UI. Для каждого этапа сначала создать содержательные проверки и убедиться, что они обнаруживают отсутствие поведения. PostgreSQL-проверки, пропущенные из-за недоступного окружения, не считать пройденными.

**Формат**: `- [ ] TNNN [P?] [USn?] действие и путь`. `[P]` означает разные файлы и отсутствие зависимости от других одновременно выполняемых задач. Пути указаны относительно корня репозитория.

## Phase 1: Setup

**Цель**: подготовить воспроизводимую проверку существующего проекта и чистого набора данных.

- [X] T001 Проверить Java 21, локальное разрешение lingua-core и доступность отдельной PostgreSQL/Testcontainers; записать реальные ограничения окружения в `specs/008-linked-card-examples/quickstart.md` без секретов и без изменения рабочей базы.
- [X] T002 [P] Подготовить независимый обезличенный набор слов, фраз, фильмов и ожидаемых связей, включая 0/1/2/5/100 примеров и запятую без границы предложения, в `src/test/resources/fixtures/linked-examples/`.
- [X] T003 [P] Подготовить инструкцию однократного пересоздания только учебных таблиц на отдельной тестовой PostgreSQL с проверкой сохранности users, session tokens, export jobs и TSV в `specs/008-linked-card-examples/fresh-start.md`; не выполнять удаление в рамках создания задач и не добавлять очистку в startup/import.

## Phase 2: Foundational

**Цель**: создать общую типизированную модель и чистую учебную схему для всех историй.

- [X] T004 Создать PostgreSQL-проверки чистой схемы, повторного старта, уникальности user/entry и составных FK, запрещающих связи разных пользователей, в `src/test/java/com/puzzlemovies/export/repo/LinkedVocabularySchemaTest.java`.
- [X] T005 [P] Создать тесты точного структурированного снимка до дедупликации, включая оригиналы, отдельные переводы, inline examples, позиции, два фильма и предупреждения, в `src/test/java/com/puzzlemovies/export/export/StructuredVocabularySnapshotTest.java`.
- [X] T006 [P] Реализовать пользовательские VocabularyEntry и VocabularyExample с исходными значениями, версиями, source identity, порядком и независимыми переводами в `src/main/java/com/puzzlemovies/export/model/VocabularyEntry.java` и `src/main/java/com/puzzlemovies/export/model/VocabularyExample.java`.
- [X] T007 Реализовать WordExampleLink с уникальной парой entry/example на владельца и отдельными ручными overrides в `src/main/java/com/puzzlemovies/export/model/WordExampleLink.java`.
- [X] T008 Реализовать версионированный VocabularyImportSnapshot с владельцем, export job, typed payload, контрольной суммой и статусом публикации в `src/main/java/com/puzzlemovies/export/model/VocabularyImportSnapshot.java`.
- [X] T009 Перестроить ReviewCard на обязательную ссылку на VocabularyEntry и одну карточку владельца на единицу, сохранив существующие поля scheduler; обеспечить принадлежность ReviewAttempt той же карточке и владельцу в `src/main/java/com/puzzlemovies/export/model/ReviewCard.java` и `src/main/java/com/puzzlemovies/export/model/ReviewAttempt.java`.
- [X] T010 Добавить owner-scoped репозитории слов, примеров, связей и снимков в `src/main/java/com/puzzlemovies/export/repo/VocabularyEntryRepository.java`, `VocabularyExampleRepository.java`, `WordExampleLinkRepository.java` и `VocabularyImportSnapshotRepository.java`.
- [X] T011 Настроить чистую схему PostgreSQL: mapped tables, составные UNIQUE/FK и идемпотентные ограничения после Hibernate, без очистки данных при старте и без миграции старых карточек, в `src/main/resources/schema.sql` и `src/main/resources/application.yml`.
- [X] T012 Адаптировать либо заменить прежние тесты миграции тестами чистой схемы и убедиться, что тестовые настройки используют PostgreSQL и не разрушают пользовательскую базу, в `src/test/java/com/puzzlemovies/export/repo/ReviewSchemaMigrationTest.java` и `src/test/java/com/puzzlemovies/export/repo/LinkedVocabularySchemaTest.java`.

**Checkpoint**: чистая схема и модель доступны; бизнес-истории проверяются на изолированной базе или независимом эталоне. Старая учебная коллекция не преобразуется.

## Phase 3: User Story 1 — Все подходящие примеры у слова (P1, MVP matching)

**Цель**: корректно найти все связи слов/выражений с фразами без ограничения двумя примерами.

**Независимая проверка**: сравнить результат обработки подготовленного набора с независимым эталоном: пять и 100 примеров, два фильма, одно предложение с запятой, 0–3 слова разрыва и отрицательные случаи.

### Проверки

- [X] T013 [P] [US1] Добавить эталонные проверки начальных форм, полного ключа выражения, `to`/артиклей, повторов, `he`/`the`, запятой, границ предложений и разрывов 0/1/3/4 для каждой пары в `src/test/java/com/puzzlemovies/export/export/VocabularyNormalizerTest.java`.
- [X] T014 [P] [US1] Добавить проверки полного множества совпадений, повторных позиций, отсутствия частичных совпадений, фразы для нескольких слов и 100 примеров в `src/test/java/com/puzzlemovies/export/export/DictionaryMatcherTest.java`.
- [X] T015 [P] [US1] Добавить проверки различения `to mince`/`to run`, сохранения одинаковых фраз из разных фильмов, inline примеров и источников без стабильного ID в `src/test/java/com/puzzlemovies/export/export/VocabularyDeduplicatorTest.java` и `src/test/java/com/puzzlemovies/export/export/DictionaryPhraseSourceTest.java`.

### Реализация

- [X] T016 [US1] Реализовать разделение английских предложений и упорядоченные повторяющиеся токены с lingua-core lemma и безопасным fallback в `src/main/java/com/puzzlemovies/export/export/VocabularyNormalizer.java`; запятая не прерывает предложение и не считается словом.
- [X] T017 [US1] Разделить полную source identity единицы и matching form; исключить первый токен/общий `to` как ключ объединения и сохранить все источники фильмов в `src/main/java/com/puzzlemovies/export/export/VocabularyDeduplicator.java`, `DictionaryWord.java` и `DictionaryPhrase.java`.
- [X] T018 [US1] Передавать все исходные поля, порядок, стабильный ID при наличии и inline примеры без потери до смысловой дедупликации в `src/main/java/com/puzzlemovies/export/export/DictionaryParser.java` и `src/main/java/com/puzzlemovies/export/export/RawDictionaryEntry.java`.
- [X] T019 [US1] Реализовать поиск полной цепочки достижимых позиций в одном предложении с разрывом 0–3 слов на каждую пару, полное множество совпадений и отдельную ограниченную TSV-проекцию в `src/main/java/com/puzzlemovies/export/export/DictionaryMatcher.java` и `CompleteWordExampleMatches.java`.
- [X] T020 [US1] Прогнать проверки T013–T015 и сверить вывод с независимыми данными `src/test/resources/fixtures/linked-examples/`; зафиксировать ограничения реальной лемматизации и разбиения предложений в `specs/008-linked-card-examples/quickstart.md`.

**Checkpoint**: matching доказывается без UI и импорта; для доступной пользователю карточки дополнительно нужны US3 и US2.

## Phase 4: User Story 3 — Сохранить связанный личный словарь (P1)

**Цель**: долговечно импортировать данные пользователя, создавать одну новую карточку слова и сохранять новые ответы и правки.

**Независимая проверка**: импортировать подготовленные структурированные данные двух пользователей; проверить повтор импорта, restart, PHRASES-only, новые ответы и запрет доступа к чужим идентификаторам.

### Проверки

- [X] T021 [P] [US3] Добавить PostgreSQL-тесты идемпотентного upsert, уникальности links, двух владельцев, новых карточек NEW и сохранения ручных overrides в `src/test/java/com/puzzlemovies/export/repo/LinkedVocabularyRepositoryTest.java`.
- [X] T022 [P] [US3] Добавить сервисные тесты свежего и повторного импорта, PHRASES-only, последующего сопоставления ранее сохранённых фраз, двух фильмов, частичного импорта и сохранения новых ответов/расписания в `src/test/java/com/puzzlemovies/export/service/ReviewCardImportServiceTest.java`.
- [X] T023 [P] [US3] Добавить проверки сохранения завершённого структурированного снимка до restart, атомарного завершения задания, warning gate и отказа публикации при ошибке в `src/test/java/com/puzzlemovies/export/service/ExportServiceTest.java` и `src/test/java/com/puzzlemovies/export/web/ReviewControllerTest.java`.
- [X] T024 [P] [US3] Добавить проверки редактирования слова и конкретного примера, сохранения override только у выбранной связи, stale version и неизменности расписания в `src/test/java/com/puzzlemovies/export/service/ReviewServiceTest.java`.

### Реализация

- [X] T025 [US3] Сохранить typed снимок accepted source entries, отдельных переводов, всех источников и warnings до semantic dedupe; завершать job и сохранять снимок в одной внешней транзакции после записи TSV в `src/main/java/com/puzzlemovies/export/service/ExportService.java` и `src/main/java/com/puzzlemovies/export/service/ExportSnapshotStore.java`.
- [X] T026 [US3] Реализовать транзакционную публикацию снимка: owner lock, upsert source identities и ссылок, полное повторное сопоставление сохранённых слов/фраз, одна карточка на entry, отсутствие удаления при частичном результате и отсутствие standalone phrase cards в `src/main/java/com/puzzlemovies/export/service/ReviewCardImportService.java`.
- [X] T027 [US3] Заменить генерацию контекстных/самостоятельных drafts связанной проекцией из app-owned структурированных записей, сохраняя исходные переводы раздельно и не разбирая TSV или HTML back, в `src/main/java/com/puzzlemovies/export/review/ReviewCardDraftFactory.java` и `ReviewExportSource.java`.
- [X] T028 [US3] Подключить POST `/reviews/cards` к долговечному снимку вместо process cache, сохранить owner/completion/warning gates и сообщать результат PHRASES-only в `src/main/java/com/puzzlemovies/export/web/ReviewController.java` и `src/main/java/com/puzzlemovies/export/service/ExportService.java`.
- [X] T029 [US3] Перевести очередь, прямую статистику attempts и библиотеку на карточки с VocabularyEntry без умножения ответов из-за JOIN с примерами в `src/main/java/com/puzzlemovies/export/repo/ReviewCardRepository.java`, `ReviewAttemptRepository.java` и `src/main/java/com/puzzlemovies/export/service/ReviewService.java`.
- [X] T030 [US3] Реализовать owner-scoped чтение и optimistic редактирование слова и выбранного по exampleId примера с пересчётом связей и сохранением overrides без изменения других слов в `src/main/java/com/puzzlemovies/export/service/ReviewService.java`, `src/main/java/com/puzzlemovies/export/web/ReviewController.java` и `ReviewDtos.java`.
- [X] T031 [US3] Обновить таблицу библиотеки и её редактор: количество примеров, выбор конкретного примера, только новые карточки и сохранение ответа/версии при правке в `src/main/resources/templates/cards.html` и `src/main/resources/static/js/cards.js`.
- [X] T032 [US3] Запустить проверки T021–T024 на реальной тестовой PostgreSQL и подтвердить сохранение новых данных при двух импортах и restart по `specs/008-linked-card-examples/quickstart.md`.

**Checkpoint**: каталог и карточки сохраняются и изолируются; US3 проверяется независимо через сервис/репозиторий и существующую библиотеку, без нового review dialog.

## Phase 5: User Story 2 — Листать примеры и отдельно раскрывать переводы (P1)

**Цель**: окно всех примеров с названием фильма и отдельным раскрытием перевода после раскрытия основной карточки.

**Независимая проверка**: выдать тестовой карточке связанные примеры и проверить состояния main hidden/revealed, листание, повторное открытие, переходы между карточками и keyboard access.

### Проверки

- [X] T033 [P] [US2] Добавить MVC/сервисные проверки пагинации, владельца, presentationId, запрета перевода до основного reveal, 409 stale и отсутствия новых ответов от просмотра в `src/test/java/com/puzzlemovies/export/web/ReviewControllerTest.java` и `src/test/java/com/puzzlemovies/export/service/ReviewServiceTest.java`.
- [X] T034 [P] [US2] Подготовить исполняемую браузерную проверку нуля и 100 примеров, перехода 25/26, позднего ответа, повторного открытия, Escape/focus, мобильной ширины и reduced motion в `src/test/java/com/puzzlemovies/export/web/ReviewExamplesBrowserTest.java` и описать её запуск в `specs/008-linked-card-examples/quickstart.md`.

### Реализация

- [X] T035 [US2] Добавить временное состояние показа в HttpSession с отдельным presentationId, владельцем, версиями, reveal и истечением, без записи попытки в БД, в `src/main/java/com/puzzlemovies/export/review/ReviewPresentation.java` и `src/main/java/com/puzzlemovies/export/service/ReviewService.java`.
- [X] T036 [US2] Реализовать owner-scoped GET `/reviews/cards/{id}/examples`, POST reveal и POST перевода примера с проверкой presentation, принадлежности ссылки и версий; возвращать примеры страницами без переводов до отдельного запроса в `src/main/java/com/puzzlemovies/export/web/ReviewController.java`, `ReviewDtos.java` и `src/main/java/com/puzzlemovies/export/service/ReviewService.java`.
- [X] T037 [US2] Добавить доступный dialog, управление предыдущим/следующим примером, номер/total, фильм, отдельную кнопку перевода и возврат фокуса в `src/main/resources/templates/reviews.html` и `src/main/resources/static/css/app.css`.
- [X] T038 [US2] Реализовать состояние main reveal и example reveal, пагинацию, сбросы, защиту от поздних ответов и сетевых сбоев в `src/main/resources/static/js/reviews.js`; lookup перевода примера доступен только после main reveal.
- [X] T039 [US2] Прогнать MVC и браузерные проверки T033–T034; отдельно подтвердить, что просмотр, листание и перевод не записывают ReviewAttempt и не меняют расписание, по `specs/008-linked-card-examples/quickstart.md`.

**Checkpoint**: US2 проверяется на подготовленных связанных данных; весь пользовательский путь использует US1 и US3.

## Phase 6: Polish & Cross-Cutting

- [X] T040 [P] Проверить и при необходимости обновить регрессионные проверки двух колонок TSV, escaping, warnings и WORDS/PHRASES/COMBINED в `src/test/java/com/puzzlemovies/export/export/AnkiExportFormatterTest.java` и `src/test/java/com/puzzlemovies/export/service/ExportServiceTest.java`.
- [X] T041 Проверить исходные и новые поля/ограничения на пустой PostgreSQL, повторный запуск, конкурентные импорты и изоляцию по `src/test/java/com/puzzlemovies/export/repo/LinkedVocabularySchemaTest.java` и `specs/008-linked-card-examples/quickstart.md`.
- [X] T042 Выполнить `mvn test`, целевые PostgreSQL-тесты и браузерные сценарии; записать команды, фактический результат, пропуски и ограничения в `specs/008-linked-card-examples/quickstart.md`.
- [X] T043 Сверить FR-001–FR-017 и SC-001–SC-007, границы проекта, существующие endpoints/предупреждения и отсутствие механизма переноса старых карточек в `specs/008-linked-card-examples/spec.md` и `specs/008-linked-card-examples/quickstart.md`.

## Dependencies & Execution Order

- Setup T001–T003 → foundation T004–T012 → US1 T013–T020 → US3 T021–T032 → US2 T033–T039 → cross-cutting T040–T043.
- Все три истории имеют P1. US1 проверяет matching на независимом эталоне без БД/UI; US3 использует готовый matcher и проверяется через сервисы; US2 может тестироваться на засеянных связанных данных, но конечная интеграция зависит от US3.
- Внутри каждой истории тесты создаются до соответствующей реализации. Модели до сервисов, сервисы до контроллеров, серверный контракт до интеграции клиента.
- T025 предшествует T028; T026 зависит от T019 и репозиториев; T029–T031 используют T026. T035–T036 предшествуют T038. Общие файлы `ReviewService.java`, `ReviewController.java`, `ReviewDtos.java` и общие тесты не изменять параллельно между фазами.
- Начальное пересоздание учебной коллекции — отдельный явный шаг по `fresh-start.md`; никогда не выполнять сброс базы автоматически при импорте/старте. Не преобразовывать старые карточки и не удалять users, токены, export jobs или TSV.

## Parallel Examples

- **US1**: T013, T014 и T015 независимы между собой; после них T016–T019 выполняются по указанному порядку интеграции.
- **US3**: T021 и T024 могут выполняться одновременно в разных тестовых файлах; T022 и T023 не параллелить с другой задачей, изменяющей тот же файл. Реализацию общего сервиса/контроллера выполнять последовательно.
- **US2**: T033 и T034 работают с разными тестовыми файлами и могут идти параллельно; T037 может выполняться отдельно после фиксации контракта T036, но T038 объединяется с ней последовательно.

## Implementation Strategy

1. **MVP**: Phase 1–3 — проверяемый matching всех фраз на подготовленных данных; это технический MVP, не законченный пользовательский интерфейс.
2. Добавить Phase 4 — постоянный каталог и карточки пользователя с чистым стартом и повторным импортом без потерь новых данных.
3. Добавить Phase 5 — полный пользовательский сценарий окна примеров и управляемого раскрытия переводов.
4. Пройти Phase 6 и все применимые SC; не объявлять весь функционал завершённым по одному matching checkpoint.

Финальная проверка 2026-11-11 прошла на PostgreSQL 17 и Chrome. Итоги зафиксированы в [quickstart.md](quickstart.md#результаты-реализации-2026-11-11).
