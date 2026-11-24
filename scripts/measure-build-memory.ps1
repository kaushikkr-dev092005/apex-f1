param([int]$TimeoutSeconds = 900, [string]$OutputName = 'build-memory.json', [string]$Workload = 'Native APK build and serial unit/Compose tests; project Java processes only')
$ErrorActionPreference = 'Stop'
$workspace = Split-Path -Parent $PSScriptRoot
$samples = [System.Collections.Generic.List[object]]::new()
$observedBuild = $false
$emptySamples = 0
$started = Get-Date
while (((Get-Date) - $started).TotalSeconds -lt $TimeoutSeconds) {
    $processIds = @(Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object { $_.CommandLine -like "*$workspace*" } | Select-Object -ExpandProperty ProcessId)
    [long]$workingBytes = 0
    [long]$privateBytes = 0
    foreach ($buildProcessId in $processIds) {
        $buildProcess = Get-Process -Id $buildProcessId -ErrorAction SilentlyContinue
        if ($buildProcess) { $workingBytes += $buildProcess.WorkingSet64; $privateBytes += $buildProcess.PrivateMemorySize64 }
    }
    if ($processIds.Count -gt 0) { $observedBuild = $true; $emptySamples = 0 } else { $emptySamples++ }
    $samples.Add([pscustomobject]@{ elapsedSeconds = [math]::Round(((Get-Date) - $started).TotalSeconds, 1); javaProcesses = $processIds.Count; workingSetMiB = [math]::Round($workingBytes / 1MB, 1); privateCommitMiB = [math]::Round($privateBytes / 1MB, 1) })
    if ($observedBuild -and $emptySamples -ge 3) { break }
    Start-Sleep -Seconds 2
}
$directory = Join-Path $workspace 'docs/measurements'
New-Item -ItemType Directory -Force -Path $directory | Out-Null
$summary = [pscustomobject]@{
    workload = $Workload
    sampleIntervalSeconds = 2
    samples = $samples.Count
    peakObservedWorkingSetMiB = ($samples | Measure-Object -Property workingSetMiB -Maximum).Maximum
    peakObservedPrivateCommitMiB = ($samples | Measure-Object -Property privateCommitMiB -Maximum).Maximum
    maxJavaProcesses = ($samples | Measure-Object -Property javaProcesses -Maximum).Maximum
    gradleHeapLimitMiB = 768
    testHeapLimitMiB = 384
    limits = 'Two-second sampled sum of process working sets, which can double-count shared pages; not an OS-wide peak or a RAM guarantee. Native allocations, metaspace and JVM code caches are additional to heap limits.'
}
if ($OutputName -notmatch '^[a-zA-Z0-9_-]+\.json$') { throw 'OutputName must be a JSON filename without directory components.' }
$summary | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $directory $OutputName) -Encoding utf8
$summary | ConvertTo-Json
