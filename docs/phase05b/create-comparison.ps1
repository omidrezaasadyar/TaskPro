$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$reference = [System.Drawing.Image]::FromFile((Join-Path $repo 'docs/phase04a/references/01-launch-unlock-home-tasks.png'))
$render = [System.Drawing.Image]::FromFile((Join-Path $repo 'app/build/reports/dashboard/home-dark-en.png'))
$directory = Join-Path $PSScriptRoot 'visual-review'
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$image = [System.Drawing.Bitmap]::new(768, 834)
$graphics = [System.Drawing.Graphics]::FromImage($image)
$font = [System.Drawing.Font]::new('Segoe UI', 10)
try {
    $graphics.Clear([System.Drawing.Color]::FromArgb(235, 240, 237))
    $graphics.DrawString('Screen 02 reference (content crop)', $font, [System.Drawing.Brushes]::Black, 16, 10)
    $graphics.DrawString('Android Compose render (360 dp)', $font, [System.Drawing.Brushes]::Black, 392, 10)
    $graphics.DrawImage($reference, [System.Drawing.Rectangle]::new(16, 40, 360, 714),
        [System.Drawing.Rectangle]::new(475, 88, 358, 710), [System.Drawing.GraphicsUnit]::Pixel)
    $graphics.DrawImage($render, 392, 40, 360, 778)
    $image.Save((Join-Path $directory 'home-reference-comparison.png'), [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $font.Dispose(); $graphics.Dispose(); $image.Dispose(); $render.Dispose(); $reference.Dispose()
}
