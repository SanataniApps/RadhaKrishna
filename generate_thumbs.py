import os
from PIL import Image

# Resize configuration
MAX_WIDTH = 400
QUALITY = 70

def generate_thumbnails(directory):
    # Repo ke har folder mein check karega
    for root, dirs, files in os.walk(directory):
        # .git folder ya hidden folders ko ignore karega
        if '.git' in root:
            continue
            
        for file in files:
            # Sirf images ko pakdega
            if file.lower().endswith(('.png', '.jpg', '.jpeg', '.webp')):
                # Agar image pehle se thumbnail hai, toh chhod dega
                if file.startswith('thumb_'):
                    continue
                
                original_path = os.path.join(root, file)
                thumb_filename = f"thumb_{file}"
                thumb_path = os.path.join(root, thumb_filename)
                
                # Agar is image ka thumbnail pehle se nahi bana hai, tabhi banayega
                if not os.path.exists(thumb_path):
                    try:
                        with Image.open(original_path) as img:
                            # Aspect ratio maintain karte hue width 400px karega
                            ratio = MAX_WIDTH / float(img.size[0])
                            max_height = int((float(img.size[1]) * float(ratio)))
                            img_resized = img.resize((MAX_WIDTH, max_height), Image.Resampling.LANCZOS)
                            
                            # Thumbnail save karega
                            img_resized.save(thumb_path, optimize=True, quality=QUALITY)
                            print(f"✅ Thumbnail Generated: {thumb_path}")
                    except Exception as e:
                        print(f"❌ Error in {file}: {e}")

if __name__ == "__main__":
    # Current folder (repo root) se shuru karega
    generate_thumbnails(".")
