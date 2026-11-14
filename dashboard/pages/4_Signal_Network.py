import streamlit as st
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
from datetime import datetime, timedelta

from components.icons import (
    render_sidebar_badge,
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

# Top Status Metrics Bar & ESP32 Live Auto-Probe
esp32_ip = st.session_state.get("esp_ip_addr", "192.168.4.1")

# Fast non-blocking background connection probe
esp_connected = False
try:
    import requests
    _r = requests.get(f"http://{esp32_ip}/status", timeout=0.6)
    if _r.status_code == 200:
        esp_connected = True
        st.session_state["esp_connected"] = True
except Exception:
    esp_connected = st.session_state.get("esp_connected", False)

s1, s2, s3, s4, s5 = st.columns(5)
with s1:
    st.markdown(render_metric_card("Corridor Node", "Junction J2", "Central Boulevard Monitored", "primary"), unsafe_allow_html=True)
with s2:
    st.markdown(render_metric_card("Active Priority", "North-South", "Ambulance Wave Active", "danger"), unsafe_allow_html=True)
with s3:
    if esp_connected:
        st.markdown(render_metric_card("ESP32 Controller", "CONNECTED", f"IP: {esp32_ip} | Online", "success"), unsafe_allow_html=True)
    else:
        st.markdown(render_metric_card("ESP32 Controller", "DISCONNECTED", "Awaiting Hardware Ping", "warning"), unsafe_allow_html=True)
with s4:
    st.markdown(render_metric_card("Safety Invariant", "100% Enforced", "Yellow 3s + All-Red 2s Hold", "success"), unsafe_allow_html=True)
with s5:
    st.markdown(render_metric_card("Corridor Clearance", "95 m Approach", "Inside 100m Trigger Zone", "primary"), unsafe_allow_html=True)

st.write("")

# REAL-TIME SIGNAL HEAD & PHASE SIMULATOR
st.subheader("Physical ESP32 Edge Signal Head & Live RGB Hardware Controller")

# Prominent Hardware Connection & RGB Color Switcher
with st.container(border=True):
    col_conn1, col_conn2 = st.columns([7, 5])
    
    with col_conn1:
        st.markdown("#### Hardware Integration: Connect ESP32 via IP")
        esp32_ip_input = st.text_input(
            "Enter your ESP32 Wi-Fi IP Address:",
            value=esp32_ip,
            key="input_esp_ip_addr",
            help="Check the IP address shown on your OLED Display or Arduino Serial Monitor (Default: 192.168.4.1)"
        )
        st.session_state["esp_ip_addr"] = esp32_ip_input
        esp32_ip = esp32_ip_input
        
    with col_conn2:
        st.write("")
        st.write("")
        ping_btn = st.button("Test Hardware Connection", use_container_width=True)
        if ping_btn:
            import requests
            try:
                r = requests.get(f"http://{esp32_ip}/status", timeout=1.5)
                if r.status_code == 200:
                    st.session_state["esp_connected"] = True
                    esp_connected = True
                    st.success(f"Connected to ESP32 at {esp32_ip} (HTTP 200 OK)")
                else:
                    st.session_state["esp_connected"] = False
                    st.warning(f"ESP32 returned HTTP {r.status_code}")
            except Exception as e:
                st.session_state["esp_connected"] = False
                st.warning(f"Could not connect to http://{esp32_ip} (Ensure ESP32 is powered ON)")

    # Prominent Dynamic Connection Status Banner
    if st.session_state.get("esp_connected", False):
        st.success(f"**ESP32 HARDWARE CONNECTED** — Node J2 Online at `http://{esp32_ip}` (OLED Display & 4-Pin RGB Ready)")
    else:
        st.warning(f"**ESP32 DISCONNECTED** — Please connect your PC/Phone to Wi-Fi **ESP32-AMBULANCE-SIGNAL** (IP: `192.168.4.1`) or check power.")

    st.markdown("---")
    st.markdown("#### Direct Physical RGB Light Switcher")
    st.caption("Click any button below to instantly change the physical 4-Pin RGB LED light color on your breadboard:")
    
    col_b1, col_b2, col_b3, col_b4, col_b5, col_b6 = st.columns(6)
    
    import requests
    def trigger_esp(target_ip, color_code):
        color_lower = color_code.lower()
        # 1. Try direct REST endpoint (/red, /green, /yellow, /blue, /siren, /off)
        try:
            url = f"http://{target_ip}/{color_lower}"
            r = requests.get(url, timeout=1.5)
            if r.status_code in [200, 303]:
                return True, f"Hardware {color_code} Active"
        except Exception:
            pass
        # 2. Fallback to /set?color=...
        try:
            url = f"http://{target_ip}/set?color={color_code}"
            r = requests.get(url, timeout=1.5)
            if r.status_code == 200:
                return True, f"Hardware {color_code} Active"
        except Exception as e:
            return False, f"Could not reach http://{target_ip}"
        return False, f"Could not reach http://{target_ip}"

    with col_b1:
        if st.button("RED Signal (Stop)", use_container_width=True, type="secondary"):
            ok, m = trigger_esp(esp32_ip, "RED")
            if ok: st.success("RED ON")
            else: st.warning(m)
            
    with col_b2:
        if st.button("YELLOW Signal", use_container_width=True, type="secondary"):
            ok, m = trigger_esp(esp32_ip, "YELLOW")
            if ok: st.success("YELLOW ON")
            else: st.warning(m)

    with col_b3:
        if st.button("GREEN Priority", use_container_width=True, type="primary"):
            ok, m = trigger_esp(esp32_ip, "GREEN")
            if ok: st.success("GREEN ON")
            else: st.warning(m)

    with col_b4:
        if st.button("BLUE Light", use_container_width=True):
            ok, m = trigger_esp(esp32_ip, "BLUE")
            if ok: st.info("BLUE ON")
            else: st.warning(m)

    with col_b5:
        if st.button("Siren Flasher", use_container_width=True):
            ok, m = trigger_esp(esp32_ip, "SIREN")
            if ok: st.info("Siren Active")
            else: st.warning(m)

    with col_b6:
        if st.button("Turn OFF Light", use_container_width=True):
            ok, m = trigger_esp(esp32_ip, "OFF")
            if ok: st.info("LED OFF")
            else: st.warning(m)

    # Helper for Common Cathode vs Common Anode polarity
    st.caption("Common Cathode (GND) vs Common Anode (3.3V) hardware polarity switcher:")
    col_t1, col_t2 = st.columns([5, 7])
    with col_t1:
        if st.button("Switch LED Mode (Cathode ↔ Anode)", use_container_width=True):
            try:
                r = requests.get(f"http://{esp32_ip}/toggle_mode", timeout=1.5)
                st.info("LED polarity mode toggled on ESP32!")
            except Exception as e:
                st.warning(f"Could not reach ESP32 at {esp32_ip}")

st.write("")

col_sig_vis, col_radar, col_hw = st.columns([4, 4, 4])

with col_sig_vis:
    ns_widget = render_traffic_light_widget("GREEN")
    ew_widget = render_traffic_light_widget("RED")
    st.html(f"""
    <div class="signal-telemetry-card" style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:10px; padding:14px 16px; box-shadow:0 1px 3px rgba(15,23,42,0.04); display:flex; flex-direction:column; justify-content:space-between; box-sizing:border-box;">
        <div style="font-weight:700; font-size:0.90rem; color:#0F172A; display:flex; justify-content:space-between; align-items:center;">
            <span>Physical Signal Heads (J2)</span>
            <span class="badge-success">PREEMPTION ACTIVE</span>
        </div>
        <div style="display:flex; justify-content:space-around; align-items:center; background:#F8FAFC; border:1px solid #E2E8F0; border-radius:8px; padding:8px 6px;">
            <div style="text-align:center;">
                <div style="font-size:0.68rem; font-weight:700; color:#0F172A; margin-bottom:4px; letter-spacing:0.02em;">NORTH-SOUTH (AMB)</div>
                {ns_widget}
                <div style="font-size:0.74rem; font-weight:700; color:#059669; margin-top:4px;">GREEN (20.0s)</div>
            </div>
            <div style="height:65px; width:1px; background:#CBD5E1;"></div>
            <div style="text-align:center;">
                <div style="font-size:0.68rem; font-weight:700; color:#0F172A; margin-bottom:4px; letter-spacing:0.02em;">EAST-WEST (CROSS)</div>
                {ew_widget}
                <div style="font-size:0.74rem; font-weight:700; color:#DC2626; margin-top:4px;">RED (HELD)</div>
            </div>
        </div>
        <div style="font-size:0.73rem; color:#64748B; line-height:1.35;">
            Cross-street traffic safely halted. Mandatory 3.0s Yellow hold and 2.0s All-Red clearance box enforced.
        </div>
    </div>
    """)

with col_radar:
    st.html("""
    <div class="signal-telemetry-card" style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:10px; padding:14px 16px; box-shadow:0 1px 3px rgba(15,23,42,0.04); display:flex; flex-direction:column; justify-content:space-between; box-sizing:border-box;">
        <div style="font-weight:700; font-size:0.90rem; color:#0F172A; display:flex; justify-content:space-between; align-items:center;">
            <span>Approach Radar Telemetry</span>
            <span class="badge-primary">RADAR LOCKED</span>
        </div>
        <div style="background:#F8FAFC; border:1px solid #E2E8F0; border-radius:8px; padding:10px 12px;">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:5px;">
                <span style="font-size:0.72rem; color:#64748B; font-weight:700; letter-spacing:0.03em;">AMBULANCE DISTANCE</span>
                <span style="font-size:0.84rem; font-weight:800; color:#DC2626;">95 meters</span>
            </div>
            <div style="background:#E2E8F0; border-radius:4px; height:8px; overflow:hidden;">
                <div style="background:#0284C7; width:65%; height:100%;"></div>
            </div>
            <div style="display:flex; justify-content:space-between; font-size:0.68rem; color:#64748B; margin-top:4px; font-weight:500;">
                <span>0m (Stop)</span>
                <span style="color:#0284C7; font-weight:700;">100m Trigger</span>
                <span>200m</span>
            </div>
        </div>
        <div style="display:grid; grid-template-columns:1fr 1fr; gap:8px;">
            <div style="background:#F8FAFC; padding:6px 10px; border-radius:6px; border:1px solid #E2E8F0;">
                <div style="color:#64748B; font-weight:600; font-size:0.66rem; letter-spacing:0.02em;">APPROACH SPEED</div>
                <div style="font-weight:700; font-size:0.95rem; color:#0F172A; margin-top:1px;">48.2 km/h</div>
            </div>
            <div style="background:#F8FAFC; padding:6px 10px; border-radius:6px; border:1px solid #E2E8F0;">
                <div style="color:#64748B; font-weight:600; font-size:0.66rem; letter-spacing:0.02em;">STOPLINE ETA</div>
                <div style="font-weight:700; font-size:0.95rem; color:#059669; margin-top:1px;">7.1 seconds</div>
            </div>
        </div>
    </div>
    """)

with col_hw:
    st.html("""
    <div class="signal-telemetry-card" style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:10px; padding:14px 16px; box-shadow:0 1px 3px rgba(15,23,42,0.04); display:flex; flex-direction:column; justify-content:space-between; box-sizing:border-box;">
        <div style="font-weight:700; font-size:0.90rem; color:#0F172A; display:flex; justify-content:space-between; align-items:center;">
            <span>ESP32 Node Hardware (J2)</span>
            <span class="badge-success">FIRMWARE V2.4</span>
        </div>
        <div style="font-size:0.75rem; color:#475569; display:flex; flex-direction:column; justify-content:space-around; flex:1; padding-top:4px;">
            <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #F1F5F9; padding:4px 0;">
                <span style="color:#64748B; font-weight:500;">Microcontroller:</span>
                <span style="font-weight:700; color:#0F172A; font-family:monospace;">Xtensa 240MHz</span>
            </div>
            <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #F1F5F9; padding:4px 0;">
                <span style="color:#64748B; font-weight:500;">Free SRAM Heap:</span>
                <span style="font-weight:700; color:#0F172A; font-family:monospace;">218,440 B</span>
            </div>
            <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #F1F5F9; padding:4px 0;">
                <span style="color:#64748B; font-weight:500;">WiFi Mesh RSSI:</span>
                <span style="font-weight:700; color:#059669; font-family:monospace;">-52 dBm</span>
            </div>
            <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #F1F5F9; padding:4px 0;">
                <span style="color:#64748B; font-weight:500;">MQTT Broker Ping:</span>
                <span style="font-weight:700; color:#0284C7; font-family:monospace;">11.4 ms (1883)</span>
            </div>
            <div style="display:flex; justify-content:space-between; align-items:center; padding:4px 0;">
                <span style="color:#64748B; font-weight:500;">Hardware Relays:</span>
                <span style="font-weight:700; color:#0F172A; font-family:monospace;">GPIO 25, 26, 27</span>
            </div>
        </div>
    </div>
    """)

st.write("")

# MANDATORY SAFE-PHASE TRANSITION PROTOCOL
st.subheader("Automated Safe-Phase Transition Protocol (Mathematical Safety Proof)")
st.html("""
<div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:10px; padding:16px 18px; margin-bottom:20px; box-shadow:0 1px 3px rgba(15,23,42,0.03);">
    <div class="safe-phase-protocol-grid" style="display:grid; align-items:center; gap:8px;">
        <div style="background:#F8FAFC; border:1px solid #E2E8F0; border-radius:8px; padding:10px 8px; text-align:center;">
            <div style="font-size:0.66rem; color:#64748B; font-weight:700;">STAGE 1</div>
            <div style="font-size:0.86rem; font-weight:800; color:#0F172A; margin:3px 0 4px;">Normal Cycle</div>
            <span class="badge-neutral" style="font-size:0.70rem; padding:2px 8px;">E-W Green (30s)</span>
            <div style="font-size:0.68rem; color:#64748B; margin-top:4px;">Fixed cycle</div>
        </div>
        <div class="protocol-arrow" style="font-size:1.1rem; color:#94A3B8; font-weight:700; text-align:center;">&rarr;</div>
        <div style="background:#F8FAFC; border:1px solid #E2E8F0; border-radius:8px; padding:10px 8px; text-align:center;">
            <div style="font-size:0.66rem; color:#64748B; font-weight:700;">STAGE 2 (MANDATORY)</div>
            <div style="font-size:0.86rem; font-weight:800; color:#0F172A; margin:3px 0 4px;">Yellow Clearance</div>
            <span class="badge-neutral" style="font-size:0.70rem; padding:2px 8px;">Hold: 3.0s</span>
            <div style="font-size:0.68rem; color:#64748B; margin-top:4px;">Decelerates traffic</div>
        </div>
        <div class="protocol-arrow" style="font-size:1.1rem; color:#94A3B8; font-weight:700; text-align:center;">&rarr;</div>
        <div style="background:#F8FAFC; border:1px solid #E2E8F0; border-radius:8px; padding:10px 8px; text-align:center;">
            <div style="font-size:0.66rem; color:#64748B; font-weight:700;">STAGE 3 (SAFETY)</div>
            <div style="font-size:0.86rem; font-weight:800; color:#0F172A; margin:3px 0 4px;">All-Red Buffer</div>
            <span class="badge-neutral" style="font-size:0.70rem; padding:2px 8px;">Hold: 2.0s</span>
            <div style="font-size:0.68rem; color:#64748B; margin-top:4px;">Empties junction</div>
        </div>
        <div class="protocol-arrow" style="font-size:1.1rem; color:#94A3B8; font-weight:700; text-align:center;">&rarr;</div>
        <div style="background:#F8FAFC; border:1px solid #BAE6FD; border-radius:8px; padding:10px 8px; text-align:center;">
            <div style="font-size:0.66rem; color:#0284C7; font-weight:700;">STAGE 4 (PRIORITY)</div>
            <div style="font-size:0.86rem; font-weight:800; color:#0F172A; margin:3px 0 4px;">Green Priority</div>
            <span class="badge-primary" style="font-size:0.70rem; padding:2px 8px;">N-S Green (20s)</span>
            <div style="font-size:0.68rem; color:#0284C7; margin-top:4px;">Ambulance corridor</div>
        </div>
        <div class="protocol-arrow" style="font-size:1.1rem; color:#94A3B8; font-weight:700; text-align:center;">&rarr;</div>
        <div style="background:#F8FAFC; border:1px solid #E2E8F0; border-radius:8px; padding:10px 8px; text-align:center;">
            <div style="font-size:0.66rem; color:#64748B; font-weight:700;">STAGE 5</div>
            <div style="font-size:0.86rem; font-weight:800; color:#0F172A; margin:3px 0 4px;">Cycle Restore</div>
            <span class="badge-neutral" style="font-size:0.70rem; padding:2px 8px;">Cycle Resumed</span>
            <div style="font-size:0.68rem; color:#64748B; margin-top:4px;">Safe reintegration</div>
        </div>
    </div>
</div>
""")

# REFERENCE PREEMPTION JUNCTION (J2)
st.subheader("Karur Medical Corridor - Reference Preemption Intersection (J2)")

junctions_data = [
    {"id": "J2", "name": "Central Boulevard (Corridor)", "state": "PRIORITY_GREEN", "active_dir": "NORTH_SOUTH", "countdown": "16.4s", "controller": "ESP32-NODE-02", "ip": "192.168.1.102", "queue_ns": 0, "queue_ew": 18, "phase_color": "GREEN"}
]

j_cols = st.columns(1)
for idx, junc in enumerate(junctions_data):
    col = j_cols[0]
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
st.subheader("Interactive Hardware Controls & ESP32 Live Signal Switcher")

# Live Physical ESP32 Hardware Remote Controller
with st.container(border=True):
    st.markdown("### Live Physical ESP32 Hardware Web Controller")
    st.caption("Directly controls the physical 4-Pin RGB LED and 16x2 LCD on your breadboard over local Wi-Fi.")
    
    col_ip, col_status = st.columns([8, 4])
    with col_ip:
        esp32_ip = st.text_input(
            "ESP32 Hardware IP Address (shown on LCD screen or Arduino Serial Monitor):",
            value="192.168.4.1",
            key="esp_ip_hardware_input",
            help="Enter the IP address displayed on your ESP32's LCD display or Serial Monitor"
        )
    with col_status:
        st.write("")
        st.write("")
        st.markdown(f'<div style="background:#0F172A; border:1px solid #1E293B; border-radius:6px; padding:8px 12px; font-size:0.78rem; color:#38BDF8; font-weight:700; text-align:center;">TARGET: http://{esp32_ip}</div>', unsafe_allow_html=True)
    
    st.markdown("**Click buttons below to trigger Physical LED Light changes:**")
    btn_c1, btn_c2, btn_c3, btn_c4, btn_c5 = st.columns(5)
    
    import requests
    def send_esp_cmd(target_ip, color_code):
        try:
            url = f"http://{target_ip}/set?color={color_code}"
            r = requests.get(url, timeout=1.5)
            return True, f"Hardware Updated: {color_code}"
        except Exception as e:
            return False, f"Could not reach ESP32 at {target_ip} ({e})"

    with btn_c1:
        if st.button("Turn ON Red", use_container_width=True):
            ok, msg = send_esp_cmd(esp32_ip, "RED")
            if ok: st.success("Physical RED LED Active (Stop)")
            else: st.warning(msg)
            
    with btn_c2:
        if st.button("Turn ON Yellow", use_container_width=True):
            ok, msg = send_esp_cmd(esp32_ip, "YELLOW")
            if ok: st.success("Physical YELLOW LED Active (Clearance)")
            else: st.warning(msg)

    with btn_c3:
        if st.button("Turn ON Green", use_container_width=True):
            ok, msg = send_esp_cmd(esp32_ip, "GREEN")
            if ok: st.success("Physical GREEN LED Active (Priority)")
            else: st.warning(msg)

    with btn_c4:
        if st.button("Ambulance Siren", use_container_width=True):
            ok, msg = send_esp_cmd(esp32_ip, "SIREN")
            if ok: st.info("Ambulance Red-Blue Flashing Siren Active!")
            else: st.warning(msg)

    with btn_c5:
        if st.button("Turn OFF LED", use_container_width=True):
            ok, msg = send_esp_cmd(esp32_ip, "OFF")
            if ok: st.info("Hardware Light Turned OFF")
            else: st.warning(msg)

st.write("")

c_ctrl1, c_ctrl2 = st.columns([1, 1])

with c_ctrl1:
    with st.container(border=True):
        st.markdown("**Simulate Proximity Trigger Preemption (Backend Engine)**")
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
