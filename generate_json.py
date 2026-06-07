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

# ⭐ NAYA BULLETPROOF FUNCTION
def get_file_age_in_days(filepath):
    try:
        # File ka sabse purana (first) commit time nikalega
        cmd = f'git log --format=%at -- "{filepath}"'
        result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, text=True)
        output = result.stdout.strip()
        
        if output:
            # Output mein list of timestamps hoti hai, hum sabse aakhiri (oldest) uthayenge
            oldest_timestamp = int(output.split('\n')[-1])
            current_time = time.time()
            return (current_time - oldest_timestamp) / (24 * 3600)
        else:
            # ⭐ MAIN FIX: Agar history na mile, toh file ko 999 din purani maan lo (Taaki sab galti se NEW na banein)
            return 999.0
    except Exception:
        return 999.0

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
            # Unix path format (important for Git bash)
            file_path = f"{folder}/{file}"
            
            if folder == "S":
                file_url = f"{base_status_url}{folder}/{file}"
            else:
                file_url = f"{base_cdn_url}{folder}/{file}"

            # Age nikalo aur 10 din ka rule lagao
            age_in_days = get_file_age_in_days(file_path)
            
            is_new = "true" if age_in_days <= 10.0 else "false"

            item = {
                "url": file_url,
                "category": category_name,
                "isNew": is_new
            }
            wallpaper_list.append(item)

with open("wallpapers.json", "w") as f:
    json.dump(wallpaper_list, f, indent=2)

print("Bhai, JSON naye Bulletproof logic ke saath ekdum ready hai!")
