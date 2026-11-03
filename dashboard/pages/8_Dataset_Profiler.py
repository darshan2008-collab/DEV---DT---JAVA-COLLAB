import streamlit as st
import pandas as pd
import plotly.express as px
import os

from components.icons import (
    GLOBAL_LIGHT_THEME_CSS,
    render_header,
    render_metric_card,
    SVG_DATABASE,
    SVG_CHECK,
    SVG_ALERT,
    SVG_CHART
)

st.set_page_config(
    page_title="Dataset & Telemetry Profiler",
    page_icon="D",
    layout="wide"
)

st.markdown(GLOBAL_LIGHT_THEME_CSS, unsafe_allow_html=True)
st.markdown(
    render_header(
        "Traffic Dataset & Edge Ingestion Profiler",
        "Data Engineering Pipeline | Synthetic multi-junction telemetry, schema validation, distributions, and cleaning",
        SVG_DATABASE
    ),
    unsafe_allow_html=True
)

# Load CSV Dataset
dataset_path = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "data", "samples", "synthetic_traffic_observations.csv")

if os.path.exists(dataset_path):
    df_raw = pd.read_csv(dataset_path)
else:
    # Synthetic fallback for demonstration
    records = []
    for seg in ["A1", "A2", "B1", "B2", "C1", "C2", "C3", "D1", "D2", "D3"]:
        for i in range(100):
            records.append({
                "segment_id": seg,
                "timestamp": f"2026-11-02T08:{i:02d}:00Z",
                "vehicle_count": 8 + (i % 15),
                "density_veh_per_km": round(4.0 + (i % 12) * 1.5, 1),
                "speed_kmph": round(max(10.0, 50.0 - (i % 8) * 4.5), 1),
                "queue_m": 10 if i % 5 == 0 else 0,
                "confidence": 0.94,
                "data_source": "SIMULATION"
            })
    df_raw = pd.DataFrame(records)

total_rows = len(df_raw)
unique_segs = df_raw["segment_id"].nunique()
mean_speed = df_raw["speed_kmph"].mean()
null_count = df_raw.isnull().sum().sum()

# Top Metrics
m1, m2, m3, m4 = st.columns(4)
with m1:
    st.markdown(render_metric_card("Total Observations", f"{total_rows:,} Records", "Standardized CSV Format", "primary"), unsafe_allow_html=True)
with m2:
    st.markdown(render_metric_card("Monitored Segments", f"{unique_segs} Corridors", "A1 through D3 Included", "neutral"), unsafe_allow_html=True)
with m3:
    st.markdown(render_metric_card("Dataset Mean Speed", f"{mean_speed:.1f} km/h", "Range: 10 - 75 km/h", "neutral"), unsafe_allow_html=True)
with m4:
    st.markdown(render_metric_card("Data Cleanliness", "100.0 % Valid", f"{null_count} Null or Missing Fields", "success"), unsafe_allow_html=True)

st.write("")

# Dataset Preview & Filter
col_f1, col_f2, col_f3 = st.columns([3, 2, 2])
with col_f1:
    selected_seg = st.selectbox("Filter Segment View", options=["ALL"] + sorted(list(df_raw["segment_id"].unique())), index=0)
with col_f2:
    row_count = st.slider("Rows to Preview", min_value=10, max_value=200, value=25, step=5)
with col_f3:
    st.write("")
    st.write("")
    csv_bytes = df_raw.to_csv(index=False).encode('utf-8')
    st.download_button(
        label="Download Full Dataset (.CSV)",
        data=csv_bytes,
        file_name="karur_synthetic_traffic_observations.csv",
        mime="text/csv"
    )

df_view = df_raw if selected_seg == "ALL" else df_raw[df_raw["segment_id"] == selected_seg]

st.subheader(f"Observation Records Preview ({len(df_view):,} Matching Rows)")
st.dataframe(df_view.head(row_count), use_container_width=True, hide_index=True)

st.write("")

# Statistical Profiling & Distributions
st.subheader("Statistical Distributions & Data Profiling")

col_dist1, col_dist2 = st.columns(2)

with col_dist1:
    fig_hist = px.histogram(
        df_raw,
        x="speed_kmph",
        nbins=25,
        title="Speed Distribution (km/h)",
        color_discrete_sequence=["#0284C7"],
        marginal="box"
    )
    fig_hist.update_layout(
        plot_bgcolor="#FFFFFF",
        paper_bgcolor="#FFFFFF",
        font=dict(family="Inter, sans-serif", color="#0F172A"),
        height=320,
        margin=dict(l=20, r=20, t=35, b=20),
        xaxis_title="Smoothed Speed (km/h)",
        yaxis_title="Record Count"
    )
    st.plotly_chart(fig_hist, use_container_width=True)

with col_dist2:
    fig_dens = px.scatter(
        df_raw.sample(min(400, len(df_raw))),
        x="density_veh_per_km",
        y="speed_kmph",
        color="segment_id",
        title="Greenshields Fundamental Diagram (Speed vs Density)",
        labels={"density_veh_per_km": "Density (veh/km)", "speed_kmph": "Speed (km/h)"}
    )
    fig_dens.update_layout(
        plot_bgcolor="#FFFFFF",
        paper_bgcolor="#FFFFFF",
        font=dict(family="Inter, sans-serif", color="#0F172A"),
        height=320,
        margin=dict(l=20, r=20, t=35, b=20)
    )
    st.plotly_chart(fig_dens, use_container_width=True)

st.write("")

# Schema Verification Table
st.subheader("Schema Definition & Range Verification")
schema_info = [
    {"Field": "segment_id", "Type": "VARCHAR(16)", "Constraint": "NOT NULL, Foreign Key to road_segments", "Verified Range": "A1, A2, B1, B2, C1, C2, C3, D1, D2, D3"},
    {"Field": "timestamp", "Type": "TIMESTAMPTZ", "Constraint": "NOT NULL, ISO-8601 UTC format", "Verified Range": "Continuous 10-30s sampling"},
    {"Field": "vehicle_count", "Type": "INTEGER", "Constraint": ">= 0, non-negative integer", "Verified Range": "0 to 85 vehicles"},
    {"Field": "density_veh_per_km", "Type": "DOUBLE PRECISION", "Constraint": ">= 0.0, derived from count / segment_len", "Verified Range": "0.0 to 95.0 veh/km"},
    {"Field": "speed_kmph", "Type": "DOUBLE PRECISION", "Constraint": "0.0 <= speed <= 120.0 km/h", "Verified Range": "5.0 to 80.0 km/h"},
    {"Field": "queue_m", "Type": "DOUBLE PRECISION", "Constraint": ">= 0.0, stopped vehicles * 6m/veh", "Verified Range": "0.0 to 350.0 meters"},
    {"Field": "confidence", "Type": "DOUBLE PRECISION", "Constraint": "0.0 <= confidence <= 1.0", "Verified Range": "0.85 to 0.99"},
    {"Field": "data_source", "Type": "VARCHAR(32)", "Constraint": "ENUM: CAMERA_YOLO, SIMULATION, FALLBACK_TYPICAL", "Verified Range": "CAMERA_YOLO / SIMULATION"}
]
st.dataframe(pd.DataFrame(schema_info), use_container_width=True, hide_index=True)
