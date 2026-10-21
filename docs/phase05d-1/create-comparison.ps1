param([string]$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$review = Join-Path $RepositoryRoot 'docs/phase05d-1/visual-review'
$reference = [System.Drawing.Bitmap]::new((Join-Path $RepositoryRoot 'docs/phase04a/references/02-vault-details-editor.png'))
$actual = [System.Drawing.Bitmap]::new((Join-Path $review 'vault-dashboard-reference-sized.png'))
$output = [System.Drawing.Bitmap]::new(768, 779)
$g = [System.Drawing.Graphics]::FromImage($output)
$font = [System.Drawing.Font]::new('Segoe UI', 11)
$brush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#E7EEEC'))
try {
    $g.Clear([System.Drawing.ColorTranslator]::FromHtml('#131B19'))
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawString('Original Screen 04 (reference)', $font, $brush, 16, 12)
    $g.DrawString('05D-1 native Android / Compose', $font, $brush, 392, 12)
    # App content only, excluding mock bezel/status/gesture areas. Width-only normalization.
    $g.DrawImage($reference, [System.Drawing.Rectangle]::new(16, 40, 360, 723),
        [System.Drawing.Rectangle]::new(16, 88, 356, 715), [System.Drawing.GraphicsUnit]::Pixel)
    $g.DrawImageUnscaled($actual, 392, 40)
    $output.Save((Join-Path $review 'vault-dashboard-reference-comparison.png'), [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $g.Dispose(); $font.Dispose(); $brush.Dispose()
    $reference.Dispose(); $actual.Dispose(); $output.Dispose()
}
