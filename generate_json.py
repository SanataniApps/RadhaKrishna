import os
import json

# Aapke links ke base URLs
base_raw_url = "https://raw.githubusercontent.com/shriradheranigopala/HDWallpaper/refs/heads/main/"
base_status_url = "https://github.com/shriradheranigopala/HDWallpaper/raw/refs/heads/main/"

# Folders aur unki category ka naam
folders = {
    "Status": "Whatsapp Status",
    "RadhaKrishna": "Radha Krishna",
    "Others": "Others",
    "Radha": "Radha",
    "Krishna": "Krishna"
}

wallpaper_list = []

for folder, category_name in folders.items():
    if os.path.exists(folder):
        files = os.listdir(folder)
        # Sirf images aur videos ko filter karein
        valid_files = [f for f in files if f.endswith(('.jpg', '.jpeg', '.png', '.mp4'))]
        
        # Files ko number ke hisaab se sort karein (jaise 17.jpg sabse nayi hogi)
        def get_num(filename):
            try:
                return int(filename.split('.')[0])
            except ValueError:
                return 0
        
        valid_files.sort(key=get_num, reverse=True)
        
        for index, file in enumerate(valid_files):
            # Status wale folder ka URL thoda alag hai aapke JSON mein
            if folder == "Status":
                file_url = f"{base_status_url}{folder}/{file}"
            else:
                file_url = f"{base_raw_url}{folder}/{file}"
            
            # Har category ki top 4 latest files ko 'isNew: true' do, baaki ko 'false'
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

print("Bhai, JSON ekdum makkhan ban gaya!")
