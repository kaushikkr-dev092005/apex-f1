param([switch]$RunTests, [switch]$RecordScreenshots, [switch]$PhoneApk)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path -Parent $PSScriptRoot
$jdkPath = Join-Path $workspace '.tools/java/jdk17.0.21_0'
$gradlePath = Join-Path $workspace '.tools/gradle/gradle-9.3.1/bin/gradle.bat'
if (!(Test-Path -LiteralPath $jdkPath) -or !(Test-Path -LiteralPath $gradlePath)) {
    throw 'Project-local tools are unavailable. Install JDK 17 and the Android SDK, then run gradlew.bat :app:assembleDebug.'
}
$env:JAVA_HOME = $jdkPath
$env:ANDROID_HOME = Join-Path $workspace '.tools/sdk'
$env:GRADLE_USER_HOME = Join-Path $workspace '.tools/gradle-cache'
Push-Location $workspace
try {
    $buildTask = if ($PhoneApk) { ':app:assemblePhone' } else { ':app:assembleDebug' }
    $buildArguments = @($buildTask, '--console=plain')
    & $gradlePath @buildArguments
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
    if ($PhoneApk) {
        $artifactDirectory = Join-Path $workspace 'artifacts'
        New-Item -ItemType Directory -Force -Path $artifactDirectory | Out-Null
        $sourceApk = Join-Path $workspace 'app/build/outputs/apk/phone/app-phone.apk'
        $phoneApkPath = Join-Path $artifactDirectory 'APEX-phone.apk'
        Copy-Item -LiteralPath $sourceApk -Destination $phoneApkPath -Force
        ((Get-FileHash -LiteralPath $phoneApkPath -Algorithm SHA256).Hash.ToLowerInvariant() + '  APEX-phone.apk') | Set-Content -LiteralPath ($phoneApkPath + '.sha256') -Encoding ASCII
        $apkMetadata = Get-Content -LiteralPath (Join-Path $workspace 'app/build/outputs/apk/phone/output-metadata.json') -Raw | ConvertFrom-Json
        $artifactVersion = $apkMetadata.elements[0].versionName.Split('-')[0]
        if ($artifactVersion -notmatch '^\d+\.\d+\.\d+$') { throw 'Unexpected APK version in build metadata' }
        $versionedName = 'APEX-' + $artifactVersion + '.apk'
        $versionedPath = Join-Path $artifactDirectory $versionedName
        Copy-Item -LiteralPath $phoneApkPath -Destination $versionedPath -Force
        ((Get-FileHash -LiteralPath $versionedPath -Algorithm SHA256).Hash.ToLowerInvariant() + '  ' + $versionedName) | Set-Content -LiteralPath ($versionedPath + '.sha256') -Encoding ASCII
        Write-Output "Phone APK: $phoneApkPath"
        Write-Output "Versioned APK: $versionedPath"
    }
    if ($RunTests -or $RecordScreenshots) {
        # Fresh compiler process: do not retain both phone and test compiler classes.
        $testArguments = @(':app:testDebugUnitTest', '--console=plain')
        if ($RecordScreenshots) { $testArguments += '-Proborazzi.test.record=true' }
        & $gradlePath @testArguments
        if ($LASTEXITCODE -ne 0) { throw "Tests failed with exit code $LASTEXITCODE" }
    }
} finally { Pop-Location }
