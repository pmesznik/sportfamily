# Buduje podpisaną wersję, publikuje ją na GitHub Releases i robi kopię na Dysk Google.
# Użycie: .\scripts\wydaj.ps1 -Notes "Co nowego"
# Przed wydaniem zwiększ versionCode (o 1) i versionName w app\build.gradle.kts.
# Telefony z zainstalowaną aplikacją zobaczą pasek "Nowa wersja" przy następnym uruchomieniu.
param([string]$Notes = "", [switch]$NoPublish)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

$props = @{}
Get-Content "update.properties" | Where-Object { $_ -match "^\s*[^#].*=" } | ForEach-Object { $k, $v = $_ -split "=", 2; $props[$k.Trim()] = $v.Trim() }
$repo = $props["githubRepo"]

& .\gradlew.bat assembleRelease --console=plain -q
if ($LASTEXITCODE -ne 0) { throw "Budowanie nie powiodło się" }

$meta = Get-Content "app\build\outputs\apk\release\output-metadata.json" | ConvertFrom-Json
$code = $meta.elements[0].versionCode
$name = $meta.elements[0].versionName
$tag = "v$name"

$out = Join-Path $root "wydania"
New-Item -ItemType Directory -Force $out | Out-Null
$apk = Join-Path $out "SportFamily.apk"
Copy-Item "app\build\outputs\apk\release\app-release.apk" $apk -Force
Copy-Item $apk (Join-Path $out "SportFamily-$name.apk") -Force

# APK wskazujemy przez konkretny tag, żeby update.json i plik zawsze do siebie pasowały.
$manifest = [ordered]@{
    versionCode = $code
    versionName = $name
    apkUrl      = "https://github.com/$repo/releases/download/$tag/SportFamily.apk"
    notes       = $Notes
}
$json = Join-Path $out "update.json"
# Bez BOM: parser JSON w aplikacji nie lubi znacznika BOM.
[System.IO.File]::WriteAllText($json, ($manifest | ConvertTo-Json), (New-Object System.Text.UTF8Encoding $false))

Write-Output "Wersja $name ($code) zbudowana."

if (-not $NoPublish) {
    $exists = $true
    try { gh release view $tag --repo $repo *> $null; if ($LASTEXITCODE -ne 0) { $exists = $false } } catch { $exists = $false }
    if ($exists) { throw "Wydanie $tag już istnieje. Zwiększ versionName i versionCode." }
    $notesText = if ($Notes) { $Notes } else { "Wersja $name" }
    gh release create $tag $apk $json --repo $repo --title "SportFamily Kalendarz $name" --notes $notesText
    if ($LASTEXITCODE -ne 0) { throw "Publikacja na GitHub nie powiodła się" }
    Write-Output "Opublikowano: https://github.com/$repo/releases/tag/$tag"
}

& (Join-Path $PSScriptRoot "kopia-na-dysk.ps1")
