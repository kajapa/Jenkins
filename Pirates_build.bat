@echo off

REM Ustaw ścieżkę do repozytorium
set "repo_path=D:\git\PiratesDynasty_Preproduction"

REM Ustaw ścieżkę do buildów
set "builds_path=D:\Builds"

REM Ustaw ścieżkę do zip
set "zip_path=D:\Pirates-Dev-Compressed"

REM Przejdź do katalogu repozytorium
cd /d "%repo_path%"

REM Wywołaj git reset
git reset

REM Wywołaj git pull
git pull

REM Pobierz datę najnowszego commitu
for /f "tokens=2,3,6" %%a in ('git log -1 --format^="%%ad"') do (
    set "commit_day=%%b"
    set "commit_month=%%a"
    
)

REM Mapuj trzyliterowy skrót miesiąca na wartość liczbową
if "%commit_month%"=="Jan" set "commit_month=01"
if "%commit_month%"=="Feb" set "commit_month=02"
if "%commit_month%"=="Mar" set "commit_month=03"
if "%commit_month%"=="Apr" set "commit_month=04"
if "%commit_month%"=="May" set "commit_month=05"
if "%commit_month%"=="Jun" set "commit_month=06"
if "%commit_month%"=="Jul" set "commit_month=07"
if "%commit_month%"=="Aug" set "commit_month=08"
if "%commit_month%"=="Sep" set "commit_month=09"
if "%commit_month%"=="Oct" set "commit_month=10"
if "%commit_month%"=="Nov" set "commit_month=11"
if "%commit_month%"=="Dec" set "commit_month=12"


REM Pobierz hasha najnowszego commitu
for /f %%a in ('git rev-parse HEAD') do (
    set "commit_hash=%%a"
)
REM Pobierz skrócony hash najnowszego commitu
for /f %%a in ('git rev-parse --short HEAD') do (
    set "commit_hash_short=%%a"
)
set "commit_date=%commit_day%_%commit_month%_2023"

echo %commit_date%
REM Utwórz ścieżkę z daty i skróconego hasha commitu (8 pierwszych znaków)
set "path=%commit_date%_%commit_hash_short%"

echo %path%

REM Utwórz ścieżkę do folderu w podanym katalogu
set "full_path=%builds_path%\%path%"



if exist "%full_path%" (
    echo Folder %full_path% istnieje.
	
) else (
	echo Tworze sciezke %full_path%
	mkdir "%full_path%"
	
    echo "Compiling the code..."
"C:\Program Files\Unity\Hub\Editor\2021.3.23f1\Editor\Unity.exe" -batchmode -quit -projectPath "%repo_path%" -buildPath "%full_path%" -executeMethod BuildingTool.BuildScript.BuildGameDevelop
C:\Windows\System32\curl.exe -X POST -H "Content-type: application/json" ^
  --data "{\"text\": '<@U0525KMA6HW><@U051VA48N0P><@U040QH00ACU><@U05KPADLNRY> New build available: %path%'}" ^
  https://hooks.slack.com/services/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
	  
echo %path% > %full_path%\PiratesDynasty_Preproduction\Version.txt
"C:\Program Files\7-Zip\7z.exe" a -mx0 "%zip_path%\%path%.zip" "%full_path%\"
)