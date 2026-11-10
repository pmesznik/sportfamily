# Kopiuje cały folder "Kalendarz Rodzinny" na Dysk Google, bez plików tymczasowych kompilacji.
# Uruchamiaj po każdej sesji pracy (Claude robi to automatycznie po zmianach).
# Kopia jest lustrem: pliki usunięte tutaj znikają też na Dysku.

$src = "D:\Kalendarz Rodzinny"
$dst = "G:\Mój dysk\PROJEKTY AI\Kalendarz Rodzinny"

if (-not (Test-Path "G:\Mój dysk")) { Write-Error "Dysk Google (G:) nie jest podłączony."; exit 1 }

robocopy $src $dst /MIR /R:2 /W:2 /NFL /NDL /NP /NJH `
    /XD build .gradle .kotlin .idea captures `
    /XF local.properties *.iml

# robocopy: kody 0-7 oznaczają sukces
if ($LASTEXITCODE -ge 8) { Write-Error "Kopia nie powiodła się (kod $LASTEXITCODE)"; exit 1 }
Write-Output "Kopia gotowa: $dst"
exit 0
