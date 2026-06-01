param(
    [string]$Jar = 'build\libs\mimesis-1.0.0.jar',
    [string]$Source,
    [string]$Entry
)
Add-Type -AssemblyName System.IO.Compression.FileSystem
if (-not (Test-Path $Jar)) { Write-Error "JAR not found: $Jar"; exit 2 }
if (-not (Test-Path $Source)) { Write-Error "Source file not found: $Source"; exit 3 }
$zip = [System.IO.Compression.ZipFile]::Open($Jar, 'Update')
try {
    $e = $zip.GetEntry($Entry)
    if ($e -ne $null) { $e.Delete() }
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $Source, $Entry)
    Write-Output 'OK'
} finally {
    $zip.Dispose()
}
