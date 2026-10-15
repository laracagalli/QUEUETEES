$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskServer = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe'
$taskJava = 'C:\Program Files\Java\jdk-24\bin\java.exe'
if (Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue) { throw 'Port 3307 is in use. No test server was started.' }
$taskData = Join-Path $taskRoot ('bin\mysql-tests\' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskData -Force | Out-Null
& $taskServer --no-defaults --initialize-insecure "--datadir=$taskData" '--basedir=C:\Program Files\MySQL\MySQL Server 8.0' --console
if ($LASTEXITCODE -ne 0) { throw 'Could not initialize isolated MySQL test server.' }
$taskProcess = $null
try {
    $taskArgs = @('--no-defaults', "--datadir=`"$taskData`"", '--basedir="C:\Program Files\MySQL\MySQL Server 8.0"', '--port=3307', '--bind-address=127.0.0.1', '--mysqlx=OFF', "--log-error=`"$taskData\server.log`"")
    $taskProcess = Start-Process -FilePath $taskServer -ArgumentList $taskArgs -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $taskData 'launcher.out.log') -RedirectStandardError (Join-Path $taskData 'launcher.err.log')
    $taskReady = $false
    for ($taskAttempt=0; $taskAttempt -lt 30; $taskAttempt++) {
        if ($taskProcess.HasExited) {
            $taskLauncherError = Get-Content -LiteralPath (Join-Path $taskData 'launcher.err.log') -Raw -ErrorAction SilentlyContinue
            throw "Isolated MySQL server stopped before accepting connections (exit $($taskProcess.ExitCode)). $taskLauncherError"
        }
        try {
            $taskSocket = [Net.Sockets.TcpClient]::new()
            $taskSocket.Connect('127.0.0.1',3307)
            $taskSocket.Dispose();$taskReady=$true;break
        } catch { Start-Sleep -Milliseconds 500 }
    }
    if (-not $taskReady) { throw 'Isolated server did not become ready.' }
    Push-Location $taskRoot
    try {
        & $taskJava '-Djava.awt.headless=true' -cp 'bin;src;lib/*' MySqlIntegrationTest isolated-3307
        if ($LASTEXITCODE -ne 0) { throw 'MySQL integration checks failed.' }
    } finally { Pop-Location }
} finally {
    # mysqld can launch a child process on Windows; shut down only our verified data directory.
    $taskListener = Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($taskListener) {
        $taskOwner = Get-CimInstance Win32_Process -Filter "ProcessId = $($taskListener.OwningProcess)"
        if ($taskOwner.CommandLine.Contains($taskData)) {
            & (Join-Path (Split-Path $taskServer) 'mysqladmin.exe') --no-defaults --protocol=TCP --host=127.0.0.1 --port=3307 --user=root shutdown
        }
    }
    if ($taskProcess -and -not $taskProcess.HasExited) { Stop-Process -Id $taskProcess.Id; $taskProcess.WaitForExit() }
}
