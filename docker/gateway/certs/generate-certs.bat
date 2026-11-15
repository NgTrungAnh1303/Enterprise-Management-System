@echo off
REM Generate self-signed TLS certificate for Nginx Gateway on Windows
setlocal

set "DIR=%~dp0"
echo Generating self-signed SSL/TLS certificate for localhost...
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout "%DIR%server.key" -out "%DIR%server.crt" -subj "/C=VN/ST=Hanoi/L=ThachThat/O=FPT-EMS/OU=IT/CN=localhost"
echo Certificate generated successfully at %DIR%server.crt and %DIR%server.key
endlocal

