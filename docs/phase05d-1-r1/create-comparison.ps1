param([string]$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$review = Join-Path $PSScriptRoot 'visual-review'
New-Item -ItemType Directory -Path $review -Force | Out-Null
$previous = Join-Path $RepositoryRoot 'docs/phase05d-1/visual-review'
$rendered = Join-Path $RepositoryRoot 'app/build/reports/vault-dashboard'
$copies = @{
    'before-dark-en.png' = Join-Path $previous 'vault-dashboard-dark-en.png'
    'after-dark-en.png' = Join-Path $rendered 'vault-dashboard-dark-en.png'
    'after-dark-fa.png' = Join-Path $rendered 'vault-dashboard-dark-fa.png'
    'after-light-en.png' = Join-Path $rendered 'vault-dashboard-light-en.png'
    'after-empty.png' = Join-Path $rendered 'vault-dashboard-empty.png'
    'after-en-large.png' = Join-Path $rendered 'vault-dashboard-en-large.png'
    'after-fa-large.png' = Join-Path $rendered 'vault-dashboard-fa-large.png'
    'after-landscape-scrolled.png' = Join-Path $rendered 'vault-dashboard-landscape-scrolled.png'
    'after-wide.png' = Join-Path $rendered 'vault-dashboard-wide.png'
}
foreach ($name in $copies.Keys) {
    Copy-Item -LiteralPath $copies[$name] -Destination (Join-Path $review $name)
    if ((Get-FileHash -LiteralPath $copies[$name]).Hash -ne (Get-FileHash -LiteralPath (Join-Path $review $name)).Hash) {
        throw "Screenshot copy mismatch: $name"
    }
}

$before = [System.Drawing.Bitmap]::new((Join-Path $review 'before-dark-en.png'))
$after = [System.Drawing.Bitmap]::new((Join-Path $review 'after-dark-en.png'))
$output = [System.Drawing.Bitmap]::new(760, 834)
$graphics = [System.Drawing.Graphics]::FromImage($output)
$font = [System.Drawing.Font]::new('Segoe UI', 11)
$brush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#E7EEEC'))
try {
    if ($before.Width -ne 360 -or $before.Height -ne 778 -or $after.Width -ne 360 -or $after.Height -ne 778) {
        throw 'Expected two 360x778 native renders.'
    }
    $graphics.Clear([System.Drawing.ColorTranslator]::FromHtml('#131B19'))
    $graphics.DrawString('Before - Phase 05D-1', $font, $brush, 16, 12)
    $graphics.DrawString('After - Phase 05D-1-R1', $font, $brush, 384, 12)
    $graphics.DrawImageUnscaled($before, 16, 40)
    $graphics.DrawImageUnscaled($after, 384, 40)
    $output.Save((Join-Path $review 'before-after-dark-en.png'), [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $graphics.Dispose(); $font.Dispose(); $brush.Dispose()
    $before.Dispose(); $after.Dispose(); $output.Dispose()
}
