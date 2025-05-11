from PIL import Image
import os

def create_mipmap_folders(base_path):
    densities = ['mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi']
    for density in densities:
        folder_path = os.path.join(base_path, f'mipmap-{density}')
        if not os.path.exists(folder_path):
            os.makedirs(folder_path)
    
    # Create v26 folder for adaptive icons
    v26_folder = os.path.join(base_path, 'mipmap-anydpi-v26')
    if not os.path.exists(v26_folder):
        os.makedirs(v26_folder)

def resize_image(image_path, output_path, size):
    with Image.open(image_path) as img:
        # Convert to RGBA if not already
        img = img.convert('RGBA')
        
        # Create a transparent background
        background = Image.new('RGBA', img.size, (0, 0, 0, 0))
        
        # Paste the image, keeping transparency
        background.paste(img, (0, 0), img)
        
        # Resize maintaining aspect ratio, but scale down more to add padding
        img_w, img_h = background.size
        # Use 70% of the available space (down from 100%)
        target_size = (int(size[0] * 0.7), int(size[1] * 0.7))
        ratio = min(target_size[0]/img_w, target_size[1]/img_h)
        new_size = (int(img_w*ratio), int(img_h*ratio))
        
        resized_img = background.resize(new_size, Image.Resampling.LANCZOS)
        
        # Create new image with padding
        new_img = Image.new('RGBA', size, (0, 0, 0, 0))
        paste_x = (size[0] - new_size[0]) // 2
        paste_y = (size[1] - new_size[1]) // 2
        new_img.paste(resized_img, (paste_x, paste_y), resized_img)
        
        new_img.save(output_path, 'PNG')

def create_adaptive_icon_xml(base_path):
    # Create ic_launcher.xml
    launcher_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>"""
    
    # Create ic_launcher_round.xml
    round_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>"""
    
    v26_path = os.path.join(base_path, 'mipmap-anydpi-v26')
    
    with open(os.path.join(v26_path, 'ic_launcher.xml'), 'w') as f:
        f.write(launcher_xml)
    
    with open(os.path.join(v26_path, 'ic_launcher_round.xml'), 'w') as f:
        f.write(round_xml)

def create_colors_xml(base_path):
    colors_xml = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#FFFFFF</color>
</resources>"""
    
    values_path = os.path.join(base_path, 'values')
    if not os.path.exists(values_path):
        os.makedirs(values_path)
    
    # Check if ic_launcher_background.xml exists
    background_file = os.path.join(values_path, 'ic_launcher_background.xml')
    if not os.path.exists(background_file):
        with open(background_file, 'w') as f:
            f.write(colors_xml)

def generate_launcher_icons(logo_path, base_path):
    # Icon sizes for different densities (mdpi as base)
    sizes = {
        'mdpi': (48, 48),
        'hdpi': (72, 72),
        'xhdpi': (96, 96),
        'xxhdpi': (144, 144),
        'xxxhdpi': (192, 192)
    }
    
    # Foreground sizes (larger to account for padding in adaptive icons)
    foreground_sizes = {
        'mdpi': (108, 108),
        'hdpi': (162, 162),
        'xhdpi': (216, 216),
        'xxhdpi': (324, 324),
        'xxxhdpi': (432, 432)
    }
    
    # Create icons for each density
    for density, size in sizes.items():
        # Regular icons
        output_path = os.path.join(base_path, f'mipmap-{density}', 'ic_launcher.png')
        resize_image(logo_path, output_path, size)
        
        # Round icons
        round_output_path = os.path.join(base_path, f'mipmap-{density}', 'ic_launcher_round.png')
        resize_image(logo_path, round_output_path, size)
        
        # Foreground icons for adaptive icon
        foreground_size = foreground_sizes[density]
        foreground_path = os.path.join(base_path, f'mipmap-{density}', 'ic_launcher_foreground.png')
        resize_image(logo_path, foreground_path, foreground_size)

if __name__ == '__main__':
    logo_path = 'Bundl Logo Black.png'
    base_path = 'app/src/main/res'
    
    # Create necessary folders
    create_mipmap_folders(base_path)
    
    # Generate icons
    generate_launcher_icons(logo_path, base_path)
    
    # Create adaptive icon XML files
    create_adaptive_icon_xml(base_path)
    
    # Create colors XML file
    create_colors_xml(base_path) 