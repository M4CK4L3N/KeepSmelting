# Перенос истории чатов Roo

## Способ 1: Экспорт/Импорт через интерфейс Zoo Code

### Шаг 1. Создай папку со СТАРЫМ именем проекта

```powershell
mkdir D:\projects\minecraft\firstmod
```

### Шаг 2. Открой эту папку в отдельном окне VS Code

`File → Open Folder → D:\projects\minecraft\firstmod`

VS Code вычислит workspace ID на основе пути. Если это тот же путь, что был раньше — workspace ID совпадёт, и в истории Roo появятся старые чаты.

### Шаг 3. Экспортируй задачи

В панели Roo → кнопка **History** → выбери задачу → **Export**

### Шаг 4. Импортируй в новый проект

Вернись в окно `KeepSmelting` → панель Roo → **Import**

## Способ 2: Прямое редактирование файлов (если экспорт/импорт не нужен)

Замена пути во всех файлах задач:

```powershell
$OLD_PATH = "d:\projects\minecraft\firstmod"       # СТАРЫЙ полный путь
$NEW_PATH = "d:\projects\minecraft\KeepSmelting"   # НОВЫЙ полный путь

$tasks = "$env:APPDATA\Code\User\globalStorage\zoocodeorganization.zoo-code\tasks"
$dirs = Get-ChildItem $tasks -Directory | Where-Object { $_.Name -ne 'checkpoints' }

foreach ($d in $dirs) {
    $files = Get-ChildItem $d.FullName -File
    foreach ($f in $files) {
        $c = [System.IO.File]::ReadAllText($f.FullName)
        $search = '"workspace":"' + $OLD_PATH + '"'
        if ($c.Contains($search)) {
            $c2 = $c.Replace($search, '"workspace":"' + $NEW_PATH + '"')
            [System.IO.File]::WriteAllText($f.FullName, $c2, [System.Text.UTF8Encoding]::new($false))
            Write-Host "✅ $($d.Name)/$($f.Name)"
        }
    }
}

Write-Host "🎉 Готово! Перезагрузи VS Code"
```

## Где Roo хранит историю

```
%APPDATA%\Code\User\globalStorage\zoocodeorganization.zoo-code\tasks\
├── _index.json           # индекс задач (содержит workspace)
├── {task-id}\            # папки задач
│   ├── history_item.json # метаданные (содержит workspace)
│   ├── task_metadata.json
│   ├── api_conversation_history.json
│   └── ui_messages.json
```

Workspace ID в VS Code вычисляется из пути к проекту:  
`D:\projects\minecraft\firstmod` → `63106ec20ea8887365850239cabaad8c`
