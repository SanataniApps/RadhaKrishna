import os
import json
import subprocess
import time

base_raw_url = "https://github.com/s-n-t-ni-a-p/res-rk/raw/refs/heads/main/"

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
        valid_files = [f for f in files if f.endswith(('.jpg', '.jpeg', '.png', '.mp4'))]
        
        def get_num(filename):
            try: return int(filename.split('.')[0])
            except ValueError: return 0
        
        valid_files.sort(key=get_num, reverse=True)
        
        for file in valid_files:
            # Sirf raw url use hoga sabke liye
            file_url = f"{base_raw_url}{folder}/{file}"
            age_in_days = get_file_age_in_days(f"{folder}/{file}")
            is_new = "true" if age_in_days <= 10.0 else "false"
            
            wallpaper_list.append({
                "url": file_url,
                "category": category_name,
                "isNew": is_new
            })

with open("wallpapers.json", "w") as f:
    json.dump(wallpaper_list, f, indent=2)
