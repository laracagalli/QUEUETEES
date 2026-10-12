$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskJava = 'C:\Program Files\Java\jdk-24\bin\java.exe'
$taskReport = Join-Path $taskRoot 'bin\defense-checks'
New-Item -ItemType Directory -Path $taskReport -Force | Out-Null
$taskResults = @()
foreach ($taskFile in (Get-ChildItem (Join-Path $taskRoot 'tests') -Recurse -Filter '*.java')) {
    if ($taskFile.BaseName -eq 'MySqlIntegrationTest') { continue }
    $taskSource = [IO.File]::ReadAllText($taskFile.FullName)
    if ($taskSource -notmatch 'public static void main') { continue }
    $taskPackage = [regex]::Match($taskSource,'(?m)^package\s+([\w.]+);')
    $taskClass = if ($taskPackage.Success) { $taskPackage.Groups[1].Value + '.' + $taskFile.BaseName } else { $taskFile.BaseName }
    $taskMode = if ($taskClass -eq 'gui.auth.EmailAuthPresentationTest') { 'false' } else { 'true' }
    $taskOut = Join-Path $taskReport ($taskClass + '.out.log')
    $taskErr = Join-Path $taskReport ($taskClass + '.err.log')
    $taskProcess = Start-Process -FilePath $taskJava -ArgumentList @("-Djava.awt.headless=$taskMode",'-cp','"bin;src;lib/*"',$taskClass) -WorkingDirectory $taskRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput $taskOut -RedirectStandardError $taskErr
    if (-not $taskProcess.WaitForExit(30000)) {
        Stop-Process -Id $taskProcess.Id
        $taskState = 'TIMEOUT'
    } else { $taskState = if ($taskProcess.ExitCode -eq 0) { 'PASS' } else { 'FAIL' } }
    $taskResults += [pscustomobject]@{Test=$taskClass;Result=$taskState}
    Write-Output "$taskState $taskClass"
    if ($taskState -eq 'FAIL') { Get-Content -LiteralPath $taskErr -TotalCount 14 }
}
$taskResults | Export-Csv -LiteralPath (Join-Path $taskReport 'summary.csv') -NoTypeInformation
if ($taskResults.Result -contains 'FAIL' -or $taskResults.Result -contains 'TIMEOUT') { exit 1 }
