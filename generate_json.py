import os
import json
import subprocess
import time

base_cdn_url = "https://cdn.jsdelivr.net/gh/s-n-t-ni-a-p/res-rk@main/"
base_status_url = "https://github.com/s-n-t-ni-a-p/res-rk/raw/refs/heads/main/"

folders = {
    "S": "Videos",
    "RK": "Radha Krishna",
    "O": "Others",
    "R": "Radha",
    "K": "Krishna"
}

def get_file_age_in_days(filepath):
    try:
        cmd = f'git log --format=%at -- "{filepath}"'
        result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, text=True)
        output = result.stdout.strip()
        if output:
            oldest_timestamp = int(output.split('\n')[-1])
            return (time.time() - oldest_timestamp) / (24 * 3600)
        else:
            return 999.0
    except Exception:
        return 999.0

wallpaper_list = []

for folder, category_name in folders.items():
    if os.path.exists(folder):
        files = os.listdir(folder)
        # Sirf original media files ko filter karo, thumbnails ko nahi
        valid_files = [f for f in files if f.endswith(('.jpg', '.jpeg', '.png', '.mp4')) and not f.startswith('thumb_')]
        
        def get_num(filename):
            try: return int(filename.split('.')[0])
            except ValueError: return 0
        
        valid_files.sort(key=get_num, reverse=True)
        
        for file in valid_files:
            file_url = f"{base_status_url}{folder}/{file}" if folder == "S" else f"{base_cdn_url}{folder}/{file}"
            
            # ⭐ NAYA: Thumb URL generate karne ka logic
            file_name_no_ext = os.path.splitext(file)[0]
            thumb_filename = f"thumb_{file_name_no_ext}.jpg"
            thumb_path = os.path.join(folder, thumb_filename)
            
            # Agar local folder mein thumbnail exist karta hai, toh uska CDN URL banao
            if os.path.exists(thumb_path):
                thumb_url = f"{base_cdn_url}{folder}/{thumb_filename}"
            else:
                thumb_url = file_url # Fallback: Agar thumbnail nahi mila to original image hi dikha do
            
            age_in_days = get_file_age_in_days(f"{folder}/{file}")
            is_new = "true" if age_in_days <= 10.0 else "false"
            
            wallpaper_list.append({
                "url": file_url,
                "thumbUrl": thumb_url, # ⭐ NAYA: JSON mein ab thumbUrl field add ho gayi
                "category": category_name,
                "isNew": is_new
            })

with open("wallpapers.json", "w") as f:
    json.dump(wallpaper_list, f, indent=2)
