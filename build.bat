@echo off
setlocal
if defined JAVA_HOME (
    if not exist "%JAVA_HOME%\bin\java.exe" (
        echo JAVA_HOME does not point to a valid JDK.
        exit /b 1
    )
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)
where java >nul 2>nul
if errorlevel 1 (
    echo A JDK 21 or newer must be available through JAVA_HOME or PATH.
    exit /b 1
)
where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven 3.9 or newer must be installed and available on PATH.
    exit /b 1
)
mvn %*
exit /b %ERRORLEVEL%
