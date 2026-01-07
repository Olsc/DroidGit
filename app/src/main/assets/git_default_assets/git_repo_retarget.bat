@echo off
chcp 65001 >nul

for /f %%b in ('git branch -r ^| findstr origin/') do git checkout -B %%b %%b

echo ==============================
echo Current Git remote addresses:
echo ==============================
git remote -v
echo.

set /p NEW_URL=Please enter the new Git repository address (full URL):

if "%NEW_URL%"=="" (
    echo No address entered, operation cancelled.
    pause
    exit /b 1
)

echo.
echo Updating all remote addresses (applies to all branches)...
for /f %%r in ('git remote') do (
    echo Modifying remote: %%r
    git remote set-url %%r %NEW_URL%
)

echo.
echo ==============================
echo Modification completed (all branches now use the new address):
echo ==============================
git remote -v
echo.

git push --mirror

git fetch --all --prune
git branch
git push origin --all --force
git push origin --tags --force

git checkout main
git branch

pause
