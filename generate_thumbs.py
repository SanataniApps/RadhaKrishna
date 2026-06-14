import os
import cv2  
from PIL import Image

MAX_WIDTH = 400
QUALITY = 70

def process_and_save_image(img, thumb_path):
    # Agar photo me transparency (RGBA) hai, to use normal RGB me badal do
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
            thumb_filename = f"thumb_{file_name_no_ext}.jpg" 
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
                        
            # 2. VIDEOS KE LIYE LOGIC
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

# NAYA LOGIC: Faaltu Thumbnails ko automatically delete karne ke liye
def cleanup_orphaned_thumbnails(directory):
    for root, dirs, files in os.walk(directory):
        if '.git' in root:
            continue
        
        # Pehle check karo ki folder mein konsi Asli (Original) files bachi hain
        original_files_no_ext = set()
        for file in files:
            if not file.startswith('thumb_'):
                name_without_ext = os.path.splitext(file)[0]
                original_files_no_ext.add(name_without_ext)
        
        # Ab check karo kya koi aisa thumbnail hai jiska asli photo delete ho chuka hai
        for file in files:
            if file.startswith('thumb_'):
                # Ex: 'thumb_14.jpg' -> '14'
                thumb_base_name = file.replace('thumb_', '').rsplit('.', 1)[0]
                
                # Agar '14' naam ka photo/video ab nahi hai, toh thumb_14.jpg ko bhi uda do
                if thumb_base_name not in original_files_no_ext:
                    thumb_path = os.path.join(root, file)
                    try:
                        os.remove(thumb_path)
                        print(f"🗑️ Faltu Thumbnail Delete Kiya: {thumb_path}")
                    except Exception as e:
                        print(f"❌ Delete karne me error aayi: {e}")

if __name__ == "__main__":
    # Sirf in 5 folders ke andar hi thumbnail banenge aur cleanup hoga
    target_folders = ["S", "RK", "O", "R", "K"]
    for folder in target_folders:
        if os.path.exists(folder):
            generate_thumbnails(folder)
            cleanup_orphaned_thumbnails(folder)
