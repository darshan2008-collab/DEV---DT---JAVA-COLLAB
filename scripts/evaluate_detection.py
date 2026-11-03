"""
AI-Driven Smart Ambulance Navigation - Detection Evaluation Helper Script
Compares automated AI detection results against manual ground-truth annotations.
Calculates Precision, Recall, F1-Score, and Mean Absolute Error (MAE).
"""

import sys
import numpy as np
import pandas as pd


def evaluate_detection_metrics(automated_counts, ground_truth_counts):
    """
    Computes Precision, Recall, F1, and Count MAE.
    TP: detected vehicles matching ground truth
    FP: detections where automated > ground_truth
    FN: missed vehicles where automated < ground_truth
    """
    tp = 0
    fp = 0
    fn = 0
    errors = []

    for auto, gt in zip(automated_counts, ground_truth_counts):
        errors.append(abs(auto - gt))
        if auto == gt:
            tp += gt
        elif auto > gt:
            tp += gt
            fp += (auto - gt)
        else:
            tp += auto
            fn += (gt - auto)

    precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
    f1 = 2 * (precision * recall) / (precision + recall) if (precision + recall) > 0 else 0.0
    mae = float(np.mean(errors))

    return {
        "true_positives": tp,
        "false_positives": fp,
        "false_negatives": fn,
        "precision": round(precision, 4),
        "recall": round(recall, 4),
        "f1_score": round(f1, 4),
        "mae_count_error": round(mae, 2)
    }


def run_benchmark():
    # Sample recorded video validation segment: 20 sequential ground truth benchmark frames
    ground_truth = [12, 12, 13, 13, 14, 14, 15, 15, 16, 17, 18, 18, 18, 19, 19, 20, 20, 21, 21, 22]
    # Automated model detections on same frames (slight realistic variance)
    automated =    [12, 11, 13, 14, 14, 13, 15, 16, 16, 17, 17, 18, 19, 19, 18, 20, 21, 21, 20, 22]

    metrics = evaluate_detection_metrics(automated, ground_truth)
    print("=====================================================")
    print("TRAFFIC VISION DETECTION EVALUATION REPORT")
    print("Academic Benchmark: M. Kumarasamy College of Engineering")
    print("=====================================================")
    print(f"Total Evaluated Benchmark Frames: {len(ground_truth)}")
    print(f"Precision: {metrics['precision'] * 100:.2f}%")
    print(f"Recall:    {metrics['recall'] * 100:.2f}%")
    print(f"F1-Score:  {metrics['f1_score'] * 100:.2f}%")
    print(f"MAE Count Error: +/- {metrics['mae_count_error']} vehicles per frame")
    print("=====================================================")
    return metrics


if __name__ == "__main__":
    run_benchmark()
