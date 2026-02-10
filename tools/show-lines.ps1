param(
  [Parameter(Mandatory=$true)][string]$Path,
  [int]$Start = 1,
  [int]$End = 200
)

if (!(Test-Path $Path)) {
  Write-Error "File not found: $Path"
  exit 1
}

$c = Get-Content -LiteralPath $Path
for ($i = $Start; $i -le $End; $i++) {
  if ($i -le $c.Length) {
    "{0,4}: {1}" -f $i, $c[$i-1]
  }
}
