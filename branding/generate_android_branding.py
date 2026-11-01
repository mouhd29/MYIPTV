"""Package the supplied logo into Android icon, in-app logo and TV banner sizes."""
from pathlib import Path
import sys
from PIL import Image, ImageOps


def main():
    project = Path(sys.argv[1])
    source = Path(__file__).with_name("myiptv-logo-source.jpg")
    with Image.open(source) as uploaded:
        logo = ImageOps.exif_transpose(uploaded).convert("RGB")
    res = project / "app/src/main/res"
    for density, size in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        folder = res / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        ImageOps.fit(logo, (size, size), method=Image.Resampling.LANCZOS).save(folder / "ic_launcher.png", optimize=True)
    drawable = res / "drawable-nodpi"
    drawable.mkdir(parents=True, exist_ok=True)
    ImageOps.contain(logo, (512, 512), method=Image.Resampling.LANCZOS).save(drawable / "myiptv_logo.png", optimize=True)
    banner = Image.new("RGB", (320, 180), "#080b13")
    mark = ImageOps.contain(logo, (180, 180), method=Image.Resampling.LANCZOS)
    banner.paste(mark, ((320 - mark.width) // 2, (180 - mark.height) // 2))
    banner.save(drawable / "tv_banner.png", optimize=True)
    print("Updated launcher icons, in-app logo and TV launcher banner")


if __name__ == "__main__":
    main()
