# Data Model: Личный словарь и связанные примеры

**Date**: 2026-11-10. Проект схемы; SQL и сущности создаются на этапе реализации. Поля ниже обязательны для контракта, конкретные Java-типы JSON-компонентов могут уточняться в tasks.

## Общие правила

Все новые ключи UUID; user_id NOT NULL с FK к users. Каждая сущность, на которую ссылается составной FK, имеет UNIQUE(user_id,id). Связь использует одинаковый user_id с обеих сторон. ON DELETE RESTRICT для новых учебных данных и истории. Однократная подготовка чистой коллекции описана отдельно в fresh-start.md. Optimistic version bigint NOT NULL на изменяемых агрегатах; даты Instant/timestamptz.

Source identity не изменяется от ручного display override. Строки для хранения исходного материала не обрезаются молча. Непустой original обязателен, переводы могут быть пусты. Для больших исходных текстов использовать text; существующие пределы интерактивного редактора 1000/4000/8000 сохраняются с понятной ошибкой, а не потерей исходной строки.

## VocabularyEntry → vocabulary_entries

| Поле | Назначение |
| --- | --- |
| id, user_id, version | Идентичность и владелец |
| source_key, source_identity | Хеш и полная каноническая исходная строка слова/выражения |
| source_original, source_translations | Исходный текст и упорядоченный список переводов, без примеров |
| original_override, translation_override | Nullable ручные значения; null = нет override, пустая строка перевода = намеренная пустота |
| matching_form | Версионированная ordered lemma-последовательность эффективного оригинала |
| normalization_version | Версия правила нормализации и сравнения |
| content_revision | Монотонная версия эффективного слова и списка его примеров; увеличивается при изменении source, override, состава или метаданных связанных примеров |
| created_at, updated_at | Даты |

UNIQUE(user_id,source_key); полная строка проверяется при совпадении хеша. Источники с одинаковой полной исходной единицей объединяют переводы без потери вариантов. matching_form не имеет UNIQUE и не служит идентичностью. Ручная правка оригинала меняет matching_form и пересчитывает связи владельца, но не source_key, состояние повторения или историю.

## VocabularyExample → vocabulary_examples

| Поле | Назначение |
| --- | --- |
| id, user_id, version | Идентичность и владелец |
| source_key, source_identity | Stable upstream ID с разделом либо fallback по полному содержимому и источнику |
| source_original, source_translations | Оригинал и отдельные русские переводы |
| movie_title, movie_url, source_label | Данные фильма/источника, nullable |
| provenance | Исходный раздел, upstream ID, snapshot ID, позиция и источник встроенного примера |
| first_seen_order | Стабильный порядок первого появления у владельца; assigned под блокировкой владельца |
| matching_sentences | Версионированные списки token occurrences по предложениям |
| created_at, updated_at | Даты |

UNIQUE(user_id,source_key). Происхождение фильма участвует в fallback-ключе: разные фильмы не сливаются. Ни позиция страницы, ни начальные формы не входят в fallback как основа идентичности. Встроенный пример строки слова сохраняется даже без соответствия разделу PHRASES. Если надёжного ID нет, изменение источника может создать новую source occurrence; прежняя не удаляется. Для стабильного ID обновление сохраняет исходные прежние значения в snapshot, ручные overrides находятся отдельно на связях.

JSON-поля структурированные и версионированные; это не HTML и не TSV. Оригинал, переводы и фильм остаются явно отдельными полями сущности.

## WordExampleLink → word_example_links

| Поле | Назначение |
| --- | --- |
| id, user_id, entry_id, example_id, version | Явная связь и проверка владельца |
| match_kind | LEMMA_SEQUENCE или SOURCE_INLINE для достоверного примера самой строки слова |
| normalization_version | Версия доказательства автоматической связи |
| original_override, translation_override | Правки примера для конкретной карточки; null = источник, пустой перевод = намеренно пустой |
| customized | Наличие пользовательской правки контекста |
| created_at, updated_at | Даты |

UNIQUE(user_id,entry_id,example_id). FK(user_id,entry_id) и FK(user_id,example_id) гарантируют общего владельца. Индексы по user+entry, user+example. Порядок списка: example.first_seen_order, example.id. Один source example может иметь много links; физически принадлежать только одному слову не требуется.

Повторный импорт обновляет доказательства автоматических связей, не стирая overrides. Ссылку с ручной правкой не удалять при повторном автоматическом пересчёте: это сохранённый пользовательский контекст. Автоматические связи без правок можно пересобрать атомарно по полной опубликованной коллекции владельца; отсутствие строки в частичном импорте не уменьшает коллекцию.

## ReviewCard → review_cards

- id, user_id, version — UUID, владелец и optimistic version.
- vocabulary_entry_id NOT NULL; UNIQUE(user_id,vocabulary_entry_id) гарантирует одну карточку на изучаемую единицу.
- Составной FK(user_id,vocabulary_entry_id) ссылается на VocabularyEntry того же владельца.
- state, due_at, interval_days, ease_factor, review_count, lapse_count, last_reviewed_at — существующее состояние scheduler.
- created_at, updated_at — даты.

Оригинал и перевод читаются через VocabularyEntry; примеры — через WordExampleLink. Текстовые копии original/instance/translation/source и старый content_key не нужны как идентичность новой карточки. ReviewCardView строится проекцией связанных данных. При первом создании state NEW, review_count=0, lapse_count=0, due_at соответствует существующему правилу новой карточки; повторный импорт не сбрасывает эти поля.

Due/library/count queries используют обычные карточки, кроме ReviewCardState.SUSPENDED. Никаких lifecycle, merge targets, архивных участников или состояний неразрешённого перехода нет.

## ReviewAttempt → review_attempts

Существующие поля: id, user_id, review_card_id, answer, reviewed_at, snapshots расписания и response_millis. Составной FK(user_id,review_card_id) ссылается на карточку владельца. Каждое новое событие добавляется один раз и впоследствии не переписывается.

История и процент правильных ответов считаются напрямую по ReviewAttempt своей карточки. JOIN с examples не должен умножать число ответов. Scheduler использует существующие counters; групповой агрегации и перепривязки истории нет. Старая история не импортируется.

## VocabularyImportSnapshot → vocabulary_import_snapshots

- id, user_id, export_job_id, schema_version, created_at, published_at nullable.
- payload JSONB: typed structured ExportRecord source components: accepted word/phrase entries до semantic dedupe, отдельные переводы, inline examples, provenance/source order и completion warnings.
- content_checksum для проверки целостности и повторного чтения, без cookie/password.
- UNIQUE(user_id,export_job_id), составной FK к ExportJob владельца.

Переходы: completed job + staged snapshot → published snapshot. Повтор POST по published snapshot возвращает существующий успешный итог без повторной публикации или сброса расписания. Обновлённый источник требует нового export job. Для старого completed job без снимка — явное предложение нового экспорта, без парсинга файла.

## Временный ReviewPresentation (HttpSession, не таблица)

presentationId, ownerId, cardId, cardVersion, effectiveContentVersion (= entry.content_revision), mainRevealed, expiresAt. Изолирован по вкладке/показу; срок 30 минут и ограниченный LRU до 32 показов на session — инженерный предел временного состояния, не лимит примеров. Новый показ всегда mainRevealed=false. Ответ, смена контента или истечение инвалидируют контекст. Истечение даёт 409 с предложением повторного показа, не меняет учебные данные. Обновление общего source example увеличивает content_revision всех затронутых entries того же владельца в той же транзакции.

## Инициализация и транзакционные инварианты

- Чистая учебная схема готовится однократно согласно [fresh-start.md](fresh-start.md). Старые карточки не проходят backfill; ни архивы, ни таблицы выбора прежних правок не создаются.
- JPA создаёт mapped tables; существующий Spring SQL initializer выполняет schema.sql после Hibernate с defer-datasource-initialization=true. Дополнительные UNIQUE/FK создаются идемпотентно. Ошибка SQL останавливает startup; отдельный custom initializer не нужен.
- Snapshot и COMPLETED сохраняются одним внешним transactional service; сетевое получение и запись файла — до этой транзакции.
- Публикация snapshot, upsert каталога, rematch, создание недостающих карточек и published_at — одна транзакция владельца; блокировка не удерживается во время сетевых операций.
- Импорт, answer и content edit соблюдают порядок блокировок user row → card/entry/link. Версии и unique constraints защищают конкурентные изменения.
- Ошибка откатывает публикацию; не остаются частичные links. Повторный импорт и startup не очищают новые учебные данные.
