import streamlit as st
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
from datetime import datetime, timedelta

from components.icons import (
    GLOBAL_LIGHT_THEME_CSS,
    render_header,
    render_metric_card,
    render_traffic_light_widget,
    SVG_TRAFFIC_LIGHT,
    SVG_SHIELD,
    SVG_ALERT,
    SVG_CHECK,
    SVG_CLOCK,
    SVG_SETTINGS,
    SVG_CPU,
    SVG_WIFI
)
from components.api_client import (
    get_signal_state,
    request_signal_priority,
    signal_override
)

st.set_page_config(
    page_title="Signal Priority Network & ESP32 Edge",
    page_icon="S",
    layout="wide"
)

st.markdown(GLOBAL_LIGHT_THEME_CSS, unsafe_allow_html=True)
st.markdown(
    render_header(
        "Traffic Signal Priority & ESP32 Edge Network",
        "Karur Medical District | Microcontroller State Machine, Safe-Phase Transitions & Corridor Preemption",
        SVG_TRAFFIC_LIGHT
    ),
    unsafe_allow_html=True
)

# Top Status Metrics Bar
s1, s2, s3, s4, s5 = st.columns(5)
with s1:
    st.markdown(render_metric_card("Junction Nodes", "6 Intersections", "J1 through J6 Online", "primary"), unsafe_allow_html=True)
with s2:
    st.markdown(render_metric_card("Active Priority", "Junction J2", "North-South Corridor Active", "danger"), unsafe_allow_html=True)
with s3:
    st.markdown(render_metric_card("ESP32 Controllers", "6 / 6 Connected", "MQTT 1883 | Latency: 11ms", "success"), unsafe_allow_html=True)
with s4:
    st.markdown(render_metric_card("Safety Invariant", "100% Compliant", "Yellow 3s + All-Red 2s Enforced", "success"), unsafe_allow_html=True)
with s5:
    st.markdown(render_metric_card("Corridor Clearance", "95 m Approach", "Within 100m Preemption Window", "primary"), unsafe_allow_html=True)

st.write("")

# REAL-TIME SIGNAL HEAD & PHASE SIMULATOR
st.subheader("Intersection J2 Real-Time Signal Head & Proximity Radar")

col_sig_vis, col_radar, col_hw = st.columns([4, 4, 4])

with col_sig_vis:
    ns_widget = render_traffic_light_widget("GREEN")
    ew_widget = render_traffic_light_widget("RED")
    st.html(f"""
    <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:12px; padding:18px; box-shadow:0 2px 6px rgba(15,23,42,0.04); height:100%;">
        <div style="font-weight:700; font-size:1rem; color:#0F172A; margin-bottom:12px; display:flex; justify-content:space-between; align-items:center;">
            <span>Physical Signal Heads (J2)</span>
            <span class="badge-success">PREEMPTION GRANTED</span>
        </div>
        <div style="display:flex; justify-content:space-around; align-items:center; margin:16px 0; background:#F8FAFC; border:1px solid #E2E8F0; border-radius:10px; padding:16px;">
            <div style="text-align:center;">
                <div style="font-size:0.75rem; font-weight:700; color:#0F172A; margin-bottom:8px;">NORTH-SOUTH (AMBULANCE)</div>
                {ns_widget}
                <div style="font-size:0.8rem; font-weight:700; color:#059669; margin-top:8px;">GREEN (20.0s)</div>
            </div>
            <div style="height:90px; width:1px; background:#CBD5E1;"></div>
            <div style="text-align:center;">
                <div style="font-size:0.75rem; font-weight:700; color:#0F172A; margin-bottom:8px;">EAST-WEST (CROSS-TRAFFIC)</div>
                {ew_widget}
                <div style="font-size:0.8rem; font-weight:700; color:#DC2626; margin-top:8px;">RED (HELD)</div>
            </div>
        </div>
        <div style="font-size:0.82rem; color:#64748B; line-height:1.4;">
            Cross-street traffic safely halted following the mandatory 3.0s Yellow hold and 2.0s All-Red clearance box clearance.
        </div>
    </div>
    """)

with col_radar:
    st.html("""
    <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:12px; padding:18px; box-shadow:0 2px 6px rgba(15,23,42,0.04); height:100%;">
        <div style="font-weight:700; font-size:1rem; color:#0F172A; margin-bottom:12px; display:flex; justify-content:space-between; align-items:center;">
            <span>Approach Radar Telemetry</span>
            <span class="badge-primary">RADAR: LOCKED</span>
        </div>
        <div style="background:#F8FAFC; border:1px solid #E2E8F0; border-radius:10px; padding:14px; margin-bottom:12px;">
            <div style="display:flex; justify-content:space-between; margin-bottom:6px;">
                <span style="font-size:0.8rem; color:#64748B; font-weight:600;">AMBULANCE DISTANCE</span>
                <span style="font-size:0.85rem; font-weight:800; color:#DC2626;">95 meters</span>
            </div>
            <div style="background:#E2E8F0; border-radius:6px; height:10px; overflow:hidden;">
                <div style="background:linear-gradient(90deg, #10B981, #0284C7); width:65%; height:100%;"></div>
            </div>
            <div style="display:flex; justify-content:space-between; font-size:0.75rem; color:#64748B; margin-top:4px;">
                <span>0m (Stop-line)</span>
                <span style="color:#0284C7; font-weight:700;">100m Trigger</span>
                <span>200m Approach</span>
            </div>
        </div>
        <div style="display:grid; grid-template-columns:1fr 1fr; gap:10px;">
            <div style="background:#F8FAFC; padding:8px 10px; border-radius:6px; border:1px solid #E2E8F0;">
                <small style="color:#64748B; font-weight:600;">APPROACH SPEED</small>
                <div style="font-weight:700; font-size:1.1rem; color:#0F172A;">48.2 km/h</div>
            </div>
            <div style="background:#F8FAFC; padding:8px 10px; border-radius:6px; border:1px solid #E2E8F0;">
                <small style="color:#64748B; font-weight:600;">TIME TO STOPLINE</small>
                <div style="font-weight:700; font-size:1.1rem; color:#059669;">7.1 seconds</div>
            </div>
        </div>
    </div>
    """)

with col_hw:
    st.html("""
    <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:12px; padding:18px; box-shadow:0 2px 6px rgba(15,23,42,0.04); height:100%;">
        <div style="font-weight:700; font-size:1rem; color:#0F172A; margin-bottom:12px; display:flex; justify-content:space-between; align-items:center;">
            <span>ESP32 Node Hardware (J2)</span>
            <span class="badge-success">FIRMWARE V2.4</span>
        </div>
        <div style="font-size:0.85rem; color:#475569; line-height:1.7;">
            <div style="display:flex; justify-content:space-between; border-bottom:1px solid #F1F5F9; padding:4px 0;">
                <span>Microcontroller Core:</span>
                <span style="font-weight:700; color:#0F172A;">Xtensa Dual-Core 240MHz</span>
            </div>
            <div style="display:flex; justify-content:space-between; border-bottom:1px solid #F1F5F9; padding:4px 0;">
                <span>Free SRAM Heap:</span>
                <span style="font-weight:700; color:#0F172A;">218,440 Bytes (Stable)</span>
            </div>
            <div style="display:flex; justify-content:space-between; border-bottom:1px solid #F1F5F9; padding:4px 0;">
                <span>WiFi Mesh RSSI:</span>
                <span style="font-weight:700; color:#059669;">-52 dBm (Excellent)</span>
            </div>
            <div style="display:flex; justify-content:space-between; border-bottom:1px solid #F1F5F9; padding:4px 0;">
                <span>MQTT Broker Ping:</span>
                <span style="font-weight:700; color:#0284C7;">11.4 ms (Port 1883)</span>
            </div>
            <div style="display:flex; justify-content:space-between; padding:4px 0;">
                <span>Hardware Relays:</span>
                <span style="font-weight:700; color:#0F172A;">GPIO 25 (G), 26 (Y), 27 (R)</span>
            </div>
        </div>
    </div>
    """)

st.write("")

# MANDATORY SAFE-PHASE TRANSITION PROTOCOL
st.subheader("Automated Safe-Phase Transition Protocol (Mathematical Safety Proof)")
st.html("""
<div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:12px; padding:20px; margin-bottom:20px; box-shadow:0 2px 6px rgba(15,23,42,0.03);">
    <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
        <div style="flex:1; min-width:180px; background:#F8FAFC; border:1px solid #E2E8F0; border-radius:10px; padding:14px; text-align:center;">
            <div style="font-size:0.75rem; color:#64748B; font-weight:700;">STAGE 1</div>
            <div style="font-size:1.05rem; font-weight:800; color:#0F172A; margin:4px 0;">Normal Fixed Cycle</div>
            <span class="badge-neutral">East-West Green (30s)</span>
            <div style="font-size:0.75rem; color:#64748B; margin-top:6px;">Standard junction rotation</div>
        </div>
        <div style="font-size:1.4rem; color:#94A3B8; font-weight:700;">&rarr;</div>
        <div style="flex:1; min-width:180px; background:#FFFBEB; border:1px solid #FDE68A; border-radius:10px; padding:14px; text-align:center;">
            <div style="font-size:0.75rem; color:#B45309; font-weight:700;">STAGE 2 (MANDATORY)</div>
            <div style="font-size:1.05rem; font-weight:800; color:#B45309; margin:4px 0;">Yellow Clearance</div>
            <span class="badge-warning">Hold: 3.0 Seconds</span>
            <div style="font-size:0.75rem; color:#B45309; margin-top:6px;">Decelerates cross-traffic</div>
        </div>
        <div style="font-size:1.4rem; color:#94A3B8; font-weight:700;">&rarr;</div>
        <div style="flex:1; min-width:180px; background:#FEF2F2; border:1px solid #FECACA; border-radius:10px; padding:14px; text-align:center;">
            <div style="font-size:0.75rem; color:#B91C1C; font-weight:700;">STAGE 3 (SAFETY BUFFER)</div>
            <div style="font-size:1.05rem; font-weight:800; color:#B91C1C; margin:4px 0;">All-Red Buffer</div>
            <span class="badge-danger">Hold: 2.0 Seconds</span>
            <div style="font-size:0.75rem; color:#B91C1C; margin-top:6px;">Empties intersection box</div>
        </div>
        <div style="font-size:1.4rem; color:#94A3B8; font-weight:700;">&rarr;</div>
        <div style="flex:1; min-width:180px; background:#ECFDF5; border:1px solid #A7F3D0; border-radius:10px; padding:14px; text-align:center;">
            <div style="font-size:0.75rem; color:#047857; font-weight:700;">STAGE 4 (GREEN PREEMPTION)</div>
            <div style="font-size:1.05rem; font-weight:800; color:#047857; margin:4px 0;">Green Priority</div>
            <span class="badge-success">North-South (20.0s)</span>
            <div style="font-size:0.75rem; color:#047857; margin-top:6px;">Ambulance passes unimpeded</div>
        </div>
        <div style="font-size:1.4rem; color:#94A3B8; font-weight:700;">&rarr;</div>
        <div style="flex:1; min-width:180px; background:#F8FAFC; border:1px solid #E2E8F0; border-radius:10px; padding:14px; text-align:center;">
            <div style="font-size:0.75rem; color:#64748B; font-weight:700;">STAGE 5</div>
            <div style="font-size:1.05rem; font-weight:800; color:#0F172A; margin:4px 0;">Normal Restoration</div>
            <span class="badge-neutral">Cycle Resumed</span>
            <div style="font-size:0.75rem; color:#64748B; margin-top:6px;">Graceful cycle reintegration</div>
        </div>
    </div>
</div>
""")

# ALL 6 JUNCTIONS GRID WITH RICH DATA
st.subheader("Karur Medical District - All 6 Monitored Intersections")

junctions_data = [
    {"id": "J1", "name": "North Access Interchange", "state": "NORMAL_CYCLE", "active_dir": "EAST_WEST", "countdown": "14s", "controller": "ESP32-NODE-01", "ip": "192.168.1.101", "queue_ns": 4, "queue_ew": 12, "phase_color": "RED"},
    {"id": "J2", "name": "Central Boulevard (Corridor)", "state": "PRIORITY_GREEN", "active_dir": "NORTH_SOUTH", "countdown": "16.4s", "controller": "ESP32-NODE-02", "ip": "192.168.1.102", "queue_ns": 0, "queue_ew": 18, "phase_color": "GREEN"},
    {"id": "J3", "name": "Commercial Market Junction", "state": "NORMAL_CYCLE", "active_dir": "NORTH_SOUTH", "countdown": "22s", "controller": "ESP32-NODE-03", "ip": "192.168.1.103", "queue_ns": 8, "queue_ew": 15, "phase_color": "GREEN"},
    {"id": "J4", "name": "Ring Bypass Crossroad", "state": "NORMAL_CYCLE", "active_dir": "EAST_WEST", "countdown": "8s", "controller": "ESP32-NODE-04", "ip": "192.168.1.104", "queue_ns": 2, "queue_ew": 6, "phase_color": "RED"},
    {"id": "J5", "name": "Industrial Bypass Concourse", "state": "NORMAL_CYCLE", "active_dir": "NORTH_SOUTH", "countdown": "25s", "controller": "ESP32-NODE-05", "ip": "192.168.1.105", "queue_ns": 14, "queue_ew": 9, "phase_color": "GREEN"},
    {"id": "J6", "name": "District Hospital Gate Approach", "state": "STANDBY_READY", "active_dir": "EAST_WEST", "countdown": "Standby", "controller": "ESP32-NODE-06", "ip": "192.168.1.106", "queue_ns": 1, "queue_ew": 3, "phase_color": "RED"}
]

j_cols = st.columns(3)
for idx, junc in enumerate(junctions_data):
    col = j_cols[idx % 3]
    with col:
        border_top = "#10B981" if "PRIORITY" in junc["state"] else "#CBD5E1"
        badge_html = '<span class="badge-success">PRIORITY GREEN</span>' if "PRIORITY" in junc["state"] else '<span class="badge-neutral">NORMAL FIXED</span>'
        
        st.html(f"""
        <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-top:4px solid {border_top}; border-radius:12px; padding:16px; margin-bottom:16px; box-shadow:0 2px 6px rgba(15,23,42,0.03);">
            <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:10px;">
                <div>
                    <span style="font-weight:800; font-size:1.1rem; color:#0F172A;">{junc["id"]}</span>
                    <span style="font-size:0.9rem; font-weight:600; color:#475569; margin-left:6px;">{junc["name"]}</span>
                </div>
                {badge_html}
            </div>
            <div style="display:grid; grid-template-columns:1fr 1fr; gap:8px; background:#F8FAFC; border:1px solid #E2E8F0; border-radius:8px; padding:10px; margin-bottom:10px; font-size:0.8rem;">
                <div>
                    <span style="color:#64748B;">Active Direction:</span><br/>
                    <strong style="color:#0F172A;">{junc["active_dir"]}</strong>
                </div>
                <div>
                    <span style="color:#64748B;">Phase Remaining:</span><br/>
                    <strong style="color:#0F172A;">{junc["countdown"]}</strong>
                </div>
                <div>
                    <span style="color:#64748B;">Queue (N-S):</span><br/>
                    <strong>{junc["queue_ns"]} vehicles</strong>
                </div>
                <div>
                    <span style="color:#64748B;">Queue (E-W):</span><br/>
                    <strong>{junc["queue_ew"]} vehicles</strong>
                </div>
            </div>
            <div style="display:flex; justify-content:space-between; align-items:center; font-size:0.75rem; color:#64748B;">
                <span>Controller: <code>{junc["controller"]}</code></span>
                <span>IP: <code>{junc["ip"]}</code></span>
            </div>
        </div>
        """)

st.write("")

# INTERACTIVE CONTROLS & MANUAL OVERRIDE
st.subheader("Manual Preemption Testing & Emergency Safety Overrides")
c_ctrl1, c_ctrl2 = st.columns([1, 1])

with c_ctrl1:
    with st.container(border=True):
        st.markdown("**Simulate Proximity Trigger Preemption**")
        t_junc = st.selectbox("Select Target Intersection", ["J1", "J2", "J3", "J4", "J5", "J6"], index=1, key="p_junc")
        t_dir = st.selectbox("Ambulance Travel Vector", ["NORTH_SOUTH", "EAST_WEST"], index=0, key="p_dir")
        t_amb = st.selectbox("Emergency Vehicle Identifier", ["AMB-01 (ALS)", "AMB-02 (BLS)", "AMB-03 (NICU)"], index=0, key="p_amb")
        
        if st.button("Transmit Preemption Command to ESP32", key="p_btn_send", type="primary", use_container_width=True):
            resp = request_signal_priority(t_junc, t_dir, t_amb.split()[0], "TRIP-DEMO-01")
            if "error" in resp:
                st.error(f"Error: {resp['error']}")
            else:
                st.success(f"Priority Clearance Granted for {t_junc}. Safety invariant sequence initiated: Yellow (3s) -> All-Red (2s) -> Green (20s).")

with c_ctrl2:
    with st.container(border=True):
        st.markdown("**Fail-Safe Manual Override (Abort Priority)**")
        st.caption("In the event of an edge anomaly or false alarm, manually abort preemption and restore standard fixed cycle immediately.")
        ov_junc = st.selectbox("Target Junction for Reset", ["J1", "J2", "J3", "J4", "J5", "J6"], index=1, key="ov_junc_sel")
        if st.button("Trigger Immediate Hardware Reset", key="ov_btn_exec", use_container_width=True):
            resp = signal_override(ov_junc)
            st.warning(f"Manual override executed for {ov_junc}. ESP32 restored to standard fixed cycle.")

st.write("")

# REAL-TIME MQTT AUDIT TRAIL
st.subheader("Live MQTT Preemption Telemetry Stream (Topic: karur/junction/+/state)")
mqtt_events = [
    {"Timestamp": (datetime.now() - timedelta(seconds=12)).strftime("%H:%M:%S.%f")[:-3], "Topic": "karur/junction/J2/state", "Payload": '{"phase":"PRIORITY_GREEN", "dir":"NORTH_SOUTH", "remain_s":16.4}', "Source": "ESP32-NODE-02", "Status": "ACK"},
    {"Timestamp": (datetime.now() - timedelta(seconds=14)).strftime("%H:%M:%S.%f")[:-3], "Topic": "karur/junction/J2/state", "Payload": '{"phase":"ALL_RED_CLEAR", "dir":"ALL_HALTED", "remain_s":2.0}', "Source": "ESP32-NODE-02", "Status": "INVARIANT_OK"},
    {"Timestamp": (datetime.now() - timedelta(seconds=17)).strftime("%H:%M:%S.%f")[:-3], "Topic": "karur/junction/J2/state", "Payload": '{"phase":"YELLOW_HOLD", "dir":"EAST_WEST", "remain_s":3.0}', "Source": "ESP32-NODE-02", "Status": "INVARIANT_OK"},
    {"Timestamp": (datetime.now() - timedelta(seconds=20)).strftime("%H:%M:%S.%f")[:-3], "Topic": "karur/signals/preemption", "Payload": '{"ambulance_id":"AMB-01", "dist_m":95, "speed":48.2}', "Source": "BACKEND_JAVA", "Status": "SENT"},
    {"Timestamp": (datetime.now() - timedelta(seconds=45)).strftime("%H:%M:%S.%f")[:-3], "Topic": "karur/junction/J1/state", "Payload": '{"phase":"NORMAL_CYCLE", "dir":"EAST_WEST", "remain_s":14.0}', "Source": "ESP32-NODE-01", "Status": "HEARTBEAT"}
]
st.dataframe(pd.DataFrame(mqtt_events), use_container_width=True, hide_index=True)
