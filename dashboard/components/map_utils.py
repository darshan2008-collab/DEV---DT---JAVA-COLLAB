"""
Folium Map Rendering for Karur Road Network & District Healthcare Infrastructure
Renders junctions, route polylines, congestion colors, live ambulance markers,
Karur District boundary, and all Hospitals, Trauma Centers & Clinics across Karur District.
"""

import folium
from folium import plugins

# ============================================================================
# KARUR DISTRICT COMPREHENSIVE HEALTHCARE DIRECTORY (HOSPITALS & CLINICS)
# ============================================================================
KARUR_DISTRICT_HOSPITALS = [
    {
        "id": "GMCH-01",
        "name": "Govt Medical College Hospital (GMCH) Karur",
        "category": "Govt Medical College",
        "coords": [10.9332, 78.0841],
        "type": "Apex Level-1 Trauma & Tertiary Care (750+ Beds)",
        "address": "Gandhigramam / Sanapiratti, Karur, TN 639004",
        "beds": 750,
        "icu_beds": 36,
        "trauma_bays": 8,
        "ambulance_contact": "108 / 04324-240001",
        "blood_bank": "24x7 Available (All Groups)",
        "specialties": ["Trauma & Ortho", "Cardiology", "Neurology", "Emergency ICU", "Burn Unit"],
        "icon_color": "red",
        "icon_name": "plus",
        "is_apex": True
    },
    {
        "id": "KGH-02",
        "name": "Karur Govt District Headquarters Hospital (Old GH)",
        "category": "Govt Hospital",
        "coords": [10.9605, 78.0772],
        "type": "Government General & Emergency Hospital",
        "address": "Azad Road / Bus Stand Road, Karur, TN 639001",
        "beds": 250,
        "icu_beds": 12,
        "trauma_bays": 4,
        "ambulance_contact": "04324-260224",
        "blood_bank": "24x7 Available",
        "specialties": ["Casualty & Emergency", "Maternity (OG)", "Pediatrics", "General Surgery"],
        "icon_color": "red",
        "icon_name": "h-square",
        "is_apex": False
    },
    {
        "id": "AMR-03",
        "name": "Amaravathi Hospital",
        "category": "Private Multispeciality",
        "coords": [10.9602, 78.0685],
        "type": "24x7 Multispeciality & Trauma Care",
        "address": "74, Ramanujam Nagar, Kovai Main Road, Karur, TN 639002",
        "beds": 120,
        "icu_beds": 18,
        "trauma_bays": 3,
        "ambulance_contact": "04324-220022",
        "blood_bank": "Tie-up Storage",
        "specialties": ["Accident & Trauma", "Orthopedics", "Critical Care", "Laparoscopy"],
        "icon_color": "blue",
        "icon_name": "ambulance",
        "is_apex": False
    },
    {
        "id": "APL-04",
        "name": "Apollo Speciality Clinics / Apollo Loga Hospital",
        "category": "Private Multispeciality",
        "coords": [10.9601, 78.0675],
        "type": "Super Speciality & Cardiac Care",
        "address": "163, Allwyn Nagar, Kovai Main Road, Karur, TN 639002",
        "beds": 80,
        "icu_beds": 14,
        "trauma_bays": 3,
        "ambulance_contact": "04324-274000",
        "blood_bank": "Storage Ready",
        "specialties": ["Cardiology", "Neurology", "Cath Lab", "Dialysis", "Intensive Care"],
        "icon_color": "blue",
        "icon_name": "heartbeat",
        "is_apex": False
    },
    {
        "id": "KVY-05",
        "name": "Kauvery Medical Centre / Sri Kavery Hospital",
        "category": "Private Multispeciality",
        "coords": [10.9585, 78.0732],
        "type": "Comprehensive Emergency & Ortho Hospital",
        "address": "Sengunthapuram Main Road, Karur, TN 639002",
        "beds": 95,
        "icu_beds": 12,
        "trauma_bays": 3,
        "ambulance_contact": "04324-233300",
        "blood_bank": "Available",
        "specialties": ["Emergency Medicine", "Joint Replacement", "Gastroenterology", "Nephrology"],
        "icon_color": "blue",
        "icon_name": "user-md",
        "is_apex": False
    },
    {
        "id": "KGH-06",
        "name": "K.G. Hospital & Trauma Centre",
        "category": "Private Multispeciality",
        "coords": [10.9575, 78.0815],
        "type": "Trauma & Surgical Care",
        "address": "Old Bypass Road, Karur, TN 639002",
        "beds": 60,
        "icu_beds": 8,
        "trauma_bays": 2,
        "ambulance_contact": "04324-241515",
        "blood_bank": "On-Call",
        "specialties": ["Trauma Surgery", "Emergency Resuscitation", "General Surgery"],
        "icon_color": "blue",
        "icon_name": "medkit",
        "is_apex": False
    },
    {
        "id": "SAR-07",
        "name": "Saraswathi Hospital",
        "category": "Private Multispeciality",
        "coords": [10.9560, 78.0765],
        "type": "Multispeciality Hospital",
        "address": "80 Feet Road, Sengunthapuram, Karur, TN 639002",
        "beds": 70,
        "icu_beds": 8,
        "trauma_bays": 2,
        "ambulance_contact": "04324-236688",
        "blood_bank": "Available",
        "specialties": ["Laparoscopic Surgery", "General Medicine", "ICU", "Gynecology"],
        "icon_color": "blue",
        "icon_name": "h-square",
        "is_apex": False
    },
    {
        "id": "ABI-08",
        "name": "Abirami Hospital",
        "category": "Private Multispeciality",
        "coords": [10.9592, 78.0788],
        "type": "Maternity & Surgical Care",
        "address": "North Pradhakshanam Road, Karur, TN 639001",
        "beds": 50,
        "icu_beds": 6,
        "trauma_bays": 2,
        "ambulance_contact": "04324-261234",
        "blood_bank": "On-Call",
        "specialties": ["Obstetrics", "Pediatrics", "Neonatal ICU (NICU)"],
        "icon_color": "blue",
        "icon_name": "user-md",
        "is_apex": False
    },
    {
        "id": "ATL-09",
        "name": "Atlas Hospital",
        "category": "Private Multispeciality",
        "coords": [10.9598, 78.0712],
        "type": "Critical Care & Neuro Hospital",
        "address": "Kovai Road, Karur, TN 639002",
        "beds": 65,
        "icu_beds": 10,
        "trauma_bays": 2,
        "ambulance_contact": "04324-222888",
        "blood_bank": "Available",
        "specialties": ["Neurology", "Neuro Surgery", "Trauma ICU"],
        "icon_color": "blue",
        "icon_name": "medkit",
        "is_apex": False
    },
    {
        "id": "ARV-10",
        "name": "Aravind Eye Hospital",
        "category": "Speciality Clinic",
        "coords": [10.9620, 78.0710],
        "type": "Apex Ophthalmology Center",
        "address": "Kovai Road, Karur, TN 639002",
        "beds": 40,
        "icu_beds": 2,
        "trauma_bays": 1,
        "ambulance_contact": "04324-225300",
        "blood_bank": "N/A",
        "specialties": ["Ophthalmology", "Cornea", "Eye Trauma & Surgery"],
        "icon_color": "purple",
        "icon_name": "eye",
        "is_apex": False
    },
    {
        "id": "DPK-11",
        "name": "Deepa Kannan Hospital & Clinic",
        "category": "Clinic & Nursing Home",
        "coords": [10.9540, 78.0720],
        "type": "Pediatric & Family Healthcare Clinic",
        "address": "Muthu Nagar, Karur, TN 639002",
        "beds": 35,
        "icu_beds": 4,
        "trauma_bays": 1,
        "ambulance_contact": "04324-230911",
        "blood_bank": "N/A",
        "specialties": ["Pediatrics", "Neonatology", "General OPD"],
        "icon_color": "cadetblue",
        "icon_name": "stethoscope",
        "is_apex": False
    },
    {
        "id": "MNK-12",
        "name": "Meenakshi Multispeciality Clinic",
        "category": "Clinic & Nursing Home",
        "coords": [10.9615, 78.0810],
        "type": "Outpatient & Urgent Care Clinic",
        "address": "Jawahar Bazaar, Karur, TN 639001",
        "beds": 20,
        "icu_beds": 2,
        "trauma_bays": 1,
        "ambulance_contact": "04324-263100",
        "blood_bank": "N/A",
        "specialties": ["General Medicine", "Diabetology", "Urgent Care"],
        "icon_color": "cadetblue",
        "icon_name": "user-md",
        "is_apex": False
    },
    # Taluk & Regional Hospitals in Karur District
    {
        "id": "KUL-13",
        "name": "Kulithalai Govt Headquarters Hospital",
        "category": "Govt Hospital",
        "coords": [10.9318, 78.4180],
        "type": "Sub-District Hospital (East Karur Corridor)",
        "address": "Kulithalai Main Road, Karur District, TN 639104",
        "beds": 120,
        "icu_beds": 6,
        "trauma_bays": 3,
        "ambulance_contact": "04323-222233",
        "blood_bank": "Available",
        "specialties": ["Emergency Medicine", "General Surgery", "Maternity"],
        "icon_color": "red",
        "icon_name": "h-square",
        "is_apex": False
    },
    {
        "id": "ARV-14",
        "name": "Aravakurichi Government Hospital",
        "category": "Govt Hospital",
        "coords": [10.7710, 77.9090],
        "type": "Taluk Hospital (South-West Karur Corridor)",
        "address": "Aravakurichi, Karur District, TN 639201",
        "beds": 80,
        "icu_beds": 4,
        "trauma_bays": 2,
        "ambulance_contact": "04320-232230",
        "blood_bank": "Blood Storage Unit",
        "specialties": ["Trauma First Care", "Maternity", "General Ward"],
        "icon_color": "red",
        "icon_name": "h-square",
        "is_apex": False
    },
    {
        "id": "PGL-15",
        "name": "Velayuthampalayam / Pugalur Govt Hospital",
        "category": "Govt Hospital",
        "coords": [11.0720, 78.0120],
        "type": "Taluk Hospital (North Karur Corridor / TNPL)",
        "address": "Velayuthampalayam, Pugalur, Karur District, TN 639117",
        "beds": 60,
        "icu_beds": 4,
        "trauma_bays": 2,
        "ambulance_contact": "04324-270250",
        "blood_bank": "Emergency Supply",
        "specialties": ["Industrial Emergency", "Trauma", "General Care"],
        "icon_color": "red",
        "icon_name": "h-square",
        "is_apex": False
    },
    {
        "id": "KRP-16",
        "name": "Krishnarayapuram Government Hospital",
        "category": "Govt Hospital",
        "coords": [10.9330, 78.2750],
        "type": "Taluk Hospital (Cauvery Basin Corridor)",
        "address": "Krishnarayapuram, Karur District, TN 639102",
        "beds": 50,
        "icu_beds": 2,
        "trauma_bays": 1,
        "ambulance_contact": "04323-242220",
        "blood_bank": "Blood Storage",
        "specialties": ["Emergency Care", "Maternity"],
        "icon_color": "red",
        "icon_name": "h-square",
        "is_apex": False
    },
    # Primary Health Centres (PHC / UPHC) across Karur District
    {
        "id": "PHC-17",
        "name": "Thanthonimalai Urban PHC (UPHC)",
        "category": "Primary Health Centre (PHC/UPHC)",
        "coords": [10.9250, 78.0850],
        "type": "Urban Primary Health Centre",
        "address": "Thanthonimalai, Karur, TN 639005",
        "beds": 10,
        "icu_beds": 0,
        "trauma_bays": 1,
        "ambulance_contact": "108 / 104",
        "blood_bank": "N/A",
        "specialties": ["24x7 Normal Delivery", "First Aid Triage", "Immunization"],
        "icon_color": "green",
        "icon_name": "plus-square",
        "is_apex": False
    },
    {
        "id": "PHC-18",
        "name": "Inamkarur Urban PHC",
        "category": "Primary Health Centre (PHC/UPHC)",
        "coords": [10.9750, 78.0820],
        "type": "Urban Primary Health Centre",
        "address": "Vengamedu, Karur, TN 639006",
        "beds": 10,
        "icu_beds": 0,
        "trauma_bays": 1,
        "ambulance_contact": "108 / 104",
        "blood_bank": "N/A",
        "specialties": ["First Response Triage", "Basic Medical Care", "Vaccination"],
        "icon_color": "green",
        "icon_name": "plus-square",
        "is_apex": False
    },
    {
        "id": "PHC-19",
        "name": "Manmangalam Primary Health Centre",
        "category": "Primary Health Centre (PHC/UPHC)",
        "coords": [11.0200, 78.0700],
        "type": "Rural Primary Health Centre",
        "address": "Manmangalam Block, Karur District, TN 639006",
        "beds": 12,
        "icu_beds": 0,
        "trauma_bays": 1,
        "ambulance_contact": "108",
        "blood_bank": "N/A",
        "specialties": ["Outpatient Care", "Maternity", "Ambulance Feeder"],
        "icon_color": "green",
        "icon_name": "plus-square",
        "is_apex": False
    },
    {
        "id": "PHC-20",
        "name": "Uppidamangalam Primary Health Centre",
        "category": "Primary Health Centre (PHC/UPHC)",
        "coords": [10.9200, 78.1800],
        "type": "Block Primary Health Centre",
        "address": "Uppidamangalam, Karur District, TN 639114",
        "beds": 15,
        "icu_beds": 0,
        "trauma_bays": 1,
        "ambulance_contact": "108",
        "blood_bank": "N/A",
        "specialties": ["Emergency First Care", "Maternity Care", "Preventive Medicine"],
        "icon_color": "green",
        "icon_name": "plus-square",
        "is_apex": False
    }
]

# Approximate boundary points defining Karur District boundary (for perimeter visual demarcation)
KARUR_DISTRICT_BORDER = [
    [11.0950, 78.0100],  # North-West: Velayuthampalayam / Pugalur / Cauvery river
    [11.0650, 78.1400],  # North: Vangal / Nerur
    [10.9850, 78.3300],  # North-East: Mayanur
    [10.9450, 78.4350],  # East: Kulithalai / Lalapet
    [10.7700, 78.2600],  # South-East: Kadavur / Tharagampatti hills
    [10.6600, 77.9900],  # South: Aravakurichi South
    [10.7450, 77.8200],  # South-West: Chinnadharapuram / Amaravathi basin
    [10.9100, 77.9150],  # West: K.Paramathi border
    [11.0950, 78.0100]   # Close polygon back to start
]

# ============================================================================
# TRAFFIC CORRIDOR JUNCTIONS & SEGMENTS
# ============================================================================
JUNCTION_COORDS = {
    "S": [10.9400, 78.0700],   # Dispatch Base / Thanthonimalai
    "J1": [10.9500, 78.0550],  # Junction 1: Kovai Road West
    "J2": [10.9550, 78.0750],  # Junction 2: Central Bypass / Bus Stand Node
    "J3": [10.9600, 78.0950],  # Junction 3: Salem Highway East
    "H": [10.9332, 78.0841]    # Apex Destination: Karur Govt Medical College Hospital (GMCH)
}

SEGMENT_PATHS = {
    "A1": [JUNCTION_COORDS["S"], JUNCTION_COORDS["J1"]],
    "A2": [JUNCTION_COORDS["J1"], JUNCTION_COORDS["H"]],
    "B1": [JUNCTION_COORDS["S"], JUNCTION_COORDS["J2"]],
    "B2": [JUNCTION_COORDS["J2"], JUNCTION_COORDS["H"]],
    "C1": [JUNCTION_COORDS["S"], JUNCTION_COORDS["J3"]],
    "C2": [JUNCTION_COORDS["J3"], JUNCTION_COORDS["H"]]
}

CONGESTION_COLORS = {
    "LOW": "#2ECC71",       # Green
    "MEDIUM": "#F39C12",    # Orange
    "HIGH": "#E74C3C",      # Red
    "CRITICAL": "#962D22"   # Dark Red
}


def create_network_map(
    traffic_data=None,
    active_route="Route B",
    ambulance_pos=None,
    ambulance_location=None,
    congested_segments=None,
    hospital_pos=None,
    show_hospitals=True,
    category_filter=None,
    zoom_start=12,
    center_coords=None,
    **kwargs
):
    """
    Renders an interactive Folium Map centered on Karur District.
    Accurately highlights all Karur District Hospitals, Clinics, PHCs, 
    and demarcates the Karur District Boundary.
    """
    amb_coord = ambulance_pos if ambulance_pos is not None else ambulance_location
    # Center on Karur City / District Medical Hub
    center_lat = center_coords[0] if center_coords else 10.9560
    center_lon = center_coords[1] if center_coords else 78.0760

    m = folium.Map(
        location=[center_lat, center_lon],
        zoom_start=zoom_start,
        tiles="OpenStreetMap",
        control_scale=True,
    )

    # 1. Demarcate Karur District Boundary 
    folium.Polygon(
        locations=KARUR_DISTRICT_BORDER,
        color="#0284C7",
        weight=2.5,
        opacity=0.75,
        fill=True,
        fill_color="#38BDF8",
        fill_opacity=0.04,
        dash_array="6, 8",
        popup="<b>KARUR DISTRICT BOUNDARY </b><br>Active Medical & Ambulance Priority Zone",
        tooltip="Karur District Boundary"
    ).add_to(m)

    # 2. Draw Road Network Segments & Congestion Levels
    seg_congestion = {}
    seg_speeds = {}
    if traffic_data:
        for t in traffic_data:
            sid = t.get("segment_id")
            seg_congestion[sid] = t.get("congestion_level", "LOW")
            seg_speeds[sid] = t.get("speed_kmph", 40.0)

    if congested_segments:
        for cs in congested_segments:
            seg_congestion[cs] = "HIGH"
            seg_speeds[cs] = 15.0

    for seg_id in list(SEGMENT_PATHS.keys()):
        path = SEGMENT_PATHS[seg_id]
        cong = seg_congestion.get(seg_id, "LOW")
        speed = seg_speeds.get(seg_id, 40.0)
        color = CONGESTION_COLORS.get(cong, "#2ECC71")

        is_on_active_route = False
        if active_route:
            r_str = " ".join(active_route) if isinstance(active_route, list) else str(active_route)
            if "A" in r_str and seg_id.startswith("A"): is_on_active_route = True
            if "B" in r_str and seg_id.startswith("B"): is_on_active_route = True
            if "C" in r_str and seg_id.startswith("C"): is_on_active_route = True
            if seg_id in r_str: is_on_active_route = True

        weight = 7 if is_on_active_route else 4
        opacity = 0.95 if is_on_active_route else 0.55

        popup_text = f"<b>Segment {seg_id}</b><br>Speed: {speed:.1f} km/h<br>Congestion: {cong}"
        folium.PolyLine(
            path,
            color=color,
            weight=weight,
            opacity=opacity,
            popup=popup_text,
            tooltip=f"{seg_id}: {speed:.1f} km/h ({cong})"
        ).add_to(m)

    # 3. Draw Corridor Junctions (S, J1, J2, J3)
    for jid in list(JUNCTION_COORDS.keys()):
        coords = JUNCTION_COORDS[jid]
        if jid == "S":
            folium.Marker(
                coords,
                popup="<b>Source S (Dispatch Base / Thanthonimalai)</b><br>Karur Ambulance Fleet Depot",
                tooltip="Dispatch Point S",
                icon=folium.Icon(color="darkblue", icon="home", prefix="fa")
            ).add_to(m)
        elif jid != "H":  # Hospital markers are handled comprehensively below
            folium.CircleMarker(
                coords,
                radius=8,
                color="#0F172A",
                fill=True,
                fill_color="#334155",
                fill_opacity=0.9,
                popup=f"<b>Junction {jid}</b><br>Smart Traffic Signal Node",
                tooltip=f"Junction {jid} (Signal Node)"
            ).add_to(m)

    # 4. Highlight All Real Hospitals & Clinics in Karur District
    if show_hospitals:
        for hosp in KARUR_DISTRICT_HOSPITALS:
            # Apply optional category filtering
            if category_filter and category_filter != "All":
                if hosp["category"] != category_filter:
                    continue

            # Rich Popup Card HTML
            badge_color = "#DC2626" if "Govt" in hosp["category"] else ("#0284C7" if "Private" in hosp["category"] else "#16A34A")
            specs_html = "".join([f"<span style='display:inline-block; background:#F1F5F9; color:#334155; font-size:10px; padding:2px 6px; border-radius:4px; margin:2px;'>{s}</span>" for s in hosp["specialties"]])

            popup_html = f"""
            <div style='font-family:sans-serif; width:260px; line-height:1.4;'>
                <div style='background:{badge_color}; color:white; padding:6px 10px; border-radius:6px 6px 0 0; font-weight:bold; font-size:12px;'>
                    {hosp['category']}
                </div>
                <div style='padding:10px; border:1px solid #E2E8F0; border-top:none; border-radius:0 0 6px 6px; background:#FFFFFF;'>
                    <h4 style='margin:0 0 4px 0; color:#0F172A; font-size:14px;'>{hosp['name']}</h4>
                    
                    <div style='font-size:11px; color:#334155; margin-bottom:6px;'>
                        <b>Address:</b> {hosp['address']}
                    </div>
                    
                    <div style='display:grid; grid-template-columns:1fr 1fr; gap:6px; margin:8px 0; background:#F8FAFC; padding:6px; border-radius:4px; font-size:11px;'>
                        <div><b>Total Beds:</b> <span style='color:#0284C7;'>{hosp['beds']}</span></div>
                        <div><b>ICU Beds:</b> <span style='color:#DC2626;'>{hosp['icu_beds']}</span></div>
                        <div><b>Trauma Bays:</b> <span style='color:#D97706;'>{hosp['trauma_bays']}</span></div>
                        <div><b>Blood Bank:</b> <span>{hosp['blood_bank']}</span></div>
                    </div>
                    
                    <div style='margin-bottom:6px;'>
                        <div style='font-size:10px; color:#64748B; font-weight:bold;'>KEY SPECIALTIES:</div>
                        {specs_html}
                    </div>
                    
                    <div style='background:#FEF2F2; border:1px solid #FECACA; padding:4px 8px; border-radius:4px; font-size:11px; color:#991B1B;'>
                        📞 <b>Emergency:</b> {hosp['ambulance_contact']}
                    </div>
                </div>
            </div>
            """

            # Apex Hospital (GMCH) gets an animated pulsating radar circle & extra prominence
            if hosp.get("is_apex"):
                folium.CircleMarker(
                    hosp["coords"],
                    radius=22,
                    color="#EF4444",
                    weight=3,
                    fill=True,
                    fill_color="#F87171",
                    fill_opacity=0.35,
                    popup=folium.Popup(popup_html, max_width=300),
                ).add_to(m)

            folium.Marker(
                hosp["coords"],
                popup=folium.Popup(popup_html, max_width=300),
                tooltip=f"{hosp['name']} - {hosp['category']}",
                icon=folium.Icon(
                    color=hosp["icon_color"],
                    icon=hosp.get("icon_name", "plus"),
                    prefix="fa"
                )
            ).add_to(m)

    # 5. Draw Live Moving Ambulance Marker if Active
    if amb_coord:
        folium.Marker(
            amb_coord,
            popup="<b>Emergency Ambulance (Code Red)</b><br>Karur Green Wave Navigation Active",
            tooltip="Ambulance (Karur Corridor)",
            icon=folium.Icon(color="darkred", icon="ambulance", prefix="fa")
        ).add_to(m)

    return m


def render_network_map(
    active_route="Route B",
    ambulance_location=None,
    ambulance_pos=None,
    congested_segments=None,
    traffic_data=None,
    height=420,
    show_hospitals=True,
    category_filter=None,
    **kwargs
):
    """Convenience helper to render Folium map inside Streamlit with full Karur District Hospital GIS overlay"""
    from streamlit_folium import st_folium
    amb_coord = ambulance_pos if ambulance_pos is not None else ambulance_location
    m = create_network_map(
        traffic_data=traffic_data,
        active_route=active_route,
        ambulance_pos=amb_coord,
        congested_segments=congested_segments,
        show_hospitals=show_hospitals,
        category_filter=category_filter,
        **kwargs
    )
    return st_folium(m, width=None, height=height, returned_objects=[])
