param(
    [Parameter(Mandatory = $true)]
    [string]$Recording,
    [string]$OutputDirectory = ""
)

$ErrorActionPreference = "Stop"
function Test-Executable([string]$Path) {
    return -not [string]::IsNullOrWhiteSpace($Path) -and (Test-Path -LiteralPath $Path -PathType Leaf)
}

$recordingPath = (Resolve-Path $Recording).Path
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Split-Path $recordingPath -Parent
}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

$jfr = $null
if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $jfr = Join-Path $env:JAVA_HOME "bin\jfr.exe"
}
if (-not (Test-Executable $jfr)) {
    $jfrCommand = Get-Command jfr.exe -ErrorAction SilentlyContinue
    if ($null -ne $jfrCommand) {
        $jfr = $jfrCommand.Source
    }
}
if (-not (Test-Executable $jfr)) {
    $installedJdk = Get-ChildItem "C:\Program Files\Java" -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName "bin\jfr.exe") } |
        Sort-Object Name -Descending |
        Select-Object -First 1
    if ($null -ne $installedJdk) {
        $jfr = Join-Path $installedJdk.FullName "bin\jfr.exe"
    }
}
if (-not (Test-Executable $jfr)) {
    throw "No se ha encontrado la herramienta jfr del JDK."
}
$summary = Join-Path $OutputDirectory "jfr-summary.txt"
$hotMethods = Join-Path $OutputDirectory "jfr-hot-methods.txt"
$allocation = Join-Path $OutputDirectory "jfr-allocation-by-site.txt"
& $jfr summary $recordingPath | Set-Content -Path $summary -Encoding utf8
& $jfr view hot-methods $recordingPath | Set-Content -Path $hotMethods -Encoding utf8
& $jfr view allocation-by-site $recordingPath | Set-Content -Path $allocation -Encoding utf8
Write-Output "Resumen: $summary"
Write-Output "CPU: $hotMethods"
Write-Output "Asignaciones: $allocation"
