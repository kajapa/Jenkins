@echo off
setlocal enabledelayedexpansion

set "folder=D:\Builds\SHS"
set "deleteCount=0"

REM Pobierz liczbę plików i folderów w folderze
for /f %%i in ('dir /b /ad "%folder%" 2^>nul ^| find /c /v ""') do set "totalCount=%%i"

REM Ustaw liczbę plików i folderów do usunięcia na połowę
set /a "deleteCount=totalCount / 2"

REM Pobierz listę folderów w folderze w kolejności alfabetycznej
for /f "delims=" %%f in ('dir /b /ad /on "%folder%" 2^>nul') do (
    if !deleteCount! gtr 0 (
        set "item=%folder%\%%f"
        if exist "!item!" (
            echo Usuwanie folderu: "!item!"
            rd /s /q "!item!" 2>nul
            set /a "deleteCount-=1"
        )
    ) else (
        exit /b
    )
)



pause