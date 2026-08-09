param(
    [string]$OutputDirectory = "",
    [ValidateRange(1, 20)]
    [int]$Runs = 3
)

$ErrorActionPreference = "Stop"
function Test-Executable([string]$Path) {
    return -not [string]::IsNullOrWhiteSpace($Path) -and (Test-Path -LiteralPath $Path -PathType Leaf)
}

function Get-Median([double[]]$Values) {
    $orderedValues = @($Values | Sort-Object)
    $middle = [math]::Floor($orderedValues.Count / 2)
    if ($orderedValues.Count % 2 -eq 1) {
        return $orderedValues[$middle]
    }
    return ($orderedValues[$middle - 1] + $orderedValues[$middle]) / 2.0
}

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $root "build\performance\output-inserter"
}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

$javac = $null
$java = $null
if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $javac = Join-Path $env:JAVA_HOME "bin\javac.exe"
    $java = Join-Path $env:JAVA_HOME "bin\java.exe"
}
if (-not (Test-Executable $javac) -or -not (Test-Executable $java)) {
    $javacCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($null -ne $javacCommand -and $null -ne $javaCommand) {
        $javac = $javacCommand.Source
        $java = $javaCommand.Source
    }
}
if (-not (Test-Executable $javac) -or -not (Test-Executable $java)) {
    $installedJdk = Get-ChildItem "C:\Program Files\Java" -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName "bin\javac.exe") } |
        Sort-Object Name -Descending |
        Select-Object -First 1
    if ($null -ne $installedJdk) {
        $javac = Join-Path $installedJdk.FullName "bin\javac.exe"
        $java = Join-Path $installedJdk.FullName "bin\java.exe"
    }
}
if (-not (Test-Executable $javac) -or -not (Test-Executable $java)) {
    throw "No se ha encontrado un JDK. Define JAVA_HOME antes de ejecutar la prueba."
}
& $javac -d $OutputDirectory (Join-Path $PSScriptRoot "OutputInserterEquivalence.java")
if ($LASTEXITCODE -ne 0) {
    throw "No se pudo compilar el verificador del insertador."
}

$measurements = @()
for ($run = 1; $run -le $Runs; $run++) {
    $lines = & $java -cp $OutputDirectory OutputInserterEquivalence
    if ($LASTEXITCODE -ne 0) {
        throw "La equivalencia aleatoria del insertador ha fallado en la ejecución $run."
    }
    $measurement = $lines[-1] | ConvertFrom-Csv -Header cases,legacy_ms,optimized_ms,improvement_percent
    $measurements += [pscustomobject]@{
        run = $run
        cases = [int]$measurement.cases
        legacy_ms = [double]::Parse($measurement.legacy_ms, [Globalization.CultureInfo]::InvariantCulture)
        optimized_ms = [double]::Parse($measurement.optimized_ms, [Globalization.CultureInfo]::InvariantCulture)
        improvement_percent = [double]::Parse(
            $measurement.improvement_percent,
            [Globalization.CultureInfo]::InvariantCulture
        )
    }
}

$resultsFile = Join-Path $OutputDirectory "runs.csv"
$measurements | Export-Csv -Path $resultsFile -NoTypeInformation
$medianLegacy = Get-Median @($measurements | ForEach-Object { $_.legacy_ms })
$medianOptimized = Get-Median @($measurements | ForEach-Object { $_.optimized_ms })
$medianImprovement = 100.0 * ($medianLegacy - $medianOptimized) / $medianLegacy
$invariant = [Globalization.CultureInfo]::InvariantCulture
Write-Output "cases,legacy_ms,optimized_ms,improvement_percent"
Write-Output ([string]::Join(',', @(
    $measurements[0].cases.ToString(),
    $medianLegacy.ToString('F3', $invariant),
    $medianOptimized.ToString('F3', $invariant),
    $medianImprovement.ToString('F2', $invariant)
)))
Write-Output "Resultados: $resultsFile"
