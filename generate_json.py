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

# ⭐ NAYA SMART FUNCTION: Ye check karega file ko add hue kitne din ho gaye
def get_file_age_in_days(filepath):
    try:
        # Git history se file add hone ka time nikalega
        result = subprocess.run(
            ['git', 'log', '--diff-filter=A', '--format=%at', '-1', '--', filepath],
            stdout=subprocess.PIPE, text=True
        )
        output = result.stdout.strip()
        
        if output:
            timestamp = int(output) # Git wala time
        else:
            timestamp = os.path.getmtime(filepath) # Agar file abhi commit nahi hui hai (Local fallback)
            
        current_time = time.time()
        return (current_time - timestamp) / (24 * 3600) # Difference in days
    except Exception:
        return 999 # Agar error aaye to purani file maan lo

wallpaper_list = []

for folder, category_name in folders.items():
    if os.path.exists(folder):
        files = os.listdir(folder)
        valid_files = [f for f in files if f.endswith(('.jpg', '.jpeg', '.png', '.mp4'))]

        def get_num(filename):
            try:
                return int(filename.split('.')[0])
            except ValueError:
                return 0

        valid_files.sort(key=get_num, reverse=True)

        for file in valid_files:
            file_path = os.path.join(folder, file)
            
            if folder == "S":
                file_url = f"{base_status_url}{folder}/{file}"
            else:
                file_url = f"{base_cdn_url}{folder}/{file}"

            # ⭐ NAYA MAGIC: Age nikalo aur 10 din ka rule lagao
            age_in_days = get_file_age_in_days(file_path)
            
            # Agar file 10 din ya usse kam purani hai, toh NEW rahegi, warna hat jayegi!
            is_new = "true" if age_in_days <= 10.0 else "false"

            item = {
                "url": file_url,
                "category": category_name,
                "isNew": is_new
            }
            wallpaper_list.append(item)

with open("wallpapers.json", "w") as f:
    json.dump(wallpaper_list, f, indent=2)

print("Bhai, JSON 10-day auto-expiry logic ke saath ekdum ready hai!")
