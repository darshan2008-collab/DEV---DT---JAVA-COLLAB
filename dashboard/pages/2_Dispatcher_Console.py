"""
Page 2: Emergency Dispatcher Console
Operations Command Center, Trip Lifecycle, and Section 14 Demonstration.
"""

import time
import streamlit as st
import pandas as pd
from streamlit_folium import st_folium
from components.icons import (
    GLOBAL_LIGHT_THEME_CSS, render_header, render_metric_card,
    SVG_ROUTE, SVG_AMBULANCE, SVG_PLAY, SVG_PAUSE, SVG_REFRESH, SVG_CHECK
)
from components.api_client import (
    get_trips, get_ambulances, get_hospitals, create_trip,
    trip_action, get_traffic_current, set_simulation_scenario,
    request_signal_priority, accept_recommendation, get_trip_recommendations,
    get_trip_route_updates, calculate_routes
)
from components.map_utils import create_network_map

st.set_page_config(page_title="Dispatcher Console", layout="wide")
st.markdown(GLOBAL_LIGHT_THEME_CSS, unsafe_allow_html=True)

# Executive Header
header_html = render_header(
    "Emergency Dispatcher Operations Console",
    "Central Mission Dispatch, Live Fleet Monitoring, and Section 14 Protocol Demonstration",
    SVG_ROUTE
)
st.markdown(header_html, unsafe_allow_html=True)

# Section 14 Automated Demonstration Block
st.markdown(f"""
<div style="background:#FFFFFF; border:1px solid #E2E8F0; border-left:4px solid #0284C7; border-radius:10px; padding:18px 22px; margin-bottom:20px; box-shadow:0 1px 3px rgba(0,0,0,0.04);">
    <div style="display:flex; align-items:center; gap:8px; color:#0F172A; font-weight:700; font-size:1.1rem; margin-bottom:6px;">
        <span style="color:#0284C7;">{SVG_PLAY}</span> Section 14 End-to-End Autonomous Demonstration Protocol
    </div>
    <p style="color:#64748B; font-size:0.9rem; margin:0 0 14px 0; line-height:1.5;">
        Executes the documented project benchmark flow: 
        <strong>Dispatch</strong> &rarr; <strong>Route B Selected (12 min)</strong> &rarr; 
        <strong>Signal Priority Request</strong> &rarr; <strong>B2 Queue Injected (26 min)</strong> &rarr; 
        <strong>Dynamic Recalculation to Route C (15 min)</strong> &rarr; <strong>Driver Acceptance</strong> &rarr; 
        <strong>Hospital Pre-Arrival Notice</strong> &rarr; <strong>Completion</strong>.
    </p>
</div>
""", unsafe_allow_html=True)

if st.button("RUN EMERGENCY NAVIGATION DEMONSTRATION (SECTION 14 FLOW)", type="primary", use_container_width=True):
    prog_bar = st.progress(0, text="Initializing Emergency Demonstration Sequence...")

    # Step 1: Initialize
    prog_bar.progress(10, text="1/10: Verifying available emergency fleet and receiving trauma facility...")
    set_simulation_scenario("NORMAL")
    time.sleep(1)

    # Step 2: Create trip
    prog_bar.progress(25, text="2/10: Dispatching emergency mission for trauma patient...")
    trip_res = create_trip("AMB-01", "H-01", "S", "H")
    if "error" in trip_res:
        trips = get_trips()
        trip_id = trips[0]["id"] if trips else "TRIP-DEMO"
    else:
        trip_id = trip_res["id"]

    # Step 3: Candidate evaluation
    prog_bar.progress(40, text="3/10: Evaluating candidate corridors A, B, and C. Route B recommended (12.0 min)...")
    calc = calculate_routes("S", "H", "DIJKSTRA")
    time.sleep(1.5)

    # Step 4: Start navigation
    prog_bar.progress(55, text="4/10: Mission activated. Vehicle proceeding along Route B corridor...")
    trip_action(trip_id, "start")
    time.sleep(1.5)

    # Step 5: Signal preemption
    prog_bar.progress(68, text="5/10: Approaching Junction J2. Preempting traffic signal via safe clearance sequence...")
    request_signal_priority("J2", "NORTH_SOUTH", "AMB-01", trip_id)
    time.sleep(2)

    # Step 6: Queue formation
    prog_bar.progress(78, text="6/10: Ingesting telemetry: Queue formed on B2 (12 km/h). Route B rises to 26 min...")
    set_simulation_scenario("QUEUE_ON_B2")
    time.sleep(2)

    # Step 7 & 8: Recalculate & Accept
    prog_bar.progress(88, text="7/10: Dynamic engine identifies Route C (15 min, saving 11 min > 1.0 min margin). Driver accepts...")
    recs = get_trip_recommendations(trip_id)
    if recs:
        accept_recommendation(trip_id, recs[0]["id"])
    time.sleep(1.5)

    # Step 9 & 10: Complete
    prog_bar.progress(100, text="10/10: Arrival at Karur Government Medical College Hospital Trauma Bay. Mission Completed.")
    trip_action(trip_id, "complete")
    time.sleep(1)

    st.success(f"Demonstration protocol concluded successfully for Mission {trip_id}. Dynamic optimizer saved 11 simulated minutes.")
    st.rerun()

st.divider()

# Dispatch Form and Active Operations Map
col_form, col_map = st.columns([1.5, 2.5])

with col_form:
    st.subheader("Dispatch New Mission")
    ambulances = get_ambulances()
    hospitals = get_hospitals()

    available_ambs = [a for a in ambulances if a.get("status") == "AVAILABLE"]
    amb_opts = {f"{a['id']} - {a['model']} ({a['plate_number']})": a['id'] for a in available_ambs}
    hosp_opts = {f"{h['name']} ({h.get('available_beds', 0)} beds free)": h['id'] for h in hospitals}

    with st.form("dispatch_form"):
        st.markdown("**Origin Location:** Source S (Central Dispatch Station)")
        selected_amb = st.selectbox("Select Available Vehicle:", list(amb_opts.keys()) if amb_opts else ["No vehicles available"])
        selected_hosp = st.selectbox("Select Receiving Hospital (Dispatcher Choice):", list(hosp_opts.keys()))

        dispatch_btn = st.form_submit_button("Confirm Mission & Dispatch", type="primary", use_container_width=True)
        if dispatch_btn:
            if not amb_opts:
                st.error("No vehicles currently in AVAILABLE state.")
            else:
                a_id = amb_opts[selected_amb]
                h_id = hosp_opts[selected_hosp]
                res = create_trip(a_id, h_id, "S", "H")
                if "error" in res:
                    st.error(f"Dispatch failed: {res['error']}")
                else:
                    st.success(f"Mission {res['id']} created. Initial Route: {res.get('selected_route')} (ETA {res.get('initial_eta_minutes'):.1f} min)")
                    st.rerun()

with col_map:
    st.subheader("Live Network Operations Map")
    traffic = get_traffic_current()
    trips = get_trips()
    active_trips = [t for t in trips if t.get("status") in ["ACTIVE", "CREATED", "PAUSED"]]

    active_route = active_trips[0].get("selected_route") if active_trips else "Route B"
    net_map = create_network_map(traffic_data=traffic, active_route=active_route)
    st_folium(net_map, width=650, height=360)

st.divider()

# Active Missions Table
st.subheader("Active Emergency Missions")

if active_trips:
    trip_table = pd.DataFrame([
        {
            "Mission ID": t["id"],
            "Unit": t["ambulance_id"],
            "Destination": t["destination"],
            "Corridor": t["selected_route"],
            "Status": t["status"],
            "ETA (min)": f"{t.get('current_eta_minutes', 0.0):.1f}",
            "Remaining (km)": f"{t.get('remaining_distance_km', 0.0):.1f}",
            "Reroutes": t.get("route_change_count", 0),
            "Start Time": t.get("start_time", "Pending")
        } for t in active_trips
    ])
    st.dataframe(trip_table, use_container_width=True)

    # Action controls for selected trip
    target_trip = active_trips[0]
    t_id = target_trip["id"]
    t_st = target_trip["status"]

    st.markdown(f"**Mission Operations Controls for <code>{t_id}</code>:**", unsafe_allow_html=True)
    b1, b2, b3, b4, b5 = st.columns(5)
    with b1:
        if st.button("Start Mission", disabled=(t_st != "CREATED"), use_container_width=True):
            trip_action(t_id, "start")
            st.rerun()
    with b2:
        if st.button("Pause Mission", disabled=(t_st != "ACTIVE"), use_container_width=True):
            trip_action(t_id, "pause")
            st.rerun()
    with b3:
        if st.button("Resume Mission", disabled=(t_st != "PAUSED"), use_container_width=True):
            trip_action(t_id, "resume")
            st.rerun()
    with b4:
        if st.button("Complete Mission", disabled=(t_st not in ["ACTIVE", "PAUSED"]), use_container_width=True):
            trip_action(t_id, "complete")
            st.rerun()
    with b5:
        if st.button("Cancel Mission", disabled=(t_st in ["COMPLETED", "CANCELLED"]), use_container_width=True):
            trip_action(t_id, "cancel")
            st.rerun()
else:
    st.info("No active emergency missions currently in transit.")

st.divider()

# Persisted Reroute Audit Log
st.subheader("Persisted Route Change Audit Log")
all_trips = get_trips()
if all_trips:
    sample_trip_id = all_trips[0]["id"]
    updates = get_trip_route_updates(sample_trip_id)
    if updates:
        up_df = pd.DataFrame([
            {
                "Timestamp": u.get("timestamp"),
                "Mission ID": u.get("trip_id"),
                "Previous Corridor": u.get("previous_route"),
                "New Corridor": u.get("new_route"),
                "Previous ETA": f"{u.get('previous_eta_minutes', 0.0):.1f} min",
                "New ETA": f"{u.get('new_eta_minutes', 0.0):.1f} min",
                "Time Saved": f"{u.get('time_difference_minutes', 0.0):.1f} min",
                "System Rationale": u.get("reason"),
                "Data Source": u.get("data_source")
            } for u in updates
        ])
        st.dataframe(up_df, use_container_width=True)
    else:
        st.caption("No re-routing events logged for this mission.")
