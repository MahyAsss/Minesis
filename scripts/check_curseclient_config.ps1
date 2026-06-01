$config = Get-Content 'C:\Users\MahyAss\curseforge\minecraft\Instances\LaFriteuse Horror\minecraftinstance.json' -Raw | ConvertFrom-Json

Write-Output '=== Total mods installed ==='
Write-Output $config.installedAddons.Count

Write-Output "`n=== Looking for Mimesis ==="
$mimesis = $config.installedAddons | Where-Object { $_.installedFile.displayName -like '*mimesis*' -or $_.installedFile.fileName -like '*mimesis*' }

if ($mimesis) {
    Write-Output 'Found Mimesis mod in installedAddons:'
    Write-Output ""
    Write-Output "Display Name: $($mimesis.installedFile.displayName)"
    Write-Output "File Name: $($mimesis.installedFile.fileName)"
    Write-Output "Addon ID: $($mimesis.addonID)"
    Write-Output "File ID: $($mimesis.installedFile.id)"
    Write-Output "`nFull entry:"
    $mimesis | ConvertTo-Json
} else {
    Write-Output 'Mimesis NOT found in installedAddons'
    Write-Output "`nFirst 10 installed mods:"
    $config.installedAddons | Select-Object -First 10 -ExpandProperty installedFile | Select-Object displayName, fileName
}
