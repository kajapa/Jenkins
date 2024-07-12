@echo off
setlocal enabledelayedexpansion

set "folder=D:\Builds_Compressed\SHS"
set "deleteCount=0"

REM Pobierz liczbę plików i folderów w folderze
for /f %%i in ('dir /b /a-d "%folder%" ^| find /c /v ""') do set "totalCount=%%i"

REM Ustaw liczbę plików i folderów do usunięcia na połowę
set /a "deleteCount=totalCount / 2"

REM Pobierz listę plików i folderów w folderze w kolejności od najstarszego do najnowszego
for /f "delims=" %%f in ('dir /b /a-d /o:d "%folder%" ^| findstr /i /e ".zip"') do (
    if !deleteCount! gtr 0 (
        set "item=%folder%\%%f"
        if exist "!item!" (
            echo Usuwanie: "!item!"
            del /q "!item!" 2>nul
            set /a "deleteCount-=1"
        )
    ) else (
        exit /b
    )
)

pause