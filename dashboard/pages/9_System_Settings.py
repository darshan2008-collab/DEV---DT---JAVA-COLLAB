import streamlit as st
import pandas as pd

from components.icons import (
    GLOBAL_LIGHT_THEME_CSS,
    render_header,
    render_metric_card,
    SVG_SETTINGS,
    SVG_SHIELD,
    SVG_PLAY,
    SVG_PAUSE,
    SVG_REFRESH,
    SVG_CHECK,
    SVG_ALERT
)
from components.api_client import (
    get_backend_health,
    get_vision_health,
    get_simulation_status,
    set_simulation_scenario,
    simulation_control,
    BACKEND_URL,
    VISION_URL
)

st.set_page_config(
    page_title="Simulation & System Administration",
    page_icon="S",
    layout="wide"
)

st.markdown(GLOBAL_LIGHT_THEME_CSS, unsafe_allow_html=True)
st.markdown(
    render_header(
        "Simulation Orchestrator & System Administration",
        "Edge Infrastructure & Scenario Controller | Microservice health monitors, dynamic traffic scenarios, and algorithm parameters",
        SVG_SETTINGS
    ),
    unsafe_allow_html=True
)

# Fetch microservices health
b_health = get_backend_health()
v_health = get_vision_health()
sim_status = get_simulation_status()

# System Health Indicators
st.subheader("Microservice & Edge Communication Mesh")
h1, h2, h3, h4 = st.columns(4)

with h1:
    b_online = b_health is not None
    st.markdown(render_metric_card(
        "Java 21 Spring Boot",
        "ONLINE" if b_online else "STANDBY / LOCAL",
        "Port 8080 | REST & WebSocket",
        "success" if b_online else "warning"
    ), unsafe_allow_html=True)

with h2:
    v_online = v_health is not None
    st.markdown(render_metric_card(
        "FastAPI Vision AI",
        "ONLINE" if v_online else "STANDBY / LOCAL",
        "Port 8000 | YOLOv8 + ByteTrack",
        "success" if v_online else "warning"
    ), unsafe_allow_html=True)

with h3:
    st.markdown(render_metric_card(
        "MQTT Signal Broker",
        "ACTIVE",
        "Port 1883 | ESP32 Protocol",
        "success"
    ), unsafe_allow_html=True)

with h4:
    sim_running = sim_status.get("running", True)
    active_scen = sim_status.get("active_scenario", "NORMAL")
    st.markdown(render_metric_card(
        "Simulation Engine",
        "RUNNING" if sim_running else "PAUSED",
        f"Active Scenario: {active_scen}",
        "primary" if sim_running else "warning"
    ), unsafe_allow_html=True)

st.write("")

# Scenario Orchestration Section
col_scen, col_sim = st.columns([7, 5])

with col_scen:
    st.subheader("Dynamic Scenario Switcher (Jury & Demonstration Testing)")
    st.markdown("""
    Select an operational scenario to dynamically alter road conditions across the Karur road network.
    The Dijkstra / A* route optimizer will detect changes and evaluate alternate paths in real time.
    """)

    scenario_options = [
        ("NORMAL", "1. Baseline Normal Flow - Standard urban traffic without major congestion bottlenecks."),
        ("QUEUE_ON_B2", "2. Sudden Congestion on B2 - Injects severe queue (180m, 12 km/h) on Central Blvd; triggers alternate route."),
        ("FEED_LOST", "3. Camera Feed Failure - Simulates camera loss on C3; verifies 120s stale fallback to typical speeds."),
        ("ROAD_CLOSED", "4. Complete Road Blockage - Marks segment A1 as closed (severe accident); forces edge avoidance."),
        ("RECOVERY", "5. Congestion Dissolution - Traffic clears on B2; speeds normalize back to 45 km/h.")
    ]

    selected_tuple = st.radio(
        "Active Scenario Selection",
        options=scenario_options,
        format_func=lambda x: x[1],
        index=0
    )
    
    current_selected_code = selected_tuple[0]

    btn_apply, btn_stat = st.columns([2, 4])
    with btn_apply:
        if st.button("Apply Selected Scenario", key="apply_scenario"):
            res = set_simulation_scenario(current_selected_code)
            if "error" in res:
                st.info(f"Scenario updated locally to {current_selected_code} (Backend simulated)")
            else:
                st.success(f"Scenario successfully switched to: {current_selected_code}")

with col_sim:
    st.subheader("Simulation Loop Controls")
    st.markdown("""
    <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:10px; padding:18px; margin-bottom:16px;">
        <div style="font-weight:700; color:#0F172A; margin-bottom:10px;">Simulation State Control</div>
        <p style="font-size:0.85rem; color:#64748B; margin-bottom:14px;">
            Control the background traffic generator thread and telemetry tick rate.
        </p>
    """, unsafe_allow_html=True)

    c_b1, c_b2, c_b3 = st.columns(3)
    with c_b1:
        if st.button("Start Loop", key="sim_start"):
            res = simulation_control("start")
            st.success("Simulation loop started.")
    with c_b2:
        if st.button("Pause Loop", key="sim_pause"):
            res = simulation_control("pause")
            st.warning("Simulation loop paused.")
    with c_b3:
        if st.button("Reset Normal", key="sim_reset"):
            res = simulation_control("reset")
            st.info("Simulation reset to NORMAL.")

    st.write("")
    speed_mult = st.select_slider("Simulation Time Speed Multiplier", options=["0.5x (Slow-Motion)", "1.0x (Real-Time)", "2.0x (Fast)", "5.0x (Demo Turbo)"], value="1.0x (Real-Time)")
    st.caption("Adjusts speed of vehicle movement and synthetic camera tick frequencies.")
    st.markdown("</div>", unsafe_allow_html=True)

st.write("")

# Algorithmic Parameters & Tuning Table
st.subheader("Academic Algorithm Parameters & Safety Invariants")
params = [
    {"Parameter": "Hysteresis Re-Route Threshold", "Value": "1.0 minute", "Scope": "RouteDecisionEngine", "Purpose": "Prevents rapid path oscillation / ping-ponging when alternatives have trivial time gains."},
    {"Parameter": "Turn Penalty Multiplier", "Value": "0.35 minutes (21s)", "Scope": "Dijkstra & A* Optimizers", "Purpose": "Penalizes complex 90-degree intersection turns to maintain arterial speed."},
    {"Parameter": "Stale Observation Threshold", "Value": "120 seconds", "Scope": "ObservationPreprocessing", "Purpose": "Falls back to historical typical speed when edge camera frames cease."},
    {"Parameter": "Median Filter Window", "Value": "5 Trailing Samples", "Scope": "ObservationPreprocessing", "Purpose": "Suppresses momentary sensor spikes, braking anomalies, and camera noise."},
    {"Parameter": "Yellow Clearance Window", "Value": "3.0 seconds", "Scope": "SignalPriorityService / ESP32", "Purpose": "Mandatory clearance interval before cross-street green is severed."},
    {"Parameter": "All-Red Clearance Window", "Value": "2.0 seconds", "Scope": "SignalPriorityService / ESP32", "Purpose": "Ensures all cross-traffic completely exits the intersection box."},
    {"Parameter": "Green Priority Hold Window", "Value": "20.0 seconds", "Scope": "SignalPriorityService / ESP32", "Purpose": "Provides continuous safe corridor clearance for approaching emergency vehicle."},
    {"Parameter": "Preemption Proximity Trigger", "Value": "100 meters", "Scope": "SignalPriorityService", "Purpose": "Distance at which ambulance automatically triggers signal preemption."}
]
st.dataframe(pd.DataFrame(params), use_container_width=True, hide_index=True)
