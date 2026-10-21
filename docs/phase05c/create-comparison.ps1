param([string]$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$review = Join-Path $RepositoryRoot 'docs/phase05c/visual-review'
$reference = [System.Drawing.Bitmap]::new((Join-Path $RepositoryRoot 'docs/phase04a/references/01-launch-unlock-home-tasks.png'))
$actual = [System.Drawing.Bitmap]::new((Join-Path $review 'timeline-reference-sized.png'))
$output = [System.Drawing.Bitmap]::new(768, 779)
$g = [System.Drawing.Graphics]::FromImage($output)
$font = [System.Drawing.Font]::new('Segoe UI', 11)
$brush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#E7EEEC'))
try {
    $g.Clear([System.Drawing.ColorTranslator]::FromHtml('#131B19'))
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawString('Reference Screen 03 (mock)', $font, $brush, 16, 12)
    $g.DrawString('Actual Android / Compose render', $font, $brush, 392, 12)
    # Crop app-owned content, excluding the mock phone frame/status and gesture areas.
    # Normalize width only. 356x715 -> 360x723, also the actual capture viewport.
    $g.DrawImage($reference, [System.Drawing.Rectangle]::new(16, 40, 360, 723),
        [System.Drawing.Rectangle]::new(916, 88, 356, 715), [System.Drawing.GraphicsUnit]::Pixel)
    $g.DrawImageUnscaled($actual, 392, 40)
    $output.Save((Join-Path $review 'timeline-reference-comparison.png'), [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $g.Dispose(); $font.Dispose(); $brush.Dispose()
    $reference.Dispose(); $actual.Dispose(); $output.Dispose()
}
