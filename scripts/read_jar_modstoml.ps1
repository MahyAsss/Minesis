Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = 'build\libs\mimesis-1.0.0.jar'
if (-not (Test-Path $jar)) { Write-Error "JAR not found: $jar"; exit 2 }
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
try {
    $entry = $zip.GetEntry('META-INF/mods.toml')
    if ($entry -eq $null) { Write-Error 'mods.toml not found in JAR'; exit 3 }
    $sr = New-Object System.IO.StreamReader($entry.Open())
    $content = $sr.ReadToEnd()
    $sr.Dispose()
    Write-Output $content
} finally {
    $zip.Dispose()
}
