param(
    [string]$OutputDirectory = "",
    [ValidateRange(3, 20)]
    [int]$Runs = 5,
    [double]$MinimumImprovement = 10.0
)

$ErrorActionPreference = "Stop"

function Test-Executable([string]$Path) {
    return -not [string]::IsNullOrWhiteSpace($Path) -and (Test-Path -LiteralPath $Path -PathType Leaf)
}

function Get-Median([double[]]$Values) {
    $ordered = @($Values | Sort-Object)
    $middle = [math]::Floor($ordered.Count / 2)
    if ($ordered.Count % 2 -eq 1) {
        return $ordered[$middle]
    }
    return ($ordered[$middle - 1] + $ordered[$middle]) / 2.0
}

function Find-JdkTool([string]$Name) {
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $candidate = Join-Path $env:JAVA_HOME "bin\$Name.exe"
        if (Test-Executable $candidate) {
            return $candidate
        }
    }
    $command = Get-Command "$Name.exe" -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }
    $candidate = Get-ChildItem "$env:USERPROFILE\.gradle\jdks" -Recurse -Filter "$Name.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending |
        Select-Object -First 1
    if ($null -ne $candidate) {
        return $candidate.FullName
    }
    throw "A JDK with $Name.exe is required."
}

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $root "build\performance\autotrader-readiness"
}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

$javac = Find-JdkTool "javac"
$java = Find-JdkTool "java"
& $javac -d $OutputDirectory (Join-Path $PSScriptRoot "AutotraderReadinessEquivalence.java")
if ($LASTEXITCODE -ne 0) {
    throw "Could not compile the Autotrader readiness verifier."
}

$measurements = @()
for ($run = 1; $run -le $Runs; $run++) {
    $lines = & $java -cp $OutputDirectory AutotraderReadinessEquivalence
    if ($LASTEXITCODE -ne 0) {
        throw "Autotrader readiness equivalence failed in run $run."
    }
    $measurement = $lines[-1] | ConvertFrom-Csv -Header cases,legacy_ms,optimized_ms,improvement_percent
    $measurements += [pscustomobject]@{
        run = $run
        cases = [int]$measurement.cases
        legacy_ms = [double]::Parse($measurement.legacy_ms, [Globalization.CultureInfo]::InvariantCulture)
        optimized_ms = [double]::Parse($measurement.optimized_ms, [Globalization.CultureInfo]::InvariantCulture)
    }
}

$resultsFile = Join-Path $OutputDirectory "runs.csv"
$measurements | Export-Csv -Path $resultsFile -NoTypeInformation
$legacy = Get-Median @($measurements | ForEach-Object { $_.legacy_ms })
$optimized = Get-Median @($measurements | ForEach-Object { $_.optimized_ms })
$improvement = 100.0 * ($legacy - $optimized) / $legacy
$invariant = [Globalization.CultureInfo]::InvariantCulture
Write-Output "cases,legacy_ms,optimized_ms,improvement_percent"
Write-Output ([string]::Join(',', @(
    $measurements[0].cases.ToString(),
    $legacy.ToString('F3', $invariant),
    $optimized.ToString('F3', $invariant),
    $improvement.ToString('F2', $invariant)
)))
Write-Output "Results: $resultsFile"
if ($improvement -lt $MinimumImprovement) {
    throw "Measured improvement $($improvement.ToString('F2', $invariant))% is below $MinimumImprovement%."
}
