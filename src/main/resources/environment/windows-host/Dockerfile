FROM mcr.microsoft.com/windows/servercore:ltsc2022
RUN powershell -Command " \
    Install-WindowsFeature SNMP; \
    Set-Service -Name SNMP -StartupType 'Automatic'; \
    Start-Service -Name SNMP"
