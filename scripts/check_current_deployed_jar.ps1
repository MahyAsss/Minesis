Add-Type -AssemblyName System.IO.Compression.FileSystem

$jarPath = 'C:\Users\MahyAss\curseforge\minecraft\Instances\LaFriteuse Horror\mods\mimesis-1.0.0.jar'

Write-Output "=== Checking deployed JAR at: $jarPath ==="
Write-Output ""

# Get file info
$info = Get-Item $jarPath
Write-Output "Last modified: $($info.LastWriteTime)"
Write-Output "Size: $($info.Length) bytes"
Write-Output ""

# Read mods.toml
Write-Output "=== mods.toml content ==="
try {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
    $entry = $zip.GetEntry('META-INF/mods.toml')
    if ($entry) {
        $sr = New-Object System.IO.StreamReader($entry.Open())
        $content = $sr.ReadToEnd()
        $sr.Dispose()
        Write-Output $content
    } else {
        Write-Output "ERROR: mods.toml not found in JAR"
    }
    $zip.Dispose()
} catch {
    Write-Output "ERROR reading JAR: $_"
}
