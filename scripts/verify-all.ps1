# Builds and tests every step folder and every final folder, course by course.
#
# Usage (from anywhere):
#   .\scripts\verify-all.ps1           build and test everything
#   .\scripts\verify-all.ps1 -Clean    also delete build\ and .gradle\ after each project
#
# Needs JDK 25 (JAVA_HOME pointing at it, or java 25 on the PATH) and a running Docker,
# because the tests start PostgreSQL and Redis with Testcontainers.

param(
    [switch]$Clean
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$passed = 0
$failed = @()

foreach ($course in @('fundamentals', 'production-apis', 'advanced')) {
    $coursePath = Join-Path $repoRoot $course
    if (-not (Test-Path $coursePath)) { continue }

    $projects = @()
    $stepsPath = Join-Path $coursePath 'steps'
    if (Test-Path $stepsPath) {
        $projects += Get-ChildItem -Path $stepsPath -Directory | Sort-Object Name | ForEach-Object { $_.FullName }
    }
    $projects += Join-Path $coursePath 'final'

    foreach ($dir in $projects) {
        if (-not (Test-Path (Join-Path $dir 'gradlew.bat'))) { continue }
        Write-Host "==> $dir"
        Push-Location $dir
        try {
            & .\gradlew.bat build --console=plain -q
            $ok = ($LASTEXITCODE -eq 0)
        } finally {
            Pop-Location
        }
        if ($ok) {
            Write-Host '    PASS'
            $passed++
        } else {
            Write-Host '    FAIL'
            $failed += $dir
        }
        if ($Clean) {
            foreach ($folder in @('build', '.gradle')) {
                $target = Join-Path $dir $folder
                if (Test-Path $target) { Remove-Item -Recurse -Force $target }
            }
        }
    }
}

Write-Host ''
Write-Host "Passed: $passed  Failed: $($failed.Count)"
if ($failed.Count -gt 0) {
    Write-Host 'Failed projects:'
    $failed | ForEach-Object { Write-Host "  $_" }
    exit 1
}
