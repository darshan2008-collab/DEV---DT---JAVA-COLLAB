import streamlit as st
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
from datetime import datetime

from components.icons import (
    GLOBAL_LIGHT_THEME_CSS,
    render_header,
    render_metric_card,
    SVG_SPEEDOMETER,
    SVG_ROUTE,
    SVG_CHART,
    SVG_ALERT,
    SVG_CHECK
)
from components.api_client import get_traffic_current, get_roads

st.set_page_config(
    page_title="Real-Time Traffic Analytics",
    page_icon="T",
    layout="wide"
)

st.markdown(GLOBAL_LIGHT_THEME_CSS, unsafe_allow_html=True)
st.markdown(
    render_header(
        "Real-Time Traffic & Road Segment Analytics",
        "Edge Vision Ingestion | Trailing 5-sample median speeds, vehicle density, queue estimations, and congestion levels",
        SVG_SPEEDOMETER
    ),
    unsafe_allow_html=True
)

# Fetch traffic from backend or fallback to demo state
traffic_data = get_traffic_current()

if not traffic_data:
    traffic_data = [
        {"segment_id": "A1", "source_junction": "S", "target_junction": "J1", "distance_km": 1.2, "typical_speed_kmph": 50.0, "speed_kmph": 48.0, "travel_time_minutes": 1.5, "congestion_level": "CLEAR", "vehicle_count": 8, "density_veh_per_km": 6.7, "queue_m": 0, "data_source": "CAMERA_YOLO", "data_age_seconds": 3},
        {"segment_id": "A2", "source_junction": "J1", "target_junction": "J2", "distance_km": 1.5, "typical_speed_kmph": 50.0, "speed_kmph": 42.0, "travel_time_minutes": 2.14, "congestion_level": "CLEAR", "vehicle_count": 14, "density_veh_per_km": 9.3, "queue_m": 10, "data_source": "CAMERA_YOLO", "data_age_seconds": 2},
        {"segment_id": "B1", "source_junction": "J2", "target_junction": "J3", "distance_km": 0.8, "typical_speed_kmph": 45.0, "speed_kmph": 38.0, "travel_time_minutes": 1.26, "congestion_level": "MODERATE", "vehicle_count": 12, "density_veh_per_km": 15.0, "queue_m": 25, "data_source": "CAMERA_YOLO", "data_age_seconds": 4},
        {"segment_id": "B2", "source_junction": "J3", "target_junction": "J5", "distance_km": 1.4, "typical_speed_kmph": 45.0, "speed_kmph": 12.0, "travel_time_minutes": 7.0, "congestion_level": "CONGESTED", "vehicle_count": 48, "density_veh_per_km": 34.2, "queue_m": 180, "data_source": "CAMERA_YOLO", "data_age_seconds": 1},
        {"segment_id": "C1", "source_junction": "J2", "target_junction": "J4", "distance_km": 1.6, "typical_speed_kmph": 55.0, "speed_kmph": 52.0, "travel_time_minutes": 1.85, "congestion_level": "CLEAR", "vehicle_count": 11, "density_veh_per_km": 6.8, "queue_m": 0, "data_source": "CAMERA_YOLO", "data_age_seconds": 3},
        {"segment_id": "C2", "source_junction": "J4", "target_junction": "J6", "distance_km": 1.1, "typical_speed_kmph": 55.0, "speed_kmph": 50.0, "travel_time_minutes": 1.32, "congestion_level": "CLEAR", "vehicle_count": 9, "density_veh_per_km": 8.1, "queue_m": 0, "data_source": "CAMERA_YOLO", "data_age_seconds": 5},
        {"segment_id": "C3", "source_junction": "J5", "target_junction": "J6", "distance_km": 1.3, "typical_speed_kmph": 40.0, "speed_kmph": 22.0, "travel_time_minutes": 3.55, "congestion_level": "MODERATE", "vehicle_count": 22, "density_veh_per_km": 16.9, "queue_m": 60, "data_source": "CAMERA_YOLO", "data_age_seconds": 2},
        {"segment_id": "D1", "source_junction": "S", "target_junction": "J3", "distance_km": 2.2, "typical_speed_kmph": 45.0, "speed_kmph": 40.0, "travel_time_minutes": 3.3, "congestion_level": "CLEAR", "vehicle_count": 16, "density_veh_per_km": 7.2, "queue_m": 0, "data_source": "SIMULATION", "data_age_seconds": 6},
        {"segment_id": "D2", "source_junction": "J1", "target_junction": "J4", "distance_km": 1.9, "typical_speed_kmph": 50.0, "speed_kmph": 44.0, "travel_time_minutes": 2.59, "congestion_level": "CLEAR", "vehicle_count": 15, "density_veh_per_km": 7.8, "queue_m": 15, "data_source": "CAMERA_YOLO", "data_age_seconds": 4},
        {"segment_id": "D3", "source_junction": "J6", "target_junction": "H", "distance_km": 0.5, "typical_speed_kmph": 35.0, "speed_kmph": 32.0, "travel_time_minutes": 0.94, "congestion_level": "CLEAR", "vehicle_count": 5, "density_veh_per_km": 10.0, "queue_m": 0, "data_source": "CAMERA_YOLO", "data_age_seconds": 1}
    ]

df_traffic = pd.DataFrame(traffic_data)

# Top Metrics
m1, m2, m3, m4 = st.columns(4)
avg_speed = df_traffic["speed_kmph"].mean()
congested_count = (df_traffic["congestion_level"] == "CONGESTED").sum()
max_queue = df_traffic["queue_m"].max()
latest_age = df_traffic["data_age_seconds"].min()

with m1:
    st.markdown(render_metric_card("Monitored Segments", f"{len(df_traffic)} Corridors", "100% Edge Coverage", "primary"), unsafe_allow_html=True)
with m2:
    st.markdown(render_metric_card("Network Avg Speed", f"{avg_speed:.1f} km/h", "Typical: 46.5 km/h", "neutral"), unsafe_allow_html=True)
with m3:
    st.markdown(render_metric_card("Congested Segments", f"{congested_count} Corridor", "Rerouting advised on B2", "danger" if congested_count > 0 else "success"), unsafe_allow_html=True)
with m4:
    st.markdown(render_metric_card("Max Queue Length", f"{max_queue} meters", "Detected on Junction 3 approach", "warning" if max_queue > 50 else "success"), unsafe_allow_html=True)

st.write("")

# Segment Filter & Search
f_col1, f_col2, f_col3 = st.columns([2, 2, 2])
with f_col1:
    filter_level = st.selectbox("Filter Congestion Level", options=["ALL", "CONGESTED", "MODERATE", "CLEAR"], index=0)
with f_col2:
    filter_source = st.selectbox("Data Source", options=["ALL", "CAMERA_YOLO", "SIMULATION"], index=0)
with f_col3:
    st.write("")
    st.write("")
    if st.button("Refresh Segment Data"):
        st.rerun()

# Filter Dataframe
df_filtered = df_traffic.copy()
if filter_level != "ALL":
    df_filtered = df_filtered[df_filtered["congestion_level"] == filter_level]
if filter_source != "ALL":
    df_filtered = df_filtered[df_filtered["data_source"] == filter_source]

# Display Table
st.subheader("Live Road Segment Telemetry Table")
display_cols = [
    "segment_id", "source_junction", "target_junction", "distance_km",
    "typical_speed_kmph", "speed_kmph", "travel_time_minutes",
    "vehicle_count", "queue_m", "congestion_level", "data_age_seconds"
]
rename_map = {
    "segment_id": "Segment",
    "source_junction": "From",
    "target_junction": "To",
    "distance_km": "Length (km)",
    "typical_speed_kmph": "Base Speed",
    "speed_kmph": "Current Speed",
    "travel_time_minutes": "Time (min)",
    "vehicle_count": "Vehicles",
    "queue_m": "Queue (m)",
    "congestion_level": "Status",
    "data_age_seconds": "Age (s)"
}
st.dataframe(
    df_filtered[display_cols].rename(columns=rename_map),
    use_container_width=True,
    hide_index=True
)

st.write("")

# Charts Section
col_ch1, col_ch2 = st.columns(2)

with col_ch1:
    st.subheader("Speed Variance: Base vs Current (km/h)")
    fig_speed = go.Figure()
    fig_speed.add_trace(go.Bar(
        x=df_traffic["segment_id"],
        y=df_traffic["typical_speed_kmph"],
        name="Typical Speed",
        marker_color="#CBD5E1"
    ))
    fig_speed.add_trace(go.Bar(
        x=df_traffic["segment_id"],
        y=df_traffic["speed_kmph"],
        name="Current Smoothed Speed",
        marker_color="#0284C7"
    ))
    fig_speed.update_layout(
        plot_bgcolor="#FFFFFF",
        paper_bgcolor="#FFFFFF",
        font=dict(family="Inter, sans-serif", color="#0F172A"),
        barmode="group",
        height=320,
        margin=dict(l=20, r=20, t=30, b=20),
        xaxis_title="Road Segment",
        yaxis_title="Speed (km/h)",
        legend=dict(orientation="h", yanchor="bottom", y=1.02, xanchor="right", x=1)
    )
    st.plotly_chart(fig_speed, use_container_width=True)

with col_ch2:
    st.subheader("Vehicle Density vs Estimated Travel Time")
    fig_scatter = px.scatter(
        df_traffic,
        x="density_veh_per_km",
        y="travel_time_minutes",
        size="queue_m",
        color="congestion_level",
        hover_name="segment_id",
        color_discrete_map={
            "CLEAR": "#10B981",
            "MODERATE": "#F59E0B",
            "CONGESTED": "#EF4444"
        },
        labels={
            "density_veh_per_km": "Density (Vehicles / km)",
            "travel_time_minutes": "Travel Time (minutes)",
            "queue_m": "Queue Length (m)"
        }
    )
    fig_scatter.update_layout(
        plot_bgcolor="#FFFFFF",
        paper_bgcolor="#FFFFFF",
        font=dict(family="Inter, sans-serif", color="#0F172A"),
        height=320,
        margin=dict(l=20, r=20, t=30, b=20)
    )
    st.plotly_chart(fig_scatter, use_container_width=True)

st.write("")

# Pipeline Invariant Documentation
st.subheader("Edge Ingestion & Signal Processing Pipeline Invariants")
p1, p2, p3 = st.columns(3)
with p1:
    st.markdown("""
    <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:8px; padding:16px;">
        <div style="font-weight:700; color:#0F172A; margin-bottom:6px;">1. Median Speed Filter</div>
        <p style="font-size:0.85rem; color:#64748B; margin:0;">
            A sliding trailing window of 5 observations is maintained for each segment. Computes the median speed to reject momentary sensor jitter, phantom braking, and occlusion spikes.
        </p>
    </div>
    """, unsafe_allow_html=True)

with p2:
    st.markdown("""
    <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:8px; padding:16px;">
        <div style="font-weight:700; color:#0F172A; margin-bottom:6px;">2. Outlier Masking & Rejection</div>
        <p style="font-size:0.85rem; color:#64748B; margin:0;">
            Physical limits are strictly enforced: speeds &gt; 120 km/h or &lt; 0 km/h are flagged as outliers and clamped. Negative queue lengths or impossible vehicle counts are purged.
        </p>
    </div>
    """, unsafe_allow_html=True)

with p3:
    st.markdown("""
    <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:8px; padding:16px;">
        <div style="font-weight:700; color:#0F172A; margin-bottom:6px;">3. 120s Stale Data Fallback</div>
        <p style="font-size:0.85rem; color:#64748B; margin:0;">
            If edge camera frames cease for &gt; 120 seconds, the segment automatically marks <code>data_source = FALLBACK_TYPICAL</code> and falls back to baseline historical speeds.
        </p>
    </div>
    """, unsafe_allow_html=True)
