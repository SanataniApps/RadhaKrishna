import os
import cv2  # ⭐ NAYA: Video frame nikalne ke liye
from PIL import Image

MAX_WIDTH = 400
QUALITY = 70

def process_and_save_image(img, thumb_path):
    # ⭐ FIX: Agar photo me transparency (RGBA) hai, to use normal RGB me badal do taki error na aaye
    if img.mode in ("RGBA", "P"):
        img = img.convert("RGB")
        
    ratio = MAX_WIDTH / float(img.size[0])
    max_height = int((float(img.size[1]) * float(ratio)))
    img_resized = img.resize((MAX_WIDTH, max_height), Image.Resampling.LANCZOS)
    
    # Hamesha JPEG me save karenge
    img_resized.save(thumb_path, "JPEG", optimize=True, quality=QUALITY)

def generate_thumbnails(directory):
    for root, dirs, files in os.walk(directory):
        if '.git' in root:
            continue
            
        for file in files:
            if file.startswith('thumb_'):
                continue
                
            original_path = os.path.join(root, file)
            # Extension ke bina file ka naam nikal lo (e.g., '1.mp4' -> '1')
            file_name_no_ext = os.path.splitext(file)[0]
            thumb_filename = f"thumb_{file_name_no_ext}.jpg" # Sabka thumb .jpg banega
            thumb_path = os.path.join(root, thumb_filename)
            
            # 1. IMAGES KE LIYE LOGIC
            if file.lower().endswith(('.png', '.jpg', '.jpeg', '.webp')):
                if not os.path.exists(thumb_path):
                    try:
                        with Image.open(original_path) as img:
                            process_and_save_image(img, thumb_path)
                            print(f"✅ Image Thumb Ban Gaya: {thumb_path}")
                    except Exception as e:
                        print(f"❌ Error Image me {file}: {e}")
                        
            # 2. VIDEOS KE LIYE LOGIC (Naya)
            elif file.lower().endswith(('.mp4', '.mov', '.avi', '.mkv')):
                if not os.path.exists(thumb_path):
                    try:
                        # Video open karo
                        cap = cv2.VideoCapture(original_path)
                        # Video ke 1 second aage ka frame lo taaki black screen na aaye
                        fps = cap.get(cv2.CAP_PROP_FPS)
                        cap.set(cv2.CAP_PROP_POS_FRAMES, int(fps) if fps > 0 else 1)
                        success, frame = cap.read()
                        
                        if success:
                            # OpenCV BGR me colour deta hai, hume RGB chahiye
                            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                            img = Image.fromarray(frame_rgb)
                            process_and_save_image(img, thumb_path)
                            print(f"✅ Video Thumb Ban Gaya: {thumb_path}")
                        else:
                            print(f"❌ Video frame nahi mila: {file}")
                        cap.release()
                    except Exception as e:
                        print(f"❌ Error Video me {file}: {e}")

if __name__ == "__main__":
    generate_thumbnails(".")
