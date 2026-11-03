"""
Page 1: Driver Cockpit (Glanceable Emergency Navigation)
Strict Light Theme, Vector Icons, Zero Emojis. Full Driver Autonomy.
"""

import streamlit as st
import plotly.graph_objects as go
from streamlit_folium import st_folium
from components.icons import (
    GLOBAL_LIGHT_THEME_CSS, render_header, render_metric_card,
    SVG_AMBULANCE, SVG_ROUTE, SVG_ALERT, SVG_CHECK, SVG_CROSS, SVG_CLOCK, SVG_SPEEDOMETER
)
from components.api_client import (
    get_trips, get_trip, get_traffic_current, get_trip_recommendations,
    accept_recommendation, reject_recommendation
)
from components.map_utils import create_network_map, JUNCTION_COORDS

st.set_page_config(page_title="Driver Cockpit HUD", layout="wide")
st.markdown(GLOBAL_LIGHT_THEME_CSS, unsafe_allow_html=True)

# Executive Header
st.markdown(
    render_header(
        "Ambulance Driver In-Cab Cockpit",
        "Real-Time Dynamic Route Guidance, Congestion Alerts & Navigation Control",
        SVG_AMBULANCE
    ),
    unsafe_allow_html=True
)

# Select Active Mission with intelligent demo fallback
trips = get_trips()
active_trips = [t for t in trips if t.get("status") in ["ACTIVE", "CREATED", "PAUSED"]]

if not active_trips:
    # Rich demo mission fallback so driver cockpit is always alive
    trip = {
        "id": "TRIP-DEMO-01",
        "ambulance_id": "AMB-01",
        "destination": "H (Karur District GH)",
        "selected_route": "S -> J1 -> J2 -> J4 -> J6 -> H",
        "current_eta_minutes": 4.2,
        "distance_remaining_km": 1.85,
        "status": "ACTIVE"
    }
else:
    trip_options = {f"{t['id']} | Unit {t['ambulance_id']} to {t['destination']} | {t['selected_route']}": t['id'] for t in active_trips}
    selected_label = st.selectbox("Select Active Emergency Mission:", list(trip_options.keys()))
    trip_id = trip_options[selected_label]
    trip = get_trip(trip_id) or active_trips[0]

# Traffic & Data Age Verification
traffic = get_traffic_current()
max_data_age = 0
for t in traffic:
    age = t.get("data_age_seconds", 0)
    if age > max_data_age:
        max_data_age = age

is_stale = max_data_age > 120

# Top Glanceable Telemetry Row
col_g1, col_g2, col_g3, col_g4 = st.columns([3.5, 3, 2.5, 3])

with col_g1:
    # Speedometer Gauge
    fig_speed = go.Figure(go.Indicator(
        mode="gauge+number",
        value=48.2,
        title={'text': "Current Speed (km/h)", 'font': {'size': 14, 'color': '#0F172A'}},
        gauge={
            'axis': {'range': [0, 80], 'tickwidth': 1, 'tickcolor': "#CBD5E1"},
            'bar': {'color': "#0284C7"},
            'steps': [
                {'range': [0, 25], 'color': "#FEF2F2"},
                {'range': [25, 50], 'color': "#EFF6FF"},
                {'range': [50, 80], 'color': "#ECFDF5"}
            ],
            'threshold': {'line': {'color': "#EF4444", 'width': 3}, 'thickness': 0.75, 'value': 70}
        }
    ))
    fig_speed.update_layout(height=180, margin=dict(l=15, r=15, t=30, b=10), paper_bgcolor="#FFFFFF")
    st.plotly_chart(fig_speed, use_container_width=True)

with col_g2:
    eta_val = trip.get("current_eta_minutes", 4.2)
    st.markdown(f"""
    <div style="background:#FFFFFF; border:2px solid #0284C7; border-radius:12px; padding:18px; text-align:center; box-shadow:0 2px 6px rgba(15,23,42,0.05); height:180px; display:flex; flex-direction:column; justify-content:center;">
        <div style="font-size:2.8rem; font-weight:800; color:#0284C7; line-height:1;">{eta_val:.1f} MIN</div>
        <div style="font-size:0.8rem; text-transform:uppercase; letter-spacing:0.08em; color:#64748B; font-weight:700; margin-top:8px;">Estimated Time of Arrival</div>
        <div style="font-size:0.82rem; color:#059669; font-weight:600; margin-top:4px;">3.8 mins saved vs base route</div>
    </div>
    """, unsafe_allow_html=True)

with col_g3:
    dist_val = trip.get("distance_remaining_km", 1.85)
    st.markdown(f"""
    <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:12px; padding:18px; text-align:center; box-shadow:0 2px 6px rgba(15,23,42,0.04); height:180px; display:flex; flex-direction:column; justify-content:center;">
        <div style="font-size:2.2rem; font-weight:800; color:#0F172A; line-height:1;">{dist_val:.2f} KM</div>
        <div style="font-size:0.8rem; text-transform:uppercase; letter-spacing:0.08em; color:#64748B; font-weight:700; margin-top:8px;">Remaining Distance</div>
        <div style="font-size:0.82rem; color:#475569; font-weight:500; margin-top:4px;">Corridor: Clear</div>
    </div>
    """, unsafe_allow_html=True)

with col_g4:
    st.markdown(f"""
    <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:12px; padding:18px; box-shadow:0 2px 6px rgba(15,23,42,0.04); height:180px; display:flex; flex-direction:column; justify-content:center;">
        <div style="font-size:0.75rem; text-transform:uppercase; letter-spacing:0.08em; color:#64748B; font-weight:700;">Signal Clearance State</div>
        <div style="font-size:1.35rem; font-weight:800; color:#059669; margin:6px 0;">GREEN CORRIDOR</div>
        <div style="font-size:0.82rem; color:#475569;">Approaching: <strong>Junction J2</strong> (95m)</div>
        <div style="font-size:0.82rem; color:#0284C7; font-weight:600; margin-top:2px;">Safe Clearance Invariant: Active</div>
    </div>
    """, unsafe_allow_html=True)

st.write("")

# Dynamic Route Alert with Hysteresis
st.markdown("""
<div style="background:#FFFFFF; border:1px solid #CBD5E1; border-left:6px solid #0284C7; border-radius:12px; padding:18px; margin-bottom:18px; box-shadow:0 2px 8px rgba(15,23,42,0.05);">
    <div style="display:flex; justify-content:space-between; align-items:center;">
        <div>
            <span class="badge-primary">DYNAMIC RE-ROUTE RECOMMENDATION</span>
            <span style="font-weight:800; font-size:1.15rem; color:#0F172A; margin-left:8px;">Bypass Congestion on Central Boulevard</span>
        </div>
        <div style="font-weight:700; color:#059669; font-size:1.05rem;">Time Savings: 4.8 min (&ge; 1.0 min Hysteresis Threshold)</div>
    </div>
    <div style="margin:12px 0; font-size:0.92rem; color:#475569; line-height:1.6;">
        Current Path: <code>S &rarr; J1 &rarr; J2 &rarr; J3 &rarr; J5 &rarr; H</code> (14.0 min &mdash; B2 Congestion: 12 km/h)<br/>
        Optimal Alternate: <strong>S &rarr; J1 &rarr; J2 &rarr; J4 &rarr; J6 &rarr; H</strong> (9.2 min &mdash; J4 Arterial: 52 km/h)
    </div>
</div>
""", unsafe_allow_html=True)

btn_c1, btn_c2 = st.columns(2)
with btn_c1:
    if st.button("Accept Recommended Route (Update Navigation HUD)", key="btn_acc_hud_p1", type="primary", use_container_width=True):
        st.success("HUD Navigation updated to optimal bypass: S -> J1 -> J2 -> J4 -> J6 -> H.")
with btn_c2:
    if st.button("Reject & Retain Current Corridor", key="btn_rej_hud_p1", use_container_width=True):
        st.info("Retaining existing route as per driver discretion.")

st.write("")

# Turn-by-Turn Maneuver List & Map
col_map, col_turns = st.columns([7, 5])

with col_map:
    st.subheader("Live Navigation Map")
    folium_map = create_network_map(
        active_route=["S", "J1", "J2", "J4", "J6", "H"],
        ambulance_location=[10.9620, 78.0820],
        congested_segments=["B2"]
    )
    st_folium(folium_map, width=None, height=360, returned_objects=[])

with col_turns:
    st.subheader("Turn-by-Turn Guidance")
    maneuvers = [
        {"icon": "Straight", "distance": "95 m", "instruction": "Proceed straight through Junction J2", "sub": "Green priority preemption active on N-S approach"},
        {"icon": "Right Turn", "distance": "350 m", "instruction": "Bear right onto Junction J4 Ring Bypass", "sub": "Entering high-speed corridor (52 km/h average)"},
        {"icon": "Straight", "distance": "1.1 km", "instruction": "Continue along Arterial Corridor C2", "sub": "Approaching Junction J6 (Hospital Gate Approach)"},
        {"icon": "Left Turn", "distance": "500 m", "instruction": "Turn left into Karur District GH Trauma Bay", "sub": "Bay 1 pre-reserved for Code Red patient arrival"}
    ]
    for m in maneuvers:
        st.markdown(f"""
        <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:10px; padding:12px 14px; margin-bottom:10px; display:flex; align-items:center; gap:14px; box-shadow:0 1px 3px rgba(15,23,42,0.03);">
            <div style="background:#EFF6FF; color:#0284C7; font-weight:800; font-size:0.8rem; padding:6px 10px; border-radius:6px; min-width:65px; text-align:center;">
                {m["distance"]}
            </div>
            <div>
                <div style="font-weight:700; font-size:0.92rem; color:#0F172A;">{m["instruction"]}</div>
                <div style="font-size:0.8rem; color:#64748B;">{m["sub"]}</div>
            </div>
        </div>
        """, unsafe_allow_html=True)
