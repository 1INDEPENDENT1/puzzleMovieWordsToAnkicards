# Implementation Plan: Все примеры в связанных карточках слов

**Branch**: `codex/008-fix-example` | **Date**: 2026-11-10 | **Spec**: [spec.md](spec.md)

**Input**: `specs/008-linked-card-examples/spec.md`; последнее решение пользователя — начать учебную коллекцию с нуля без переноса прежних карточек.

## Summary

Создать личный сохраняемый словарь: VocabularyEntry, VocabularyExample и WordExampleLink; одна ReviewCard на слово/выражение. Сопоставлять упорядоченные начальные формы с разрывом до трёх слов между соседними частями внутри одного предложения. Все примеры доступны в отдельном окне; их переводы раскрываются отдельно после раскрытия основной карточки.

Первоначальное построение выполняется из свежего структурированного импорта в чистую учебную коллекцию. Перенос старой истории, расписаний и ручных правок исключён. Архивы карточек, объединение событий, выбор донора расписания и интерфейс разрешения старых значений не нужны. После первоначального построения повторные импорты сохраняют уже новые ответы и ручные правки.

Завершённый экспорт сохраняет структурированный снимок до потери полей при дедупликации; POST создания карточек публикует его в каталог с проверкой владельца и предупреждениями о частичном результате. TSV остаётся отдельной двухколоночной проекцией.

## Technical Context

**Language/Version**: Java 21; существующие JavaScript, HTML и CSS.

**Primary Dependencies**: Spring Boot 3.2.5, MVC, Thymeleaf, Data JPA, PostgreSQL JDBC, Java HttpClient, Jsoup 1.17.2, org.drugov:lingua-core:0.1.0. Новые runtime-зависимости и изменения lingua-core не требуются. Разбиение предложений — JDK BreakIterator(Locale.ENGLISH) до tokenize/lemma.

**Storage**: PostgreSQL: каталог слов, примеры, связи, снимки импорта, новые карточки и ответы. TSV на диске. Для чистой схемы JPA создаёт mapped tables; schema.sql после Hibernate добавляет/проверяет нужные составные ограничения. Использовать существующий Spring SQL initializer с defer-datasource-initialization=true, без отдельного VocabularySchemaInitializer и без механизма data migration. Старый блок переноса review_cards заменить чистой инициализацией; не применять новую схему поверх заполненных старых учебных таблиц.

**Testing**: JUnit 5, Mockito, MVC, PostgreSQL/Testcontainers и браузерные проверки. Прежние тесты переноса старых карточек заменяются проверками чистой схемы, уникальности и повторного старта. Пропущенные PostgreSQL-тесты не считаются пройденными проверками БД.

**Target Platform**: Существующий Spring Boot web app, PostgreSQL, настольный и мобильный браузеры с dialog.

**Project Type**: Серверное web-приложение без frontend-фреймворка.

**Performance Goals**: Доступны 0/1/2/5/100 примеров без обрезания, страницы по 25 с точным total и без N+1. Измерить matching на эталоне около 1500 слов/800 фраз; дополнительный SLA не вводится.

**Constraints**: Изоляция владельцев, сохранность новых данных при повторном импорте/restart, раздельные переводы и все источники. Структурированные app-owned записи вместо парсинга TSV/back HTML. Scheduler, login/session authentication, предупреждения и проверки загрузки 007 сохраняются.

**Scale/Scope**: Каталог каждого пользователя; импорт, matching, новые таблицы, библиотека и окно повторения. Перенос старой коллекции, новая регистрация, общий словарь, универсальное распознавание идиом и удаление отсутствующих в импорте записей вне задачи.

## Constitution Check

*GATE: Повторно проверено после изменения требований и упрощения дизайна.*

| Принцип | Результат |
| --- | --- |
| I: требования | PASS: начальное построение с нуля явно выбрано пользователем; прежнее требование переноса отменено |
| II: ограниченное исследование | PASS: технические результаты сохранены, ненужная ветвь перехода удалена |
| III: границы | PASS: пересоздание касается только учебных данных; пользователи, авторизация, задания и файлы экспорта сохраняются |
| IV: архитектура | PASS: текущие Spring Boot пакеты, PostgreSQL, Thymeleaf; без CLI и новой платформы |
| V: данные | PASS: отказ от старой учебной коллекции явно запрошен; новые записи, переводы, источники и ответы не теряются |
| Источник карточек | PASS: typed snapshot структурированных исходных полей; без восстановления из TSV/HTML |
| Проверка | PASS: quickstart покрывает SC-001…SC-007, чистую PostgreSQL, restart и браузер |

Конституция не изменяется. PROJECT_CONTEXT отстаёт в части наличия review; учитываются фактический код и AGENTS.md. Эта работа меняет только документы и не выполняет очистку базы.

## Project Structure

### Documentation (this feature)

```text
specs/008-linked-card-examples/
  spec.md
  discovery.md
  checklists/requirements.md
  plan.md
  research.md
  data-model.md
  fresh-start.md
  contracts/http-and-ui.md
  quickstart.md
```

`tasks.md` создаётся следующей командой `$speckit-tasks`.

### Source Code (repository root)

```text
src/main/java/com/puzzlemovies/export/
  export/       # ordered normalization, typed source, matcher; TSV projection
  model/        # VocabularyEntry, VocabularyExample, WordExampleLink,
                # VocabularyImportSnapshot; ReviewCard с entry_id
  repo/         # owner-scoped queries и обычная статистика карточки
  review/       # sentence adapter, linked card view, presentation state
  service/      # import, examples, existing review/edit services
  web/          # ReviewController/ReviewDtos и endpoints примеров
src/main/resources/
  application.yml
  schema.sql
  templates/reviews.html
  templates/cards.html
  static/js/reviews.js
  static/js/cards.js
  static/css/app.css
src/test/java/com/puzzlemovies/export/
  export/
  review/
  service/
  repo/
  web/
```

**Structure Decision**: Существующие границы. Отдельный transactional service атомарно завершает экспорт со снимком; внутренний вызов @Transactional/@Async не считать новой transaction boundary. Инициализация чистой схемы использует штатный порядок Hibernate → Spring SQL initialization, без пользовательских запросов до завершения startup.

## Phase 0 — Research

[research.md](research.md) сохраняет результаты matching/import/изоляции, а прежний дизайн переноса заменён решением начать с нуля. Неопределённостей, требующих продуктового выбора, нет.

## Phase 1 — Design

[data-model.md](data-model.md), [fresh-start.md](fresh-start.md), [контракты](contracts/http-and-ui.md), [quickstart.md](quickstart.md).

Порядок реализации: эталон matching и чистая схема; structured snapshot и импорт; чтение/редактирование каталога и обычная статистика; окно примеров; регрессия TSV и сквозная проверка. Первоначальный сброс — отдельный однократный шаг подготовки, не обработчик импорта и не действие при каждом старте.

## Complexity Tracking

Нарушений конституции нет. Явные связи нужны для одного примера у нескольких слов и правок конкретного контекста. Достаточно одной карточки на entry и прямой истории её ответов; lifecycle, merge links, архивы, checkpoints и варианты старых правок исключены.
