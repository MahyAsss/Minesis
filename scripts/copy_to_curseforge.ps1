param(
    [string]$Src = 'build\\libs\\mimesis-1.0.0.jar',
    [string]$Dst = 'C:\\Users\\MahyAss\\curseforge\\minecraft\\Instances\\LaFriteuse Horror\\mods\\mimesis-1.0.0.jar'
)

if (-not (Test-Path $Src)) { Write-Error "Source JAR not found: $Src"; exit 2 }
$dstDir = Split-Path $Dst -Parent
if (-not (Test-Path $dstDir)) {
    try { New-Item -Path $dstDir -ItemType Directory -Force | Out-Null } catch { Write-Error "Failed to create destination dir: $dstDir"; exit 3 }
}
if (Test-Path $Dst) {
    $ts = Get-Date -Format yyyyMMddHHmmss
    $bak = "$Dst.$ts.bak"
    Move-Item -Path $Dst -Destination $bak -Force
    Write-Output "Backed up existing JAR to: $bak"
}
Copy-Item -Path $Src -Destination $Dst -Force
Write-Output "Copied JAR to destination: $Dst"

Write-Output '--- Source SHA1 ---'
Get-FileHash $Src -Algorithm SHA1 | Format-List
Write-Output '--- Destination SHA1 ---'
Get-FileHash $Dst -Algorithm SHA1 | Format-List
Write-Output '--- Destination file info ---'
Get-Item $Dst | Select-Object FullName, LastWriteTime, Length | Format-List

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($Dst)
try {
    $entry = $zip.GetEntry('META-INF/mods.toml')
    if ($entry -ne $null) {
        $sr = New-Object System.IO.StreamReader($entry.Open())
        Write-Output '--- mods.toml in destination JAR ---'
        Write-Output $sr.ReadToEnd()
        $sr.Dispose()
    } else { Write-Output 'mods.toml not found in JAR' }

    $entry2 = $zip.GetEntry('META-INF/MANIFEST.MF')
    if ($entry2 -ne $null) {
        $sr2 = New-Object System.IO.StreamReader($entry2.Open())
        Write-Output '--- MANIFEST.MF in destination JAR ---'
        Write-Output $sr2.ReadToEnd()
        $sr2.Dispose()
    } else { Write-Output 'MANIFEST.MF not found in JAR' }
} finally {
    $zip.Dispose()
}
