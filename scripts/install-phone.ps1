param(
    [string]$ApkPath = '',
    [string]$DeviceSerial = ''
)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $workspace '.tools/sdk/platform-tools/adb.exe'
$aapt = Join-Path $workspace '.tools/sdk/build-tools/36.0.0/aapt2.exe'
if (!(Test-Path -LiteralPath $adb) -or !(Test-Path -LiteralPath $aapt)) {
    throw 'The project-local Android tools are missing. No Android Studio installation is required, but adb and aapt2 must be available under .tools/sdk.'
}
if (!$ApkPath) { $ApkPath = Join-Path $workspace 'artifacts/APEX-phone.apk' }
$apk = (Resolve-Path -LiteralPath $ApkPath).Path
$metadata = & $aapt dump badging $apk
if ($LASTEXITCODE -ne 0) { throw 'The APK could not be parsed. Transfer or build it again.' }
$packageLine = $metadata | Select-String "^package: name='([^']+)'"
if (!$packageLine) { throw 'No Android package ID found in APK.' }
$packageId = $packageLine.Matches[0].Groups[1].Value
$devices = @(& $adb devices | ForEach-Object {
    if ($_ -match '^([^\s]+)\s+device$') { $Matches[1] }
})
if ($DeviceSerial -and $DeviceSerial -notin $devices) {
    throw 'The selected phone is unavailable or USB debugging is not authorized. Unlock it and accept the USB debugging prompt.'
}
if (!$DeviceSerial) {
    if ($devices.Count -eq 0) {
        & $adb devices -l
        throw 'Connect and unlock your phone, enable USB debugging in Developer options, and accept the authorization prompt. Then run this script again.'
    }
    if ($devices.Count -gt 1) { throw 'More than one Android device is connected. Choose one with -DeviceSerial.' }
    $DeviceSerial = $devices[0]
}
$reportPath = Join-Path $workspace '.tools/install-phone.log'
$report = @(
    'APEX phone installation diagnostic',
    ('Time: ' + (Get-Date -Format o)),
    ('APK: ' + $apk),
    ('Bytes: ' + (Get-Item -LiteralPath $apk).Length),
    ('SHA256: ' + (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash),
    ('Package: ' + $packageId),
    ('Phone: ' + (& $adb -s $DeviceSerial shell getprop ro.product.model)),
    ('Android: ' + (& $adb -s $DeviceSerial shell getprop ro.build.version.release)),
    ('API: ' + (& $adb -s $DeviceSerial shell getprop ro.build.version.sdk)),
    ('ABIs: ' + (& $adb -s $DeviceSerial shell getprop ro.product.cpu.abilist)),
    ('Page size: ' + (& $adb -s $DeviceSerial shell getconf PAGE_SIZE)),
    'Install result (no uninstall, downgrade, test-only override, or permission grants):'
)
# Keep data when updating an existing installation. Android still enforces signatures.
$previousErrorAction = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$installOutput = @(& $adb -s $DeviceSerial install --no-streaming -r $apk 2>&1)
$installExitCode = $LASTEXITCODE
$ErrorActionPreference = $previousErrorAction
$report += $installOutput | ForEach-Object { $_.ToString() }
$report | Set-Content -LiteralPath $reportPath -Encoding UTF8
$report | Write-Output
if ($installExitCode -ne 0) {
    $failure = $installOutput -join "`n"
    if ($failure -match 'UPDATE_INCOMPATIBLE|CONFLICTING_PROVIDER') {
        Write-Output 'Android reports a conflict with an existing app or signing key. The separate phone variant avoids the original package ID; no existing app has been removed.'
    } elseif ($failure -match 'INSUFFICIENT_STORAGE') {
        Write-Output 'Android reports insufficient storage. Free some internal storage, then retry.'
    } elseif ($failure -match 'USER_RESTRICTED|VERIFICATION_FAILURE') {
        Write-Output 'Android reports an installation restriction. Read the phone installer prompt or device policy details.'
    }
    throw "Installation failed. The exact Android error is saved in $reportPath"
}
$launchOutput = @(& $adb -s $DeviceSerial shell am start -W -n "$packageId/com.example.MainActivity" 2>&1)
$launchExitCode = $LASTEXITCODE
$launchOutput | Add-Content -LiteralPath $reportPath -Encoding UTF8
$launchOutput | Write-Output
if ($launchExitCode -ne 0 -or ($launchOutput -join "`n") -match '(?m)^Error') {
    throw "The APK installed, but Android could not start its launcher. Report: $reportPath"
}
Start-Sleep -Seconds 2
$appProcess = (& $adb -s $DeviceSerial shell pidof $packageId) -join ' '
('App process after launch: ' + $appProcess) | Add-Content -LiteralPath $reportPath -Encoding UTF8
if (!$appProcess.Trim()) { throw "The APK installed, but its process did not stay running. Report: $reportPath" }
Write-Output "Installed and running (process $appProcess). Report: $reportPath"
