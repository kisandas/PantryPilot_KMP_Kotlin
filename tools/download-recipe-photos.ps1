$ErrorActionPreference = 'Stop'
$photos = @(
    @{Name='recipe_01'; Id=89238; Author='Ela Haney'; Page='https://www.pexels.com/photo/fried-eggs-with-tomatoes-89238/'},
    @{Name='recipe_02'; Id=9218745; Author='Loren Castillo'; Page='https://www.pexels.com/photo/food-on-plate-9218745/'},
    @{Name='recipe_03'; Id=19805083; Author='mustafa türkeri'; Page='https://www.pexels.com/photo/pasta-with-mushrooms-on-a-black-plate-a-bowl-of-salad-and-a-dipping-sauce-19805083/'},
    @{Name='recipe_04'; Id=23384627; Author='Adrianna CA'; Page='https://www.pexels.com/photo/cooked-chickpeas-on-plate-23384627/'},
    @{Name='recipe_05'; Id=36293909; Author='Sóc Năng Động'; Page='https://www.pexels.com/photo/delicious-plate-of-vegetarian-fried-rice-36293909/'},
    @{Name='recipe_06'; Id=25402351; Author='Oğuzhan Karataş'; Page='https://www.pexels.com/photo/breakfast-on-plate-25402351/'},
    @{Name='recipe_07'; Id=4725732; Author='Polina Tankilevitch'; Page='https://www.pexels.com/photo/black-ceramic-bowl-with-food-4725732/'},
    @{Name='recipe_08'; Id=8299597; Author='Razane Adra'; Page='https://www.pexels.com/photo/cooked-food-on-brown-ceramic-bowl-8299597/'},
    @{Name='recipe_09'; Id=18097893; Author='Customers 1st'; Page='https://www.pexels.com/photo/close-up-of-a-bowl-of-a-creamy-tomato-soup-18097893/'},
    @{Name='recipe_10'; Id=12737804; Author='dhiraj jain'; Page='https://www.pexels.com/photo/noodles-on-ceramic-bowl-12737804/'},
    @{Name='recipe_11'; Id=7994282; Author='Ecrin'; Page='https://www.pexels.com/photo/a-dessert-in-a-glass-7994282/'},
    @{Name='recipe_12'; Id=6120503; Author='Alesia Kozik'; Page='https://www.pexels.com/photo/cooked-food-on-ceramic-bowl-6120503/'}
)
$projectRoot = Split-Path -Parent $PSScriptRoot
$destination = Join-Path $projectRoot 'shared/src/commonMain/composeResources/drawable'
New-Item -ItemType Directory -Force $destination | Out-Null
foreach ($photo in $photos) {
    $extension = if ($photo.Id -eq 89238) { "png" } else { "jpeg" }
    $url = "https://images.pexels.com/photos/$($photo.Id)/pexels-photo-$($photo.Id).$($extension)?auto=compress&cs=tinysrgb&w=900&h=600&fit=crop&fm=jpg"
    $target = Join-Path $destination "$($photo.Name).jpg"
    Invoke-WebRequest -Uri $url -OutFile $target
    if ((Get-Item -LiteralPath $target).Length -lt 2000) { throw "Invalid image download: $($photo.Name)" }
    Write-Output "$($photo.Name): $((Get-Item -LiteralPath $target).Length) bytes"
}
$photos | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $projectRoot 'document/PHOTO_CREDITS.json')



