"""
Folium Map Rendering for Karur Road Network
Renders junctions, route polylines, congestion colors, and live ambulance markers
"""

import folium

JUNCTION_COORDS = {
    "S": [10.9400, 78.0700],
    "J1": [10.9500, 78.0550],
    "J2": [10.9550, 78.0750],
    "J3": [10.9600, 78.0950],
    "H": [10.9700, 78.0800]
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


def create_network_map(traffic_data=None, active_route="Route B", ambulance_pos=None, hospital_pos=None):
    # Center around Karur
    center_lat = 10.9550
    center_lon = 78.0750
    m = folium.Map(location=[center_lat, center_lon], zoom_start=13, tiles="CartoDB positron")

    # Map segment id to congestion level
    seg_congestion = {}
    seg_speeds = {}
    if traffic_data:
        for t in traffic_data:
            sid = t.get("segment_id")
            seg_congestion[sid] = t.get("congestion_level", "LOW")
            seg_speeds[sid] = t.get("speed_kmph", 40.0)

    # Draw all road segments
    for seg_id, path in SEGMENT_PATHS.items():
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

        weight = 8 if is_on_active_route else 4
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

    # Draw Junction Markers
    for jid, coords in JUNCTION_COORDS.items():
        if jid == "S":
            folium.Marker(
                coords,
                popup="<b>Source S (Dispatch Base)</b>",
                tooltip="Dispatch Point S",
                icon=folium.Icon(color="blue", icon="home", prefix="fa")
            ).add_to(m)
        elif jid == "H":
            folium.Marker(
                coords,
                popup="<b>Karur Govt Medical College Hospital (H)</b><br>Designated Emergency Bay",
                tooltip="Destination Hospital H",
                icon=folium.Icon(color="red", icon="plus", prefix="fa")
            ).add_to(m)
        else:
            folium.CircleMarker(
                coords,
                radius=8,
                color="#34495E",
                fill=True,
                fill_color="#2C3E50",
                fill_opacity=0.9,
                popup=f"<b>Junction {jid}</b><br>Traffic Signal Node",
                tooltip=f"Junction {jid}"
            ).add_to(m)

    # Draw moving Ambulance marker if active
    if ambulance_pos:
        folium.Marker(
            ambulance_pos,
            popup="<b>Emergency Ambulance</b><br>Navigation in progress",
            tooltip="Ambulance (Emergency Mode)",
            icon=folium.Icon(color="darkred", icon="ambulance", prefix="fa")
        ).add_to(m)

    return m


def render_network_map(active_route="Route B", ambulance_location=None, congested_segments=None, traffic_data=None, height=360):
    """Convenience helper to render Folium map inside Streamlit"""
    from streamlit_folium import st_folium
    m = create_network_map(
        traffic_data=traffic_data,
        active_route=active_route,
        ambulance_pos=ambulance_location
    )
    return st_folium(m, width=None, height=height, returned_objects=[])

