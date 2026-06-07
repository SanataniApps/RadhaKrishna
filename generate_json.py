import os
import json

# ⭐ NAYA SUPER-FAST CDN URL (Images goli ki raftar se load hongi)
base_cdn_url = "https://cdn.jsdelivr.net/gh/s-n-t-ni-a-p/res-rk@main/"

# ⭐ Videos ke liye purana raw link hi best hai (video player fast chalta hai ispe)
base_status_url = "https://github.com/s-n-t-ni-a-p/res-rk/raw/refs/heads/main/"

# ⭐ Naye 'Secret' Folders aur unki category ka naam
folders = {
    "S": "Videos",
    "RK": "Radha Krishna",
    "O": "Others",
    "R": "Radha",
    "K": "Krishna"
}

wallpaper_list = []

for folder, category_name in folders.items():
    if os.path.exists(folder):
        files = os.listdir(folder)
        # Sirf images aur videos ko filter karein
        valid_files = [f for f in files if f.endswith(('.jpg', '.jpeg', '.png', '.mp4'))]
        
        # Files ko number ke hisaab se sort karein
        def get_num(filename):
            try:
                return int(filename.split('.')[0])
            except ValueError:
                return 0
        
        valid_files.sort(key=get_num, reverse=True)
        
        for index, file in enumerate(valid_files):
            # Videos ('S' folder) ke liye direct Github, Images ke liye CDN
            if folder == "S":
                file_url = f"{base_status_url}{folder}/{file}"
            else:
                file_url = f"{base_cdn_url}{folder}/{file}"
            
            # Har category ki top 4 latest files ko 'isNew: true' do
            is_new = "true" if index < 4 else "false"
            
            item = {
                "url": file_url,
                "category": category_name,
                "isNew": is_new
            }
            wallpaper_list.append(item)

# Naya JSON banakar save karein
with open("wallpapers.json", "w") as f:
    json.dump(wallpaper_list, f, indent=2)

print("Bhai, JSON naye Super-Fast CDN path ke saath ekdum ready hai!")
