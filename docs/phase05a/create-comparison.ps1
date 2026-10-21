$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$referencePath = Join-Path $repo 'docs/phase04a/references/01-launch-unlock-home-tasks.png'
$renderPath = Join-Path $repo 'app/build/reports/workspace/dark-en-360.png'
$outputDirectory = Join-Path $PSScriptRoot 'visual-review'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$reference = [System.Drawing.Image]::FromFile($referencePath)
$render = [System.Drawing.Image]::FromFile($renderPath)
$comparison = [System.Drawing.Bitmap]::new(768, 834)
$graphics = [System.Drawing.Graphics]::FromImage($comparison)
$font = [System.Drawing.Font]::new('Segoe UI', 10)
try {
    $graphics.Clear([System.Drawing.Color]::FromArgb(235, 240, 237))
    $graphics.DrawString('Screen 00 reference (content crop)', $font, [System.Drawing.Brushes]::Black, 16, 10)
    $graphics.DrawString('Android Compose render (360 dp)', $font, [System.Drawing.Brushes]::Black, 392, 10)
    # Crop only the supplied rightmost phone, below its status bar. Keep source aspect ratio.
    $source = [System.Drawing.Rectangle]::new(1355, 88, 357, 711)
    $destination = [System.Drawing.Rectangle]::new(16, 40, 360, 717)
    $graphics.DrawImage($reference, $destination, $source, [System.Drawing.GraphicsUnit]::Pixel)
    $graphics.DrawImage($render, 392, 40, 360, 778)
    $comparison.Save((Join-Path $outputDirectory 'screen00-comparison.png'), [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $font.Dispose()
    $graphics.Dispose()
    $comparison.Dispose()
    $render.Dispose()
    $reference.Dispose()
}
