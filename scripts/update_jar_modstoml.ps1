Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = 'build\libs\mimesis-1.0.0.jar'
$src = 'build\resources\main\META-INF\mods.toml'
if (-not (Test-Path $jar)) { Write-Error "JAR not found: $jar"; exit 2 }
if (-not (Test-Path $src)) { Write-Error "Source mods.toml not found: $src"; exit 2 }
$zip = [System.IO.Compression.ZipFile]::Open($jar, 'Update')
try {
    $entry = $zip.GetEntry('META-INF/mods.toml')
    if ($entry -ne $null) { $entry.Delete() }
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $src, 'META-INF/mods.toml')
    Write-Output 'OK'
} finally {
    $zip.Dispose()
}
