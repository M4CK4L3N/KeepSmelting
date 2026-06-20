---
name: tool-guide
description: >-
  Universal tool selection guide. Load FIRST for any task: decides which MCP server
  (context7, tavily, playwright, filesystem) or built-in tool (read_file, apply_diff,
  execute_command) or skill (cavecrew, caveman, caveman-commit) to use for the current
  request. Prevents wrong tool choice. Triggers on any code task, question, internet
  search, web browsing, file operation, commit, review, or delegation request.
  Russian: выбор инструмента для любой задачи, что использовать когда.
---

# Tool Selector Guide

## When to use this skill

**ALWAYS.** Этот скилл — мандаторная проверка перед любым запросом. Загружать при старте сессии и перед каждым новым заданием.

## When NOT to use this skill

Только когда скилл уже загружен в текущей сессии и инструкции уже в контексте. Повторно не загружать.

---

## Mandatory Pre-Task Check

Перед выполнением ЛЮБОГО запроса проверить по порядку:

### 1. Вопрос по API/библиотеке/фреймворку? → context7

**Триггеры в запросе:**
- "как сделать X в Y"
- "документация по Z"
- "пример кода для W"
- любое упоминание конкретной библиотеки (React, axios, Forge, Fabric и т.д.)
- "API", "метод", "функция"

**Процесс:** `resolve-library-id` → `query-docs`
**НЕ использовать:** общие вопросы без конкретной технологии

### 2. Нужны данные из интернета? → tavily

| Подзадача | Инструмент |
|-----------|-----------|
| Поиск информации/новостей | tavily search (`topic: general` или `topic: news`) |
| Прочитать статью по URL | tavily extract |
| Обойти сайт/документацию | tavily crawl |
| Карта ссылок сайта | tavily map |

### 3. Нужно визуально посмотреть страницу? → playwright

**Процесс:** `navigate` → `snapshot` (для кликов/взаимодействия) → `screenshot` (для визуала)
**НЕ использовать:** API-запросы, чтение текста (там tavily extract)

### 4. Работа с файлами → выбор инструмента

| Задача | Инструмент | Почему |
|--------|-----------|--------|
| **Чтение кода** (с line numbers, семантические блоки) | `read_file` (built-in) | indentation mode |
| **Хирургическая правка** (search/replace) | `apply_diff` (built-in) | точность |
| **Запись нового файла** | `write_to_file` (built-in) | надёжность |
| **Поиск в коде** (regex с контекстом) | `search_files` (built-in) | регекс+контекст |
| **Переименовать/переместить** файл | filesystem `move_file` | built-in не умеет |
| **Метаданные** (размер, дата) | filesystem `get_file_info` | built-in не умеет |
| **Дерево папок** (JSON-структура) | filesystem `directory_tree` | built-in даёт список |
| **Чтение медиа** (PNG, аудио) | filesystem `read_media_file` | built-in не умеет |
| **Чтение нескольких файлов** | filesystem `read_multiple_files` | built-in по одному |
| **Поиск файла по имени** (glob) | filesystem `search_files` | glob-паттерны |
| **Создать папку** | built-in `write_to_file` или filesystem | оба |
| **Запуск команд, git, билды** | `execute_command` (built-in) | единственный способ |

### 5. Контекст на исходе / задача большая? → cavecrew

**Триггеры:** задача требует 3+ файлов, контекст подходит к концу, задача сложная
- `cavecrew-investigator` — найти/исследовать код
- `cavecrew-builder` — 1-2 файла изменить
- `cavecrew-reviewer` — дифф-ревью

### 6. Ответы слишком длинные? → caveman

**Уровни:** `lite` (нет воды, полные предложения), `full` (фрагменты), `ultra` (макс. сжатие)

### 7. Нужен коммит? → caveman-commit

### 8. Код-ревью? → caveman-review

### 9. Файлы памяти разрослись? → caveman-compress

### 10. Статистика сессии? → caveman-stats

---

## Quick Decision Table

| Ситуация | Инструмент |
|----------|-----------|
| Вопрос по API/библиотеке | context7 (resolve → query-docs) |
| Поиск в интернете | tavily search |
| Прочитать URL | tavily extract |
| Обойти сайт | tavily crawl |
| Посмотреть страницу визуально | playwright |
| Чтение/запись/правка кода | read_file / write_to_file / apply_diff |
| Поиск в коде (regex) | search_files (built-in) |
| Переместить/метаданные/дерево | filesystem MCP |
| Поиск файла по имени (glob) | filesystem search_files |
| Запуск команд / git / билды | execute_command |
| Большая задача / спасти контекст | cavecrew |
| Длинные ответы | caveman |
| Коммит | caveman-commit |
| Код-ревью | caveman-review |
| Сжать файлы памяти | caveman-compress |

---

## Fallback

Если ни один инструмент не подходит → стандартные `execute_command`, `read_file`, `apply_diff` и т.д. Не пытаться притянуть инструмент там где он не нужен.
