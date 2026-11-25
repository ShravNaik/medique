@echo off
call "%~dp0build.bat" javafx:run
exit /b %ERRORLEVEL%
