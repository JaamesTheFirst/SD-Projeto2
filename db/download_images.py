#!/usr/bin/env python3
"""
Downloads car images from Wikipedia for AutoUBI seed data.
Run from the project root: python3 db/download_images.py
"""
import os
import json
import urllib.request
import urllib.parse
import time

OUTPUT_DIR = "src/main/resources/static/images"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# (filename, wikipedia_article_title)
CARS = [
    # --- Citadinos ---
    ("citadino_polo.jpg",       "Volkswagen Polo"),
    ("citadino_clio.jpg",       "Renault Clio"),
    ("citadino_yaris.jpg",      "Toyota Yaris"),
    ("citadino_208.jpg",        "Peugeot 208"),
    ("citadino_ibiza.jpg",      "SEAT Ibiza"),
    ("citadino_corsa.jpg",      "Opel Corsa"),
    ("citadino_fiesta.jpg",     "Ford Fiesta"),
    ("citadino_i20.jpg",        "Hyundai i20"),
    ("citadino_picanto.jpg",    "Kia Picanto"),
    ("citadino_fiat500.jpg",    "Fiat 500"),
    ("citadino_mini.jpg",       "Mini (marque)"),
    ("citadino_aygo.jpg",       "Toyota Aygo"),
    # --- Sedã / Berlina ---
    ("sedan_bmw3.jpg",          "BMW 3 Series"),
    ("sedan_mercedes_c.jpg",    "Mercedes-Benz C-Class"),
    ("sedan_audi_a4.jpg",       "Audi A4"),
    ("sedan_passat.jpg",        "Volkswagen Passat"),
    ("sedan_octavia.jpg",       "Škoda Octavia"),
    ("sedan_camry.jpg",         "Toyota Camry"),
    ("sedan_accord.jpg",        "Honda Accord"),
    ("sedan_peugeot508.jpg",    "Peugeot 508"),
    ("sedan_mazda6.jpg",        "Mazda6"),
    ("sedan_giulia.jpg",        "Alfa Romeo Giulia"),
    ("sedan_volvo_s60.jpg",     "Volvo S60"),
    ("sedan_mondeo.jpg",        "Ford Mondeo"),
    # --- SUV ---
    ("suv_rav4.jpg",            "Toyota RAV4"),
    ("suv_tiguan.jpg",          "Volkswagen Tiguan"),
    ("suv_bmw_x5.jpg",          "BMW X5"),
    ("suv_audi_q5.jpg",         "Audi Q5"),
    ("suv_sportage.jpg",        "Kia Sportage"),
    ("suv_tucson.jpg",          "Hyundai Tucson"),
    ("suv_peugeot3008.jpg",     "Peugeot 3008"),
    ("suv_karoq.jpg",           "Škoda Karoq"),
    ("suv_kuga.jpg",            "Ford Kuga"),
    ("suv_duster.jpg",          "Dacia Duster"),
    ("suv_qashqai.jpg",         "Nissan Qashqai"),
    ("suv_ateca.jpg",           "SEAT Ateca"),
    # --- Elétricos / Híbridos ---
    ("eletrico_tesla_m3.jpg",   "Tesla Model 3"),
    ("eletrico_tesla_my.jpg",   "Tesla Model Y"),
    ("eletrico_id4.jpg",        "Volkswagen ID.4"),
    ("eletrico_leaf.jpg",       "Nissan Leaf"),
    ("eletrico_zoe.jpg",        "Renault Zoe"),
    ("eletrico_bmw_i4.jpg",     "BMW i4"),
    ("eletrico_e208.jpg",       "Peugeot e-208"),
    ("eletrico_prius.jpg",      "Toyota Prius"),
    ("eletrico_ioniq5.jpg",     "Hyundai Ioniq 5"),
    ("eletrico_ev6.jpg",        "Kia EV6"),
    ("eletrico_bmw_ix.jpg",     "BMW iX"),
    ("eletrico_q4etron.jpg",    "Audi Q4 e-tron"),
    # --- Desportivos ---
    ("desportivo_911.jpg",      "Porsche 911"),
    ("desportivo_f8.jpg",        "Ferrari F8"),
    ("desportivo_huracan.jpg",  "Lamborghini Huracán"),
    ("desportivo_bmw_m3.jpg",   "BMW M3"),
    ("desportivo_audi_r8.jpg",  "Audi R8"),
    ("desportivo_corvette.jpg", "Chevrolet Corvette (C8)"),
    ("desportivo_mustang.jpg",  "Ford Mustang"),
    ("desportivo_720s.jpg",     "McLaren 720S"),
    ("desportivo_amg_gt.jpg",   "Mercedes-AMG GT"),
    ("desportivo_ftype.jpg",    "Jaguar F-Type"),
    ("desportivo_gtr.jpg",      "Nissan GT-R"),
    ("desportivo_challenger.jpg","Dodge Challenger"),
    # --- Comerciais ---
    ("comercial_sprinter.jpg",  "Mercedes-Benz Sprinter"),
    ("comercial_transit.jpg",   "Ford Transit"),
    ("comercial_transporter.jpg","Volkswagen Transporter"),
    ("comercial_trafic.jpg",    "Renault Trafic"),
    ("comercial_berlingo.jpg",  "Citroën Berlingo"),
    ("comercial_partner.jpg",   "Citroën Berlingo"),
    ("comercial_proace.jpg",    "Citroën Jumpy"),
    ("comercial_iveco.jpg",     "Iveco Daily"),
    ("comercial_movano.jpg",    "Opel Movano"),
    ("comercial_ducato.jpg",    "Fiat Ducato"),
    ("comercial_nv400.jpg",     "Renault Master"),
    ("comercial_boxer.jpg",     "Fiat Ducato"),
    # --- Motos ---
    ("moto_cb500f.jpg",         "Honda CB series"),
    ("moto_mt07.jpg",           "Yamaha MT-07"),
    ("moto_z900.jpg",           "Kawasaki Z900"),
    ("moto_bmw_gs.jpg",         "BMW R1250GS"),
    ("moto_ducati_monster.jpg", "Ducati Monster"),
    ("moto_gsxs750.jpg",        "Suzuki GSX-S750"),
    ("moto_ktm_duke.jpg",       "KTM Duke series"),
    ("moto_street_triple.jpg",  "Triumph Street Triple"),
    ("moto_sportster.jpg",      "Harley-Davidson Sportster"),
    ("moto_africa_twin.jpg",    "Honda Africa Twin"),
    ("moto_ninja400.jpg",       "Kawasaki Ninja 400"),
    ("moto_yamaha_r1.jpg",      "Yamaha YZF-R1"),
]

HEADERS = {"User-Agent": "AutoUBI-SchoolProject/1.0 (educational use)"}

def get_wiki_image_url(title):
    api = (
        "https://en.wikipedia.org/w/api.php"
        "?action=query"
        "&prop=pageimages"
        "&format=json"
        "&pithumbsize=800"
        "&titles=" + urllib.parse.quote(title)
    )
    req = urllib.request.Request(api, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=10) as resp:
        data = json.loads(resp.read())
    pages = data["query"]["pages"]
    page = next(iter(pages.values()))
    return page.get("thumbnail", {}).get("source")


def download(url, dest):
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=15) as resp:
        with open(dest, "wb") as f:
            f.write(resp.read())


def try_download(filename, title, retries=4):
    dest = os.path.join(OUTPUT_DIR, filename)
    if os.path.exists(dest):
        print(f"  SKIP  {filename} (already exists)")
        return "skip"
    for attempt in range(retries):
        try:
            url = get_wiki_image_url(title)
            if not url:
                print(f"  MISS  {filename} — no image for '{title}'")
                return "fail"
            download(url, dest)
            print(f"  OK    {filename}  ← {title}")
            return "ok"
        except urllib.error.HTTPError as e:
            if e.code == 429:
                wait = 5 * (attempt + 1)
                print(f"  WAIT  {filename} — rate limited, sleeping {wait}s (attempt {attempt+1}/{retries})")
                time.sleep(wait)
            else:
                print(f"  FAIL  {filename}: HTTP {e.code}")
                return "fail"
        except Exception as e:
            print(f"  FAIL  {filename}: {e}")
            return "fail"
    print(f"  FAIL  {filename}: gave up after {retries} retries")
    return "fail"


ok, skip, fail = 0, 0, 0
for filename, title in CARS:
    result = try_download(filename, title)
    if result == "ok":
        ok += 1
        time.sleep(1.5)   # be polite to the API
    elif result == "skip":
        skip += 1
    else:
        fail += 1

print(f"\nDone. {ok} downloaded, {skip} skipped, {fail} failed.")
