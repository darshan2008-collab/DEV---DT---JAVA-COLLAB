import streamlit as st
import pandas as pd
import plotly.express as px
from datetime import datetime, timedelta

from components.icons import (
    GLOBAL_LIGHT_THEME_CSS,
    render_header,
    render_metric_card,
    SVG_HOSPITAL,
    SVG_AMBULANCE,
    SVG_ALERT,
    SVG_CHECK,
    SVG_CLOCK,
    SVG_SHIELD
)
from components.api_client import get_hospitals, get_trips, get_ambulances

st.set_page_config(
    page_title="Hospital Inflow & Trauma Console",
    page_icon="H",
    layout="wide"
)

st.markdown(GLOBAL_LIGHT_THEME_CSS, unsafe_allow_html=True)
st.markdown(
    render_header(
        "Hospital Inflow & Emergency Trauma Console",
        "Karur Medical District | Inbound ambulance telemetry, triage classification, and trauma team readiness",
        SVG_HOSPITAL
    ),
    unsafe_allow_html=True
)

# Fetch data from backend or fallback to demo state
hospitals = get_hospitals()
if not hospitals:
    hospitals = [
        {"id": "HOSP-01", "name": "Karur District General Hospital", "address": "Junction 6, Medical District", "trauma_bays": 4, "icu_beds": 8},
        {"id": "HOSP-02", "name": "Kauvery Emergency Care Center", "address": "North Bypass Road", "trauma_bays": 2, "icu_beds": 4},
        {"id": "HOSP-03", "name": "Apollo Specialty Trauma Unit", "address": "South Ring Road", "trauma_bays": 3, "icu_beds": 6}
    ]

trips = get_trips()
ambulances = get_ambulances()

# Top Controls Bar
col_hosp, col_refresh, col_alert = st.columns([3, 1, 2])
with col_hosp:
    selected_hosp_id = st.selectbox(
        "Target Receiving Facility",
        options=[h["id"] for h in hospitals],
        format_func=lambda hid: next((h["name"] for h in hospitals if h["id"] == hid), hid)
    )

with col_refresh:
    st.write("")
    st.write("")
    if st.button("Refresh Telemetry"):
        st.rerun()

with col_alert:
    st.markdown("""
    <div style="background:#EFF6FF; border:1px solid #BFDBFE; border-radius:8px; padding:10px 14px; margin-top:14px; display:flex; align-items:center; gap:8px;">
        <span class="badge-primary">STATUS: STANDBY ALERT</span>
        <span style="font-size:0.85rem; color:#1E40AF; font-weight:500;">Trauma Bay 1 Reserved for Inbound AMB-01</span>
    </div>
    """, unsafe_allow_html=True)

st.write("")

# Capacity & Readiness Metric Cards
m1, m2, m3, m4 = st.columns(4)
with m1:
    st.markdown(render_metric_card("Inbound Ambulances", "2 Units", "1 Critical / 1 Urgent", "danger"), unsafe_allow_html=True)
with m2:
    st.markdown(render_metric_card("Trauma Bays Available", "2 / 4", "Bay 1 reserved, Bay 2 prepped", "warning"), unsafe_allow_html=True)
with m3:
    st.markdown(render_metric_card("ICU Bed Capacity", "5 Available", "3 Reserved for surgery transition", "success"), unsafe_allow_html=True)
with m4:
    st.markdown(render_metric_card("OR / Surgical Status", "OR-1 Ready", "Cardiothoracic team paged", "primary"), unsafe_allow_html=True)

st.write("")

# Inbound Transports Section
col_left, col_right = st.columns([7, 5])

with col_left:
    st.subheader("Active Inbound Telemetry")
    
    # Ambulance 1: Critical Code Red
    with st.container():
        st.markdown("""
        <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-left:5px solid #EF4444; border-radius:10px; padding:18px; margin-bottom:16px; box-shadow:0 1px 3px rgba(15,23,42,0.05);">
            <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:12px;">
                <div>
                    <span class="badge-danger">CODE RED - CRITICAL</span>
                    <span style="font-size:1.15rem; font-weight:700; color:#0F172A; margin-left:8px;">AMB-01 (Advanced Life Support)</span>
                </div>
                <div style="text-align:right;">
                    <div style="font-size:1.3rem; font-weight:800; color:#DC2626;">ETA: 4.1 min</div>
                    <div style="font-size:0.8rem; color:#64748B;">Distance: 1.85 km | Speed: 42 km/h</div>
                </div>
            </div>
            <div style="display:grid; grid-template-columns:repeat(4, 1fr); gap:10px; background:#F8FAFC; border:1px solid #E2E8F0; border-radius:8px; padding:12px; margin-bottom:14px;">
                <div>
                    <div style="font-size:0.75rem; color:#64748B; font-weight:600; text-transform:uppercase;">Heart Rate</div>
                    <div style="font-size:1.1rem; font-weight:700; color:#DC2626;">122 bpm</div>
                </div>
                <div>
                    <div style="font-size:0.75rem; color:#64748B; font-weight:600; text-transform:uppercase;">Blood Pressure</div>
                    <div style="font-size:1.1rem; font-weight:700; color:#0F172A;">92 / 58 mmHg</div>
                </div>
                <div>
                    <div style="font-size:0.75rem; color:#64748B; font-weight:600; text-transform:uppercase;">Oxygen Saturation</div>
                    <div style="font-size:1.1rem; font-weight:700; color:#D97706;">91 % SpO2</div>
                </div>
                <div>
                    <div style="font-size:0.75rem; color:#64748B; font-weight:600; text-transform:uppercase;">Glasgow Coma</div>
                    <div style="font-size:1.1rem; font-weight:700; color:#0F172A;">GCS 10 (Moderate)</div>
                </div>
            </div>
            <div style="display:flex; justify-content:space-between; align-items:center; font-size:0.85rem; color:#475569;">
                <div>Route Corridor: <b>S &rarr; J1 &rarr; J2 &rarr; J4 &rarr; J6 (Hospital Gate)</b></div>
                <div>Preemption: <b>J2 Green Cleared (95m away)</b></div>
            </div>
        </div>
        """, unsafe_allow_html=True)
        
        # Live Lead II Cardiac Rhythm Waveform
        ecg_x = list(range(100))
        ecg_y = [0] * 100
        for i in range(10, 100, 20):
            ecg_y[i] = 0.2
            ecg_y[i+1] = -0.3
            ecg_y[i+2] = 1.8   # R wave peak
            ecg_y[i+3] = -0.6  # S wave
            ecg_y[i+4] = 0.4   # T wave
        fig_ecg = px.line(x=ecg_x, y=ecg_y, title="Lead II Continuous Cardiac Rhythm (AMB-01 Telemetry Stream)")
        fig_ecg.update_traces(line_color="#DC2626", line_width=2)
        fig_ecg.update_layout(
            height=130, margin=dict(l=15, r=15, t=30, b=10),
            xaxis_visible=False, yaxis_visible=False,
            plot_bgcolor="#FFFBFB", paper_bgcolor="#FFFBFB"
        )
        st.plotly_chart(fig_ecg, use_container_width=True)
        
        btn_c1, btn_c2, btn_c3 = st.columns(3)
        with btn_c1:
            if st.button("Confirm Trauma Bay 1 Prep", key="prep_tb1"):
                st.success("Trauma Bay 1 status updated to READY. Airway and IV crash carts deployed.")
        with btn_c2:
            if st.button("Page On-Call Neurosurgeon", key="page_neuro"):
                st.info("Emergency pager alert dispatched to Dr. R. Sundaram (Neurotrauma).")
        with btn_c3:
            if st.button("Request Blood Bank Crossmatch", key="req_blood"):
                st.warning("4 Units O-Negative whole blood requested at Blood Bank.")

    st.write("")

    # Ambulance 2: Code Yellow
    with st.container():
        st.markdown("""
        <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-left:5px solid #F59E0B; border-radius:10px; padding:18px; margin-bottom:16px; box-shadow:0 1px 3px rgba(15,23,42,0.05);">
            <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:12px;">
                <div>
                    <span class="badge-warning">CODE YELLOW - URGENT</span>
                    <span style="font-size:1.15rem; font-weight:700; color:#0F172A; margin-left:8px;">AMB-02 (Basic Life Support)</span>
                </div>
                <div style="text-align:right;">
                    <div style="font-size:1.3rem; font-weight:800; color:#D97706;">ETA: 11.4 min</div>
                    <div style="font-size:0.8rem; color:#64748B;">Distance: 4.80 km | Speed: 38 km/h</div>
                </div>
            </div>
            <div style="display:grid; grid-template-columns:repeat(4, 1fr); gap:10px; background:#F8FAFC; border:1px solid #E2E8F0; border-radius:8px; padding:12px; margin-bottom:14px;">
                <div>
                    <div style="font-size:0.75rem; color:#64748B; font-weight:600; text-transform:uppercase;">Heart Rate</div>
                    <div style="font-size:1.1rem; font-weight:700; color:#0F172A;">86 bpm</div>
                </div>
                <div>
                    <div style="font-size:0.75rem; color:#64748B; font-weight:600; text-transform:uppercase;">Blood Pressure</div>
                    <div style="font-size:1.1rem; font-weight:700; color:#0F172A;">128 / 82 mmHg</div>
                </div>
                <div>
                    <div style="font-size:0.75rem; color:#64748B; font-weight:600; text-transform:uppercase;">Oxygen Saturation</div>
                    <div style="font-size:1.1rem; font-weight:700; color:#059669;">97 % SpO2</div>
                </div>
                <div>
                    <div style="font-size:0.75rem; color:#64748B; font-weight:600; text-transform:uppercase;">Classification</div>
                    <div style="font-size:1.1rem; font-weight:700; color:#0F172A;">Closed Fracture</div>
                </div>
            </div>
            <div style="display:flex; justify-content:space-between; align-items:center; font-size:0.85rem; color:#475569;">
                <div>Route Corridor: <b>East Arterial &rarr; Ring Road &rarr; J6</b></div>
                <div>Preemption: <b>Scheduled at J5 (in 6 min)</b></div>
            </div>
        </div>
        """, unsafe_allow_html=True)
        
        btn_y1, btn_y2 = st.columns(2)
        with btn_y1:
            if st.button("Assign ER Examination Bed 3", key="assign_bed3"):
                st.success("ER Examination Bed 3 allocated to AMB-02.")
        with btn_y2:
            if st.button("Notify Orthopedics Duty Registrar", key="notify_ortho"):
                st.info("Orthopedic duty team alerted.")

with col_right:
    st.subheader("Trauma Team Readiness Checklist")
    with st.container():
        st.markdown("""
        <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:10px; padding:16px; margin-bottom:16px;">
            <div style="font-size:0.85rem; font-weight:700; color:#0F172A; margin-bottom:10px; text-transform:uppercase; letter-spacing:0.05em;">Protocol 4-A: Acute Severe Trauma Inflow</div>
        """, unsafe_allow_html=True)
        
        st.checkbox("Emergency Physician present at bay", value=True)
        st.checkbox("Airway / Intubation tray verified", value=True)
        st.checkbox("Rapid blood infuser primed", value=True)
        st.checkbox("Chest tube insertion kit staged", value=False)
        st.checkbox("Portable X-Ray technician alerted", value=True)
        st.checkbox("CT Scanner held in clear state", value=True)
        
        st.markdown("</div>", unsafe_allow_html=True)

    st.subheader("Inflow Projection (Next 45 Mins)")
    # Plotly timeline of projected arrivals
    now = datetime.now()
    arrival_df = pd.DataFrame([
        {"Ambulance": "AMB-01 (Code Red)", "Time": now + timedelta(minutes=4), "Severity": "Critical", "Minutes": 4.1},
        {"Ambulance": "AMB-02 (Code Yellow)", "Time": now + timedelta(minutes=11), "Severity": "Urgent", "Minutes": 11.4},
        {"Ambulance": "AMB-04 (Routine Transfer)", "Time": now + timedelta(minutes=28), "Severity": "Stable", "Minutes": 28.0},
        {"Ambulance": "AMB-07 (Dialysis Transport)", "Time": now + timedelta(minutes=40), "Severity": "Routine", "Minutes": 40.0}
    ])
    
    fig = px.bar(
        arrival_df,
        x="Ambulance",
        y="Minutes",
        color="Severity",
        color_discrete_map={
            "Critical": "#EF4444",
            "Urgent": "#F59E0B",
            "Stable": "#0284C7",
            "Routine": "#64748B"
        },
        title="Projected Arrival Timeline (Minutes Remaining)",
        text="Minutes"
    )
    fig.update_layout(
        plot_bgcolor="#FFFFFF",
        paper_bgcolor="#FFFFFF",
        font=dict(family="Inter, sans-serif", color="#0F172A"),
        margin=dict(l=20, r=20, t=40, b=20),
        height=260,
        xaxis_title="",
        yaxis_title="Minutes to Arrival"
    )
    fig.update_traces(texttemplate='%{text:.1f}m', textposition='outside')
    st.plotly_chart(fig, use_container_width=True)

# Hospital Inflow Audit Log
st.subheader("Hospital Gate & Inflow Telemetry Log")
log_data = [
    {"Timestamp": (datetime.now() - timedelta(minutes=1, seconds=10)).strftime("%H:%M:%S"), "Event": "AMB-01 Telemetry Heartbeat", "Details": "Vitals stable, SpO2 91%, ETA 4.1 min", "Severity": "INFO"},
    {"Timestamp": (datetime.now() - timedelta(minutes=2, seconds=45)).strftime("%H:%M:%S"), "Event": "Signal Preemption Granted", "Details": "Junction J2 switched to Green Priority", "Severity": "SUCCESS"},
    {"Timestamp": (datetime.now() - timedelta(minutes=5, seconds=0)).strftime("%H:%M:%S"), "Event": "Trauma Bay 1 Reserved", "Details": "Assigned to AMB-01 by Dispatch Console", "Severity": "NOTICE"},
    {"Timestamp": (datetime.now() - timedelta(minutes=8, seconds=15)).strftime("%H:%M:%S"), "Event": "Dispatch Incident Initiated", "Details": "Trip TRIP-DEMO-01 created with Code Red status", "Severity": "ALERT"}
]
st.dataframe(pd.DataFrame(log_data), use_container_width=True, hide_index=True)
